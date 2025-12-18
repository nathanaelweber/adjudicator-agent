package ch.adjudicator.agent.bitboard.generator;

import ch.adjudicator.agent.bitboard.adapter.ChessLibAdapter;
import ch.adjudicator.agent.bitboard.model.BoardState;
import ch.adjudicator.agent.bitboard.model.FastMove;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DebugBlockCheckTest {

    @Test
    void debugBlockCheck() {
        // White king on e1, black rook on e8, white rook on h1
        String fen = "4r3/8/8/8/8/8/8/4K2R w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        System.out.println("[DEBUG_LOG] Total moves: " + moves.size());
        
        for (FastMove move : moves) {
            int from = move.originSquare;
            int to = move.destinationSquare;
            String fromSquare = squareToString(from);
            String toSquare = squareToString(to);
            
            System.out.println("[DEBUG_LOG] Move: " + fromSquare + " -> " + toSquare);
        }
        
        // Check which piece is on h1
        long h1Bit = 1L << 7;
        System.out.println("[DEBUG_LOG] Rook on h1: " + ((state.whitePieces[BoardState.INDEX_ROOK] & h1Bit) != 0));
        
        // Check which piece is on e1
        long e1Bit = 1L << 4;
        System.out.println("[DEBUG_LOG] King on e1: " + ((state.whitePieces[BoardState.INDEX_KING] & e1Bit) != 0));
    }

    private String squareToString(int square) {
        int file = square % 8;
        int rank = square / 8;
        char fileChar = (char) ('a' + file);
        char rankChar = (char) ('1' + rank);
        return "" + fileChar + rankChar;
    }
}
