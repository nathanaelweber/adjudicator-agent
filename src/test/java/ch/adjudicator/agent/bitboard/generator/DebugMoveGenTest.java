package ch.adjudicator.agent.bitboard.generator;

import ch.adjudicator.agent.bitboard.adapter.ChessLibAdapter;
import ch.adjudicator.agent.bitboard.model.BoardState;
import ch.adjudicator.agent.bitboard.model.FastMove;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DebugMoveGenTest {

    @Test
    void debugRookMoves() {
        // Rook on e4 with clear board
        String fen = "4k3/8/8/8/4R3/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        System.out.println("[DEBUG_LOG] Total moves: " + moves.size());
        
        int rookMoves = 0;
        int kingMoves = 0;
        
        for (FastMove move : moves) {
            int from = move.originSquare;
            int to = move.destinationSquare;
            String fromSquare = squareToString(from);
            String toSquare = squareToString(to);
            
            long rookBit = 1L << from;
            if ((state.whitePieces[BoardState.INDEX_ROOK] & rookBit) != 0) {
                rookMoves++;
                System.out.println("[DEBUG_LOG] Rook move: " + fromSquare + " -> " + toSquare);
            } else {
                kingMoves++;
                System.out.println("[DEBUG_LOG] King move: " + fromSquare + " -> " + toSquare);
            }
        }
        
        System.out.println("[DEBUG_LOG] Rook moves: " + rookMoves);
        System.out.println("[DEBUG_LOG] King moves: " + kingMoves);
        
        // Expected: Rook on e4 has moves to:
        // Vertical: e1, e2, e3, e5, e6, e7, e8 (7 squares)
        // Horizontal: a4, b4, c4, d4, f4, g4, h4 (7 squares)
        // Total: 14 rook moves
    }

    @Test
    void debugQueenMoves() {
        // Queen on e4 with clear board
        String fen = "4k3/8/8/8/4Q3/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        System.out.println("[DEBUG_LOG] Total moves: " + moves.size());
        
        int queenMoves = 0;
        int kingMoves = 0;
        
        for (FastMove move : moves) {
            int from = move.originSquare;
            int to = move.destinationSquare;
            String fromSquare = squareToString(from);
            String toSquare = squareToString(to);
            
            long queenBit = 1L << from;
            if ((state.whitePieces[BoardState.INDEX_QUEEN] & queenBit) != 0) {
                queenMoves++;
                System.out.println("[DEBUG_LOG] Queen move: " + fromSquare + " -> " + toSquare);
            } else {
                kingMoves++;
                System.out.println("[DEBUG_LOG] King move: " + fromSquare + " -> " + toSquare);
            }
        }
        
        System.out.println("[DEBUG_LOG] Queen moves: " + queenMoves);
        System.out.println("[DEBUG_LOG] King moves: " + kingMoves);
    }

    private String squareToString(int square) {
        int file = square % 8;
        int rank = square / 8;
        char fileChar = (char) ('a' + file);
        char rankChar = (char) ('1' + rank);
        return "" + fileChar + rankChar;
    }
}
