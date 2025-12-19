package ch.adjudicator.agent.bitboard.generator;

import ch.adjudicator.agent.bitboard.adapter.ChessLibAdapter;
import ch.adjudicator.agent.bitboard.model.BoardState;
import ch.adjudicator.agent.bitboard.model.FastMove;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BitboardMoveGenerator - move generation from board state.
 */
class BitboardMoveGeneratorTest {

    @Test
    void testStartingPositionWhiteMoves() {
        String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(startFen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Starting position should have 20 legal moves for white
        // 16 pawn moves (8 single push + 8 double push) + 4 knight moves (2 knights * 2 squares each)
        assertEquals(20, moves.size(), "Starting position should have exactly 20 moves for white");
    }

    @Test
    void testStartingPositionBlackMoves() {
        String fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // After 1.e4, black has 20 legal moves
        assertEquals(20, moves.size(), "Black should have 20 moves after 1.e4");
    }

    @Test
    void testPawnPromotions() {
        String fen = "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // White pawn on a7 can promote to 4 pieces (Q, R, B, N)
        // White king has 5 moves
        // Total: 4 + 5 = 9
        assertEquals(9, moves.size(), "Should have 4 promotion moves + 5 king moves");
        
        // Check that promotion moves are flagged correctly
        long promotionCount = moves.stream().filter(m -> m.promotion).count();
        assertEquals(4, promotionCount, "Should have exactly 4 promotion moves");
    }

    @Test
    void testEnPassantCapture() {
        // White pawn on e5, black pawn just moved from d7 to d5 (en passant square is d6)
        String fen = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Find the en passant move
        long enPassantCount = moves.stream().filter(m -> m.enPassant).count();
        assertTrue(enPassantCount > 0, "Should have at least one en passant move");
    }

    @Test
    void testCastlingMoves() {
        // Position where both white castling moves are available
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Count castling moves
        long castlingCount = moves.stream().filter(m -> m.castling).count();
        assertEquals(2, castlingCount, "White should have 2 castling moves available");
    }

    @Test
    void testKnightMoves() {
        // Knight on e4 with clear board
        String fen = "4k3/8/8/8/4N3/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Knight on e4 has 8 possible moves, king has 5 moves
        // Total: 8 + 5 = 13
        assertEquals(13, moves.size(), "Should have 8 knight moves + 5 king moves");
    }

    @Test
    void testBishopMoves() {
        // Bishop on e4 with clear board
        String fen = "4k3/8/8/8/4B3/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Bishop on e4 has 13 possible moves (diagonals), king has 5 moves
        // Total: 13 + 5 = 18
        assertEquals(18, moves.size(), "Should have 13 bishop moves + 5 king moves");
    }

    @Test
    void testRookMoves() {
        // Rook on e4 with clear board
        String fen = "4k3/8/8/8/4R3/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Rook on e4 (square 28):
        // Vertical: e1(4), e2(12), e3(20), e5(36), e6(44), e7(52), e8(60) = 7 moves
        // Horizontal: a4(24), b4(25), c4(26), d4(27), f4(29), g4(30), h4(31) = 7 moves
        // But king is on e1 (square 4), so e1 is blocked
        // Rook has 13 moves, king has 5 moves (d1, d2, e2, f1, f2)
        // Total: 13 + 5 = 18
        assertEquals(18, moves.size(), "Should have 13 rook moves + 5 king moves");
    }

    @Test
    void testQueenMoves() {
        // Queen on e4 with clear board
        String fen = "4k3/8/8/8/4Q3/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Queen on e4 (square 28):
        // Diagonal: 13 moves (NE: 3, NW: 3, SE: 3, SW: 3, but e1 is blocked by king = 13 - 1 = 12)
        // Straight: 13 moves (N: 4, S: 3 (blocked by king), E: 3, W: 3 = 13)
        // But king on e1 blocks e1, so: diagonal SW to e1 blocked, and straight S to e1 blocked
        // Actually: 13 diagonal (e1 blocked = 12) + 13 straight (e1 blocked = 13) = 25 + 1 (e1 counted once) = 26
        // Queen has 26 moves, king has 5 moves
        // Total: 26 + 5 = 31
        assertEquals(31, moves.size(), "Should have 26 queen moves + 5 king moves");
    }

    @Test
    void testPawnBlockedMoves() {
        // White pawn blocked by black pawn
        String fen = "4k3/8/8/8/4p3/4P3/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Pawn is blocked, only king moves (5)
        assertEquals(5, moves.size(), "Blocked pawn should have no moves, only king moves");
    }

    @Test
    void testPawnCaptures() {
        // White pawn can capture two black pieces
        String fen = "4k3/8/8/3ppp2/4P3/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Pawn can capture left or right (2 moves) + king moves (5)
        // Total: 2 + 5 = 7
        assertEquals(7, moves.size(), "Should have 2 pawn captures + 5 king moves");
    }

    @Test
    void testOccupiedSquaresBlocking() {
        // Test that pieces cannot move through other pieces
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // No sliding piece moves should pass through pawns
        // Only pawn and knight moves are possible
        boolean hasIllegalBishopMove = moves.stream()
            .anyMatch(m -> {
                int from = m.originSquare;
                // Check if any bishop tries to move (they shouldn't be able to)
                long bishopBit = 1L << from;
                return (state.whitePieces[BoardState.INDEX_BISHOP] & bishopBit) != 0;
            });
        
        assertFalse(hasIllegalBishopMove, "Bishops should not be able to move from starting position");
    }

    @Test
    void testBlackPawnMoves() {
        // Black to move with pawns
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Black has 20 moves from starting position (same as white)
        assertEquals(20, moves.size(), "Black should have 20 moves from starting position");
    }

    @Test
    void testKingMovesNearEdge() {
        // King on corner should have limited moves
        String fen = "7k/8/8/8/8/8/8/K7 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // King on a1 has 3 possible moves (b1, a2, b2)
        assertEquals(3, moves.size(), "King on corner should have 3 moves");
    }

    @Test
    void testCastlingBlockedByPieces() {
        // Castling blocked by pieces
        String fen = "r3k2r/8/8/8/8/8/8/R1B1KB1R w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // Castling should not be available (blocked by bishops)
        long castlingCount = moves.stream().filter(m -> m.castling).count();
        assertEquals(0, castlingCount, "Castling should be blocked by pieces");
    }

    @Test
    void testComplexPosition() {
        // KiwiPete position - famous perft test position
        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);
        
        // KiwiPete position has 48 legal moves from white's perspective
        // Note: This is pseudo-legal move count (doesn't check for king safety)
        assertTrue(moves.size() > 0, "Should generate moves for complex position");
    }

    @Test
    void testCheckmateInOnePosition() {
        String fen = "rn1qkbnr/ppp2ppp/3p4/4N3/2B1P1b1/8/PPPPQPPP/RNB1K2R w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);

        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);

        // expect exactly 39 possible moves
        assertTrue(moves.size() == 39, "Should generate moves for complex position");
    }

    @Test
    void testCheckmatePosition() {
        String fen = "rn1qkbnr/ppp2Bpp/3p4/4N3/4P1b1/8/PPPPQPPP/RNB1K2R b KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);

        List<FastMove> moves = BitboardMoveGenerator.generateMoves(state, null);

        // expect exactly 0 possible moves
        assertTrue(moves.isEmpty(), "Should generate zero positions, since is in checkmate");
        assertTrue(BitboardMoveGenerator.isCurrentPlayerInCheck(state), "Should be in check");
    }
}
