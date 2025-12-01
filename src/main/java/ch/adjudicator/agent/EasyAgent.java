package ch.adjudicator.agent;

import ch.adjudicator.client.*;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Easy Agent Example for Adjudicator Chess Contest Platform.
 * This agent uses the chesslib library to maintain proper board state
 * and generate legal moves. It selects randomly from all legal moves.
 */
public class EasyAgent implements Agent {
    private static final Logger LOGGER = LoggerFactory.getLogger(EasyAgent.class);

    private final String name;
    private final Random random;
    private Board board;

    public EasyAgent(String name) {
        this.name = name;
        this.random = new Random();
        this.board = new Board();
    }

    @Override
    public String getMove(MoveRequest request) throws Exception {
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
        Move selectedMove = legalMoves.get(random.nextInt(legalMoves.size()));

        // Apply the move to our board
        board.doMove(selectedMove);

        // Convert to Long Algebraic Notation (LAN)
        String moveStr = moveToLAN(selectedMove);
        LOGGER.info("[{}] Playing move: {} (from {} legal moves)", name, moveStr, legalMoves.size());
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
        return moveStr;
    }

    @Override
    public void onGameStart(GameInfo info) {
        LOGGER.info("[{}] *** Game Started ***", name);
        LOGGER.info("[{}] Game ID: {}", name, info.getGameId());
        LOGGER.info("[{}] Playing as: {}", name, info.getColor());
        LOGGER.info("[{}] Time control: {}ms + {}ms increment",
                name, info.getInitialTimeMs(), info.getIncrementMs());

        // Reset the board for a new game
        board = new Board();
    }

    @Override
    public void onGameOver(GameOverInfo info) {
        LOGGER.info("[{}] *** Game Over ***", name);
        LOGGER.info("[{}] Result: {}", name, info.getResult());
        LOGGER.info("[{}] Reason: {}", name, info.getReason());
        if (!info.getFinalPgn().isEmpty()) {
            LOGGER.info("[{}] Final PGN:\n{}", name, info.getFinalPgn());
        }
    }

    @Override
    public void onError(String message) {
        LOGGER.error("[{}] ERROR: {}", name, message);
    }

    /**
     * Parse a move in Long Algebraic Notation (LAN) format.
     * Examples: "e2e4", "e7e8q" (promotion)
     */
    private Move parseMove(String lan) {
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

    /**
     * Convert a Move object to Long Algebraic Notation (LAN).
     * Examples: "e2e4", "e7e8q" (for promotion to queen)
     */
    private String moveToLAN(Move move) {
        return move.toString().toLowerCase();
    }

    public static void main(String[] args) {
        // Default values
        String defaultServer = "grpc.adjudicator.ch";
        String defaultKey = "1234";
        String defaultName = "EasyBot";

        // Check for agent.env file
        try {
            java.io.File envFile = new java.io.File("agent.env");
            if (envFile.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(envFile)) {
                    props.load(fis);
                    if (props.getProperty("SERVER") != null && !props.getProperty("SERVER").isEmpty()) {
                        defaultServer = props.getProperty("SERVER");
                    }
                    if (props.getProperty("API_KEY") != null && !props.getProperty("API_KEY").isEmpty()) {
                        defaultKey = props.getProperty("API_KEY");
                    }
                    if (props.getProperty("AGENT_NAME") != null && !props.getProperty("AGENT_NAME").isEmpty()) {
                        defaultName = props.getProperty("AGENT_NAME");
                    }
                    LOGGER.info("Loaded configuration from agent.env");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load agent.env: {}", e.getMessage());
        }

        // Parse command line arguments
        String serverAddr = getArg(args, "--server", defaultServer);
        String apiKey = getArg(args, "--key", defaultKey);
        String modeStr = getArg(args, "--mode", "TRAINING");
        String timeControl = getArg(args, "--time", "300+0");
        String agentName = getArg(args, "--name", defaultName);

        if (apiKey == null) {
            System.err.println("API key is required. Use --key <api-key>");
            System.exit(1);
        }

        // Parse game mode
        GameMode mode;
        try {
            mode = GameMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid game mode: " + modeStr);
            System.err.println("Valid modes: TRAINING, OPEN, RANKED");
            System.exit(1);
            return;
        }

        LOGGER.info("Starting {} agent...", agentName);
        LOGGER.info("Server: {}", serverAddr);
        LOGGER.info("Mode: {}", modeStr);
        LOGGER.info("Time control: {}", timeControl);
        LOGGER.info("Protocol: gRPC");

        // Create agent
        EasyAgent agent = new EasyAgent(agentName);

        // Create client and play game
        AdjudicatorClient client = new AdjudicatorClient(serverAddr, apiKey, true);

        try {
            client.playGame(agent, mode, timeControl);
            LOGGER.info("Agent finished successfully");
        } catch (Exception e) {
            LOGGER.error("Game error", e);
            System.exit(1);
        }
    }

    /**
     * Helper method to parse command line arguments.
     */
    private static String getArg(String[] args, String flag, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}
