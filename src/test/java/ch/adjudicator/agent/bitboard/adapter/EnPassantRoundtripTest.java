package ch.adjudicator.agent.bitboard.adapter;

import ch.adjudicator.agent.bitboard.model.BoardState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for en passant square roundtrip conversion using INDEX_EN_PASSANT_SQUARE_MSB 
 * and INDEX_EN_PASSANT_SQUARE_LSB (6-bit representation for board indices 0-63).
 */
class EnPassantRoundtripTest {

    @Test
    void testEnPassantSquareA3() {
        // a3 is square 16
        String fen = "rnbqkbnr/pppppppp/8/8/8/P7/1PPPPPPP/RNBQKBNR b KQkq a3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(16, state.getEnPassantSquare(), "En passant square should be a3 (16)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareA6() {
        // a6 is square 40
        String fen = "rnbqkbnr/1ppppppp/p7/8/8/8/PPPPPPPP/RNBQKBNR w KQkq a6 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(40, state.getEnPassantSquare(), "En passant square should be a6 (40)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareH3() {
        // h3 is square 23
        String fen = "rnbqkbnr/pppppppp/8/8/8/7P/PPPPPPP1/RNBQKBNR b KQkq h3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(23, state.getEnPassantSquare(), "En passant square should be h3 (23)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareH6() {
        // h6 is square 47
        String fen = "rnbqkbnr/ppppppp1/7p/8/8/8/PPPPPPPP/RNBQKBNR w KQkq h6 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(47, state.getEnPassantSquare(), "En passant square should be h6 (47)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareD3() {
        // d3 is square 19
        String fen = "rnbqkbnr/pppppppp/8/8/8/3P4/PPP1PPPP/RNBQKBNR b KQkq d3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(19, state.getEnPassantSquare(), "En passant square should be d3 (19)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareD6() {
        // d6 is square 43
        String fen = "rnbqkbnr/ppp1pppp/3p4/8/8/8/PPPPPPPP/RNBQKBNR w KQkq d6 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(43, state.getEnPassantSquare(), "En passant square should be d6 (43)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareE3() {
        // e3 is square 20
        String fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(20, state.getEnPassantSquare(), "En passant square should be e3 (20)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareE6() {
        // e6 is square 44
        String fen = "rnbqkbnr/pppp1ppp/4p3/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e6 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(44, state.getEnPassantSquare(), "En passant square should be e6 (44)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareC3() {
        // c3 is square 18
        String fen = "rnbqkbnr/pppppppp/8/8/8/2P5/PP1PPPPP/RNBQKBNR b KQkq c3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(18, state.getEnPassantSquare(), "En passant square should be c3 (18)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareF6() {
        // f6 is square 45
        String fen = "rnbqkbnr/ppppp1pp/5p2/8/8/8/PPPPPPPP/RNBQKBNR w KQkq f6 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(45, state.getEnPassantSquare(), "En passant square should be f6 (45)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareB3() {
        // b3 is square 17
        String fen = "rnbqkbnr/pppppppp/8/8/8/1P6/P1PPPPPP/RNBQKBNR b KQkq b3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(17, state.getEnPassantSquare(), "En passant square should be b3 (17)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantSquareG6() {
        // g6 is square 46
        String fen = "rnbqkbnr/pppppp1p/6p1/8/8/8/PPPPPPPP/RNBQKBNR w KQkq g6 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(46, state.getEnPassantSquare(), "En passant square should be g6 (46)");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testNoEnPassantSquare() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(-1, state.getEnPassantSquare(), "No en passant square should return -1");
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantBitRepresentation() {
        // Test that the 6-bit representation (bits 0-5) is used correctly
        // Testing all valid en passant squares (rank 3 for white, rank 6 for black)
        
        // Rank 3 squares (16-23): a3-h3
        for (int square = 16; square <= 23; square++) {
            BoardState state = new BoardState();
            state.setEnPassantSquare(square);
            
            int retrieved = state.getEnPassantSquare();
            assertEquals(square, retrieved, "En passant square " + square + " should be stored and retrieved correctly");
            
            // Verify the bits are set correctly using INDEX_EN_PASSANT_SQUARE_LSB and MSB
            long maskedValue = state.bitAuxiliaries & 0x3F; // Extract bits 0-5
            assertEquals(square, maskedValue, "6-bit representation should match square index");
            
            // Verify active flag is set
            long activeFlag = state.bitAuxiliaries & (1L << BoardState.INDEX_EN_PASSANT_SQUARE_ACTIVE);
            assertTrue(activeFlag != 0, "Active flag should be set");
        }
        
        // Rank 6 squares (40-47): a6-h6
        for (int square = 40; square <= 47; square++) {
            BoardState state = new BoardState();
            state.setEnPassantSquare(square);
            
            int retrieved = state.getEnPassantSquare();
            assertEquals(square, retrieved, "En passant square " + square + " should be stored and retrieved correctly");
            
            // Verify the bits are set correctly
            long maskedValue = state.bitAuxiliaries & 0x3F; // Extract bits 0-5
            assertEquals(square, maskedValue, "6-bit representation should match square index");
            
            // Verify active flag is set
            long activeFlag = state.bitAuxiliaries & (1L << BoardState.INDEX_EN_PASSANT_SQUARE_ACTIVE);
            assertTrue(activeFlag != 0, "Active flag should be set");
        }
    }

    @Test
    void testEnPassantClearOperation() {
        BoardState state = new BoardState();
        
        // Set an en passant square
        state.setEnPassantSquare(20); // e3
        assertEquals(20, state.getEnPassantSquare());
        
        // Clear it
        state.setEnPassantSquare(-1);
        assertEquals(-1, state.getEnPassantSquare());
        
        // Verify all en passant bits are cleared
        long enPassantBits = state.bitAuxiliaries & 0x3F; // Bits 0-5
        assertEquals(0, enPassantBits, "All en passant bits should be cleared");
    }

    @Test
    void testEnPassantWithAllCastlingRights() {
        String fen = "r3k2r/ppp2ppp/2n1pn2/3p4/3P4/2N1PN2/PPP2PPP/R3K2R w KQkq d6 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(43, state.getEnPassantSquare(), "En passant square should be d6 (43)");
        assertTrue(state.isWhiteKingsideCastlingPossible());
        assertTrue(state.isWhiteQueensideCastlingPossible());
        assertTrue(state.isBlackKingsideCastlingPossible());
        assertTrue(state.isBlackQueensideCastlingPossible());
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }

    @Test
    void testEnPassantWithNoCastlingRights() {
        String fen = "4k3/pppppppp/8/8/8/3P4/PPP1PPPP/4K3 b - d3 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(fen);
        
        assertEquals(19, state.getEnPassantSquare(), "En passant square should be d3 (19)");
        assertFalse(state.isWhiteKingsideCastlingPossible());
        assertFalse(state.isWhiteQueensideCastlingPossible());
        assertFalse(state.isBlackKingsideCastlingPossible());
        assertFalse(state.isBlackQueensideCastlingPossible());
        
        String reconstructedFen = ChessLibAdapter.boardStateToFen(state);
        assertEquals(fen, reconstructedFen, "FEN should round-trip correctly");
    }
}
