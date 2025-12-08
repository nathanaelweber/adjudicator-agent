package ch.adjudicator.agent;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BestMoveCalculatorTest {

    private BestMoveCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new BestMoveCalculator();
    }

    @Test
    void testComputeBestMove_StartingPosition() throws Exception {
        // Test from starting position - no immediate material gains
        Board board = new Board();
        
        Move bestMove = calculator.computeBestMove(board, 1000);
        
        assertNotNull(bestMove, "Should return a move from starting position");
        assertTrue(board.legalMoves().contains(bestMove), "Returned move should be legal");
    }

    @Test
    void testComputeBestMove_CaptureAvailable() throws Exception {
        // Position where black can capture a white pawn
        // Setup: White pawn on e4, black pawn can capture from d5
        Board board = new Board();
        board.loadFromFen("4k3/8/8/3p4/4P3/8/8/4K3 w KQkq d6 0 2");
        
        // Make white move first (e4 is already there, just move to get black's turn)
        board.loadFromFen("4k3/8/8/3p4/4P3/8/8/4K3 w KQkq - 0 2");
        
        Move bestMove = calculator.computeBestMove(board, 1000);
        
        assertEquals("DXE4", bestMove.toString().toUpperCase(), "Should find simple caputre for black");
        // Black should consider capturing the pawn on e4 with dxe4
    }

    @Test
    void testComputeBestMove_QueenCapture() throws Exception {
        // Position where a queen can be captured
        // White queen on e4, black pawn on d5 can capture it
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/ppp1pppp/8/3p4/4Q3/8/PPPPPPPP/RNB1KBNR b KQkq - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 1000);
        
        assertNotNull(bestMove, "Should find best move for black");
        // The best material move should be capturing the queen with dxe4
        assertEquals("E4", bestMove.getTo().toString().toUpperCase(), 
                "Should capture the queen on e4");
    }

    @Test
    void testComputeBestMove_NoLegalMoves() {
        // Stalemate position - no legal moves
        Board board = new Board();
        board.loadFromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        
        // computeBestMove throws exception when no legal moves, not return null
        assertThrows(Exception.class, () -> calculator.computeBestMove(board, 1000),
                "Should throw exception when no legal moves available");
    }

    @Test
    void testComputeBestMove_MultipleCaptures() throws Exception {
        // Position with multiple capture options
        // Black pawn on d5 can capture white pawn on c4 or white rook on e4
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/ppp1pppp/8/3p4/2P1R3/8/PP1PPPPP/RNBQKBN1 b Qkq - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 1000);
        
        assertNotNull(bestMove, "Should find best move for black");
        // Should prefer capturing the rook (500 centipawns) over pawn (100 centipawns)
        assertEquals("E4", bestMove.getTo().toString().toUpperCase(), 
                "Should capture the rook on e4 (500) rather than pawn on c4 (100)");
    }

    @Test
    void testComputeBestMove_PromotionMove() throws Exception {
        // Position where promotion is available
        Board board = new Board();
        // Black pawn on h2 can promote
        board.loadFromFen("4k3/8/8/8/8/8/7p/4K3 b - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 1000);
        
        assertNotNull(bestMove, "Should find best move for black");
        // Promotion should be highly valued from material perspective
    }

}
