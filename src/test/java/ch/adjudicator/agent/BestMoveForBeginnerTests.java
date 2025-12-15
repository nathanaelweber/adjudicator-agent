package ch.adjudicator.agent;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for simple chess tactics commonly found in beginner chess books.
 * These tests verify that the engine can recognize and execute basic tactical patterns:
 * - Forks (knight, queen)
 * - Pins (bishop, rook)
 * - Skewers
 * - Back rank mates
 * - Simple checkmates
 * - Winning hanging pieces
 * - Basic pawn promotion tactics
 */
class BestMoveForBeginnerTests {

    private BestMoveCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new BestMoveCalculator();
    }

    @Test
    void testKnightForkKingAndRook() throws Exception {
        // Knight fork: white knight can capture undefended rook
        Board board = new Board();
        board.loadFromFen("8/r7/2k5/8/2N5/8/8/4K3 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find tactic to win rook");
        // Knight should capture the rook or create winning position
        assertEquals("C4", bestMove.getFrom().toString().toUpperCase(), "Knight should move to win material");
    }

    @Test
    void testKnightForkQueenAndKing() throws Exception {
        // Knight fork: knight can capture undefended queen
        Board board = new Board();
        board.loadFromFen("8/8/2q5/3k4/3N4/8/4P3/4K3 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find tactic to win queen");
        // Knight should move from d4 to capture or attack queen
        assertEquals("D4", bestMove.getFrom().toString().toUpperCase(),
                "Knight should move from d4 to win queen");
    }

    @Test
    void testBishopPinToKing() throws Exception {
        // Bishop pins knight to king: white can capture the pinned knight
        // Black knight on e5 is pinned to king on e8 by white bishop on a1
        Board board = new Board();
        board.loadFromFen("4k3/8/8/4n3/8/8/8/B3K3 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 2000);
        
        assertNotNull(bestMove, "Should recognize pinned piece vulnerability");
        // Bishop should capture the pinned knight
        assertEquals("A1E5", bestMove.toString().toUpperCase(),
                "Bishop should capture pinned knight");
    }

    @Test
    void testRookPinQueenToKing() throws Exception {
        // Rook pins queen to king on same file
        // Black queen on d6 pinned to king on d8 by white rook on d1
        Board board = new Board();
        board.loadFromFen("3k4/8/3q4/8/8/8/8/3R3K w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 2000);
        
        assertNotNull(bestMove, "Should find pinned queen");
        assertEquals("D1D6", bestMove.toString().toUpperCase(),
                "Rook should capture pinned queen");
    }

    @Test
    void testBackRankMate() throws Exception {
        // Back rank mate: rook delivers checkmate
        Board board = new Board();
        board.loadFromFen("6k1/5ppp/8/8/8/8/8/4R1K1 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find back rank mate");
        // Rook should deliver checkmate on 8th rank
        assertEquals("E1", bestMove.getFrom().toString().toUpperCase(),
                "Rook should move from e1");
        assertEquals("E8", bestMove.getTo().toString().toUpperCase(),
                "Rook should deliver mate on e8");
    }

    @Test
    void testQueenAndKingMatePattern() throws Exception {
        // Queen mate: queen delivers checkmate
        Board board = new Board();
        board.loadFromFen("7k/8/5K2/8/8/8/6Q1/8 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find queen checkmate");
        // Queen should move to deliver checkmate
        assertEquals("G2", bestMove.getFrom().toString().toUpperCase(),
                "Queen should move from g2 to deliver mate");
    }

    @Test
    void testWinHangingQueen() throws Exception {
        // Simple tactic: capture undefended queen
        // Black queen on d5 is hanging (undefended)
        Board board = new Board();
        board.loadFromFen("4k3/8/8/3q4/8/3B4/8/4K3 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find hanging queen");
        // Bishop should capture the hanging queen
        assertEquals("D3D5", bestMove.toString().toUpperCase(),
                "Bishop should capture queen on d5");
    }

    @Test
    void testWinHangingRook() throws Exception {
        // Simple tactic: capture undefended rook
        Board board = new Board();
        board.loadFromFen("4k3/8/8/8/4r3/8/4N3/4K3 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find hanging rook");
        // Knight should capture the hanging rook
        assertEquals("E2E4", bestMove.toString().toUpperCase(),
                "Knight should capture rook on e4");
    }

    @Test
    void testSkewerKingAndQueen() throws Exception {
        // Skewer: rook attacks king, king moves, then rook captures queen
        // White rook on a1, black king on a8, black queen on a5
        Board board = new Board();
        board.loadFromFen("k7/8/8/q7/8/8/8/R3K3 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 2000);
        
        assertNotNull(bestMove, "Should find skewer");
        assertEquals("A1A5", bestMove.toString().toUpperCase(),
                "Rook should attack/capture queen creating skewer threat");
    }

    @Test
    void testPawnPromotionTactic() throws Exception {
        // Pawn promotion: white pawn on a7 promotes to queen
        Board board = new Board();
        board.loadFromFen("8/P7/8/8/8/k7/8/K7 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find pawn promotion");
        // Pawn should move from a7 to a8 (promotion)
        assertEquals("A7", bestMove.getFrom().toString().toUpperCase(),
                "Pawn should move from a7");
        assertEquals("A8", bestMove.getTo().toString().toUpperCase(),
                "Pawn should promote to a8");
    }

    @Test
    void testSimpleForkWithQueen() throws Exception {
        // Queen fork: queen can attack both rook and bishop simultaneously
        Board board = new Board();
        board.loadFromFen("4k3/8/1r3b2/8/3Q4/8/8/4K3 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 2000);
        
        assertNotNull(bestMove, "Should find queen fork");
        // Queen should move to attack both pieces, most likely capturing one
        assertEquals("D4", bestMove.getFrom().toString().toUpperCase(),
                "Queen should move from d4 to fork or capture");
    }

    @Test
    void testDiscoveredAttack() throws Exception {
        // Discovered attack: bishop on c1 discovers attack on queen at d8 when knight moves
        // White knight on d4 can move, discovering bishop attack on black queen
        Board board = new Board();
        board.loadFromFen("3q3k/8/8/8/3N4/8/8/2B1K3 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 2000);
        
        assertNotNull(bestMove, "Should find discovered attack");
        // Knight should move away discovering bishop attack on queen
        assertEquals("D4", bestMove.getFrom().toString().toUpperCase(),
                "Knight should move to discover attack on queen");
    }

    @Test
    void testRemoveDefender() throws Exception {
        // Remove the defender: capture the piece defending another valuable piece
        // Black rook on e8 defends queen on e5. White rook can capture defender.
        Board board = new Board();
        board.loadFromFen("4r2k/8/8/4q3/8/8/8/4R2K w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find removing defender tactic");
        // Rook should capture the defender rook or queen
        assertEquals("E1", bestMove.getFrom().toString().toUpperCase(),
                "Rook should move from e1");
        assertTrue(bestMove.getTo().toString().toUpperCase().equals("E8") ||
                   bestMove.getTo().toString().toUpperCase().equals("E5"),
                "Rook should capture defender or queen");
    }

    @Test
    void testDoubleAttack() throws Exception {
        // Double attack: one piece attacks two targets simultaneously
        // White queen can move to attack both black rook and bishop
        Board board = new Board();
        board.loadFromFen("4k3/8/8/b6r/8/2Q5/8/4K3 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 2000);
        
        assertNotNull(bestMove, "Should find double attack");
        assertEquals("C3", bestMove.getFrom().toString().toUpperCase(),
                "Queen should move from c3 to create double attack");
    }

    @Test
    void testSimpleCheckmate_TwoRooks() throws Exception {
        // Ladder mate: two rooks deliver checkmate
        Board board = new Board();
        board.loadFromFen("7k/8/6R1/8/8/8/8/5R1K w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find ladder mate");
        // One of the rooks should move to deliver checkmate on 8th rank
        assertTrue(bestMove.getFrom().toString().toUpperCase().equals("G6") || 
                   bestMove.getFrom().toString().toUpperCase().equals("F1"),
                "One of the rooks should move");
        // Checkmate should be on the 8th rank
        assertTrue(bestMove.getTo().toString().toUpperCase().contains("8"),
                "Rook should move to 8th rank for mate");
    }

    @Test
    void testCaptureAttackerToSaveMaterial() throws Exception {
        // Counter-attack: instead of moving the attacked piece, capture the attacker
        // Black bishop attacks white queen, but white can capture the bishop with queen
        Board board = new Board();
        board.loadFromFen("4k3/8/8/8/2b5/3Q4/8/4K3 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 2000);
        
        assertNotNull(bestMove, "Should find counter-attack");
        assertEquals("D3C4", bestMove.toString().toUpperCase(),
                "Queen should capture attacking bishop");
    }
}
