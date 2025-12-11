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

    // Opening book
    private OpeningBook openingBook;
    private boolean lastMoveWasFromBook = false;

    // Game control
    private int incrementMs = 0;
    private String name = "BestMoveCalculator";


    public BestMoveCalculator() {
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
            long budgetMs = computeTimeBudget(yourTime, incrementMs);
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

    private static long computeTimeBudget(int remainingMs, int incMs) {
        long budget = remainingMs / 20L + 3L * incMs;  // Changed from /40 to /20 for more time
        return Math.max(50L, Math.min(budget, Math.max(100L, remainingMs / 2L)));
    }


    public void setupGameInfo(GameInfo info) {
        this.incrementMs = info.getIncrementMs();
    }

    public void clearSearchHelpers() {
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
            
            int pieceValue = getPieceValue(piece);
            
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
     * Get the material value of a piece in centipawns.
     */
    private int getPieceValue(Piece piece) {
        switch (piece.getPieceType()) {
            case PAWN: return PAWN_VALUE;
            case KNIGHT: return KNIGHT_VALUE;
            case BISHOP: return BISHOP_VALUE;
            case ROOK: return ROOK_VALUE;
            case QUEEN: return QUEEN_VALUE;
            case KING: return KING_VALUE;
            default: return 0;
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
            score += getPieceValue(capturedPiece) * 10;
            Piece attacker = board.getPiece(move.getFrom());
            if (attacker != Piece.NONE) {
                score -= getPieceValue(attacker);
            }
        }
        
        // Promotion bonus
        if (move.getPromotion() != Piece.NONE) {
            score += getPieceValue(move.getPromotion()) * 10;
        }
        
        return score;
    }

    /**
     * Alpha-beta search with a fixed depth.
     */
    private int alphaBeta(Board board, int depth, int alpha, int beta, int ply) {
        // Check if position is terminal (checkmate, stalemate)
        List<Move> legalMoves = board.legalMoves();
        if (legalMoves.isEmpty()) {
            if (board.isKingAttacked()) {
                // Checkmate - return negative score (we're checkmated)
                return -CHECKMATE_SCORE + ply;
            } else {
                // Stalemate
                return DRAW_SCORE;
            }
        }
        
        // Base case: use quiescence search at leaf nodes
        if (depth <= 0) {
            return quiescence(board, alpha, beta, ply);
        }
        
        int bestScore = -CHECKMATE_SCORE - 1000;
        
        for (Move move : legalMoves) {
            board.doMove(move);
            int score = -alphaBeta(board, depth - 1, -beta, -alpha, ply + 1);
            board.undoMove();
            
            bestScore = Math.max(bestScore, score);
            alpha = Math.max(alpha, score);
            
            if (alpha >= beta) {
                break; // Beta cutoff
            }
        }
        
        return bestScore;
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
}
