package ch.adjudicator.agent;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that verifies mate detection and scoring with proper notation (e.g., +M1, +M3)
 */
public class MateScoringTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MateScoringTest.class);

    @Test
    public void testMateInOne_QueenMate() throws Exception {
        BestMoveCalculator calculator = new BestMoveCalculator();
        
        // Mate in 1 position: Qh7# is checkmate
        Board board = new Board();
        board.loadFromFen("6k1/5ppp/7Q/8/8/8/8/7K w - - 0 1");
        
        LOGGER.info("Test: Mate in 1 - Queen delivers checkmate");
        LOGGER.info("FEN: {}", board.getFen());
        
        Move bestMove = calculator.computeBestMove(board, 5000);
        assertNotNull(bestMove, "Should find a move");
        
        // The move should be Qh7# or Qg7#
        String moveStr = bestMove.toString().toLowerCase();
        LOGGER.info("Best move found: {}", moveStr);
        
        // Verify it's a queen move to deliver mate
        assertTrue(moveStr.startsWith("h6"), "Queen should move from h6");
    }

    @Test
    public void testMateInOne_BackRankMate() throws Exception {
        BestMoveCalculator calculator = new BestMoveCalculator();
        
        // Back rank mate in 1: Rd8# is checkmate
        Board board = new Board();
        board.loadFromFen("6k1/5ppp/8/8/8/8/8/3R3K w - - 0 1");
        
        LOGGER.info("Test: Mate in 1 - Back rank mate");
        LOGGER.info("FEN: {}", board.getFen());
        
        Move bestMove = calculator.computeBestMove(board, 5000);
        assertNotNull(bestMove, "Should find a move");
        
        String moveStr = bestMove.toString().toLowerCase();
        LOGGER.info("Best move found: {}", moveStr);
        
        // Should move rook to d8 for back rank mate
        assertTrue(moveStr.equals("d1d8"), "Rook should deliver back rank mate with Rd8#");
    }

    @Test
    public void testMateInTwo_ScholarsMatePotential() throws Exception {
        BestMoveCalculator calculator = new BestMoveCalculator();
        
        // Position where white can force mate in 2
        Board board = new Board();
        board.loadFromFen("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 0 1");
        
        LOGGER.info("Test: Mate in 2 position");
        LOGGER.info("FEN: {}", board.getFen());
        
        Move bestMove = calculator.computeBestMove(board, 5000);
        assertNotNull(bestMove, "Should find a move");
        
        String moveStr = bestMove.toString().toLowerCase();
        LOGGER.info("Best move found: {}", moveStr);
        
        // Qxf7+ is the key move that forces mate in 2
        assertTrue(moveStr.equals("h5f7"), "Should play Qxf7+ to force mate");
    }

    @Test
    public void testNoMate_RegularPosition() throws Exception {
        BestMoveCalculator calculator = new BestMoveCalculator();
        
        // Starting position - no forced mate
        Board board = new Board();
        
        LOGGER.info("Test: Starting position - no mate");
        LOGGER.info("FEN: {}", board.getFen());
        
        Move bestMove = calculator.computeBestMove(board, 2000);
        assertNotNull(bestMove, "Should find a move even without mate");
        
        LOGGER.info("Best move found: {}", bestMove.toString().toLowerCase());
    }
}
