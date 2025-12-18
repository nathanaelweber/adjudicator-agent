package ch.adjudicator.agent.bitboard.adapter;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import org.junit.jupiter.api.Test;

/**
 * Debug test to understand CastleRight behavior
 */
class CastleRightDebugTest {

    @Test
    void debugCastleRights() {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w Kq - 5 10";
        Board board = new Board();
        board.loadFromFen(fen);

        System.out.println("[DEBUG_LOG] White castle right: '" + board.getCastleRight(Side.WHITE) + "'");
        System.out.println("[DEBUG_LOG] Black castle right: '" + board.getCastleRight(Side.BLACK) + "'");
        System.out.println("[DEBUG_LOG] White toString: '" + board.getCastleRight(Side.WHITE).toString() + "'");
        System.out.println("[DEBUG_LOG] Black toString: '" + board.getCastleRight(Side.BLACK).toString() + "'");
        
        String combined = board.getCastleRight(Side.WHITE).toString() + 
                         board.getCastleRight(Side.BLACK).toString();
        System.out.println("[DEBUG_LOG] Combined: '" + combined + "'");
        
        // Check FEN for starting position
        String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        Board startBoard = new Board();
        startBoard.loadFromFen(startFen);
        
        System.out.println("[DEBUG_LOG] Start white: '" + startBoard.getCastleRight(Side.WHITE) + "'");
        System.out.println("[DEBUG_LOG] Start black: '" + startBoard.getCastleRight(Side.BLACK) + "'");
    }
}
