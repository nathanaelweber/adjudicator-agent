package ch.adjudicator.agent;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal test to debug the quiescence evaluation issue
 */
class QuiescenceDebugTest {

    @Test
    void testQueenCaptureRecapture() throws Exception {
        // Position: 6rk/pp1p1ppp/3q4/4P3/8/8/6PP/3RR2K b - - 0 1
        // Black queen on d6, White rooks on d1 and e1
        // If Qd6xd1, then Re1xd1 recaptures
        // Black loses queen (900) and gains rook (500) = -400 for Black
        
        BestMoveCalculator calc = new BestMoveCalculator();
        Board board = new Board();
        board.loadFromFen("6rk/pp1p1ppp/3q4/4P3/8/8/6PP/3RR2K b - - 0 1");
        
        // Find the move Qd6xd1
        Move qxd1 = null;
        for (Move m : board.legalMoves()) {
            if (m.getFrom().toString().equals("D6") && m.getTo().toString().equals("D1")) {
                qxd1 = m;
                break;
            }
        }
        
        assertNotNull(qxd1, "Should find Qd6xd1 move");
        
        // With enough time, the engine should NOT choose this move
        Move bestMove = calc.computeBestMove(board, 5000);
        
        assertNotEquals("D1", bestMove.getTo().toString(), 
            "Should not capture on d1 as it loses the queen to recapture");
    }
}
