package ch.adjudicator.agent;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for queen safety scenarios from actual chess games and instructional books.
 * 
 * Sources:
 * - "My System" by Aron Nimzowitsch (queen development principles)
 * - "Logical Chess Move by Move" by Irving Chernev (queen safety in tactical positions)
 * - "Chess Fundamentals" by Jose Raul Capablanca (queen endgames)
 * - Classic games: Morphy, Steinitz, Lasker, Capablanca
 * 
 * Key principles tested:
 * - Don't develop queen too early (vulnerable to tempo attacks)
 * - Retreat queen when attacked by lesser pieces
 * - Avoid capturing defended pieces with the queen
 * - Don't trade queens when down material
 * - Queen should create threats while staying safe
 */
class QueenSafetyTests {

    private BestMoveCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new BestMoveCalculator();
    }

    @Test
    void testQueenRetreatFromMinorPieceAttack() throws Exception {
        // Based on common beginner mistake: queen attacked by developing knight
        // From typical Italian Game position where white queen on f3 is attacked by Nc6-d4
        // Position after: 1.e4 e5 2.Nc3 Nc6 3.Bc4 Nf6 4.Qf3 Nd4 (fork threat)
        Board board = new Board();
        board.loadFromFen("r1bqkb1r/pppp1ppp/5n2/4p3/2Bn4/5Q2/PPPP1PPP/RNB1K2R w KQkq - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000000, 0, 7);
        
        assertNotNull(bestMove, "Should find move to save queen or counter-attack");
        // Best moves: queen must protect the pawn where opponent knight would fork king and rook
        String move = bestMove.toString().toUpperCase();
        assertTrue(move.equals("F3D3") || move.equals("F3D1"),
                "Should protect pawn with queen");
    }

    @Test
    void testQueenShouldNotCaptureDefendedPawn() throws Exception {
        // Scholar's Mate attempt gone wrong - based on actual beginner games
        // After: 1.e4 e5 2.Bc4 Nc6 3.Qh5 Qe7 4.Qxf7+?? (queen takes defended pawn)
        // Position before the blunder - queen should NOT take f7
        Board board = new Board();
        board.loadFromFen("r1b1kbnr/ppppqppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find best move");
        // Queen should NOT capture f7 (defended by king and queen)
        assertNotEquals("H5F7", bestMove.toString().toUpperCase(), "Queen should not capture f7 pawn defended by king and queen");
    }

    @Test
    void testQueenPinnedToKingWithOnlyOneRookAndPawn() throws Exception {
        // queen pinned on e-file
        // Black queen can be pinned on e-file by white rook.
        Board board = new Board();
        board.loadFromFen("4k3/8/8/4q3/8/8/5KPP/5R2 w - - 0 1");

        Move bestMove = calculator.computeBestMove(board, 1000000000, 5, 5);

        assertNotNull(bestMove, "Should find best move");
        // In this position, white should pin the opponent queen with one of the ground rooks
        String move = bestMove.toString().toUpperCase();
        assertTrue("F1E1".equalsIgnoreCase(move),
                "Should pin the opponent queen with the rook, it will likely be taken but this results in a win");
    }

    @Test
    void testQueenPinnedToKingWithOnlyOneRookAndPawnDepth0To6() throws Exception {
        // queen pinned on e-file
        // Black queen can be pinned on e-file by white rook.
        Board board = new Board();
        board.loadFromFen("4k3/8/8/4q3/8/8/5KPP/5R2 w - - 0 1");

        Move bestMove = calculator.computeBestMove(board, 1000000000, 0, 6);

        assertNotNull(bestMove, "Should find best move");
        // In this position, white should pin the opponent queen with one of the ground rooks
        String move = bestMove.toString().toUpperCase();
        assertTrue("F1E1".equalsIgnoreCase(move),
                "Should pin the opponent queen with the rook, it will likely be taken but this results in a win");
    }

    @Test
    void testQueenPinnedToKingWithOtherPieces() throws Exception {
        // queen pinned on e-file
        // Black queen can be pinned on e-file by white rook.
        Board board = new Board();
        board.loadFromFen("r3k2r/pp3ppp/2p5/3pq3/2P5/8/PP1Q1PPP/R4RK1 w kq - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 30000);
        
        assertNotNull(bestMove, "Should find best move");
        // In this position, white should pin the opponent queen with one of the ground rooks
        String move = bestMove.toString().toUpperCase();
        assertTrue("A1E1".equalsIgnoreCase(move) || "F1E1".equalsIgnoreCase(move),
                "Should pin the opponent queen with one of the ground rooks");
    }

    @Test
    void testQueenVsRookEndgame() throws Exception {
        // From lichess rook endgame
        // Classic example: White has queen, black has rook
        // Can force king moving away from protection of rook to capture
        Board board = new Board();
        board.loadFromFen("1rk5/4Q3/K7/8/8/8/8/8 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 300000, 0, 7);
        
        assertNotNull(bestMove, "Should find winning move with queen");
        // Queen should improve position: Qd6+ checks king, Qd5/Qd4/Qd3 centralizes, or Qd2 attacks rook
        assertEquals("E7E5", bestMove.toString().toUpperCase(),"Queen should prepare to be able to capture rook in 1 move");
    }

    @Test
    void testQueenSacrificeForCheckmate() throws Exception {
        // From Legal's Mate: classic queen sacrifice leading to checkmate
        // After: 1.e4 e5 2.Nf3 d6 3.Bc4 Bg4 4.Nc3 g6 5.Nxe5! Bxd1 6.Bxf7+ Ke7 7.Nd5#
        // Position before the queen sacrifice - white should see the combination
        Board board = new Board();
        board.loadFromFen("rn1qkbnr/ppp2ppp/3p4/4N3/2B1P1b1/8/PPPPQPPP/RNB1K2R w KQkq - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 30000, 0,3);
        
        assertNotNull(bestMove, "Should find simple mate in one and not defend the queen");
        // Best move is Bxf7+ starting the mating attack (Legal's Mate)
        assertEquals("C4F7", bestMove.toString().toUpperCase(),
                "Should play Bxf7+ (Legal's Mate)");
    }

    @Test
    void testQueenAndPawnEndgame() throws Exception { // TODO improove the position does not allow pawn push..
        // From endgame theory: queen and pawn vs queen
        // Position from Dvoretsky's Endgame Manual
        Board board = new Board();
        board.loadFromFen("8/8/3k4/8/3K4/3P4/3Q4/5q2 w - - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000);
        
        assertNotNull(bestMove, "Should find best move");
        // White should push pawn (d4) or improve king/queen coordination
        String move = bestMove.toString().toUpperCase();
        assertTrue(move.equals("D3D4") || move.startsWith("D4") || move.startsWith("D2"),
                "Should advance pawn or improve piece coordination");
    }

    @Test
    @Disabled // todo re-enable after we found reason for deathloop
    void testQueenInCheckPosition() throws Exception {
        // From Tal's games: queen gives check with tactical purpose
        // Queen check that leads to material gain
        Board board = new Board();
        board.loadFromFen("r1b1kb1r/pppp1ppp/2n5/4q3/4n3/3P1N2/PPP2PPP/RNBQKB1R b KQkq - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000000, 0, 7);
        
        assertNotNull(bestMove, "Should find defensive move by giving check");
        // Black has two valuable pieces hanging, but can give a check to defend the knight
        assertEquals("E5A5", bestMove.toString().toUpperCase(),
                "Should defend with Qe5");
    }

    @Test
    void testQueenForkPattern() throws Exception {
        // Classic queen fork from beginner tactics books
        // Queen can fork king and rook
        Board board = new Board();
        board.loadFromFen("r3k3/8/8/8/2Q5/8/8/6K1 w q - 0 1");
        
        Move bestMove = calculator.computeBestMove(board, 3000000, 0, 7);
        
        assertNotNull(bestMove, "Should find best move for black");
        // Black can fork king and rook to win the rook in 2 moves
        assertEquals("E5A5", bestMove.toString().toUpperCase(),
                "Should defend with Qe5");
    }
}
