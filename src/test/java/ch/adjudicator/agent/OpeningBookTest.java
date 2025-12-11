package ch.adjudicator.agent;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class OpeningBookTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpeningBookTest.class);

    @Test
    public void testBestMoveCalculatorInitialization() {
        // Test that BestMoveCalculator can be instantiated
        // It should load the opening book if available
        BestMoveCalculator calculator = new BestMoveCalculator();
        assertNotNull(calculator);
    }

    @Test
    public void testOpeningMoveFromStartPosition() throws Exception {
        // Test that BestMoveCalculator returns a legal move from the start position
        BestMoveCalculator calculator = new BestMoveCalculator();
        Board board = new Board();
        
        Move move = calculator.computeBestMove(board, 60000);
        
        assertNotNull(move, "Move should not be null");
        assertTrue(board.legalMoves().contains(move), "Move should be legal");
        
        LOGGER.info("[DEBUG_LOG] Opening move from start position: {}", move.toString().toLowerCase());
    }

    @Test
    public void testOpeningMovesSequence() throws Exception {
        // Test a sequence of opening moves
        BestMoveCalculator calculator = new BestMoveCalculator();
        Board board = new Board();
        
        // First move
        Move move1 = calculator.computeBestMove(board, 60000);
        assertNotNull(move1, "First move should not be null");
        assertTrue(board.legalMoves().contains(move1), "First move should be legal");
        board.doMove(move1);
        LOGGER.info("[DEBUG_LOG] Move 1: {}", move1.toString().toLowerCase());
        
        // Second move (opponent's turn - simulate)
        Move move2 = calculator.computeBestMove(board, 60000);
        assertNotNull(move2, "Second move should not be null");
        assertTrue(board.legalMoves().contains(move2), "Second move should be legal");
        board.doMove(move2);
        LOGGER.info("[DEBUG_LOG] Move 2: {}", move2.toString().toLowerCase());
        
        // Third move
        Move move3 = calculator.computeBestMove(board, 60000);
        assertNotNull(move3, "Third move should not be null");
        assertTrue(board.legalMoves().contains(move3), "Third move should be legal");
        LOGGER.info("[DEBUG_LOG] Move 3: {}", move3.toString().toLowerCase());
    }

    @Test
    public void testOutOfBookPosition() throws Exception {
        // Test that calculator handles positions not in the opening book
        BestMoveCalculator calculator = new BestMoveCalculator();
        Board board = new Board();
        
        // Play several moves to likely get out of book
        for (int i = 0; i < 20; i++) {
            Move move = calculator.computeBestMove(board, 60000);
            assertNotNull(move, "Move should not be null even out of book");
            assertTrue(board.legalMoves().contains(move), "Move should be legal");
            board.doMove(move);
        }
        
        LOGGER.info("[DEBUG_LOG] Successfully handled out-of-book positions after 20 moves");
    }

    @Test
    public void testBothSidesUseOpeningBook() throws Exception {
        // Test that both sides use opening book for at least 7 moves each
        BestMoveCalculator whiteCalculator = new BestMoveCalculator();
        BestMoveCalculator blackCalculator = new BestMoveCalculator();
        Board board = new Board();
        
        // Verify opening book is loaded
        assertNotNull(whiteCalculator.getOpeningBook(), "White should have opening book loaded");
        assertNotNull(blackCalculator.getOpeningBook(), "Black should have opening book loaded");
        
        int whiteBookMoves = 0;
        int blackBookMoves = 0;
        int moveNumber = 1;
        
        // Play moves alternating between white and black
        for (int i = 0; i < 30; i++) {
            boolean isWhiteTurn = (i % 2 == 0);
            BestMoveCalculator calculator = isWhiteTurn ? whiteCalculator : blackCalculator;
            
            Move move = calculator.computeBestMove(board, 60000);
            assertNotNull(move, "Move should not be null");
            assertTrue(board.legalMoves().contains(move), "Move should be legal");
            
            // Check if the move was from book after computation
            boolean wasFromBook = calculator.wasLastMoveFromBook();
            
            // Track book moves
            if (wasFromBook) {
                if (isWhiteTurn) {
                    whiteBookMoves++;
                    LOGGER.info("[DEBUG_LOG] Move {}: White played book move {} (total white book moves: {})", 
                               moveNumber, move.toString().toLowerCase(), whiteBookMoves);
                } else {
                    blackBookMoves++;
                    LOGGER.info("[DEBUG_LOG] Move {}: Black played book move {} (total black book moves: {})", 
                               moveNumber, move.toString().toLowerCase(), blackBookMoves);
                }
            } else {
                LOGGER.info("[DEBUG_LOG] Move {}: {} out of book, played {}", 
                           moveNumber, isWhiteTurn ? "White" : "Black", move.toString().toLowerCase());
            }
            
            board.doMove(move);
            
            if (!isWhiteTurn) {
                moveNumber++;
            }
            
            // Stop if both sides have gone out of book and we have enough book moves
            if (!wasFromBook && whiteBookMoves >= 7 && blackBookMoves >= 7) {
                break;
            }
        }
        
        LOGGER.info("[DEBUG_LOG] Total white book moves: {}, Total black book moves: {}", whiteBookMoves, blackBookMoves);
        
        assertTrue(whiteBookMoves >= 7, 
                  "White should have played at least 7 opening book moves, but only played " + whiteBookMoves);
        assertTrue(blackBookMoves >= 7, 
                  "Black should have played at least 7 opening book moves, but only played " + blackBookMoves);
    }
}
