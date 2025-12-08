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
        board.loadFromFen("4k3/8/8/3p4/4P3/8/8/4K3 b KQkq - 0 2");
        
        Move bestMove = calculator.computeBestMove(board, 1000);
        
        assertEquals("D5E4", bestMove.toString().toUpperCase(), "Should find simple capture for black");
        // Black should consider capturing the pawn on e4 with d5xe4
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

    @Test
    void testComputeBestMove_SaveQueenFromCapture() throws Exception {
        // Position where black queen is under attack and must move
        // White rook on e1 attacks black queen on e8
        Board board = new Board();
        board.loadFromFen("4q2k/8/8/8/8/8/8/4R2K b - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 1000);
        
        assertNotNull(bestMove, "Should find a move to save the queen");
        // Queen should move away from e8 to avoid capture
        assertEquals("E8", bestMove.getFrom().toString().toUpperCase(), 
                "Should move the queen away from danger");
    }

    @Test
    void testComputeBestMove_SaveQueenFromCaptureAvoidingRecapture() throws Exception {
        // Position where black queen is under attack and must move
        // White rook on e1 attacks black queen on e8
        Board board = new Board();
        board.loadFromFen("6rk/pp1p1ppp/3q4/4P3/8/8/6PP/3RR2K b - - 0 1");

        Move bestMove = calculator.computeBestMove(board, 10000);

        assertNotNull(bestMove, "Should find a move to save the queen");
        // Queen should move away from e8 to avoid capture
        assertEquals("D6", bestMove.getFrom().toString().toUpperCase(),
                "Should move the queen away from danger");
        assertNotEquals("D6E5", bestMove.toString().toUpperCase(),
                "Should move the queen to a safe spot, capture threatens recapture");
        assertFalse(bestMove.toString().toUpperCase().startsWith("D6D"),
                "Should move the queen to a safe spot, staying on d-file is threatened from the rook on d1, and rook on d1 is protected by rook on e1.");
    }

    @Test
    void testComputeBestMove_SavePieceUnderThreat() throws Exception {
        // Position where black knight on f6 is attacked by white pawn on e5
        // Black should move the knight to safety
        Board board = new Board();
        board.loadFromFen("4k3/8/5n2/4P3/8/8/8/4K3 b - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 1000);
        
        assertNotNull(bestMove, "Should find a move to save the knight");
        // Knight should move away from f6
        assertEquals("F6", bestMove.getFrom().toString().toUpperCase(), 
                "Should move the threatened knight");
    }

    @Test
    void testComputeBestMove_SaveRookVsCapturePawn() throws Exception {
        // Black rook on d8 is attacked by white queen on d1
        // Black can either save the rook or capture a white pawn on h2
        // Saving the rook (500) should be prioritized over capturing pawn (100)
        Board board = new Board();
        board.loadFromFen("3r3k/8/8/8/8/8/7P/3Q3K b - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 1000);
        
        assertNotNull(bestMove, "Should find best defensive move");
        // Should prioritize saving the rook (worth 500) over capturing pawn (worth 100)
        assertEquals("D8", bestMove.getFrom().toString().toUpperCase(), 
                "Should move the threatened rook to safety");
    }

    @Test
    void testComputeBestMove_MultiplePiecesThreatenedSaveHigherValue() throws Exception {
        // Black knight on f6 (300) and black bishop on c6 (300) both under attack
        // White pawn on e5 attacks knight, white pawn on b5 attacks bishop
        // Both pieces need to move, but bot can only move one per turn
        // Should save either piece (both equal value)
        Board board = new Board();
        board.loadFromFen("4k3/8/2b2n2/1P2P3/8/8/8/4K3 b - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 1000);
        
        assertNotNull(bestMove, "Should find a move to save one of the threatened pieces");
        // Should move either the knight or bishop to safety
        assertTrue(bestMove.getFrom().toString().toUpperCase().equals("F6") || 
                   bestMove.getFrom().toString().toUpperCase().equals("C6"),
                "Should move one of the threatened pieces (knight or bishop)");
    }

}
