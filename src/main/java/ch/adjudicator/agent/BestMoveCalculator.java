package ch.adjudicator.agent;

import ch.adjudicator.client.GameInfo;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class BestMoveCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BestMoveCalculator.class);

    // Material values in centipawns
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 300;
    private static final int BISHOP_VALUE = 300;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    private static final int KING_VALUE = 20000;
    
    private static final int CHECKMATE_SCORE = 30000;
    private static final int DRAW_SCORE = 0;

    // Transposition Table constants
    private static final int TT_SIZE = 1 << 20; // 1M entries (~64MB with 64 bytes per entry)
    private static final int TT_MASK = TT_SIZE - 1;
    
    // TT node types
    private static final byte TT_EXACT = 0;
    private static final byte TT_ALPHA = 1;
    private static final byte TT_BETA = 2;

    // Opening book
    private OpeningBook openingBook;
    private boolean lastMoveWasFromBook = false;

    // Transposition Table
    private TranspositionTableEntry[] transpositionTable;
    
    // Position history for repetition detection
    private List<Long> positionHistory;
    
    // Zobrist hashing
    private ZobristHash zobristHash;

    // Game control
    private int incrementMs = 0;
    private String name = "BestMoveCalculator";


    public BestMoveCalculator() {
        // Initialize Transposition Table
        transpositionTable = new TranspositionTableEntry[TT_SIZE];
        for (int i = 0; i < TT_SIZE; i++) {
            transpositionTable[i] = new TranspositionTableEntry();
        }
        
        // Initialize Zobrist hashing
        zobristHash = new ZobristHash();
        
        // Initialize position history for repetition detection
        positionHistory = new ArrayList<>();
        
        // Try to load opening book from docs directory
        try {
            Path bookPath = Paths.get("docs", "Perfect_2021", "BIN", "Perfect2021.bin");
            if (bookPath.toFile().exists()) {
                openingBook = new OpeningBook(bookPath);
                LOGGER.info("Opening book loaded successfully from {}", bookPath);
            } else {
                LOGGER.warn("Opening book not found at {}, will play without book", bookPath);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load opening book, will play without book", e);
        }
    }

    public OpeningBook getOpeningBook() {
        return openingBook;
    }

    public boolean wasLastMoveFromBook() {
        return lastMoveWasFromBook;
    }

    public Move computeBestMove(Board searchBoard, int yourTimeMs) throws Exception{
        try {
            // Check opening book first
            lastMoveWasFromBook = false;
            if (openingBook != null) {
                Move bookMove = openingBook.getBookMove(searchBoard);
                if (bookMove != null) {
                    lastMoveWasFromBook = true;
                    LOGGER.info("[{}] Using opening book move: {}", name, moveToLAN(bookMove));
                    return bookMove;
                }
                LOGGER.info("[{}] Position not in opening book, starting search", name);
            }
            
            // Compute time budget
            int yourTime = Math.max(0, yourTimeMs);
            long budgetMs = computeTimeBudget(searchBoard, yourTime, incrementMs);
            if (yourTime < 5000) {
                // Low time aggression: cap to 1-2 shallow iterations
                budgetMs = Math.min(budgetMs, 150L);
            }
            long startTime = System.currentTimeMillis();

            LOGGER.info("[{}] Starting search with budget {}ms", name, budgetMs);

            // Iterative deepening
            List<Move> rootMoves = searchBoard.legalMoves();
            if (rootMoves.isEmpty()) {
                throw new Exception("No legal moves available");
            }

            LOGGER.debug("[{}] Legal moves: {}", name, rootMoves.size());

            // Use alpha-beta search with iterative deepening
            Move bestMove = findBestMove(searchBoard, budgetMs);
            if (bestMove == null) {
                throw new Exception("No legal moves available");
            }
            
            LOGGER.info("[{}] Selected move: {}", name, moveToLAN(bestMove));
            return bestMove;

        } catch (Throwable e) {
            LOGGER.error("[{}] CRITICAL ERROR in computeBestMove", name, e);
            // Try to return a random legal move as last resort
            List<Move> emergency = searchBoard.legalMoves();
            if (!emergency.isEmpty()) {
                Move fallback = emergency.get(0);
                LOGGER.warn("[{}] Emergency fallback move: {}", name, moveToLAN(fallback));
                return fallback;
            }
            LOGGER.error("[{}] No legal moves available for fallback", name);
            throw e;
        }
    }

    /**
     * Dynamic time allocation based on remaining time, increment, and game phase.
     * Allocates more time in critical middlegame positions and less in opening/endgame.
     */
    private long computeTimeBudget(Board board, int remainingMs, int incMs) {
        // Estimate moves remaining until time control (assume 40 moves or end of game)
        // Note: board.getBackup() tracks moves made during THIS session, not total game moves.
        // For positions loaded from FEN (tests), this will be 0, so we use a conservative default.
        int moveNumber = board.getBackup().size() / 2; // Approximate move number in this session
        
        // If no moves in backup (FEN position or fresh start), assume we're at move 20 (mid-game)
        // This gives reasonable time allocation for test positions
        if (moveNumber == 0) {
            moveNumber = 20;
        }
        
        int estimatedMovesRemaining = Math.max(10, 40 - moveNumber);
        
        // Detect game phase based on material count
        int totalPieceCount = countPieces(board);
        GamePhase phase = detectGamePhase(totalPieceCount);
        
        // Base allocation: divide remaining time by estimated moves, plus increment
        long baseAllocation = remainingMs / Math.max(1, estimatedMovesRemaining) + incMs;
        
        // Apply phase-dependent multiplier
        double phaseMultiplier = switch (phase) {
            case OPENING -> 0.5;      // Use less time in opening (rely on book/theory)
            case MIDDLEGAME -> 1.5;   // Use more time in complex middlegame
            case ENDGAME -> 1.0;      // Normal time in endgame
        };
        
        long budget = (long) (baseAllocation * phaseMultiplier);
        
        // Safety bounds: never use less than 50ms, never more than 40% of remaining time
        long minBudget = 50L;
        long maxBudget = Math.max(100L, remainingMs * 2L / 5L); // 40% max
        
        // Extra conservatism in time trouble (< 10 seconds)
        if (remainingMs < 10000) {
            maxBudget = Math.min(maxBudget, remainingMs / 5L); // Only use 20% in time trouble
        }
        
        return Math.max(minBudget, Math.min(budget, maxBudget));
    }
    
    /**
     * Count total number of pieces on the board (excluding kings).
     */
    private int countPieces(Board board) {
        int count = 0;
        for (Square square : Square.values()) {
            if (square == Square.NONE) continue;
            Piece piece = board.getPiece(square);
            if (piece != Piece.NONE && piece.getPieceType() != com.github.bhlangonijr.chesslib.PieceType.KING) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Detect game phase based on piece count.
     */
    private GamePhase detectGamePhase(int pieceCount) {
        if (pieceCount >= 24) {
            return GamePhase.OPENING;     // Most pieces still on board
        } else if (pieceCount >= 12) {
            return GamePhase.MIDDLEGAME;  // Some pieces traded
        } else {
            return GamePhase.ENDGAME;     // Few pieces remaining
        }
    }
    
    /**
     * Game phase enumeration.
     */
    private enum GamePhase {
        OPENING,
        MIDDLEGAME,
        ENDGAME
    }


    public void setupGameInfo(GameInfo info) {
        this.incrementMs = info.getIncrementMs();
    }

    public void clearSearchHelpers() {
        // Clear position history for new game
        positionHistory.clear();
        
        // Clear transposition table to avoid contamination between searches
        for (int i = 0; i < TT_SIZE; i++) {
            transpositionTable[i] = new TranspositionTableEntry();
        }
    }

    private String moveToLAN(Move move) {
        return move.toString().toLowerCase();
    }

    /**
     * Evaluate the board position from the perspective of the side to move.
     * Positive scores favor the side to move, negative scores favor the opponent.
     */
    private int evaluate(Board board) {
        int score = 0;
        Side sideToMove = board.getSideToMove();
        
        // Count material for both sides
        for (Square square : Square.values()) {
            if (square == Square.NONE) continue;
            
            Piece piece = board.getPiece(square);
            if (piece == Piece.NONE) continue;
            
            int pieceValue = getPieceValue(piece, square);
            
            // Add value if piece belongs to side to move, subtract otherwise
            if (piece.getPieceSide() == sideToMove) {
                score += pieceValue;
            } else {
                score -= pieceValue;
            }
        }
        
        return score;
    }

    /**
     * Get the material value of a piece in centipawns, including positional bonuses.
     * Positional bonuses are based on piece-square tables from chess theory.
     * 
     * @param piece The piece to evaluate
     * @param square The square where the piece is located
     * @return The value of the piece including positional adjustment
     */
    private int getPieceValue(Piece piece, Square square) {
        int baseValue;
        switch (piece.getPieceType()) {
            case PAWN: baseValue = PAWN_VALUE; break;
            case KNIGHT: baseValue = KNIGHT_VALUE; break;
            case BISHOP: baseValue = BISHOP_VALUE; break;
            case ROOK: baseValue = ROOK_VALUE; break;
            case QUEEN: baseValue = QUEEN_VALUE; break;
            case KING: baseValue = KING_VALUE; break;
            default: return 0;
        }
        
        // Add positional bonus based on piece type and square
        int positionalBonus = getPositionalBonus(piece, square);
        return baseValue + positionalBonus;
    }
    
    /**
     * Get positional bonus for a piece on a given square.
     * Based on standard chess piece-square tables.
     * Positive values indicate better squares for the piece.
     * 
     * @param piece The piece
     * @param square The square
     * @return Positional bonus in centipawns
     */
    private int getPositionalBonus(Piece piece, Square square) {
        if (square == Square.NONE) return 0;
        
        int rank = square.getRank().ordinal(); // 0 = Rank 1, 7 = Rank 8
        int file = square.getFile().ordinal(); // 0 = File A, 7 = File H
        
        // Adjust rank based on piece color (White prefers advancing, Black prefers back ranks)
        Side side = piece.getPieceSide();
        int adjustedRank = (side == Side.WHITE) ? rank : (7 - rank);
        
        switch (piece.getPieceType()) {
            case PAWN:
                // Pawns gain value as they advance toward promotion
                // Central pawns are slightly more valuable
                int pawnBonus = adjustedRank * 10; // 0 to 70 centipawns
                // Center files (d,e = files 3,4) get small bonus
                if (file >= 2 && file <= 5) {
                    pawnBonus += 5;
                }
                return pawnBonus;
                
            case KNIGHT:
                // Knights prefer central squares and lose value on edges
                // Better in middlegame, so central control is key
                int knightBonus = 0;
                // Penalize edge files
                if (file == 0 || file == 7) knightBonus -= 10;
                // Penalize back rank
                if (adjustedRank == 0) knightBonus -= 10;
                // Bonus for central squares (files c-f, ranks 3-6)
                if (file >= 2 && file <= 5 && adjustedRank >= 2 && adjustedRank <= 5) {
                    knightBonus += 10;
                }
                return knightBonus;
                
            case BISHOP:
                // Bishops prefer central activity and open diagonals
                int bishopBonus = 0;
                // Slight bonus for central squares
                if (file >= 2 && file <= 5 && adjustedRank >= 2 && adjustedRank <= 5) {
                    bishopBonus += 5;
                }
                return bishopBonus;
                
            case ROOK:
                // Rooks prefer 7th rank (attacking pawns) and open files
                int rookBonus = 0;
                if (adjustedRank == 6) { // 7th rank from piece's perspective
                    rookBonus += 10;
                }
                return rookBonus;
                
            case QUEEN:
                // Queen doesn't have strong positional preferences
                // Slight penalty for early development
                if (adjustedRank == 0) return -5;
                return 0;
                
            case KING:
                // King safety: prefer back rank in opening/middlegame
                // This is simplified; real engines consider pawn shield, etc.
                int kingBonus = 0;
                if (adjustedRank <= 1) { // Back two ranks
                    kingBonus += 10;
                }
                // Prefer corners/edges for safety (castled position)
                if (file <= 2 || file >= 5) {
                    kingBonus += 5;
                }
                return kingBonus;
                
            default:
                return 0;
        }
    }

    /**
     * Quiescence search to handle tactical sequences (captures).
     */
    private int quiescence(Board board, int alpha, int beta, int ply) {
        // Stand-pat score
        int standPat = evaluate(board);
        
        if (standPat >= beta) {
            return beta;
        }
        if (alpha < standPat) {
            alpha = standPat;
        }
        
        // Generate and search only tactical moves (captures and promotions)
        List<Move> legalMoves = board.legalMoves();
        List<Move> tacticalMoves = new ArrayList<>();
        
        for (Move move : legalMoves) {
            // Check if it's a capture or promotion
            Piece capturedPiece = board.getPiece(move.getTo());
            boolean isCapture = capturedPiece != Piece.NONE;
            boolean isPromotion = move.getPromotion() != Piece.NONE;
            
            if (isCapture || isPromotion) {
                tacticalMoves.add(move);
            }
        }
        
        // Sort tactical moves by MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
        tacticalMoves.sort((m1, m2) -> {
            int score1 = getMoveScore(board, m1);
            int score2 = getMoveScore(board, m2);
            return Integer.compare(score2, score1);
        });
        
        for (Move move : tacticalMoves) {
            board.doMove(move);
            int score = -quiescence(board, -beta, -alpha, ply + 1);
            board.undoMove();
            
            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }
        
        return alpha;
    }
    
    /**
     * Get a heuristic score for move ordering (MVV-LVA).
     */
    private int getMoveScore(Board board, Move move) {
        int score = 0;
        
        // Capture value: victim value - attacker value / 10
        Piece capturedPiece = board.getPiece(move.getTo());
        if (capturedPiece != Piece.NONE) {
            score += getPieceValue(capturedPiece, move.getTo()) * 10;
            Piece attacker = board.getPiece(move.getFrom());
            if (attacker != Piece.NONE) {
                score -= getPieceValue(attacker, move.getFrom());
            }
        }
        
        // Promotion bonus
        if (move.getPromotion() != Piece.NONE) {
            score += getPieceValue(move.getPromotion(), move.getTo()) * 10;
        }
        
        return score;
    }

    /**
     * Alpha-beta search with a fixed depth.
     */
    private int alphaBeta(Board board, int depth, int alpha, int beta, int ply) {
        int alphaOrig = alpha;
        
        // Compute position hash
        long positionHash = zobristHash.computeHash(board);
        
        // Check for repetition (3-fold repetition is a draw)
        if (isRepetition(positionHash)) {
            return DRAW_SCORE;
        }
        
        // Transposition table lookup
        int ttIndex = (int)(positionHash & TT_MASK);
        TranspositionTableEntry ttEntry = transpositionTable[ttIndex];
        
        if (ttEntry.isValid(positionHash, depth)) {
            if (ttEntry.nodeType == TT_EXACT) {
                return ttEntry.score;
            } else if (ttEntry.nodeType == TT_ALPHA && ttEntry.score <= alpha) {
                return alpha;
            } else if (ttEntry.nodeType == TT_BETA && ttEntry.score >= beta) {
                return beta;
            }
        }
        
        // Check if position is terminal (checkmate, stalemate)
        List<Move> legalMoves = board.legalMoves();
        if (legalMoves.isEmpty()) {
            if (board.isKingAttacked()) {
                // Checkmate - return negative score (we're checkmated)
                return -CHECKMATE_SCORE - ply;
            } else {
                // Stalemate
                return DRAW_SCORE;
            }
        }
        
        // Base case: use quiescence search at leaf nodes
        if (depth <= 0) {
            return quiescence(board, alpha, beta, ply);
        }
        
        // Try TT move first if available
        Move ttMove = ttEntry.isValid(positionHash, depth) ? ttEntry.bestMove : null;
        if (ttMove != null && legalMoves.contains(ttMove)) {
            legalMoves.remove(ttMove);
            legalMoves.add(0, ttMove);
        }
        
        int bestScore = -CHECKMATE_SCORE - 1000;
        Move bestMove = null;
        
        for (Move move : legalMoves) {
            board.doMove(move);
            long newPositionHash = zobristHash.computeHash(board);
            positionHistory.add(newPositionHash);
            
            int score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1);
            
            positionHistory.remove(positionHistory.size() - 1);
            board.undoMove();
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            
            alpha = Math.max(alpha, score);
            
            if (alpha >= beta) {
                break; // Beta cutoff
            }
        }
        
        // Store in transposition table
        byte nodeType;
        if (bestScore <= alphaOrig) {
            nodeType = TT_ALPHA;
        } else if (bestScore >= beta) {
            nodeType = TT_BETA;
        } else {
            nodeType = TT_EXACT;
        }
        ttEntry.store(positionHash, depth, bestScore, bestMove, nodeType);
        
        return bestScore;
    }
    
    /**
     * Check if current position is a repetition (2-fold or more in search tree)
     */
    private boolean isRepetition(long positionHash) {
        int count = 0;
        for (int i = positionHistory.size() - 1; i >= 0; i--) {
            if (positionHistory.get(i) == positionHash) {
                count++;
                if (count >= 2) {
                    return true; // 3-fold repetition (current + 2 in history)
                }
            }
        }
        return false;
    }

    /**
     * Find the best move using iterative deepening alpha-beta search.
     */
    private Move findBestMove(Board board, long budgetMs) {
        List<Move> legalMoves = board.legalMoves();
        if (legalMoves.isEmpty()) {
            return null;
        }
        
        // Sort moves by a simple heuristic to improve move ordering
        legalMoves.sort((m1, m2) -> {
            int score1 = getMoveScore(board, m1);
            int score2 = getMoveScore(board, m2);
            return Integer.compare(score2, score1);
        });
        
        Move bestMove = legalMoves.get(0);
        long startTime = System.currentTimeMillis();
        
        // Iterative deepening
        for (int depth = 1; depth <= 20; depth++) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= budgetMs) {
                break;
            }
            
            int bestScore = -CHECKMATE_SCORE - 1000;
            Move currentBestMove = bestMove;
            int alpha = -CHECKMATE_SCORE - 1000;
            int beta = CHECKMATE_SCORE + 1000;
            
            for (Move move : legalMoves) {
                board.doMove(move);
                int score = -alphaBeta(board, depth - 1, -beta, -alpha, 1);
                board.undoMove();
                
                if (score > bestScore) {
                    bestScore = score;
                    currentBestMove = move;
                }
                
                alpha = Math.max(alpha, score);
                
                // Check time
                elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= budgetMs) {
                    break;
                }
            }
            
            bestMove = currentBestMove;
            LOGGER.debug("[{}] Depth {} completed, best move: {}, score: {}", 
                        name, depth, moveToLAN(bestMove), bestScore);
            
            // Stop if time is up
            elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= budgetMs) {
                break;
            }
        }
        
        return bestMove;
    }
    
    /**
     * Transposition Table Entry structure
     */
    private static class TranspositionTableEntry {
        long zobristKey;
        int depth;
        int score;
        Move bestMove;
        byte nodeType; // TT_EXACT, TT_ALPHA, or TT_BETA
        
        TranspositionTableEntry() {
            this.zobristKey = 0L;
            this.depth = -1;
            this.score = 0;
            this.bestMove = null;
            this.nodeType = TT_ALPHA;
        }
        
        void store(long key, int depth, int score, Move move, byte type) {
            this.zobristKey = key;
            this.depth = depth;
            this.score = score;
            this.bestMove = move;
            this.nodeType = type;
        }
        
        boolean isValid(long key, int depth) {
            return this.zobristKey == key && this.depth >= depth;
        }
    }
    
    /**
     * Zobrist Hashing for position identification
     */
    private static class ZobristHash {
        private long[][][] pieceKeys; // [piece_type][color][square]
        private long[] castlingKeys; // 4 castling rights
        private long[] enPassantKeys; // 8 files
        private long sideToMoveKey;
        private Random random;
        
        ZobristHash() {
            random = new Random(123456789L); // Fixed seed for reproducibility
            pieceKeys = new long[6][2][64];
            castlingKeys = new long[4];
            enPassantKeys = new long[8];
            
            // Initialize piece keys
            for (int piece = 0; piece < 6; piece++) {
                for (int color = 0; color < 2; color++) {
                    for (int square = 0; square < 64; square++) {
                        pieceKeys[piece][color][square] = random.nextLong();
                    }
                }
            }
            
            // Initialize castling keys
            for (int i = 0; i < 4; i++) {
                castlingKeys[i] = random.nextLong();
            }
            
            // Initialize en passant keys
            for (int i = 0; i < 8; i++) {
                enPassantKeys[i] = random.nextLong();
            }
            
            sideToMoveKey = random.nextLong();
        }
        
        long computeHash(Board board) {
            long hash = 0L;
            
            // Hash pieces
            for (Square square : Square.values()) {
                if (square == Square.NONE) continue;
                
                Piece piece = board.getPiece(square);
                if (piece == Piece.NONE) continue;
                
                int pieceType = getPieceTypeIndex(piece);
                int color = piece.getPieceSide() == Side.WHITE ? 0 : 1;
                int squareIndex = square.ordinal();
                
                hash ^= pieceKeys[pieceType][color][squareIndex];
            }
            
            // Hash castling rights
            String castleStr = board.getCastleRight(Side.WHITE).toString() +
                              board.getCastleRight(Side.BLACK).toString();
            if (castleStr.contains("K")) {
                hash ^= castlingKeys[0];
            }
            if (castleStr.contains("Q")) {
                hash ^= castlingKeys[1];
            }
            if (castleStr.contains("k")) {
                hash ^= castlingKeys[2];
            }
            if (castleStr.contains("q")) {
                hash ^= castlingKeys[3];
            }
            
            // Hash en passant
            if (board.getEnPassant() != Square.NONE) {
                int file = board.getEnPassant().getFile().ordinal();
                hash ^= enPassantKeys[file];
            }
            
            // Hash side to move
            if (board.getSideToMove() == Side.BLACK) {
                hash ^= sideToMoveKey;
            }
            
            return hash;
        }
        
        private int getPieceTypeIndex(Piece piece) {
            switch (piece.getPieceType()) {
                case PAWN: return 0;
                case KNIGHT: return 1;
                case BISHOP: return 2;
                case ROOK: return 3;
                case QUEEN: return 4;
                case KING: return 5;
                default: return 0;
            }
        }
    }
}
