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
 * - Positive scores should favor the on turn player
 * - Negative scores should favor the opponent of the one player that has the turn
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
        evaluateMethod = BestMoveCalculator.class.getDeclaredMethod("evaluate", Board.class);
        evaluateMethod.setAccessible(true);
    }

    /**
     * Helper method to call the private evaluate method
     */
    private int evaluate(Board board) throws Exception {
        return (int) evaluateMethod.invoke(calculator, board);
    }

    @Test
    void testStartingPosition_WhiteOnTurn() throws Exception {
        // Starting position should be equal (score close to 0)
        Board board = new Board();
        
        int score = evaluate(board);
        
        // Starting position should be approximately equal (within small positional bonus range)
        assertTrue(Math.abs(score) < 50, 
                "Starting position should be nearly equal, but got score: " + score);
    }

    @Test
    void testStartingPosition_BlackOnTurn() throws Exception {
        // Starting position should be equal (score close to 0)
        Board board = new Board();
        board.doMove("e2e4");
        
        int score = evaluate(board);
        
        // After first move when black is on turn should be approximately equal (within small positional bonus range)
        assertTrue(Math.abs(score) < 50, 
                "Starting position should be nearly equal, but got score: " + score);
    }

    @Test
    void testWhiteUpOnePawn_WhiteOnTurn() throws Exception {
        // White has one extra pawn
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/1ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        
        int score = evaluate(board);
        
        // White is up material, so from White's perspective the score should favor opponent
        // (because maximizing player pieces subtract from score)
        assertTrue(score > 0,
                "White up a pawn, from White's perspective should be positive, but got: " + score);
        
        // Should be approximately one pawn value (100 centipawns + positional bonus)
        assertTrue(score >= 90 && score <= 120,
                "Expected around -100 centipawns, but got: " + score);
    }

    @Test
    void testWhiteUpOnePawn_BlackOnTurn() throws Exception {
        // White has one extra pawn
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/1ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1");
        
        int score = evaluate(board);
        
        // White is up material, so from Black's perspective the score should be negative
        // (because opponent pieces add to score)
        assertTrue(score < 0,
                "White up a pawn, from Black's perspective should be negative, but got: " + score);
        
        // Should be approximately one pawn value (100 centipawns + positional bonus)
        assertTrue(score <= -90 && score >= -120,
                "Expected around -100 centipawns, but got: " + score);
    }

    @Test
    void testBlackUpOnePawn_WhiteOnTurn() throws Exception {
        // Black has one extra pawn
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/1PPPPPPP/RNBQKBNR w KQkq - 0 1");
        
        int score = evaluate(board);
        
        // Black is up material, so from White's perspective the score should be negative
        // (because opponent pieces add to score)
        assertTrue(score < 0,
                "Black up a pawn, from White's perspective should be negative, but got: " + score);
        
        // Should be approximately one pawn value
        assertTrue(score <= -90 && score >= -120,
                "Expected around -100 centipawns, but got: " + score);
    }

    @Test
    void testBlackUpOnePawn_BlackOnTurn() throws Exception {
        // Black has one extra pawn
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/1PPPPPPP/RNBQKBNR b KQkq - 0 1");
        
        int score = evaluate(board);
        
        // Black is up material, so from Black's perspective the score should be positive
        // (because maximizing player pieces subtract from score)
        assertTrue(score > 0,
                "Black up a pawn, from Black's perspective should be positive, but got: " + score);
        
        // Should be approximately one pawn value
        assertTrue(score >= 90 && score <= 120,
                "Expected around +100 centipawns, but got: " + score);
    }

    @Test
    void testWhiteUpKnight_WhiteOnTurn() throws Exception {
        // White has an extra knight
        Board board = new Board();
        board.loadFromFen("rnbqkb1r/pppppppp/8/8/8/N7/PPPPPPPP/R1BQKBNR w KQkq - 0 1");
        
        int score = evaluate(board);
        
        // White is up a knight, from White's perspective should be positive
        assertTrue(score > 0,
                "White up a knight, from White's perspective should be positive, but got: " + score);
        
        // Should be approximately 300 centipawns (knight value)
        assertTrue(score >= 280 && score <= 320,
                "Expected around 300 centipawns, but got: " + score);
    }

    @Test
    void testWhiteUpQueen_BlackOnTurn() throws Exception {
        // White has queen, Black doesn't
        Board board = new Board();
        board.loadFromFen("rnb1kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1");
        
        int score = evaluate(board);
        
        // White is up a queen, from Black's perspective should be negative (opponent advantage)
        assertTrue(score < 0,
                "White up a queen, from Black's perspective should be negative, but got: " + score);
        
        // Should be approximately 900 centipawns (queen value)
        assertTrue(score <= -880 && score >= -920,
                "Expected around -900 centipawns, but got: " + score);
    }

    @Test
    void testEmptyBoard_ExceptKings() throws Exception {
        // Only kings on the board - should be equal
        Board board = new Board();
        board.loadFromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1");
        
        int scoreWhite = evaluate(board);
        int scoreBlack = evaluate(board);
        
        // With only kings, position should be equal
        assertTrue(Math.abs(scoreWhite) < 50, 
                "Only kings should be nearly equal for White, but got: " + scoreWhite);
        assertTrue(Math.abs(scoreBlack) < 50, 
                "Only kings should be nearly equal for Black, but got: " + scoreBlack);
    }

    @Test
    void testKingAndPawn_VsKing_WhiteOnTurn() throws Exception {
        // White: King + Pawn, Black: King only
        Board board = new Board();
        board.loadFromFen("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1");
        
        int score = evaluate(board);
        
        // White has extra pawn, from White's perspective should be positive
        assertTrue(score > 0,
                "White up a pawn, from White's perspective should be positive, but got: " + score);
        
        // Should be approximately one pawn value
        assertTrue(score >= 100 && score <= 130,
                "Expected around 100 to 110 centipawns (pawn + positional), but got: " + score);
    }
}
