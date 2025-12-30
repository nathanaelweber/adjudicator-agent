package ch.adjudicator.agent;

import ch.adjudicator.agent.bitboard.adapter.ChessLibAdapter;
import ch.adjudicator.agent.bitboard.generator.BitboardMoveGenerator;
import ch.adjudicator.agent.bitboard.model.BoardState;
import ch.adjudicator.agent.bitboard.model.FastMove;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to investigate quiescence behavior with pawn promotions
 */
class PromotionQuiescenceTest {

    @Test
    void testPromotionIsIncludedInTacticalMoves() {
        // Position where white has a pawn on a7 that can promote
        Board board = new Board();
        board.loadFromFen("8/P7/8/8/8/k7/8/K7 w - - 0 1");
        
        BoardState boardState = ChessLibAdapter.fenToBoardState(board.getFen());
        
        // Generate all legal moves
        List<FastMove> allMoves = BitboardMoveGenerator.generateMoves(boardState, null);
        
        System.out.println("[DEBUG_LOG] Total legal moves: " + allMoves.size());
        for (FastMove move : allMoves) {
            System.out.println("[DEBUG_LOG] Move: " + move + " | isPromotion: " + move.promotion);
        }
        
        // Filter to tactical moves (same logic as quiescence)
        boolean isWhite = boardState.isWhiteToMove();
        long enemyOccupied = isWhite ? boardState.blackOccupied : boardState.whiteOccupied;
        
        int tacticalMoveCount = 0;
        boolean foundPromotion = false;
        for (FastMove move : allMoves) {
            long destBit = 1L << move.destinationSquare;
            // Include captures and promotions (same logic as quiescence line 502)
            if ((destBit & enemyOccupied) != 0 || move.promotion || move.enPassant) {
                tacticalMoveCount++;
                if (move.promotion) {
                    foundPromotion = true;
                    System.out.println("[DEBUG_LOG] Found promotion in tactical moves: " + move);
                }
            }
        }
        
        System.out.println("[DEBUG_LOG] Tactical moves count: " + tacticalMoveCount);
        System.out.println("[DEBUG_LOG] Found promotion: " + foundPromotion);
        
        assertTrue(foundPromotion, "Promotion should be included in tactical moves");
    }
    
    @Test
    void testEngineChoosesPromotion() throws Exception {
        // Position where white has a pawn on a7 that can promote
        Board board = new Board();
        board.loadFromFen("8/P7/8/8/8/k7/8/K7 w - - 0 1");
        
        BestMoveCalculator calc = new BestMoveCalculator();
        
        System.out.println("[DEBUG_LOG] Computing best move for promotion position...");
        Move bestMove = calc.computeBestMove(board, 3000);
        
        System.out.println("[DEBUG_LOG] Best move: " + bestMove);
        System.out.println("[DEBUG_LOG] From: " + bestMove.getFrom());
        System.out.println("[DEBUG_LOG] To: " + bestMove.getTo());
        
        assertNotNull(bestMove, "Should find a move");
        assertEquals("A7", bestMove.getFrom().toString().toUpperCase(), "Should move pawn from a7");
        assertEquals("A8", bestMove.getTo().toString().toUpperCase(), "Should promote to a8");
    }
    
    @Test
    void testPromotionInQuietPosition() throws Exception {
        // Simple position where promoting is clearly the best move
        // No captures available, just a simple promotion
        Board board = new Board();
        board.loadFromFen("8/P7/8/8/8/k7/8/K7 w - - 0 1");
        
        BestMoveCalculator calc = new BestMoveCalculator();
        
        // Test with different time controls
        for (int timeMs : new int[]{500, 1000, 2000, 3000, 5000}) {
            System.out.println("[DEBUG_LOG] Testing with time: " + timeMs + "ms");
            Move bestMove = calc.computeBestMove(board, timeMs);
            
            System.out.println("[DEBUG_LOG] Time: " + timeMs + "ms, Move: " + bestMove);
            
            assertNotNull(bestMove, "Should find a move with time " + timeMs);
            assertEquals("A7", bestMove.getFrom().toString().toUpperCase(), 
                "Should move from a7 with time " + timeMs);
            assertEquals("A8", bestMove.getTo().toString().toUpperCase(), 
                "Should promote to a8 with time " + timeMs);
        }
    }
}
