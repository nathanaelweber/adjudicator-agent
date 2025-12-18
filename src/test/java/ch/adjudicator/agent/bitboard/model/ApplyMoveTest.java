package ch.adjudicator.agent.bitboard.model;

import ch.adjudicator.agent.bitboard.adapter.ChessLibAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BoardState.applyMove method.
 * Verifies that moves are applied correctly with all flags updated.
 */
class ApplyMoveTest {

    @Test
    void testSimplePawnMove() {
        // e2-e4
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 12; // e2
        move.destinationSquare = 28; // e4
        move.promotion = false;
        move.enPassant = false;
        move.castling = false;
        
        BoardState newState = state.applyMove(move);
        
        // Check pawn moved
        assertEquals(0, newState.whitePieces[BoardState.INDEX_PAWN] & (1L << 12), "Pawn should be removed from e2");
        assertNotEquals(0, newState.whitePieces[BoardState.INDEX_PAWN] & (1L << 28), "Pawn should be on e4");
        
        // Check side to move toggled
        assertFalse(newState.isWhiteToMove(), "Should be black's turn");
        
        // Check castling rights preserved
        assertTrue(newState.isWhiteKingsideCastling());
        assertTrue(newState.isWhiteQueensideCastling());
        assertTrue(newState.isBlackKingsideCastling());
        assertTrue(newState.isBlackQueensideCastling());
        
        // Check en passant square set
        assertEquals(20, newState.getEnPassantSquare(), "En passant square should be e3 (20)");
    }

    @Test
    void testCapture() {
        // White bishop captures black pawn on f7
        String fen = "rnbqkbnr/pppp1ppp/8/4p3/2B1P3/8/PPPP1PPP/RNBQK2R w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 18; // c4
        move.destinationSquare = 53; // f7
        move.promotion = false;
        move.enPassant = false;
        move.castling = false;
        
        BoardState newState = state.applyMove(move);
        
        // Check bishop moved
        assertEquals(0, newState.whitePieces[BoardState.INDEX_BISHOP] & (1L << 18));
        assertNotEquals(0, newState.whitePieces[BoardState.INDEX_BISHOP] & (1L << 53));
        
        // Check black pawn removed
        assertEquals(0, newState.blackPieces[BoardState.INDEX_PAWN] & (1L << 53));
        
        // Check side toggled
        assertFalse(newState.isWhiteToMove());
        
        // Check en passant cleared
        assertEquals(-1, newState.getEnPassantSquare());
    }

    @Test
    void testWhiteKingsideCastling() {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 4; // e1
        move.destinationSquare = 6; // g1
        move.promotion = false;
        move.enPassant = false;
        move.castling = true;
        
        BoardState newState = state.applyMove(move);
        
        // Check king moved to g1
        assertEquals(0, newState.whitePieces[BoardState.INDEX_KING] & (1L << 4));
        assertNotEquals(0, newState.whitePieces[BoardState.INDEX_KING] & (1L << 6));
        
        // Check rook moved from h1 to f1
        assertEquals(0, newState.whitePieces[BoardState.INDEX_ROOK] & (1L << 7));
        assertNotEquals(0, newState.whitePieces[BoardState.INDEX_ROOK] & (1L << 5));
        
        // Check white castling rights removed
        assertFalse(newState.isWhiteKingsideCastling());
        assertFalse(newState.isWhiteQueensideCastling());
        
        // Check black castling rights preserved
        assertTrue(newState.isBlackKingsideCastling());
        assertTrue(newState.isBlackQueensideCastling());
        
        // Check side toggled
        assertFalse(newState.isWhiteToMove());
    }

    @Test
    void testWhiteQueensideCastling() {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 4; // e1
        move.destinationSquare = 2; // c1
        move.promotion = false;
        move.enPassant = false;
        move.castling = true;
        
        BoardState newState = state.applyMove(move);
        
        // Check king moved to c1
        assertNotEquals(0, newState.whitePieces[BoardState.INDEX_KING] & (1L << 2));
        
        // Check rook moved from a1 to d1
        assertEquals(0, newState.whitePieces[BoardState.INDEX_ROOK] & (1L << 0));
        assertNotEquals(0, newState.whitePieces[BoardState.INDEX_ROOK] & (1L << 3));
        
        // Check white castling rights removed
        assertFalse(newState.isWhiteKingsideCastling());
        assertFalse(newState.isWhiteQueensideCastling());
    }

    @Test
    void testBlackKingsideCastling() {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 60; // e8
        move.destinationSquare = 62; // g8
        move.promotion = false;
        move.enPassant = false;
        move.castling = true;
        
        BoardState newState = state.applyMove(move);
        
        // Check king moved to g8
        assertNotEquals(0, newState.blackPieces[BoardState.INDEX_KING] & (1L << 62));
        
        // Check rook moved from h8 to f8
        assertEquals(0, newState.blackPieces[BoardState.INDEX_ROOK] & (1L << 63));
        assertNotEquals(0, newState.blackPieces[BoardState.INDEX_ROOK] & (1L << 61));
        
        // Check black castling rights removed
        assertFalse(newState.isBlackKingsideCastling());
        assertFalse(newState.isBlackQueensideCastling());
        
        // Check side toggled
        assertTrue(newState.isWhiteToMove());
    }

    @Test
    void testBlackQueensideCastling() {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 60; // e8
        move.destinationSquare = 58; // c8
        move.promotion = false;
        move.enPassant = false;
        move.castling = true;
        
        BoardState newState = state.applyMove(move);
        
        // Check king moved to c8
        assertNotEquals(0, newState.blackPieces[BoardState.INDEX_KING] & (1L << 58));
        
        // Check rook moved from a8 to d8
        assertEquals(0, newState.blackPieces[BoardState.INDEX_ROOK] & (1L << 56));
        assertNotEquals(0, newState.blackPieces[BoardState.INDEX_ROOK] & (1L << 59));
        
        // Check black castling rights removed
        assertFalse(newState.isBlackKingsideCastling());
        assertFalse(newState.isBlackQueensideCastling());
    }

    @Test
    void testWhiteEnPassantCapture() {
        // White pawn on e5, black pawn on d5, en passant square is d6
        String fen = "rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 36; // e5
        move.destinationSquare = 43; // d6
        move.promotion = false;
        move.enPassant = true;
        move.castling = false;
        
        BoardState newState = state.applyMove(move);
        
        // Check white pawn moved to d6
        assertNotEquals(0, newState.whitePieces[BoardState.INDEX_PAWN] & (1L << 43));
        
        // Check black pawn removed from d5 (square 35)
        assertEquals(0, newState.blackPieces[BoardState.INDEX_PAWN] & (1L << 35));
        
        // Check en passant cleared
        assertEquals(-1, newState.getEnPassantSquare());
        
        // Check side toggled
        assertFalse(newState.isWhiteToMove());
    }

    @Test
    void testBlackEnPassantCapture() {
        // Black pawn on d4, white pawn on e4, en passant square is e3
        String fen = "rnbqkbnr/pppp1ppp/8/8/3pP3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 27; // d4
        move.destinationSquare = 20; // e3
        move.promotion = false;
        move.enPassant = true;
        move.castling = false;
        
        BoardState newState = state.applyMove(move);
        
        // Check black pawn moved to e3
        assertNotEquals(0, newState.blackPieces[BoardState.INDEX_PAWN] & (1L << 20));
        
        // Check white pawn removed from e4 (square 28)
        assertEquals(0, newState.whitePieces[BoardState.INDEX_PAWN] & (1L << 28));
        
        // Check side toggled
        assertTrue(newState.isWhiteToMove());
    }

    @Test
    void testWhitePawnPromotion() {
        String fen = "4k3/P7/8/8/8/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 48; // a7
        move.destinationSquare = 56; // a8
        move.promotion = true;
        move.enPassant = false;
        move.castling = false;
        move.pieceTypeToPromote = BoardState.INDEX_QUEEN;
        
        BoardState newState = state.applyMove(move);
        
        // Check pawn removed from a7
        assertEquals(0, newState.whitePieces[BoardState.INDEX_PAWN] & (1L << 48));
        
        // Check queen added at a8
        assertNotEquals(0, newState.whitePieces[BoardState.INDEX_QUEEN] & (1L << 56));
        
        // Check side toggled
        assertFalse(newState.isWhiteToMove());
    }

    @Test
    void testBlackPawnPromotion() {
        String fen = "4k3/8/8/8/8/8/p7/4K3 b - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 8; // a2
        move.destinationSquare = 0; // a1
        move.promotion = true;
        move.enPassant = false;
        move.castling = false;
        move.pieceTypeToPromote = BoardState.INDEX_QUEEN;
        
        BoardState newState = state.applyMove(move);
        
        // Check pawn removed from a2
        assertEquals(0, newState.blackPieces[BoardState.INDEX_PAWN] & (1L << 8));
        
        // Check queen added at a1
        assertNotEquals(0, newState.blackPieces[BoardState.INDEX_QUEEN] & (1L << 0));
        
        // Check side toggled
        assertTrue(newState.isWhiteToMove());
    }

    @Test
    void testKingMoveClearsCastlingRights() {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        FastMove move = new FastMove();
        move.originSquare = 4; // e1
        move.destinationSquare = 5; // f1 (normal king move)
        move.promotion = false;
        move.enPassant = false;
        move.castling = false;
        
        BoardState newState = state.applyMove(move);
        
        // Check white castling rights cleared
        assertFalse(newState.isWhiteKingsideCastling());
        assertFalse(newState.isWhiteQueensideCastling());
        
        // Check black castling rights preserved
        assertTrue(newState.isBlackKingsideCastling());
        assertTrue(newState.isBlackQueensideCastling());
    }

    @Test
    void testRookMoveClearsCastlingRight() {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        // Move h1 rook
        FastMove move = new FastMove();
        move.originSquare = 7; // h1
        move.destinationSquare = 6; // g1
        move.promotion = false;
        move.enPassant = false;
        move.castling = false;
        
        BoardState newState = state.applyMove(move);
        
        // Check only kingside castling cleared
        assertFalse(newState.isWhiteKingsideCastling());
        assertTrue(newState.isWhiteQueensideCastling());
    }

    @Test
    void testRookCaptureClearsCastlingRight() {
        String fen = "r3k2r/8/8/8/8/8/8/R3K1NR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        // White knight captures black rook on h8
        FastMove move = new FastMove();
        move.originSquare = 6; // g1
        move.destinationSquare = 63; // h8
        move.promotion = false;
        move.enPassant = false;
        move.castling = false;
        
        BoardState newState = state.applyMove(move);
        
        // Check black kingside castling cleared
        assertFalse(newState.isBlackKingsideCastling());
        
        // Check other castling rights preserved
        assertTrue(newState.isWhiteKingsideCastling());
        assertTrue(newState.isWhiteQueensideCastling());
        assertTrue(newState.isBlackQueensideCastling());
    }

    @Test
    void testEnPassantSquareSet() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        // e2-e4 (double pawn push)
        FastMove move = new FastMove();
        move.originSquare = 12; // e2
        move.destinationSquare = 28; // e4
        move.promotion = false;
        move.enPassant = false;
        move.castling = false;
        
        BoardState newState = state.applyMove(move);
        
        // Check en passant square set to e3 (square 20)
        assertEquals(20, newState.getEnPassantSquare());
    }

    @Test
    void testEnPassantSquareCleared() {
        String fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        // Black plays d7-d5 (different move, should clear e3 en passant)
        FastMove move = new FastMove();
        move.originSquare = 51; // d7
        move.destinationSquare = 35; // d5
        move.promotion = false;
        move.enPassant = false;
        move.castling = false;
        
        BoardState newState = state.applyMove(move);
        
        // Check new en passant square is d6 (square 43)
        assertEquals(43, newState.getEnPassantSquare());
    }

    @Test
    void testOriginalStateUnmodified() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        long originalPawns = state.whitePieces[BoardState.INDEX_PAWN];
        boolean originalTurn = state.isWhiteToMove();
        
        FastMove move = new FastMove();
        move.originSquare = 12; // e2
        move.destinationSquare = 28; // e4
        move.promotion = false;
        move.enPassant = false;
        move.castling = false;
        
        BoardState newState = state.applyMove(move);
        
        // Check original state unchanged
        assertEquals(originalPawns, state.whitePieces[BoardState.INDEX_PAWN]);
        assertEquals(originalTurn, state.isWhiteToMove());
        
        // Check new state is different
        assertNotEquals(originalPawns, newState.whitePieces[BoardState.INDEX_PAWN]);
        assertNotEquals(originalTurn, newState.isWhiteToMove());
    }

    @Test
    void testComplexSequence() {
        // Test a sequence of moves
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        // 1. e4
        FastMove move1 = new FastMove();
        move1.originSquare = 12;
        move1.destinationSquare = 28;
        BoardState state2 = state.applyMove(move1);
        
        assertFalse(state2.isWhiteToMove());
        assertEquals(20, state2.getEnPassantSquare());
        
        // 1... e5
        FastMove move2 = new FastMove();
        move2.originSquare = 52;
        move2.destinationSquare = 36;
        BoardState state3 = state2.applyMove(move2);
        
        assertTrue(state3.isWhiteToMove());
        assertEquals(44, state3.getEnPassantSquare());
        
        // 2. Nf3
        FastMove move3 = new FastMove();
        move3.originSquare = 6;
        move3.destinationSquare = 21;
        BoardState state4 = state3.applyMove(move3);
        
        assertFalse(state4.isWhiteToMove());
        assertEquals(-1, state4.getEnPassantSquare(), "En passant should be cleared after knight move");
    }
}
