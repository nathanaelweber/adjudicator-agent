package ch.adjudicator.agent;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify chesslib API methods for generating captures and promotions
 */
class ChesslibAPITest {

    @Test
    void testPseudoLegalCaptures() {
        // Position where black can capture a white pawn
        Board board = new Board();
        board.loadFromFen("4k3/8/8/3p4/4P3/8/8/4K3 b KQkq - 0 2");
        
        System.out.println("[DEBUG_LOG] Testing Board.pseudoLegalCaptures()");
        List<Move> captures = board.pseudoLegalCaptures();
        
        System.out.println("[DEBUG_LOG] Found " + captures.size() + " pseudo-legal captures:");
        for (Move move : captures) {
            System.out.println("[DEBUG_LOG]   " + move.toString());
        }
        
        // Should find d5xe4
        assertTrue(captures.size() > 0, "Should find at least one capture");
        boolean foundCapture = captures.stream()
                .anyMatch(m -> m.toString().toUpperCase().equals("D5E4"));
        assertTrue(foundCapture, "Should find d5xe4 capture");
    }

    @Test
    void testMoveGeneratorPseudoLegalCaptures() {
        // Position with multiple capture options
        Board board = new Board();
        board.loadFromFen("rnbqkbnr/ppp1pppp/8/3p4/2P1R3/8/PP1PPPPP/RNBQKBN1 b Qkq - 0 1");
        
        System.out.println("[DEBUG_LOG] Testing MoveGenerator.generatePseudoLegalCaptures()");
        List<Move> captures = MoveGenerator.generatePseudoLegalCaptures(board);
        
        System.out.println("[DEBUG_LOG] Found " + captures.size() + " pseudo-legal captures:");
        for (Move move : captures) {
            System.out.println("[DEBUG_LOG]   " + move.toString());
        }
        
        // Should find d5xe4 and d5xc4
        assertTrue(captures.size() >= 2, "Should find at least two captures");
    }

    @Test
    void testPromotionMoves() {
        // Position where promotion is available
        Board board = new Board();
        board.loadFromFen("4k3/8/8/8/8/8/7p/4K3 b - - 0 1");
        
        System.out.println("[DEBUG_LOG] Testing legal moves with promotion");
        List<Move> legalMoves = board.legalMoves();
        
        System.out.println("[DEBUG_LOG] Found " + legalMoves.size() + " legal moves:");
        for (Move move : legalMoves) {
            System.out.println("[DEBUG_LOG]   " + move.toString() + " promotion=" + move.getPromotion());
        }
        
        // Should find promotion moves h2h1
        long promotionCount = legalMoves.stream()
                .filter(m -> m.getPromotion() != null)
                .count();
        
        System.out.println("[DEBUG_LOG] Found " + promotionCount + " promotion moves");
        assertTrue(promotionCount > 0, "Should find promotion moves");
    }

    @Test
    void testCombinedCapturesAndPromotions() {
        // Position with both captures and promotions
        Board board = new Board();
        board.loadFromFen("4k3/8/8/8/8/8/6Pp/4K2R b - - 0 1");
        
        System.out.println("[DEBUG_LOG] Testing combined captures and promotions");
        
        List<Move> allLegal = board.legalMoves();
        List<Move> captures = board.pseudoLegalCaptures();
        
        System.out.println("[DEBUG_LOG] All legal moves: " + allLegal.size());
        System.out.println("[DEBUG_LOG] Pseudo-legal captures: " + captures.size());
        
        // Manually filter for promotions
        long promotions = allLegal.stream()
                .filter(m -> m.getPromotion() != null)
                .count();
        
        System.out.println("[DEBUG_LOG] Promotions: " + promotions);
        
        // For quiescence search, we need both captures and promotions
        List<Move> noisyMoves = allLegal.stream()
                .filter(m -> captures.contains(m) || m.getPromotion() != null)
                .toList();
        
        System.out.println("[DEBUG_LOG] Noisy moves (captures + promotions): " + noisyMoves.size());
        
        for (Move move : noisyMoves) {
            System.out.println("[DEBUG_LOG]   " + move.toString() + " promotion=" + move.getPromotion());
        }
    }
}
