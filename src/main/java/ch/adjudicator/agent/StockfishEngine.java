package ch.adjudicator.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manages inter-process communication with Stockfish chess engine using UCI protocol.
 * Runs in a separate thread to handle asynchronous communication.
 */
public class StockfishEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockfishEngine.class);
    private static final String STOCKFISH_PATH = "C:\\F\\projects\\devmtrail\\existing-chessbots\\stockfish\\stockfish-windows-x86-64-avx2.exe";
    private static final int MIN_ELO = 1320;
    private static final int MAX_ELO = 3190;
    
    private Process process;
    private OutputStreamWriter writer;
    private BufferedReader reader;
    private Thread readerThread;
    private volatile boolean running;
    private final BlockingQueue<String> responseQueue;
    
    public StockfishEngine() {
        this.responseQueue = new LinkedBlockingQueue<>();
        this.running = false;
    }
    
    /**
     * Starts the Stockfish engine process and initializes UCI communication.
     */
    public void start() throws IOException {
        LOGGER.info("Starting Stockfish engine from: {}", STOCKFISH_PATH);
        
        ProcessBuilder pb = new ProcessBuilder(STOCKFISH_PATH);
        pb.redirectErrorStream(true);
        process = pb.start();
        
        writer = new OutputStreamWriter(process.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        
        running = true;
        
        // Start reader thread to continuously read engine output
        readerThread = new Thread(this::readEngineOutput, "Stockfish-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
        
        // Initialize UCI mode
        sendCommand("uci");
        waitForResponse("uciok", 5000);
        
        // Configure Elo limitation if STOCKFISH_ELO environment variable is set
        String eloEnv = System.getenv("STOCKFISH_ELO");
        if (eloEnv != null && !eloEnv.trim().isEmpty()) {
            try {
                int targetElo = Integer.parseInt(eloEnv.trim());
                if (targetElo >= MIN_ELO && targetElo <= MAX_ELO) {
                    sendCommand("setoption name UCI_LimitStrength value true");
                    sendCommand(String.format("setoption name UCI_Elo value %d", targetElo));
                    LOGGER.info("Stockfish Elo limited to: {}", targetElo);
                } else {
                    LOGGER.warn("STOCKFISH_ELO value {} is out of valid range [{}, {}], ignoring", 
                                targetElo, MIN_ELO, MAX_ELO);
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid STOCKFISH_ELO value: {}, ignoring", eloEnv);
            }
        }
        
        // Check if engine is ready
        sendCommand("isready");
        waitForResponse("readyok", 5000);
        
        LOGGER.info("Stockfish engine initialized successfully");
    }
    
    /**
     * Stops the Stockfish engine and cleans up resources.
     */
    public void stop() {
        LOGGER.info("Stopping Stockfish engine");
        running = false;
        
        try {
            if (writer != null) {
                sendCommand("quit");
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (process != null) {
                process.waitFor(2, TimeUnit.SECONDS);
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            }
            if (readerThread != null) {
                readerThread.join(2000);
            }
        } catch (Exception e) {
            LOGGER.warn("Error while stopping engine", e);
        }
        
        LOGGER.info("Stockfish engine stopped");
    }
    
    /**
     * Starts a new game, clearing hash tables.
     */
    public void newGame() throws IOException {
        sendCommand("ucinewgame");
        sendCommand("isready");
        waitForResponse("readyok", 5000);
        LOGGER.debug("New game initialized");
    }
    
    /**
     * Sets the current position and move history.
     * 
     * @param moves List of moves in long algebraic notation (e.g., "e2e4", "e7e5")
     */
    public void setPosition(String... moves) throws IOException {
        StringBuilder cmd = new StringBuilder("position startpos");
        if (moves != null && moves.length > 0) {
            cmd.append(" moves");
            for (String move : moves) {
                cmd.append(" ").append(move);
            }
        }
        sendCommand(cmd.toString());
        LOGGER.debug("Position set: {}", cmd.toString());
    }
    
    /**
     * Computes the best move for the current position with time control.
     * 
     * @param timeMs Time remaining in milliseconds
     * @param incrementMs Increment per move in milliseconds
     * @return Best move in long algebraic notation (e.g., "e2e4")
     */
    public String getBestMove(long timeMs, long incrementMs) throws IOException, InterruptedException {
        // Clear the response queue before requesting a move
        responseQueue.clear();
        
        // Calculate time to allocate for this move (conservative strategy)
        long moveTime = Math.min(timeMs / 30 + incrementMs / 2, timeMs - 100);
        moveTime = Math.max(moveTime, 100); // At least 100ms
        
        String goCommand = String.format("go movetime %d", moveTime);
        sendCommand(goCommand);
        LOGGER.debug("Requesting move with command: {}", goCommand);
        
        // Wait for bestmove response
        String bestmove = waitForBestMove(moveTime + 5000);
        
        if (bestmove == null) {
            throw new IOException("No bestmove received from engine");
        }
        
        LOGGER.info("Stockfish recommends: {}", bestmove);
        return bestmove;
    }
    
    /**
     * Sends a command to the Stockfish engine.
     */
    private void sendCommand(String command) throws IOException {
        LOGGER.trace(">> {}", command);
        writer.write(command + "\n");
        writer.flush();
    }
    
    /**
     * Continuously reads output from the engine and queues responses.
     */
    private void readEngineOutput() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                LOGGER.trace("<< {}", line);
                responseQueue.offer(line);
            }
        } catch (IOException e) {
            if (running) {
                LOGGER.error("Error reading from engine", e);
            }
        }
        LOGGER.debug("Reader thread terminated");
    }
    
    /**
     * Waits for a specific response from the engine.
     */
    private void waitForResponse(String expectedResponse, long timeoutMs) throws IOException {
        long startTime = System.currentTimeMillis();
        
        try {
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                String line = responseQueue.poll(100, TimeUnit.MILLISECONDS);
                if (line != null && line.equals(expectedResponse)) {
                    return;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for response", e);
        }
        
        throw new IOException("Timeout waiting for: " + expectedResponse);
    }
    
    /**
     * Waits for bestmove response from the engine.
     * 
     * @return The best move in long algebraic notation, or null if timeout
     */
    private String waitForBestMove(long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            String line = responseQueue.poll(100, TimeUnit.MILLISECONDS);
            if (line != null && line.startsWith("bestmove ")) {
                // Parse bestmove response: "bestmove e2e4" or "bestmove e2e4 ponder e7e5"
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    return parts[1];
                }
            }
        }
        
        return null;
    }
    
    /**
     * Checks if the engine process is still alive.
     */
    public boolean isAlive() {
        return process != null && process.isAlive();
    }
}
