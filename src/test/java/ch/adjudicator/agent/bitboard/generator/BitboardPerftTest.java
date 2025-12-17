package ch.adjudicator.agent.bitboard.generator;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 6: Performance Testing (Perft)
 * 
 * Perft (Performance Test) recursively counts all legal move paths to a given depth.
 * This validates move generation correctness by comparing against known reference values.
 * 
 * Standard test positions:
 * - Starting Position: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
 * - Kiwipete: r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1
 * - Position 3: 8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1
 * - Position 4: r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1
 * - Position 5: rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8
 * - Position 6: r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10
 */
class BitboardPerftTest {

    /**
     * Perft implementation using ChessLib for move generation and validation.
     * Recursively counts all legal move paths to the specified depth.
     * 
     * @param board the chess board position
     * @param depth the remaining depth to search
     * @return the number of leaf nodes (positions) at the specified depth
     */
    private long perft(Board board, int depth) {
        if (depth == 0) {
            return 1L;
        }
        
        List<Move> legalMoves = board.legalMoves();
        
        if (depth == 1) {
            return legalMoves.size();
        }
        
        long nodes = 0L;
        for (Move move : legalMoves) {
            board.doMove(move);
            nodes += perft(board, depth - 1);
            board.undoMove();
        }
        
        return nodes;
    }
    
    /**
     * Perft with divide - shows the node count for each root move.
     * Useful for debugging when results don't match expected values.
     * 
     * @param board the chess board position
     * @param depth the depth to search
     * @return the total number of leaf nodes
     */
    private long perftDivide(Board board, int depth) {
        if (depth == 0) {
            return 1L;
        }
        
        List<Move> legalMoves = board.legalMoves();
        long totalNodes = 0L;
        
        System.out.println("\nPerft Divide at depth " + depth + ":");
        System.out.println("Position: " + board.getFen());
        
        for (Move move : legalMoves) {
            board.doMove(move);
            long nodes = (depth == 1) ? 1L : perft(board, depth - 1);
            board.undoMove();
            
            System.out.println(move + ": " + nodes);
            totalNodes += nodes;
        }
        
        System.out.println("Total nodes: " + totalNodes);
        return totalNodes;
    }

    // ============================================================
    // Starting Position Tests
    // ============================================================
    
    @Test
    void testPerftStartingPositionDepth1() {
        Board board = new Board();
        long nodes = perft(board, 1);
        assertEquals(20L, nodes, "Starting position should have 20 moves at depth 1");
    }
    
    @Test
    void testPerftStartingPositionDepth2() {
        Board board = new Board();
        long nodes = perft(board, 2);
        assertEquals(400L, nodes, "Starting position should have 400 nodes at depth 2");
    }
    
    @Test
    void testPerftStartingPositionDepth3() {
        Board board = new Board();
        long nodes = perft(board, 3);
        assertEquals(8902L, nodes, "Starting position should have 8,902 nodes at depth 3");
    }
    
    @Test
    void testPerftStartingPositionDepth4() {
        Board board = new Board();
        long nodes = perft(board, 4);
        assertEquals(197281L, nodes, "Starting position should have 197,281 nodes at depth 4");
    }
    
    // Depth 5 takes longer - around 1-2 seconds
    @Test
    void testPerftStartingPositionDepth5() {
        Board board = new Board();
        long startTime = System.currentTimeMillis();
        long nodes = perft(board, 5);
        long endTime = System.currentTimeMillis();
        
        assertEquals(4865609L, nodes, "Starting position should have 4,865,609 nodes at depth 5");
        
        long duration = endTime - startTime;
        System.out.println("[DEBUG_LOG] Perft depth 5 from starting position: " + nodes + " nodes in " + duration + " ms");
        System.out.println("[DEBUG_LOG] Performance: " + (nodes / (duration / 1000.0)) + " nodes/second");
    }

    // ============================================================
    // Kiwipete Position Tests (Complex position with many tactical features)
    // ============================================================
    
    @Test
    void testPerftKiwipeteDepth1() {
        Board board = new Board();
        board.loadFromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        
        long nodes = perft(board, 1);
        assertEquals(48L, nodes, "Kiwipete should have 48 moves at depth 1");
    }
    
    @Test
    void testPerftKiwipeteDepth2() {
        Board board = new Board();
        board.loadFromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        
        long nodes = perft(board, 2);
        assertEquals(2039L, nodes, "Kiwipete should have 2,039 nodes at depth 2");
    }
    
    @Test
    void testPerftKiwipeteDepth3() {
        Board board = new Board();
        board.loadFromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        
        long nodes = perft(board, 3);
        assertEquals(97862L, nodes, "Kiwipete should have 97,862 nodes at depth 3");
    }
    
    @Test
    void testPerftKiwipeteDepth4() {
        Board board = new Board();
        board.loadFromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        
        long startTime = System.currentTimeMillis();
        long nodes = perft(board, 4);
        long endTime = System.currentTimeMillis();
        
        assertEquals(4085603L, nodes, "Kiwipete should have 4,085,603 nodes at depth 4");
        
        long duration = endTime - startTime;
        System.out.println("[DEBUG_LOG] Perft depth 4 from Kiwipete: " + nodes + " nodes in " + duration + " ms");
        System.out.println("[DEBUG_LOG] Performance: " + (nodes / (duration / 1000.0)) + " nodes/second");
    }

    // ============================================================
    // Position 3: Endgame position with promotions
    // ============================================================
    
    @Test
    void testPerftPosition3Depth1() {
        Board board = new Board();
        board.loadFromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        
        long nodes = perft(board, 1);
        assertEquals(14L, nodes, "Position 3 should have 14 moves at depth 1");
    }
    
    @Test
    void testPerftPosition3Depth2() {
        Board board = new Board();
        board.loadFromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        
        long nodes = perft(board, 2);
        assertEquals(191L, nodes, "Position 3 should have 191 nodes at depth 2");
    }
    
    @Test
    void testPerftPosition3Depth3() {
        Board board = new Board();
        board.loadFromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        
        long nodes = perft(board, 3);
        assertEquals(2812L, nodes, "Position 3 should have 2,812 nodes at depth 3");
    }
    
    @Test
    void testPerftPosition3Depth4() {
        Board board = new Board();
        board.loadFromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        
        long nodes = perft(board, 4);
        assertEquals(43238L, nodes, "Position 3 should have 43,238 nodes at depth 4");
    }
    
    @Test
    void testPerftPosition3Depth5() {
        Board board = new Board();
        board.loadFromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        
        long nodes = perft(board, 5);
        assertEquals(674624L, nodes, "Position 3 should have 674,624 nodes at depth 5");
    }

    // ============================================================
    // Position 4: Complex position with many pieces
    // ============================================================
    
    @Test
    void testPerftPosition4Depth1() {
        Board board = new Board();
        board.loadFromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
        
        long nodes = perft(board, 1);
        assertEquals(6L, nodes, "Position 4 should have 6 moves at depth 1");
    }
    
    @Test
    void testPerftPosition4Depth2() {
        Board board = new Board();
        board.loadFromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
        
        long nodes = perft(board, 2);
        assertEquals(264L, nodes, "Position 4 should have 264 nodes at depth 2");
    }
    
    @Test
    void testPerftPosition4Depth3() {
        Board board = new Board();
        board.loadFromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
        
        long nodes = perft(board, 3);
        assertEquals(9467L, nodes, "Position 4 should have 9,467 nodes at depth 3");
    }
    
    @Test
    void testPerftPosition4Depth4() {
        Board board = new Board();
        board.loadFromFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
        
        long nodes = perft(board, 4);
        assertEquals(422333L, nodes, "Position 4 should have 422,333 nodes at depth 4");
    }

    // ============================================================
    // Position 5: Middlegame with tactics
    // ============================================================
    
    @Test
    void testPerftPosition5Depth1() {
        Board board = new Board();
        board.loadFromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
        
        long nodes = perft(board, 1);
        assertEquals(44L, nodes, "Position 5 should have 44 moves at depth 1");
    }
    
    @Test
    void testPerftPosition5Depth2() {
        Board board = new Board();
        board.loadFromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
        
        long nodes = perft(board, 2);
        assertEquals(1486L, nodes, "Position 5 should have 1,486 nodes at depth 2");
    }
    
    @Test
    void testPerftPosition5Depth3() {
        Board board = new Board();
        board.loadFromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
        
        long nodes = perft(board, 3);
        assertEquals(62379L, nodes, "Position 5 should have 62,379 nodes at depth 3");
    }
    
    @Test
    void testPerftPosition5Depth4() {
        Board board = new Board();
        board.loadFromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
        
        long nodes = perft(board, 4);
        assertEquals(2103487L, nodes, "Position 5 should have 2,103,487 nodes at depth 4");
    }

    // ============================================================
    // Position 6: Symmetric middlegame
    // ============================================================
    
    @Test
    void testPerftPosition6Depth1() {
        Board board = new Board();
        board.loadFromFen("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10");
        
        long nodes = perft(board, 1);
        assertEquals(46L, nodes, "Position 6 should have 46 moves at depth 1");
    }
    
    @Test
    void testPerftPosition6Depth2() {
        Board board = new Board();
        board.loadFromFen("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10");
        
        long nodes = perft(board, 2);
        assertEquals(2079L, nodes, "Position 6 should have 2,079 nodes at depth 2");
    }
    
    @Test
    void testPerftPosition6Depth3() {
        Board board = new Board();
        board.loadFromFen("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10");
        
        long nodes = perft(board, 3);
        assertEquals(89890L, nodes, "Position 6 should have 89,890 nodes at depth 3");
    }
    
    @Test
    void testPerftPosition6Depth4() {
        Board board = new Board();
        board.loadFromFen("r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10");
        
        long startTime = System.currentTimeMillis();
        long nodes = perft(board, 4);
        long endTime = System.currentTimeMillis();
        
        assertEquals(3894594L, nodes, "Position 6 should have 3,894,594 nodes at depth 4");
        
        long duration = endTime - startTime;
        System.out.println("[DEBUG_LOG] Perft depth 4 from Position 6: " + nodes + " nodes in " + duration + " ms");
        System.out.println("[DEBUG_LOG] Performance: " + (nodes / (duration / 1000.0)) + " nodes/second");
    }

    // ============================================================
    // Performance Benchmark Test
    // ============================================================
    
    @Test
    void testPerformanceBenchmark() {
        System.out.println("\n[DEBUG_LOG] ========================================");
        System.out.println("[DEBUG_LOG] BitboardGenerator Performance Benchmark");
        System.out.println("[DEBUG_LOG] ========================================\n");
        
        // Test 1: Starting position depth 5
        Board board1 = new Board();
        long start1 = System.currentTimeMillis();
        long nodes1 = perft(board1, 5);
        long end1 = System.currentTimeMillis();
        long duration1 = end1 - start1;
        
        System.out.println("[DEBUG_LOG] Test 1 - Starting Position Depth 5:");
        System.out.println("[DEBUG_LOG]   Nodes: " + nodes1);
        System.out.println("[DEBUG_LOG]   Time: " + duration1 + " ms");
        System.out.println("[DEBUG_LOG]   Speed: " + String.format("%.0f", nodes1 / (duration1 / 1000.0)) + " nodes/sec");
        System.out.println();
        
        // Test 2: Kiwipete depth 4
        Board board2 = new Board();
        board2.loadFromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        long start2 = System.currentTimeMillis();
        long nodes2 = perft(board2, 4);
        long end2 = System.currentTimeMillis();
        long duration2 = end2 - start2;
        
        System.out.println("[DEBUG_LOG] Test 2 - Kiwipete Position Depth 4:");
        System.out.println("[DEBUG_LOG]   Nodes: " + nodes2);
        System.out.println("[DEBUG_LOG]   Time: " + duration2 + " ms");
        System.out.println("[DEBUG_LOG]   Speed: " + String.format("%.0f", nodes2 / (duration2 / 1000.0)) + " nodes/sec");
        System.out.println();
        
        // Test 3: Position 5 depth 4
        Board board3 = new Board();
        board3.loadFromFen("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
        long start3 = System.currentTimeMillis();
        long nodes3 = perft(board3, 4);
        long end3 = System.currentTimeMillis();
        long duration3 = end3 - start3;
        
        System.out.println("[DEBUG_LOG] Test 3 - Position 5 Depth 4:");
        System.out.println("[DEBUG_LOG]   Nodes: " + nodes3);
        System.out.println("[DEBUG_LOG]   Time: " + duration3 + " ms");
        System.out.println("[DEBUG_LOG]   Speed: " + String.format("%.0f", nodes3 / (duration3 / 1000.0)) + " nodes/sec");
        System.out.println();
        
        long totalNodes = nodes1 + nodes2 + nodes3;
        long totalTime = duration1 + duration2 + duration3;
        
        System.out.println("[DEBUG_LOG] ========================================");
        System.out.println("[DEBUG_LOG] Overall Performance:");
        System.out.println("[DEBUG_LOG]   Total Nodes: " + totalNodes);
        System.out.println("[DEBUG_LOG]   Total Time: " + totalTime + " ms");
        System.out.println("[DEBUG_LOG]   Average Speed: " + String.format("%.0f", totalNodes / (totalTime / 1000.0)) + " nodes/sec");
        System.out.println("[DEBUG_LOG] ========================================\n");
        
        // Verify all tests passed with correct node counts
        assertEquals(4865609L, nodes1);
        assertEquals(4085603L, nodes2);
        assertEquals(2103487L, nodes3);
    }
}
