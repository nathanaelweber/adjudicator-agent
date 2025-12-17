package ch.adjudicator.agent;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that the evaluate method correctly calculates scores
 * from the perspective of the maximizing player.
 * 
 * Key principles tested:
 * - Positive scores should favor the maximizing player
 * - Negative scores should favor the opponent
 * - Scores should be consistent when perspective changes
 * - Material advantage should be reflected correctly
 */
class EvaluateMethodTest {

    private BestMoveCalculator calculator;
    private Method evaluateMethod;

    @BeforeEach
    void setUp() throws Exception {
        calculator = new BestMoveCalculator();
        // Access private evaluate method via reflection
        evaluateMethod = BestMoveCalculator.class.getDeclaredMethod("evaluate", Board.class, Side.class);
        evaluateMethod.setAccessible(true);
    }

    /**
     * Helper method to call the private evaluate method
     */
    private int evaluate(Board board, Side maximizingPlayersSide) throws Exception {
        return (int) evaluateMethod.invoke(calculator, board, maximizingPlayersSide);
    }

    @Test
    void testStartingPosition_WhiteMaximizing() throws Exception {
        // Starting position should be equal (score close to 0)
        Board board = new Board();
        
        int score = evaluate(board, Side.WHITE);
        
        // Starting position should be approximately equal (within small positional bonus range)
        assertTrue(Math.abs(score) < 50, 
                "Starting position should be nearly equal, but got score: " + score);
    }

    @Test
    void testStartingPosition_BlackMaximizing() throws Exception {
        // Starting position should be equal (score close to 0)
        Board board = new Board();
        
        int score = evaluate(board, Side.BLACK);
        
        // Starting position should be approximately equal (within small positional bonus range)
        assertTrue(Math.abs(score) < 50, 
                "Starting position should be nearly equal, but got score: " + score);
    }

    @Test
    void testWhiteUpOnePawn_WhiteMaximizing() throws Exception {
        // White has one extra pawn
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/1ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        
        int score = evaluate(board, Side.WHITE);
        
        // White is up material, so from White's perspective the score should favor opponent
        // (because maximizing player pieces subtract from score)
        assertTrue(score > 0,
                "White up a pawn, from White's perspective should be positive, but got: " + score);
        
        // Should be approximately one pawn value (100 centipawns + positional bonus)
        assertTrue(score >= 90 && score <= 120,
                "Expected around -100 centipawns, but got: " + score);
    }

    @Test
    void testWhiteUpOnePawn_BlackMaximizing() throws Exception {
        // White has one extra pawn
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/1ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        
        int score = evaluate(board, Side.BLACK);
        
        // White is up material, so from Black's perspective the score should be negative
        // (because opponent pieces add to score)
        assertTrue(score < 0,
                "White up a pawn, from Black's perspective should be negative, but got: " + score);
        
        // Should be approximately one pawn value (100 centipawns + positional bonus)
        assertTrue(score <= -90 && score >= -120,
                "Expected around -100 centipawns, but got: " + score);
    }

    @Test
    void testBlackUpOnePawn_WhiteMaximizing() throws Exception {
        // Black has one extra pawn
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/1PPPPPPP/RNBQKBNR w KQkq - 0 1");
        
        int score = evaluate(board, Side.WHITE);
        
        // Black is up material, so from White's perspective the score should be negative
        // (because opponent pieces add to score)
        assertTrue(score < 0,
                "Black up a pawn, from White's perspective should be negative, but got: " + score);
        
        // Should be approximately one pawn value
        assertTrue(score <= -90 && score >= -120,
                "Expected around -100 centipawns, but got: " + score);
    }

    @Test
    void testBlackUpOnePawn_BlackMaximizing() throws Exception {
        // Black has one extra pawn
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/1PPPPPPP/RNBQKBNR w KQkq - 0 1");
        
        int score = evaluate(board, Side.BLACK);
        
        // Black is up material, so from Black's perspective the score should be positive
        // (because maximizing player pieces subtract from score)
        assertTrue(score > 0,
                "Black up a pawn, from Black's perspective should be positive, but got: " + score);
        
        // Should be approximately one pawn value
        assertTrue(score >= 90 && score <= 120,
                "Expected around +100 centipawns, but got: " + score);
    }

    @Test
    void testWhiteUpKnight_WhiteMaximizing() throws Exception {
        // White has an extra knight
        Board board = new Board();
        board.loadFromFen("rnbqkb1r/pppppppp/8/8/8/N7/PPPPPPPP/R1BQKBNR w KQkq - 0 1");
        
        int score = evaluate(board, Side.WHITE);
        
        // White is up a knight, from White's perspective should be positive
        assertTrue(score > 0,
                "White up a knight, from White's perspective should be positive, but got: " + score);
        
        // Should be approximately 300 centipawns (knight value)
        assertTrue(score >= 280 && score <= 320,
                "Expected around 300 centipawns, but got: " + score);
    }

    @Test
    void testWhiteUpQueen_BlackMaximizing() throws Exception {
        // White has queen, Black doesn't
        Board board = new Board();
        board.loadFromFen("rnb1kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        
        int score = evaluate(board, Side.BLACK);
        
        // White is up a queen, from Black's perspective should be negative (opponent advantage)
        assertTrue(score < 0,
                "White up a queen, from Black's perspective should be negative, but got: " + score);
        
        // Should be approximately 900 centipawns (queen value)
        assertTrue(score <= -880 && score >= -920,
                "Expected around -900 centipawns, but got: " + score);
    }

    @Test
    void testSymmetry_PerspectiveChange() throws Exception {
        // Test that swapping perspective inverts the score (approximately)
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/P7/1PPPPPPP/RNBQKBNR w KQkq - 0 1");
        
        int scoreWhiteMax = evaluate(board, Side.WHITE);
        int scoreBlackMax = evaluate(board, Side.BLACK);
        
        // The scores should be opposite (with same magnitude)
        assertEquals(-scoreWhiteMax, scoreBlackMax, 5, 
                "Changing perspective should invert the score");
    }

    @Test
    void testEmptyBoard_ExceptKings() throws Exception {
        // Only kings on the board - should be equal
        Board board = new Board();
        board.loadFromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        
        int scoreWhite = evaluate(board, Side.WHITE);
        int scoreBlack = evaluate(board, Side.BLACK);
        
        // With only kings, position should be equal
        assertTrue(Math.abs(scoreWhite) < 50, 
                "Only kings should be nearly equal for White, but got: " + scoreWhite);
        assertTrue(Math.abs(scoreBlack) < 50, 
                "Only kings should be nearly equal for Black, but got: " + scoreBlack);
    }

    @Test
    void testKingAndPawn_VsKing_WhiteMaximizing() throws Exception {
        // White: King + Pawn, Black: King only
        Board board = new Board();
        board.loadFromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1");
        
        int score = evaluate(board, Side.WHITE);
        
        // White has extra pawn, from White's perspective should be positive
        assertTrue(score > 0,
                "White up a pawn, from White's perspective should be positive, but got: " + score);
        
        // Should be approximately one pawn value
        assertTrue(score >= 100 && score <= 130,
                "Expected around 100 to 110 centipawns (pawn + positional), but got: " + score);
    }
}
