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

    BestMoveCalculator bestMoveCalculator = new BestMoveCalculator();

    public SmartAgent(String name) {
        this.name = name;
    }



    public Move computeMove(MoveRequest request) throws Exception {
        LOGGER.info("[{}] My turn! Time remaining: {}ms", name, request.getYourTimeMs());

        // Update board with opponent's move if present
        if (!request.getOpponentMove().isEmpty()) {
            String opponentMove = request.getOpponentMove();
            LOGGER.info("[{}] Opponent played: {}", name, opponentMove);

            try {
                Move move = parseMove(opponentMove);
                board.doMove(move);
            } catch (Exception e) {
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
        Move selectedMove = bestMoveCalculator.computeBestMove(board, request.getYourTimeMs());

        // Apply the move to our board
        board.doMove(selectedMove);

        // Convert to Long Algebraic Notation (LAN)
        String moveStr = moveToLAN(selectedMove);
        LOGGER.info("[{}] Playing move: {} (from {} legal moves)", name, moveStr, legalMoves.size());
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
        return selectedMove;
    }

    @Override
    public String getMove(MoveRequest request) throws Exception {
        LOGGER.info("[{}] Received move request: {}", name, request);
        String moveToSend = moveToLAN(computeMove(request));
        LOGGER.info("[{}] Sending move: {}", name, moveToSend);
        return moveToSend;
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


    private int packMove(Move m) {
        int from = m.getFrom().ordinal();
        int to = m.getTo().ordinal();
        int promo = 0;
        if (m.getPromotion() != null) promo = m.getPromotion().ordinal() & 0xFF;
        return (from) | (to << 8) | (promo << 16);
    }

    private Move parseMove(String lan) {
        return parseMove(lan, board);
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


    // =============== Main launcher (optional) ==================
    public static void main(String[] args) {
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
