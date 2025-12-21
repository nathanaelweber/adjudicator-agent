package ch.adjudicator.agent.bitboard.adapter;

import ch.adjudicator.agent.bitboard.model.BoardState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ChessLibAdapter - FEN to BoardState conversion.
 * Phase 7: Adapter testing.
 */
class ChessLibAdapterTest {

    @Test
    void testStartingPositionFenToBoardState() {
        String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(startFen);

        // Check white pieces
        assertEquals(0x000000000000FF00L, state.whitePieces[BoardState.INDEX_PAWN], "White pawns incorrect");
        assertEquals(0x0000000000000042L, state.whitePieces[BoardState.INDEX_KNIGHT], "White knights incorrect");
        assertEquals(0x0000000000000024L, state.whitePieces[BoardState.INDEX_BISHOP], "White bishops incorrect");
        assertEquals(0x0000000000000081L, state.whitePieces[BoardState.INDEX_ROOK], "White rooks incorrect");
        assertEquals(0x0000000000000008L, state.whitePieces[BoardState.INDEX_QUEEN], "White queen incorrect");
        assertEquals(0x0000000000000010L, state.whitePieces[BoardState.INDEX_KING], "White king incorrect");

        // Check black pieces
        assertEquals(0x00FF000000000000L, state.blackPieces[BoardState.INDEX_PAWN], "Black pawns incorrect");
        assertEquals(0x4200000000000000L, state.blackPieces[BoardState.INDEX_KNIGHT], "Black knights incorrect");
        assertEquals(0x2400000000000000L, state.blackPieces[BoardState.INDEX_BISHOP], "Black bishops incorrect");
        assertEquals(0x8100000000000000L, state.blackPieces[BoardState.INDEX_ROOK], "Black rooks incorrect");
        assertEquals(0x0800000000000000L, state.blackPieces[BoardState.INDEX_QUEEN], "Black queen incorrect");
        assertEquals(0x1000000000000000L, state.blackPieces[BoardState.INDEX_KING], "Black king incorrect");

        // Check game state
        assertTrue(state.isWhiteToMove(), "White should be to move");
        assertTrue(state.isWhiteKingsideCastlingPossible(), "White kingside castling should be available");
        assertTrue(state.isWhiteQueensideCastlingPossible(), "White queenside castling should be available");
        assertTrue(state.isBlackKingsideCastlingPossible(), "Black kingside castling should be available");
        assertTrue(state.isBlackQueensideCastlingPossible(), "Black queenside castling should be available");
        assertEquals(-1, state.getEnPassantSquare(), "No en passant square");
    }

    @Test
    void testEmptyBoardWithKings() {
        String fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);

        // Only kings should be present
        assertEquals(0x0000000000000010L, state.whitePieces[BoardState.INDEX_KING], "White king on e1");
        assertEquals(0x1000000000000000L, state.blackPieces[BoardState.INDEX_KING], "Black king on e8");

        // All other pieces should be 0
        assertEquals(0L, state.whitePieces[BoardState.INDEX_PAWN]);
        assertEquals(0L, state.whitePieces[BoardState.INDEX_KNIGHT]);
        assertEquals(0L, state.whitePieces[BoardState.INDEX_BISHOP]);
        assertEquals(0L, state.whitePieces[BoardState.INDEX_ROOK]);
        assertEquals(0L, state.whitePieces[BoardState.INDEX_QUEEN]);
        assertEquals(0L, state.blackPieces[BoardState.INDEX_PAWN]);
        assertEquals(0L, state.blackPieces[BoardState.INDEX_KNIGHT]);
        assertEquals(0L, state.blackPieces[BoardState.INDEX_BISHOP]);
        assertEquals(0L, state.blackPieces[BoardState.INDEX_ROOK]);
        assertEquals(0L, state.blackPieces[BoardState.INDEX_QUEEN]);

        // No castling rights
        assertFalse(state.isWhiteKingsideCastlingPossible());
        assertFalse(state.isWhiteQueensideCastlingPossible());
        assertFalse(state.isBlackKingsideCastlingPossible());
        assertFalse(state.isBlackQueensideCastlingPossible());
    }

    @Test
    void testPositionWithEnPassant() {
        String fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);

        assertFalse(state.isWhiteToMove(), "Black to move");
        assertEquals(20, state.getEnPassantSquare(), "En passant square should be e3 (square 20)");
    }

    @Test
    void testPositionWithPartialCastlingRights() {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w Kq - 5 10";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);

        assertTrue(state.isWhiteKingsideCastlingPossible(), "White kingside castling available");
        assertFalse(state.isWhiteQueensideCastlingPossible(), "White queenside castling not available");
        assertFalse(state.isBlackKingsideCastlingPossible(), "Black kingside castling not available");
        assertTrue(state.isBlackQueensideCastlingPossible(), "Black queenside castling available");
    }

    @Test
    void testBlackToMove() {
        String fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);

        assertFalse(state.isWhiteToMove(), "Black should be to move");
    }

    @Test
    void testBoardStateToFenStartingPosition() {
        String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(startFen);
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);

        assertEquals(startFen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testBoardStateToFenEmptyBoard() {
        String fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);

        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testBoardStateToFenWithEnPassant() {
        String fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);

        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly with en passant");
    }

    @Test
    void testBoardStateToFenPartialCastling() {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w Kq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);

        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly with partial castling");
    }

    @Test
    void testKiwiPetePosition() {
        // Famous perft test position
        String fen = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);

        // Verify some key pieces
        assertNotEquals(0L, state.whitePieces[BoardState.INDEX_KING]);
        assertNotEquals(0L, state.blackPieces[BoardState.INDEX_KING]);
        assertNotEquals(0L, state.whitePieces[BoardState.INDEX_QUEEN]);
        assertNotEquals(0L, state.blackPieces[BoardState.INDEX_QUEEN]);

        // Verify castling rights
        assertTrue(state.isWhiteKingsideCastlingPossible());
        assertTrue(state.isWhiteQueensideCastlingPossible());
        assertTrue(state.isBlackKingsideCastlingPossible());
        assertTrue(state.isBlackQueensideCastlingPossible());

        // Round-trip test
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "KiwiPete position should round-trip correctly");
    }

    @Test
    void testPromotionPosition() {
        String fen = "4k3/P7/8/8/8/8/7p/4K3 w - - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);

        // White pawn on a7 (square 48)
        assertEquals(0x0001000000000000L, state.whitePieces[BoardState.INDEX_PAWN]);
        // Black pawn on h2 (square 15)
        assertEquals(0x0000000000008000L, state.blackPieces[BoardState.INDEX_PAWN]);

        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen);
    }

    @Test
    void testComplexMiddlegamePosition() {
        String fen = "r1bqk2r/pp2bppp/2n1pn2/3p4/2PP4/2N1PN2/PP2BPPP/R1BQK2R b KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);

        assertFalse(state.isWhiteToMove());
        assertTrue(state.isWhiteKingsideCastlingPossible());
        assertTrue(state.isWhiteQueensideCastlingPossible());
        assertTrue(state.isBlackKingsideCastlingPossible());
        assertTrue(state.isBlackQueensideCastlingPossible());

        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen);
    }
}
