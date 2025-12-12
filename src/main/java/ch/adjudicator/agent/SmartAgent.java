package ch.adjudicator.agent;

import ch.adjudicator.client.*;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * SmartAgent: A stronger chess agent using Iterative Deepening + Alpha-Beta + Quiescence,
 * with a Transposition Table, basic Move Ordering (PV, MVV/LVA, killers, history),
 * and a reasonably rich static evaluation with PSTs and simple phase-aware scoring.
 *
 * This class is self-contained and does not change any other parts of the SDK.
 */
public class SmartAgent implements Agent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartAgent.class);


    // Board state
    private final String name;
    private Board board = new Board();
    private final Random random;

    BestMoveCalculator bestMoveCalculator = new BestMoveCalculator();

    public SmartAgent(String name) {
        this.name = name;
        this.random = new Random();
    }



    public Move computeMove(MoveRequest request) throws Exception {
        LOGGER.info("[{}] My turn! Time remaining: {}ms", name, request.getYourTimeMs());

        // Update board with opponent's move(s) if present
        if (!request.getOpponentMove().isEmpty()) {
            String opponentMove = request.getOpponentMove();
            LOGGER.info("[{}] Opponent played: {}", name, opponentMove);

            try {
                // Handle multiple moves separated by spaces
                String[] moves = opponentMove.trim().split("\\s+");
                for (String moveStr : moves) {
                    Move move = parseSingleMove(moveStr);
                    board.doMove(move);
                }
            } catch (Throwable e) {
                LOGGER.warn("[{}] Failed to parse opponent move: {}", name, opponentMove, e);
                // If we can't parse the move, reset the board and continue
                // This shouldn't happen in normal gameplay
            }
        }

        // Generate all legal moves
        List<Move> legalMoves = board.legalMoves();

        if (legalMoves.isEmpty()) {
            LOGGER.error("[{}] No legal moves available!", name);
            throw new Exception("No legal moves available");
        }

        // Select a random legal move
        Move selectedMove = null;
        try {
            selectedMove = bestMoveCalculator.computeBestMove(board, request.getYourTimeMs());
        } catch (Throwable e) {
            LOGGER.warn("[{}] Error while computing best move: {}", name, e.getMessage(), e);
            selectedMove = legalMoves.get(random.nextInt(legalMoves.size()));
        }

        if(!legalMoves.contains(selectedMove)) {
            LOGGER.warn("[{}] Detected illegal computedMoveBy bestMoveCalculator (us): {}", name, selectedMove.toString());
            selectedMove = legalMoves.get(random.nextInt(legalMoves.size()));
        }

        // Apply the move to our board
        board.doMove(selectedMove);

        // Convert to Long Algebraic Notation (LAN)
        String moveStr = moveToLAN(selectedMove);
        LOGGER.info("[{}] Playing move: {} (from {} legal moves)", name, moveStr, legalMoves.size());
        return selectedMove;
    }

    @Override
    public String getMove(MoveRequest request) throws Exception {
        try {
            LOGGER.info("[{}] Received move request: {}", name, request);
            String moveToSend = moveToLAN(computeMove(request));
            LOGGER.info("[{}] Sending move: {}", name, moveToSend);
            return moveToSend;
        } catch (Throwable e) {
            return "a1a5";
        }
    }

    @Override
    public void onGameStart(GameInfo info) {
        LOGGER.info("[{}] Game start: {} {}ms + {}ms", name, info.getGameId(), info.getInitialTimeMs(), info.getIncrementMs());
        this.board = new Board();
        this.bestMoveCalculator.setupGameInfo(info);
        this.bestMoveCalculator.clearSearchHelpers();
    }

    @Override
    public void onGameOver(GameOverInfo info) {
        LOGGER.info("[{}] Game over: {} ({})", name, info.getResult(), info.getReason());
        if (info.getFinalPgn() != null && !info.getFinalPgn().isEmpty()) {
            LOGGER.info("[{}] PGN:\n{}", name, info.getFinalPgn());
        }
    }

    @Override
    public void onError(String message) {
        LOGGER.error("[{}] Error: {}", name, message);
    }

    /**
     * Parse a single move in Long Algebraic Notation (LAN) format.
     * Examples: "e2e4", "e7e8q" (promotion)
     */
    private Move parseSingleMove(String lan) {
        // chesslib expects moves in format like "E2E4"
        // LAN format: source square + destination square + optional promotion piece
        String upperLan = lan.toUpperCase();

        // Find the move in legal moves that matches
        List<Move> legalMoves = board.legalMoves();
        for (Move move : legalMoves) {
            String moveLan = moveToLAN(move);
            if (moveLan.equalsIgnoreCase(lan)) {
                return move;
            }
        }

        // If not found in legal moves, try to construct it
        // This is a fallback that shouldn't normally be needed
        return new Move(upperLan, board.getSideToMove());
    }

    private String moveToLAN(Move move) {
        return move.toString().toLowerCase();
    }


    // =============== Main launcher (optional) ==================
    public static void main(String[] args) {
        while (true) {
            AgentConfiguration config = new AgentConfiguration(args);
            try {
                config.validate();
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
            GameMode mode;
            try {
                mode = GameMode.valueOf(config.getMode());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid game mode: " + config.getMode());
                System.err.println("Valid modes: TRAINING, OPEN, RANKED");
                System.exit(1);
                return;
            }

            LOGGER.info("Starting {} (SmartAgent)...", config.getAgentName());
            AdjudicatorClient client = new AdjudicatorClient(config.getServerAddress(), config.getApiKey(), true);
            SmartAgent agent = new SmartAgent(config.getAgentName());
            try {
                client.playGame(agent, mode, config.getTimeControl());
                LOGGER.info("SmartAgent finished successfully");
            } catch (Exception e) {
                LOGGER.error("Game error", e);
                System.exit(1);
            }
        }
    }
}
