package ch.adjudicator.agent;

import ch.adjudicator.agent.bitboard.adapter.ChessLibAdapter;
import ch.adjudicator.agent.bitboard.generator.BitboardMoveGenerator;
import ch.adjudicator.agent.bitboard.model.BoardState;
import ch.adjudicator.agent.bitboard.model.FastMove;
import ch.adjudicator.agent.positionevaluation.ResultingScoreAndBounds;
import ch.adjudicator.agent.positionevaluation.ScoreAndMove;
import ch.adjudicator.agent.positionevaluation.SimpleBoardEvaluation;
import ch.adjudicator.agent.positionevaluation.ZobristHash;
import ch.adjudicator.client.GameInfo;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class BestMoveCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BestMoveCalculator.class);

    // Special score values
    public static final int MATE_SCORE = 300000;
    public static final int MAX_MATE_DISTANCE = 1000;
    public static final int DRAW_SCORE = 0;



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
    private List<BoardState> positionHistory;
    private List<FastMove> debugMoveHistory;
    private boolean collectDebugMoves = false;

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

        debugMoveHistory = new ArrayList<>();

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

    public Move computeBestMove(Board searchBoard, int yourTimeMs) throws Exception {
        return computeBestMove(searchBoard, yourTimeMs, 0, 30);
    }

    public Move computeBestMove(Board searchBoard, int yourTimeMs, int minDepth, int maxDepth) throws Exception {
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
            Move bestMove = findBestMove(searchBoard, budgetMs, minDepth, maxDepth);
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

    public void updatePositionHistory(BoardState board) {
        positionHistory.add(board);
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

        debugMoveHistory.clear();
    }

    private String moveToLAN(Move move) {
        return move.toString().toLowerCase();
    }

    /**
     * Alpha-beta search with a fixed depth.
     * Inspired from:
     * https://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning
     * and
     * https://www.chessprogramming.org/Alpha-Beta
     */
    private ResultingScoreAndBounds alphaBeta(BoardState board, FastMove lastMove, int depth, int alpha, int beta, final boolean isMaximizingPlayer, Consumer<ResultingScoreAndBounds> bestMoveSink, long endTime, Consumer<AtomicBoolean> abortingSink, int ply) {
        if (collectDebugMoves) {
            if (debugMoveHistory.size() > 0) {
                if (debugMoveHistory.getFirst().toString().toUpperCase().equals("F1E1")) {
                    LOGGER.info("Evaluate alphaBeta on depth {}: {}", depth, debugMoveHistory);
                }
            }
        }

        // Check for repetition (3-fold repetition is a draw)
        if (isRepetition(board)) {
            return ResultingScoreAndBounds.builder()
                    .score(DRAW_SCORE)
                    .alpha(alpha)
                    .beta(beta)
                    .ply(ply)
                    .build();
        }

        // Transposition table lookup
        // - currently not implemented -

        // Check if position is terminal (checkmate, stalemate)
        List<FastMove> legalMoves = BitboardMoveGenerator.generateMoves(board, lastMove);
        if (legalMoves.isEmpty()) {
            if (BitboardMoveGenerator.isCurrentPlayerInCheck(board)) {
                // Checkmate - we are getting mated at this position
                // Return mate score with distance = ply from root
                int movesToMate = (ply + 1) / 2;
                return ResultingScoreAndBounds.builder()
                        .score(-MATE_SCORE + movesToMate)
                        .alpha(alpha)
                        .beta(beta)
                        .ply(ply)
                        .build();
            } else {
                // Stalemate
                return ResultingScoreAndBounds.builder()
                        .score(DRAW_SCORE)
                        .alpha(alpha)
                        .beta(beta)
                        .ply(ply)
                        .build();
            }
        }

        // Base case: use quiescence search at leaf nodes
        if (depth <= 0) {
            int score = ch.adjudicator.agent.bitboard.evaluation.SimpleBoardEvaluation.evaluate(board);
            if(!isMaximizingPlayer) {
                score = -score;
            }
            //int score = quiescence(board, alpha, beta, ply);
            if (collectDebugMoves) {
                if (debugMoveHistory.size() > 0) {
                    if (debugMoveHistory.getFirst().toString().toUpperCase().equals("F1E1")) {
                        LOGGER.info("Final quiescence score: {}", score);
                    }
                }
            }
            return ResultingScoreAndBounds.builder()
                    .score(score)
                    .alpha(alpha)
                    .beta(beta)
                    .ply(ply)
                    .build();
        }

        if (isMaximizingPlayer) {
            int bestScore = -MATE_SCORE - 1000;

            for (FastMove nextMove : legalMoves) {
                BoardState nextBoardState = board.applyMove(nextMove);
                if (collectDebugMoves) {
                    debugMoveHistory.add(nextMove);
                }

                positionHistory.add(nextBoardState);

                int score = alphaBeta(nextBoardState, nextMove, depth - 1, alpha, beta, false, bestMoveSink, endTime, abortingSink, ply + 1).getScore();

                positionHistory.removeLast();
                if (collectDebugMoves) {
                    debugMoveHistory.removeLast();
                }

                if (score > bestScore) {
                    bestScore = score;

                    if (score > alpha) {
                        alpha = score;

                        bestMoveSink.accept(ResultingScoreAndBounds.builder() //TODO remove
                                .score(bestScore)
                                .alpha(alpha)
                                .beta(beta)
                                .ply(ply)
                                .build());
                        //LOGGER.info("Currently best move: {}", move);
                    }
                }

                long elapsed = System.currentTimeMillis() - endTime;
                if (elapsed >= 0) {
                    abortingSink.accept(new AtomicBoolean(true));
                    return ResultingScoreAndBounds.builder()
                            .score(bestScore)
                            .alpha(alpha)
                            .beta(beta)
                            .ply(ply)
                            .build();
                }

                // Beta cutoff - prune this branch
                if (score >= beta) {
                    return ResultingScoreAndBounds.builder()
                            .score(bestScore)
                            .alpha(alpha)
                            .beta(beta)
                            .ply(ply)
                            .build();
                }

                // If we found a winning mate, we can stop searching deeper in this branch
                // (all other moves will be evaluated, but we know we have a forced win)
                /*if (bestScore.isWinningMate()) {
                    // Continue to find the shortest mate among siblings
                }*/
            }
            return ResultingScoreAndBounds.builder()
                    .score(bestScore)
                    .alpha(alpha)
                    .beta(beta)
                    .ply(ply)
                    .build();
        } else {
            int bestScore = MATE_SCORE + 1000;

            for (FastMove nextMove : legalMoves) {
                BoardState nextBoardState = board.applyMove(nextMove);
                if (collectDebugMoves) {
                    debugMoveHistory.add(nextMove);
                }

                positionHistory.add(nextBoardState);


                int score = alphaBeta(nextBoardState, nextMove, depth - 1, alpha, beta, true, bestMoveSink, endTime, abortingSink, ply + 1).getScore();

                positionHistory.removeLast();
                if (collectDebugMoves) {
                    debugMoveHistory.removeLast();
                }

                if (score < bestScore) {
                    bestScore = score;
                    if (score < beta) {
                        beta = score;
                    }
                }

                // Alpha cutoff - prune this branch
                if (score <= alpha) {
                    return ResultingScoreAndBounds.builder()
                            .score(score)
                            .alpha(alpha)
                            .beta(beta)
                            .ply(ply)
                            .build();
                }

                beta = Math.min(beta, bestScore);

                // If we found a winning mate, we can stop searching deeper in this branch
                // (all other moves will be evaluated, but we know we have a forced win)
                /*if (bestScore.isWinningMate()) {
                    // Continue to find the shortest mate among siblings
                }*/
            }
            return ResultingScoreAndBounds.builder()
                    .score(bestScore)
                    .alpha(alpha)
                    .beta(beta)
                    .ply(ply)
                    .build();
        }
    }

    /**
     * Check if current position is a repetition (2-fold or more in search tree)
     */
    private boolean isRepetition(BoardState boardState) {
        int count = 0;
        for (int i = positionHistory.size() - 1; i >= 0; i--) {
            if (positionHistory.get(i).isEqualIgnoringAuxiliariesFlags(boardState)) {
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
    private Move findBestMove(Board board, long budgetMs, int minDepth, int maxDepth) {
        List<Move> legalMoves = board.legalMoves();
        if (legalMoves.isEmpty()) {
            return null;
        }

        // Sort moves by a simple heuristic to improve move ordering
        legalMoves.sort((m1, m2) -> {
            int score1 = SimpleBoardEvaluation.getMoveScore(board, m1);
            int score2 = SimpleBoardEvaluation.getMoveScore(board, m2);
            return Integer.compare(score2, score1);
        });

        Move bestMove = legalMoves.getFirst();
        long startTime = System.currentTimeMillis();
        int bestScore = -MATE_SCORE - 1000;

        // Iterative deepening
        for (int depth = minDepth; depth <= maxDepth; depth++) {
            long endTime = startTime + budgetMs;
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= budgetMs) {
                break;
            }

            int alpha = -MATE_SCORE - 1000;
            int beta = MATE_SCORE + 1000;

            AtomicBoolean thisDepthIsAborted = new AtomicBoolean(false);

            Consumer<ResultingScoreAndBounds> bestMoveSink = (ResultingScoreAndBounds bestMoveSoFar) -> {
                if(collectDebugMoves) {
                    LOGGER.info("BestMoveSoFar: score={} alpha={} beta={}",
                            bestMoveSoFar.getScore(), bestMoveSoFar.getAlpha(), bestMoveSoFar.getBeta());
                }
            };

            Consumer<AtomicBoolean> abortingSink = (AtomicBoolean _isSearchAborted) -> {
                LOGGER.info("Aborting search...");
                thisDepthIsAborted.set(true);
            };

            List<ScoreAndMove> scoreAndMoves = new ArrayList<>();

            for (Move move : legalMoves) {

                board.doMove(move);

                BoardState boardState = ChessLibAdapter.fenToBoardState(board.getFen());
                positionHistory.add(boardState);

                scoreAndMoves.add(ScoreAndMove.builder()
                        .score(alphaBeta(boardState, null, depth, alpha, beta, true, bestMoveSink, endTime, abortingSink, 0).negateScore())
                        .move(move)
                        .build());

                positionHistory.removeLast();
                if (collectDebugMoves) {
                    debugMoveHistory.removeLast();
                }
                board.undoMove();

                if (thisDepthIsAborted.get()) {
                    break;
                }
            }

            if (thisDepthIsAborted.get()) {
                LOGGER.debug("Unfinished search... depth={}", depth);
                break;
            }

            LOGGER.debug("Finished search for this depth={}", depth);

            var bestMoveWithinResults = searchForBestMoveWithinScores(scoreAndMoves);
            if (bestMoveWithinResults != null) {
                bestMove = bestMoveWithinResults.getMove();
                bestScore = bestMoveWithinResults.getScore().getScore();
            }

            // If we found a mate in 1, no need to search deeper
            // Mate in 1 means score is close to MATE_SCORE (within MAX_MATE_DISTANCE)
            if (bestScore >= MATE_SCORE - MAX_MATE_DISTANCE) {
                int movesToMate = MATE_SCORE - bestScore;
                if (movesToMate == 1) {
                    LOGGER.info("[{}] Mate in 1 found, stopping search", name);
                    break;
                }
            }

            // Stop if time is up
            elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= budgetMs) {
                break;
            }
        }

        return bestMove;
    }

    private ScoreAndMove searchForBestMoveWithinScores(List<ScoreAndMove> scores) {
        ScoreAndMove bestMoveSoFar = scores.getFirst();
        for (ScoreAndMove score : scores) {
            if (score.getScore().getScore() > bestMoveSoFar.getScore().getScore()) {
                bestMoveSoFar = score;
            }
        }
        return bestMoveSoFar;
    }

    private ScoreAndMove searchForBestMoveWithinResults(AtomicReference<List<ScoreAndMove>> thisDepthsBestMove) {
        var results = thisDepthsBestMove.get();
        int maxPly = 0;
        ScoreAndMove bestMoveSoFar = null;

        List<ScoreAndMove> resultsWithSanitizedAlphaSmallerThanMinBetaForSameMove = new ArrayList<>();

        for (ScoreAndMove result : results) {
            ScoreAndMove minBeta = result;
            for (ScoreAndMove minBetaCandidate : results) {
                if (result.getScore().getPly() == minBetaCandidate.getScore().getPly() && result.getMove() == minBetaCandidate.getMove()) {
                    if (minBetaCandidate.getScore().getBeta() < minBeta.getScore().getBeta()) {
                        minBeta = minBetaCandidate;
                    }
                }
            }
            if (result.getScore().getAlpha() > minBeta.getScore().getBeta()) {
                resultsWithSanitizedAlphaSmallerThanMinBetaForSameMove.add(result);
            }
        }

        for (ScoreAndMove result : resultsWithSanitizedAlphaSmallerThanMinBetaForSameMove) {
            if (result.getScore().getPly() >= maxPly) {
                if (bestMoveSoFar != null) {
                    if (result.getScore().getScore() > bestMoveSoFar.getScore().getScore()) {
                        bestMoveSoFar = result;
                    }
                } else {
                    bestMoveSoFar = result;
                }
                maxPly = result.getScore().getPly();
            }
        }
        LOGGER.info("bestMove selected is: bestMove={}", bestMoveSoFar);
        return bestMoveSoFar;
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
}
