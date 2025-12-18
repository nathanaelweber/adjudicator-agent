package ch.adjudicator.agent.bitboard.generator;

import ch.adjudicator.agent.bitboard.adapter.ChessLibAdapter;
import ch.adjudicator.agent.bitboard.model.BoardState;
import ch.adjudicator.agent.bitboard.model.FastMove;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for check detection in BitboardMoveGenerator.
 */
class CheckDetectionTest {

    @Test
    void testKingInCheckByRook() {
        // White king on e1, black rook on e8
        String fen = "4r3/8/8/8/8/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // King must move out of check, cannot stay on e-file
        for (FastMove move : moves) {
            int to = move.destinationSquare;
            int toFile = to % 8;
            assertNotEquals(4, toFile, "King cannot move to e-file when in check from rook on e8");
        }
    }

    @Test
    void testKingInCheckByBishop() {
        // White king on e1, black bishop on a5
        String fen = "8/8/8/b7/8/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // King must move, and cannot move to squares still attacked by bishop
        assertTrue(moves.size() > 0, "King should have at least one legal move");
    }

    @Test
    void testKingInCheckByKnight() {
        // White king on e1, black knight on d3
        String fen = "8/8/8/8/8/3n4/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // King must move out of check
        assertTrue(moves.size() > 0, "King should have legal moves to escape check");
    }

    @Test
    void testKingInCheckByPawn() {
        // White king on e4, black pawn on d5
        String fen = "8/8/8/3p4/4K3/8/8/8 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // King must move out of check or capture the pawn
        assertTrue(moves.size() > 0, "King should have legal moves");
        
        // Check if capturing the pawn is one of the moves
        boolean canCapture = moves.stream().anyMatch(m -> m.destinationSquare == 35);
        assertTrue(canCapture, "King should be able to capture the checking pawn");
    }

    @Test
    void testKingInCheckByQueen() {
        // White king on e1, black queen on e8
        String fen = "4q3/8/8/8/8/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // King must move out of check
        assertTrue(moves.size() > 0, "King should have legal moves to escape check");
    }

    @Test
    void testBlockCheck() {
        // White king on e1, black rook on e8, white rook on a4
        String fen = "4r3/8/8/8/R7/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // White rook can block the check by moving to e4 (on the e-file between king and attacker)
        boolean canBlock = moves.stream().anyMatch(m -> {
            int from = m.originSquare;
            int to = m.destinationSquare;
            // Check if rook from a4 (square 24) moves to e4 (square 28)
            return from == 24 && to == 28;
        });
        
        assertTrue(canBlock, "Rook should be able to block the check by moving to e4");
    }

    @Test
    void testPinnedPiece() {
        // White king on e1, white bishop on e2, black rook on e8
        String fen = "4r3/8/8/8/8/8/4B3/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Bishop is pinned and cannot move away from e-file
        boolean bishopMovesOffFile = moves.stream().anyMatch(m -> {
            int from = m.originSquare;
            int to = m.destinationSquare;
            int toFile = to % 8;
            return from == 12 && toFile != 4; // Bishop on e2 moving off e-file
        });
        
        assertFalse(bishopMovesOffFile, "Pinned bishop should not be able to move off the pin line");
    }

    @Test
    void testCastlingThroughCheck() {
        // White king on e1, black rook on f8 - cannot castle kingside
        String fen = "5r2/8/8/8/8/8/8/R3K2R w KQ - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Should not be able to castle kingside (passes through f1 which is attacked)
        boolean hasKingsideCastle = moves.stream().anyMatch(m -> m.castling && m.destinationSquare == 6);
        assertFalse(hasKingsideCastle, "Cannot castle through check");
        
        // Should be able to castle queenside
        boolean hasQueensideCastle = moves.stream().anyMatch(m -> m.castling && m.destinationSquare == 2);
        assertTrue(hasQueensideCastle, "Should be able to castle queenside");
    }

    @Test
    void testCastlingWhileInCheck() {
        // White king on e1 in check from black rook on e8
        String fen = "4r3/8/8/8/8/8/8/R3K2R w KQ - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Cannot castle while in check
        boolean hasCastling = moves.stream().anyMatch(m -> m.castling);
        assertFalse(hasCastling, "Cannot castle while in check");
    }

    @Test
    void testCastlingIntoCheck() {
        // White king on e1, black rook on g8 - cannot castle kingside into check
        String fen = "6r1/8/8/8/8/8/8/R3K2R w KQ - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Should not be able to castle kingside (ends on g1 which is attacked)
        boolean hasKingsideCastle = moves.stream().anyMatch(m -> m.castling && m.destinationSquare == 6);
        assertFalse(hasKingsideCastle, "Cannot castle into check");
    }

    @Test
    void testEnPassantExposesKing() {
        // White king on e5, white pawn on d5, black pawn on c5 (just moved from c7)
        // If white captures en passant, it exposes the king to check from black rook on a5
        String fen = "8/8/8/r1ppK3/8/8/8/8 w - c6 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // En passant capture should not be legal (exposes king to check)
        boolean hasEnPassant = moves.stream().anyMatch(m -> m.enPassant);
        assertFalse(hasEnPassant, "En passant should not be legal if it exposes king to check");
    }

    @Test
    void testMoveThatExposesKingToCheck() {
        // White king on e1, white knight on e2, black rook on e8
        String fen = "4r3/8/8/8/8/8/4N3/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Knight cannot move away from e2 (would expose king)
        boolean knightMoves = moves.stream().anyMatch(m -> m.originSquare == 12);
        assertFalse(knightMoves, "Knight should not be able to move (would expose king)");
    }

    @Test
    void testStartingPositionNoCheck() {
        String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(startFen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Should have 20 legal moves
        assertEquals(20, moves.size(), "Starting position should have 20 legal moves");
    }

    @Test
    void testCheckmate() {
        // Scholar's mate position - black is checkmated by white queen on f7
        String fen = "r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Black is in checkmate - should have no legal moves
        assertEquals(0, moves.size(), "Black is in checkmate and should have no legal moves");
    }

    @Test
    void testBlackKingInCheck() {
        // Black king on e8, white rook on e1
        String fen = "4k3/8/8/8/8/8/8/4R3 b - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Black king must move out of check
        assertTrue(moves.size() > 0, "Black king should have legal moves to escape check");
        
        // All moves should be king moves (no other piece can block or capture)
        boolean allKingMoves = moves.stream().allMatch(m -> {
            int from = m.originSquare;
            long kingBit = 1L << from;
            return (state.blackPieces[BoardState.INDEX_KING] & kingBit) != 0;
        });
        assertTrue(allKingMoves, "All moves should be king moves");
    }

    @Test
    void testBlackCastlingThroughCheck() {
        // Black king on e8, white rook on f1 - cannot castle kingside
        String fen = "r3k2r/8/8/8/8/8/8/5R2 b kq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Should not be able to castle kingside (passes through f8 which is attacked)
        boolean hasKingsideCastle = moves.stream().anyMatch(m -> m.castling && m.destinationSquare == 62);
        assertFalse(hasKingsideCastle, "Black cannot castle through check");
    }
}
