package ch.adjudicator.agent;

import ch.adjudicator.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * SmartAgent: A chess agent powered by Stockfish engine.
 * Uses inter-process communication with Stockfish via UCI protocol
 * running in a separate thread.
 */
public class SmartAgent implements Agent {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmartAgent.class);

    private final String name;
    private StockfishEngine engine;
    private List<String> moveHistory;
    private long incrementMs;

    public SmartAgent(String name) {
        this.name = name;
        this.moveHistory = new ArrayList<>();
        this.incrementMs = 0;
    }

    @Override
    public String getMove(MoveRequest request) throws Exception {
        LOGGER.info("[{}] My turn! Time remaining: {}ms", name, request.getYourTimeMs());

        // Update move history with opponent's move if present
        if (!request.getOpponentMove().isEmpty()) {
            String opponentMove = request.getOpponentMove();
            LOGGER.info("[{}] Opponent played: {}", name, opponentMove);
            moveHistory.add(opponentMove);
        }

        // Set position in Stockfish with full move history
        String[] movesArray = moveHistory.toArray(new String[0]);
        engine.setPosition(movesArray);

        // Get best move from Stockfish with time control
        long timeMs = request.getYourTimeMs();
        String bestMove = engine.getBestMove(timeMs, incrementMs);

        // Add our move to history
        moveHistory.add(bestMove);

        LOGGER.info("[{}] Playing move: {}", name, bestMove);
        return bestMove;
    }

    @Override
    public void onGameStart(GameInfo info) {
        LOGGER.info("[{}] *** Game Started ***", name);
        LOGGER.info("[{}] Game ID: {}", name, info.getGameId());
        LOGGER.info("[{}] Playing as: {}", name, info.getColor());
        LOGGER.info("[{}] Time control: {}ms + {}ms increment",
                name, info.getInitialTimeMs(), info.getIncrementMs());

        // Store increment for time management
        this.incrementMs = info.getIncrementMs();

        // Reset move history for new game
        moveHistory.clear();

        try {
            // Stop previous engine if running
            if (engine != null && engine.isAlive()) {
                engine.stop();
            }

            // Initialize new engine instance
            engine = new StockfishEngine();
            engine.start();
            engine.newGame();

            LOGGER.info("[{}] Stockfish engine ready for game", name);
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to initialize Stockfish engine", name, e);
            throw new RuntimeException("Failed to start Stockfish engine", e);
        }
    }

    @Override
    public void onGameOver(GameOverInfo info) {
        LOGGER.info("[{}] *** Game Over ***", name);
        LOGGER.info("[{}] Result: {}", name, info.getResult());
        LOGGER.info("[{}] Reason: {}", name, info.getReason());
        if (info.getFinalPgn() != null && !info.getFinalPgn().isEmpty()) {
            LOGGER.info("[{}] Final PGN:\n{}", name, info.getFinalPgn());
        }

        // Stop Stockfish engine
        if (engine != null) {
            try {
                engine.stop();
                LOGGER.info("[{}] Stockfish engine stopped", name);
            } catch (Exception e) {
                LOGGER.warn("[{}] Error stopping Stockfish engine", name, e);
            }
        }
    }

    @Override
    public void onError(String message) {
        LOGGER.error("[{}] Error: {}", name, message);
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
