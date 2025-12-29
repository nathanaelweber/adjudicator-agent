package ch.adjudicator.agent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for StockfishEngine to verify UCI communication.
 */
public class StockfishEngineTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockfishEngineTest.class);
    private StockfishEngine engine;

    @BeforeEach
    public void setUp() throws Exception {
        engine = new StockfishEngine();
        engine.start();
    }

    @AfterEach
    public void tearDown() {
        if (engine != null) {
            engine.stop();
        }
    }

    @Test
    public void testEngineStartsAndResponds() throws Exception {
        LOGGER.info("[DEBUG_LOG] Testing engine initialization");
        assertTrue(engine.isAlive(), "Engine should be alive");
    }

    @Test
    public void testNewGame() throws Exception {
        LOGGER.info("[DEBUG_LOG] Testing new game initialization");
        engine.newGame();
        assertTrue(engine.isAlive(), "Engine should be alive after new game");
    }

    @Test
    public void testSetPosition() throws Exception {
        LOGGER.info("[DEBUG_LOG] Testing position setting");
        engine.setPosition(); // Starting position
        assertTrue(engine.isAlive(), "Engine should be alive after setting position");
    }

    @Test
    public void testSetPositionWithMoves() throws Exception {
        LOGGER.info("[DEBUG_LOG] Testing position with moves");
        engine.setPosition("e2e4", "e7e5", "g1f3");
        assertTrue(engine.isAlive(), "Engine should be alive after setting position with moves");
    }

    @Test
    public void testGetBestMoveFromStartingPosition() throws Exception {
        LOGGER.info("[DEBUG_LOG] Testing best move from starting position");
        engine.setPosition();
        String bestMove = engine.getBestMove(5000, 0);
        
        assertNotNull(bestMove, "Best move should not be null");
        assertTrue(bestMove.length() >= 4 && bestMove.length() <= 5, 
                   "Best move should be 4 or 5 characters (e.g., e2e4 or e7e8q)");
        LOGGER.info("[DEBUG_LOG] Best move from starting position: {}", bestMove);
    }

    @Test
    public void testGetBestMoveAfterE4() throws Exception {
        LOGGER.info("[DEBUG_LOG] Testing best move after 1.e4");
        engine.setPosition("e2e4");
        String bestMove = engine.getBestMove(5000, 0);
        
        assertNotNull(bestMove, "Best move should not be null");
        assertTrue(bestMove.length() >= 4 && bestMove.length() <= 5, 
                   "Best move should be 4 or 5 characters");
        LOGGER.info("[DEBUG_LOG] Best move after 1.e4: {}", bestMove);
    }

    @Test
    public void testMultipleMoves() throws Exception {
        LOGGER.info("[DEBUG_LOG] Testing multiple move requests");
        
        // First move
        engine.setPosition();
        String move1 = engine.getBestMove(3000, 0);
        assertNotNull(move1, "First move should not be null");
        LOGGER.info("[DEBUG_LOG] Move 1: {}", move1);
        
        // Second move
        engine.setPosition(move1, "e7e5");
        String move2 = engine.getBestMove(3000, 0);
        assertNotNull(move2, "Second move should not be null");
        LOGGER.info("[DEBUG_LOG] Move 2: {}", move2);
    }
}
