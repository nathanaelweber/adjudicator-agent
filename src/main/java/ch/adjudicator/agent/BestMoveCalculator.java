package ch.adjudicator.agent;

import ch.adjudicator.client.Color;
import ch.adjudicator.client.GameInfo;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BestMoveCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BestMoveCalculator.class);
    
    // Opening book
    private OpeningBook openingBook;
    private boolean lastMoveWasFromBook = false;

    // Material values (centipawns)
    private static final int PAWN = 100;
    private static final int KNIGHT = 320;
    private static final int BISHOP = 330;
    private static final int ROOK = 500;
    private static final int QUEEN = 900;

    // Search limits and controls
    private static final int CHECKMATE_SCORE = 100_000; // large score for mates
    private static final int TABLE_SIZE_MB = 128; // TT size hint (not strictly enforced with HashMap)
    private static final int TIME_CHECK_EVERY_NODES = 10_000;
    private static final int MAX_DEPTH = 64; // theoretical upper bound
    private static final int MAX_QUIESCENCE_DEPTH = 20; // limit quiescence search depth to prevent stack overflow


    // Game control
    private int incrementMs = 0;
    private int initialTimeMs = 0;
    private Side myColor = Side.WHITE;
    private String name = "BestMoveCalculator";

    // Time management per move
    private long moveSearchDeadline = Long.MAX_VALUE;
    private volatile boolean timeUp = false;

    // Search state
    private long nodes;

    // Killer moves: store two killer moves per depth
    private final Move[][] killerMoves = new Move[128][2];

    // History heuristic: simple map key -> score
    private final Map<Integer, Integer> historyHeuristic = new HashMap<>();

    // Transposition Table
    private final Map<Long, TTEntry> tt = new HashMap<>(TABLE_SIZE_MB * 1024);

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
            moveSearchDeadline = startTime + budgetMs;
            timeUp = false;
            nodes = 0;

            LOGGER.info("[{}] Starting search with budget {}ms", name, budgetMs);

            // Iterative deepening
            Move bestMove = null;
            int bestScore = -CHECKMATE_SCORE;
            List<Move> rootMoves = searchBoard.legalMoves();
            if (rootMoves.isEmpty()) {
                throw new Exception("No legal moves available");
            }

            // Order root moves crudely by MVV/LVA for first iteration
            orderMoves(rootMoves, null, 0, searchBoard);

            for (int depth = 1; depth <= MAX_DEPTH; depth++) {
                if (timeExceeded()) break;

                int alpha = -CHECKMATE_SCORE;
                int beta = CHECKMATE_SCORE;

                Move localBest = bestMove; // PV from previous depth
                if (localBest != null && rootMoves.remove(localBest)) {
                    rootMoves.add(0, localBest);
                }

                int iterationBestScore = -CHECKMATE_SCORE;
                Move iterationBestMove = localBest;

                for (int i = 0; i < rootMoves.size(); i++) {
                    Move move = rootMoves.get(i);
                    if (timeExceeded()) break;

                    searchBoard.doMove(move);
                    int score = -alphaBeta(depth - 1, -beta, -alpha, false, 1, searchBoard);
                    searchBoard.undoMove();

                    if (score > iterationBestScore) {
                        iterationBestScore = score;
                        iterationBestMove = move;
                    }
                    if (score > alpha) {
                        alpha = score;
                    }
                }

                if (!timeUp && iterationBestMove != null) {
                    bestMove = iterationBestMove;
                    bestScore = iterationBestScore;
                    // Move ordering for next iteration: put PV first
                    rootMoves.remove(bestMove);
                    rootMoves.add(0, bestMove);
                    LOGGER.info("[{}] Depth {} completed. Best {} score {}. Nodes {}. Elapsed {}ms",
                            name, depth, moveToLAN(bestMove), bestScore, nodes,
                            (System.currentTimeMillis() - startTime));
                } else {
                    break; // ran out of time during this iteration
                }

                // Optional safety stop if win clearly secured and time is low
                if (System.currentTimeMillis() - startTime > budgetMs * 0.8 && depth >= 3) {
                    break;
                }
            }

            if (bestMove == null) {
                // Fallback: pick a legal move
                bestMove = rootMoves.get(0);
            }
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


    // =============== TT ==================

    private enum TTFlag { EXACT, LOWERBOUND, UPPERBOUND }

    private static class TTEntry {
        final int depth;
        final int score;
        final TTFlag flag;
        final Move bestMove;
        TTEntry(int depth, int score, TTFlag flag, Move bestMove) {
            this.depth = depth; this.score = score; this.flag = flag; this.bestMove = bestMove;
        }
    }

    // =============== PST ==================
    private static class PST {
        // These PSTs are basic and from White's POV, indices 0..63 (rank 1 -> 8, files a->h)
        // Tuned lightly; can be improved.
        static final int[] PAWN_MG = {
                0, 0, 0, 0, 0, 0, 0, 0,
                50, 50, 50, 50, 50, 50, 50, 50,
                10, 10, 20, 30, 30, 20, 10, 10,
                5, 5, 10, 25, 25, 10, 5, 5,
                0, 0, 0, 20, 20, 0, 0, 0,
                5, -5, -10, 0, 0, -10, -5, 5,
                5, 10, 10, -20, -20, 10, 10, 5,
                0, 0, 0, 0, 0, 0, 0, 0
        };
        static final int[] KNIGHT_MG = {
                -50, -40, -30, -30, -30, -30, -40, -50,
                -40, -20, 0, 5, 5, 0, -20, -40,
                -30, 5, 10, 15, 15, 10, 5, -30,
                -30, 0, 15, 20, 20, 15, 0, -30,
                -30, 5, 15, 20, 20, 15, 5, -30,
                -30, 0, 10, 15, 15, 10, 0, -30,
                -40, -20, 0, 0, 0, 0, -20, -40,
                -50, -40, -30, -30, -30, -30, -40, -50
        };
        static final int[] BISHOP_MG = {
                -20, -10, -10, -10, -10, -10, -10, -20,
                -10, 5, 0, 0, 0, 0, 5, -10,
                -10, 10, 10, 10, 10, 10, 10, -10,
                -10, 0, 10, 10, 10, 10, 0, -10,
                -10, 5, 5, 10, 10, 5, 5, -10,
                -10, 0, 5, 10, 10, 5, 0, -10,
                -10, 0, 0, 0, 0, 0, 0, -10,
                -20, -10, -10, -10, -10, -10, -10, -20
        };
        static final int[] ROOK_MG = {
                0, 0, 0, 5, 5, 0, 0, 0,
                -5, 0, 0, 0, 0, 0, 0, -5,
                -5, 0, 0, 0, 0, 0, 0, -5,
                -5, 0, 0, 0, 0, 0, 0, -5,
                -5, 0, 0, 0, 0, 0, 0, -5,
                -5, 0, 0, 0, 0, 0, 0, -5,
                5, 10, 10, 10, 10, 10, 10, 5,
                0, 0, 0, 0, 0, 0, 0, 0
        };
        static final int[] QUEEN_MG = {
                -20, -10, -10, -5, -5, -10, -10, -20,
                -10, 0, 5, 0, 0, 0, 0, -10,
                -10, 5, 5, 5, 5, 5, 0, -10,
                0, 0, 5, 5, 5, 5, 0, -5,
                -5, 0, 5, 5, 5, 5, 0, -5,
                -10, 0, 5, 5, 5, 5, 0, -10,
                -10, 0, 0, 0, 0, 0, 0, -10,
                -20, -10, -10, -5, -5, -10, -10, -20
        };
        static final int[] KING_MG = {
                -30, -40, -40, -50, -50, -40, -40, -30,
                -30, -40, -40, -50, -50, -40, -40, -30,
                -30, -40, -40, -50, -50, -40, -40, -30,
                -30, -40, -40, -50, -50, -40, -40, -30,
                -20, -30, -30, -40, -40, -30, -30, -20,
                -10, -20, -20, -20, -20, -20, -20, -10,
                20, 20, 0, 0, 0, 0, 20, 20,
                20, 30, 10, 0, 0, 10, 30, 20
        };
        static final int[] KING_EG = {
                -50, -40, -30, -20, -20, -30, -40, -50,
                -30, -20, -10, 0, 0, -10, -20, -30,
                -30, -10, 20, 30, 30, 20, -10, -30,
                -30, -10, 30, 40, 40, 30, -10, -30,
                -30, -10, 30, 40, 40, 30, -10, -30,
                -30, -10, 20, 30, 30, 20, -10, -30,
                -30, -30, 0, 0, 0, 0, -30, -30,
                -50, -40, -30, -20, -20, -30, -40, -50
        };

        static int flip(int[] arr, int idx) {
            // flip vertically (mirror for Black POV)
            int r = idx / 8, f = idx % 8;
            int fr = (7 - r) * 8 + f;
            return arr[fr];
        }
    }

    private static Map<String, String[]> createMiniBook() {
        Map<String, String[]> m = new HashMap<>();
        try {
            Board b = new Board();
            m.put(b.getFen(), new String[]{"e2e4", "d2d4", "g1f3", "c2c4"});
            // After 1.e4
            b.doMove(new Move("E2E4", Side.WHITE));
            m.put(b.getFen(), new String[]{"e7e5", "c7c5", "e7e6"});
            // After 1.d4
            b = new Board();
            b.doMove(new Move("D2D4", Side.WHITE));
            m.put(b.getFen(), new String[]{"d7d5", "g8f6", "e7e6"});
        } catch (Exception e) {
            // If book creation fails, return empty map - agent will work without book
            LOGGER.warn("Failed to create opening book", e);
        }
        return m;
    }


    // =============== SEARCH ==================

    private int alphaBeta(int depth, int alpha, int beta, boolean maximizing, int ply, Board searchBoard) {
        if ((nodes++ % TIME_CHECK_EVERY_NODES) == 0 && timeExceeded()) {
            timeUp = true;
            return 0;
        }

        // Mate distance pruning bounds (optional)
        int mateIn = CHECKMATE_SCORE - ply;
        if (alpha < -mateIn) alpha = -mateIn;
        if (beta > mateIn) beta = mateIn;
        if (alpha >= beta) return alpha;

        long key = getZKey(searchBoard);
        TTEntry tte = tt.get(key);
        if (tte != null && tte.depth >= depth) {
            if (tte.flag == TTFlag.EXACT) return tte.score;
            if (tte.flag == TTFlag.LOWERBOUND && tte.score > alpha) alpha = tte.score;
            else if (tte.flag == TTFlag.UPPERBOUND && tte.score < beta) beta = tte.score;
            if (alpha >= beta) return tte.score;
        }

        if (depth == 0) {
            return quiescence(alpha, beta, ply, searchBoard);
        }

        List<Move> moves = searchBoard.legalMoves();
        if (moves.isEmpty()) {
            // checkmate or stalemate
            if (isInCheck(searchBoard)) {
                return -CHECKMATE_SCORE + ply; // losing side to move is mated
            } else {
                return 0; // stalemate
            }
        }

        orderMoves(moves, tte != null ? tte.bestMove : null, ply, searchBoard);

        int bestScore = -CHECKMATE_SCORE;
        Move best = null;

        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            searchBoard.doMove(m);
            int score = -alphaBeta(depth - 1, -beta, -alpha, !maximizing, ply + 1, searchBoard);
            searchBoard.undoMove();

            if (timeUp) return 0;

            if (score > bestScore) {
                bestScore = score;
                best = m;
                if (score > alpha) {
                    alpha = score;
                    // History heuristic boost for quiet moves
                    if (!isCapture(m, searchBoard)) addHistory(m, depth);
                }
            }

            if (alpha >= beta) {
                storeKiller(m, ply, searchBoard);
                break; // beta cutoff
            }
        }

        // Store in TT
        TTFlag flag;
        if (bestScore <= alpha) {
            flag = TTFlag.UPPERBOUND;
        } else if (bestScore >= beta) {
            flag = TTFlag.LOWERBOUND;
        } else {
            flag = TTFlag.EXACT;
        }
        tt.put(key, new TTEntry(depth, bestScore, flag, best));

        return bestScore;
    }

    private int quiescence(int alpha, int beta, int ply, Board searchBoard) {
        if ((nodes++ % TIME_CHECK_EVERY_NODES) == 0 && timeExceeded()) {
            timeUp = true;
            return 0;
        }

        // Prevent stack overflow by limiting quiescence depth
        if (ply >= MAX_QUIESCENCE_DEPTH) {
            return evaluate(searchBoard);
        }

        int standPat = evaluate(searchBoard);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;

        List<Move> moves = searchBoard.legalMoves();
        // Consider only captures and promotions in QS
        List<Move> noisy = new ArrayList<>();
        for (Move m : moves) {
            if (isCapture(m, searchBoard) || isPromotion(m)) noisy.add(m);
        }
        // Order captures by MVV/LVA
        noisy.sort((a, b) -> Integer.compare(mvvLvaScore(b, searchBoard), mvvLvaScore(a, searchBoard)));

        for (Move m : noisy) {
            try {
                searchBoard.doMove(m);
                int score = -quiescence(-beta, -alpha, ply + 1, searchBoard);
                searchBoard.undoMove();
                if (timeUp) return 0;

                if (score >= beta) return beta;
                if (score > alpha) alpha = score;
            } catch (Exception e) {
                // Skip moves that cause errors (e.g., invalid board states from malformed FEN)
                continue;
            }
        }
        return alpha;
    }

    // =============== ORDERING & HEURISTICS ==================

    private void orderMoves(List<Move> moves, Move pv, int ply, Board searchBoard) {
        // PV first
        if (pv != null && moves.remove(pv)) moves.add(0, pv);

        // Simple scoring for ordering
        moves.sort((a, b) -> Integer.compare(scoreForOrdering(b, ply, searchBoard), scoreForOrdering(a, ply, searchBoard)));
    }

    private int scoreForOrdering(Move m, int ply, Board searchBoard) {
        int score = 0;
        if (isCapture(m, searchBoard)) score += 10_000 + mvvLvaScore(m, searchBoard);
        if (isPromotion(m)) score += 8_000;
        // killer moves
        Move k1 = killerMovesSafe(ply, 0);
        Move k2 = killerMovesSafe(ply, 1);
        if (m.equals(k1)) score += 5_000;
        else if (m.equals(k2)) score += 4_000;
        // history heuristic (quiet moves only)
        if (!isCapture(m, searchBoard)) score += historyHeuristic.getOrDefault(packMove(m), 0);
        return score;
    }

    private void storeKiller(Move m, int ply, Board searchBoard) {
        if (isCapture(m, searchBoard)) return; // only quiet moves are killers
        Move k1 = killerMoves[ply][0];
        if (!m.equals(k1)) {
            killerMoves[ply][1] = killerMoves[ply][0];
            killerMoves[ply][0] = m;
        }
    }

    private Move killerMovesSafe(int ply, int idx) {
        if (ply < 0 || ply >= killerMoves.length) return null;
        return killerMoves[ply][idx];
    }

    private void addHistory(Move m, int depth) {
        int key = packMove(m);
        historyHeuristic.put(key, historyHeuristic.getOrDefault(key, 0) + depth * depth);
    }

    private int mvvLvaScore(Move m, Board searchBoard) {
        Piece victim = searchBoard.getPiece(m.getTo());
        Piece attacker = searchBoard.getPiece(m.getFrom());
        return 10 * pieceValue(victim) - pieceValue(attacker);
    }

    private boolean isCapture(Move m, Board searchBoard) {
        Piece victim = searchBoard.getPiece(m.getTo());
        return victim != Piece.NONE;
    }

    private boolean isPromotion(Move m) {
        return m.getPromotion() != null;
    }

    private boolean isInCheck(Board b) {
        // Side to move is the side to be checked
        return b.isKingAttacked();
    }

    // =============== EVALUATION ==================

    private int evaluate(Board searchBoard) {
        // Perspective: score is from side to move's perspective relative to our color
        int materialWhite = 0;
        int materialBlack = 0;
        int pstWhite = 0;
        int pstBlack = 0;
        int mobilityWhite = 0;
        int mobilityBlack = 0;

        // Material and PST
        for (Square sq : Square.values()) {
            Piece p = searchBoard.getPiece(sq);
            if (p == Piece.NONE) continue;
            int pv = pieceValue(p);
            int idx = squareToIndex(sq, Side.WHITE); // 0..63 from White's POV
            switch (p) {
                case WHITE_PAWN -> { materialWhite += pv; pstWhite += PST.PAWN_MG[idx]; }
                case WHITE_KNIGHT -> { materialWhite += pv; pstWhite += PST.KNIGHT_MG[idx]; }
                case WHITE_BISHOP -> { materialWhite += pv; pstWhite += PST.BISHOP_MG[idx]; }
                case WHITE_ROOK -> { materialWhite += pv; pstWhite += PST.ROOK_MG[idx]; }
                case WHITE_QUEEN -> { materialWhite += QUEEN; pstWhite += PST.QUEEN_MG[idx]; }
                case WHITE_KING -> { pstWhite += PST.KING_MG[idx]; }

                case BLACK_PAWN -> { materialBlack += PAWN; pstBlack += PST.flip(PST.PAWN_MG, idx); }
                case BLACK_KNIGHT -> { materialBlack += KNIGHT; pstBlack += PST.flip(PST.KNIGHT_MG, idx); }
                case BLACK_BISHOP -> { materialBlack += BISHOP; pstBlack += PST.flip(PST.BISHOP_MG, idx); }
                case BLACK_ROOK -> { materialBlack += ROOK; pstBlack += PST.flip(PST.ROOK_MG, idx); }
                case BLACK_QUEEN -> { materialBlack += QUEEN; pstBlack += PST.flip(PST.QUEEN_MG, idx); }
                case BLACK_KING -> { pstBlack += PST.flip(PST.KING_MG, idx); }
                default -> {}
            }
        }

        // Mobility: rough count for side to move only (optimization)
        Side stm = searchBoard.getSideToMove();
        var legalMoves = searchBoard.legalMoves();
        int sideToMoveM = legalMoves == null ? 0 : legalMoves.size();
        if (stm == Side.WHITE) {
            mobilityWhite = sideToMoveM;
        } else {
            mobilityBlack = sideToMoveM;
        }

        // Hanging pieces penalty
        int hangingWhite = detectHangingPieces(Side.WHITE, searchBoard);
        int hangingBlack = detectHangingPieces(Side.BLACK, searchBoard);

        // Game phase: based on non-pawn material
        int nonPawnMaterial = (materialWhite - countPawns(Side.WHITE, searchBoard) * PAWN)
                + (materialBlack - countPawns(Side.BLACK, searchBoard) * PAWN);
        int phase = Math.max(0, Math.min(24, nonPawnMaterial / 320)); // rough 0..24

        int mgScore = (materialWhite - materialBlack) + (pstWhite - pstBlack) + 2 * (mobilityWhite - mobilityBlack)
                - (hangingWhite - hangingBlack); // Subtract hanging piece penalties

        // Endgame PST for king (encourage centralization)
        int kingEgWhite = PST.KING_EG[squareToIndex(findKing(Side.WHITE, searchBoard), Side.WHITE)];
        int kingEgBlack = PST.flip(PST.KING_EG, squareToIndex(findKing(Side.BLACK, searchBoard), Side.WHITE));
        int egScore = (materialWhite - materialBlack) + (pstWhite - pstBlack) + (kingEgWhite - kingEgBlack) * 2
                - (hangingWhite - hangingBlack); // Subtract hanging piece penalties

        int score = (mgScore * phase + egScore * (24 - phase)) / 24;

        // Return from side-to-move's perspective (required for negamax)
        // score represents White's advantage, so flip for Black
        if (stm == Side.WHITE) return score;
        return -score;
    }

    /**
     * Detects hanging pieces for a given side.
     * A piece is considered hanging if:
     * 1. It is attacked by opponent pieces
     * 2. It is not defended, OR the number of attackers > number of defenders
     * 
     * Returns a penalty score (positive value = bad for the side).
     */
    private int detectHangingPieces(Side side, Board searchBoard) {
        int hangingPenalty = 0;
        Side opponent = (side == Side.WHITE) ? Side.BLACK : Side.WHITE;
        
        for (Square sq : Square.values()) {
            Piece piece = searchBoard.getPiece(sq);
            if (piece == Piece.NONE) continue;
            
            // Check if this piece belongs to the side we're evaluating
            boolean isPieceBelongsToSide = (side == Side.WHITE && piece.getPieceSide() == Side.WHITE)
                    || (side == Side.BLACK && piece.getPieceSide() == Side.BLACK);
            
            if (!isPieceBelongsToSide) continue;
            
            // Skip kings - they can't be "hanging" in the traditional sense
            if (piece == Piece.WHITE_KING || piece == Piece.BLACK_KING) continue;
            
            // Count attackers from opponent
            long attackers = searchBoard.squareAttackedBy(sq, opponent);
            int attackerCount = Long.bitCount(attackers);
            
            if (attackerCount == 0) continue; // Not attacked, not hanging
            
            // Count defenders from our side
            long defenders = searchBoard.squareAttackedBy(sq, side);
            int defenderCount = Long.bitCount(defenders);
            
            // Piece is hanging if attacked and not defended, or more attackers than defenders
            if (defenderCount == 0) {
                // Completely undefended piece that is attacked - full penalty
                hangingPenalty += pieceValue(piece);
            } else if (attackerCount > defenderCount) {
                // More attackers than defenders - partial penalty (50% of piece value)
                hangingPenalty += pieceValue(piece) / 2;
            }
        }
        
        return hangingPenalty;
    }

    private Square findKing(Side color, Board searchBoard) {
        for (Square sq : Square.values()) {
            Piece p = searchBoard.getPiece(sq);
            if (color == Side.WHITE && p == Piece.WHITE_KING) return sq;
            if (color == Side.BLACK && p == Piece.BLACK_KING) return sq;
        }
        return Square.NONE;
    }

    private int countPawns(Side color, Board searchBoard) {
        int c = 0;
        for (Square sq : Square.values()) {
            Piece p = searchBoard.getPiece(sq);
            if (color == Side.WHITE && p == Piece.WHITE_PAWN) c++;
            if (color == Side.BLACK && p == Piece.BLACK_PAWN) c++;
        }
        return c;
    }

    private int squareToIndex(Square sq, Side pov) {
        if (sq == Square.NONE || sq == null) {
            return 0; // Safe fallback
        }
        int file = sq.getFile().ordinal(); // 0..7
        int rank = sq.getRank().ordinal(); // 0..7 (A1 -> rank 0)
        if (pov == Side.WHITE) {
            return rank * 8 + file;
        } else {
            return (7 - rank) * 8 + (7 - file);
        }
    }

    private int pieceValue(Piece p) {
        return switch (p) {
            case WHITE_PAWN, BLACK_PAWN -> PAWN;
            case WHITE_KNIGHT, BLACK_KNIGHT -> KNIGHT;
            case WHITE_BISHOP, BLACK_BISHOP -> BISHOP;
            case WHITE_ROOK, BLACK_ROOK -> ROOK;
            case WHITE_QUEEN, BLACK_QUEEN -> QUEEN;
            default -> 0;
        };
    }

    // =============== UTIL ==================

    private boolean timeExceeded() {
        return System.currentTimeMillis() >= moveSearchDeadline;
    }

    private static long computeTimeBudget(int remainingMs, int incMs) {
        long budget = remainingMs / 40L + 3L * incMs;
        return Math.max(50L, Math.min(budget, Math.max(100L, remainingMs / 2L)));
    }

    private long getZKey(Board b) {
        try {
            // chesslib exposes a Zobrist key on Board in recent versions
            return b.getZobristKey();
        } catch (Throwable t) {
            // Fallback to FEN hash if method not available
            return (long) b.getFen().hashCode();
        }
    }

    public void setupGameInfo(GameInfo info) {
        this.initialTimeMs = info.getInitialTimeMs();
        this.incrementMs = info.getIncrementMs();
        this.myColor = (info.getColor() == Color.WHITE) ? Side.WHITE : Side.BLACK;
    }

    public void clearSearchHelpers() {
        // Clear search helpers
        Arrays.stream(killerMoves).forEach(arr -> Arrays.fill(arr, null));
        historyHeuristic.clear();
        tt.clear();
    }

    private int packMove(Move m) {
        int from = m.getFrom().ordinal();
        int to = m.getTo().ordinal();
        int promo = 0;
        if (m.getPromotion() != null) promo = m.getPromotion().ordinal() & 0xFF;
        return (from) | (to << 8) | (promo << 16);
    }

    private Move parseMove(String lan, Board boardToUse) {
        // chesslib expects moves in format like "E2E4"
        // LAN format: source square + destination square + optional promotion piece
        String upperLan = lan.toUpperCase();

        // Find the move in legal moves that matches
        List<Move> legalMoves = boardToUse.legalMoves();
        for (Move move : legalMoves) {
            String moveLan = moveToLAN(move);
            if (moveLan.equalsIgnoreCase(lan)) {
                return move;
            }
        }

        // If not found in legal moves, try to construct it
        // This is a fallback that shouldn't normally be needed
        return new Move(upperLan, boardToUse.getSideToMove());
    }

    private String moveToLAN(Move move) {
        return move.toString().toLowerCase();
    }
}
