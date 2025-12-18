package ch.adjudicator.agent.bitboard.adapter;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MoveAdapter - conversion between ChessLib Move and bitboard move encoding.
 * Phase 7: Adapter testing.
 */
class MoveAdapterTest {

    @Test
    void testSimpleMoveConversion() {
        // e2 to e4
        Move move = new Move(Square.E2, Square.E4);
        int bitboardMove = MoveAdapter.toBitboardMove(move);

        assertEquals(12, MoveAdapter.getFromSquare(bitboardMove), "From square should be e2 (12)");
        assertEquals(28, MoveAdapter.getToSquare(bitboardMove), "To square should be e4 (28)");
        assertEquals(MoveAdapter.FLAG_NONE, MoveAdapter.getFlags(bitboardMove), "No special flags");
    }

    @Test
    void testKnightMoveConversion() {
        // g1 to f3
        Move move = new Move(Square.G1, Square.F3);
        int bitboardMove = MoveAdapter.toBitboardMove(move);

        assertEquals(6, MoveAdapter.getFromSquare(bitboardMove), "From square should be g1 (6)");
        assertEquals(21, MoveAdapter.getToSquare(bitboardMove), "To square should be f3 (21)");
        assertEquals(MoveAdapter.FLAG_NONE, MoveAdapter.getFlags(bitboardMove));
    }

    @Test
    void testPromotionToQueen() {
        // e7 to e8, promote to queen
        Move move = new Move(Square.E7, Square.E8, Piece.WHITE_QUEEN);
        int bitboardMove = MoveAdapter.toBitboardMove(move);

        assertEquals(52, MoveAdapter.getFromSquare(bitboardMove), "From square should be e7 (52)");
        assertEquals(60, MoveAdapter.getToSquare(bitboardMove), "To square should be e8 (60)");
        assertEquals(MoveAdapter.FLAG_PROMOTION_QUEEN, MoveAdapter.getFlags(bitboardMove), "Promotion to queen flag");
        assertTrue(MoveAdapter.isPromotion(bitboardMove), "Should be a promotion");
    }

    @Test
    void testPromotionToKnight() {
        // e7 to e8, promote to knight
        Move move = new Move(Square.E7, Square.E8, Piece.WHITE_KNIGHT);
        int bitboardMove = MoveAdapter.toBitboardMove(move);

        assertEquals(MoveAdapter.FLAG_PROMOTION_KNIGHT, MoveAdapter.getFlags(bitboardMove), "Promotion to knight flag");
        assertTrue(MoveAdapter.isPromotion(bitboardMove), "Should be a promotion");
    }

    @Test
    void testPromotionToRook() {
        // a7 to a8, promote to rook
        Move move = new Move(Square.A7, Square.A8, Piece.WHITE_ROOK);
        int bitboardMove = MoveAdapter.toBitboardMove(move);

        assertEquals(MoveAdapter.FLAG_PROMOTION_ROOK, MoveAdapter.getFlags(bitboardMove), "Promotion to rook flag");
        assertTrue(MoveAdapter.isPromotion(bitboardMove), "Should be a promotion");
    }

    @Test
    void testPromotionToBishop() {
        // h7 to h8, promote to bishop
        Move move = new Move(Square.H7, Square.H8, Piece.WHITE_BISHOP);
        int bitboardMove = MoveAdapter.toBitboardMove(move);

        assertEquals(MoveAdapter.FLAG_PROMOTION_BISHOP, MoveAdapter.getFlags(bitboardMove), "Promotion to bishop flag");
        assertTrue(MoveAdapter.isPromotion(bitboardMove), "Should be a promotion");
    }

    @Test
    void testWhiteKingsideCastling() {
        // e1 to g1 (white kingside castling)
        Move move = new Move(Square.E1, Square.G1);
        int bitboardMove = MoveAdapter.toBitboardMove(move);

        assertEquals(4, MoveAdapter.getFromSquare(bitboardMove), "From square should be e1 (4)");
        assertEquals(6, MoveAdapter.getToSquare(bitboardMove), "To square should be g1 (6)");
        assertEquals(MoveAdapter.FLAG_CASTLING, MoveAdapter.getFlags(bitboardMove), "Castling flag");
        assertTrue(MoveAdapter.isCastling(bitboardMove), "Should be castling");
    }

    @Test
    void testWhiteQueensideCastling() {
        // e1 to c1 (white queenside castling)
        Move move = new Move(Square.E1, Square.C1);
        int bitboardMove = MoveAdapter.toBitboardMove(move);

        assertEquals(4, MoveAdapter.getFromSquare(bitboardMove), "From square should be e1 (4)");
        assertEquals(2, MoveAdapter.getToSquare(bitboardMove), "To square should be c1 (2)");
        assertEquals(MoveAdapter.FLAG_CASTLING, MoveAdapter.getFlags(bitboardMove), "Castling flag");
        assertTrue(MoveAdapter.isCastling(bitboardMove), "Should be castling");
    }

    @Test
    void testBlackKingsideCastling() {
        // e8 to g8 (black kingside castling)
        Move move = new Move(Square.E8, Square.G8);
        int bitboardMove = MoveAdapter.toBitboardMove(move);

        assertEquals(60, MoveAdapter.getFromSquare(bitboardMove), "From square should be e8 (60)");
        assertEquals(62, MoveAdapter.getToSquare(bitboardMove), "To square should be g8 (62)");
        assertEquals(MoveAdapter.FLAG_CASTLING, MoveAdapter.getFlags(bitboardMove), "Castling flag");
        assertTrue(MoveAdapter.isCastling(bitboardMove), "Should be castling");
    }

    @Test
    void testBlackQueensideCastling() {
        // e8 to c8 (black queenside castling)
        Move move = new Move(Square.E8, Square.C8);
        int bitboardMove = MoveAdapter.toBitboardMove(move);

        assertEquals(60, MoveAdapter.getFromSquare(bitboardMove), "From square should be e8 (60)");
        assertEquals(58, MoveAdapter.getToSquare(bitboardMove), "To square should be c8 (58)");
        assertEquals(MoveAdapter.FLAG_CASTLING, MoveAdapter.getFlags(bitboardMove), "Castling flag");
        assertTrue(MoveAdapter.isCastling(bitboardMove), "Should be castling");
    }

    @Test
    void testBitboardToChessLibMoveSimple() {
        // Create bitboard move for e2 to e4
        int bitboardMove = MoveAdapter.createMove(12, 28, MoveAdapter.FLAG_NONE);
        Move move = MoveAdapter.toChessLibMove(bitboardMove);

        assertEquals(Square.E2, move.getFrom(), "From square should be e2");
        assertEquals(Square.E4, move.getTo(), "To square should be e4");
        assertEquals(Piece.NONE, move.getPromotion(), "No promotion");
    }

    @Test
    void testBitboardToChessLibMoveWithPromotion() {
        // Create bitboard move for e7 to e8 with queen promotion
        int bitboardMove = MoveAdapter.createMove(52, 60, MoveAdapter.FLAG_PROMOTION_QUEEN);
        Move move = MoveAdapter.toChessLibMove(bitboardMove);

        assertEquals(Square.E7, move.getFrom(), "From square should be e7");
        assertEquals(Square.E8, move.getTo(), "To square should be e8");
        assertEquals(Piece.WHITE_QUEEN, move.getPromotion(), "Promotion to queen");
    }

    @Test
    void testRoundTripConversionSimpleMove() {
        Move originalMove = new Move(Square.D2, Square.D4);
        int bitboardMove = MoveAdapter.toBitboardMove(originalMove);
        Move reconstructedMove = MoveAdapter.toChessLibMove(bitboardMove);

        assertEquals(originalMove.getFrom(), reconstructedMove.getFrom(), "From square should match");
        assertEquals(originalMove.getTo(), reconstructedMove.getTo(), "To square should match");
        assertEquals(originalMove.getPromotion(), reconstructedMove.getPromotion(), "Promotion should match");
    }

    @Test
    void testRoundTripConversionPromotion() {
        Move originalMove = new Move(Square.A7, Square.A8, Piece.WHITE_ROOK);
        int bitboardMove = MoveAdapter.toBitboardMove(originalMove);
        Move reconstructedMove = MoveAdapter.toChessLibMove(bitboardMove);

        assertEquals(originalMove.getFrom(), reconstructedMove.getFrom(), "From square should match");
        assertEquals(originalMove.getTo(), reconstructedMove.getTo(), "To square should match");
        assertEquals(Piece.WHITE_ROOK, reconstructedMove.getPromotion(), "Promotion should match");
    }

    @Test
    void testUciStringSimpleMove() {
        int bitboardMove = MoveAdapter.createMove(12, 28, MoveAdapter.FLAG_NONE);
        String uci = MoveAdapter.toUciString(bitboardMove);

        assertEquals("e2e4", uci, "UCI string should be e2e4");
    }

    @Test
    void testUciStringPromotion() {
        int bitboardMove = MoveAdapter.createMove(52, 60, MoveAdapter.FLAG_PROMOTION_QUEEN);
        String uci = MoveAdapter.toUciString(bitboardMove);

        assertEquals("e7e8q", uci, "UCI string should be e7e8q");
    }

    @Test
    void testUciStringPromotionKnight() {
        int bitboardMove = MoveAdapter.createMove(52, 60, MoveAdapter.FLAG_PROMOTION_KNIGHT);
        String uci = MoveAdapter.toUciString(bitboardMove);

        assertEquals("e7e8n", uci, "UCI string should be e7e8n");
    }

    @Test
    void testUciStringCastling() {
        int bitboardMove = MoveAdapter.createMove(4, 6, MoveAdapter.FLAG_CASTLING);
        String uci = MoveAdapter.toUciString(bitboardMove);

        assertEquals("e1g1", uci, "UCI string should be e1g1");
    }

    @Test
    void testCreateMoveFromComponents() {
        int from = 12; // e2
        int to = 28; // e4
        int flags = MoveAdapter.FLAG_NONE;

        int bitboardMove = MoveAdapter.createMove(from, to, flags);

        assertEquals(from, MoveAdapter.getFromSquare(bitboardMove));
        assertEquals(to, MoveAdapter.getToSquare(bitboardMove));
        assertEquals(flags, MoveAdapter.getFlags(bitboardMove));
    }

    @Test
    void testIsPromotionChecks() {
        int normalMove = MoveAdapter.createMove(12, 28, MoveAdapter.FLAG_NONE);
        int promotionMove = MoveAdapter.createMove(52, 60, MoveAdapter.FLAG_PROMOTION_QUEEN);
        int castlingMove = MoveAdapter.createMove(4, 6, MoveAdapter.FLAG_CASTLING);

        assertFalse(MoveAdapter.isPromotion(normalMove), "Normal move should not be promotion");
        assertTrue(MoveAdapter.isPromotion(promotionMove), "Promotion move should be promotion");
        assertFalse(MoveAdapter.isPromotion(castlingMove), "Castling move should not be promotion");
    }

    @Test
    void testIsCastlingChecks() {
        int normalMove = MoveAdapter.createMove(12, 28, MoveAdapter.FLAG_NONE);
        int castlingMove = MoveAdapter.createMove(4, 6, MoveAdapter.FLAG_CASTLING);
        int promotionMove = MoveAdapter.createMove(52, 60, MoveAdapter.FLAG_PROMOTION_QUEEN);

        assertFalse(MoveAdapter.isCastling(normalMove), "Normal move should not be castling");
        assertTrue(MoveAdapter.isCastling(castlingMove), "Castling move should be castling");
        assertFalse(MoveAdapter.isCastling(promotionMove), "Promotion move should not be castling");
    }

    @Test
    void testIsEnPassantChecks() {
        int normalMove = MoveAdapter.createMove(12, 28, MoveAdapter.FLAG_NONE);
        int enPassantMove = MoveAdapter.createMove(35, 42, MoveAdapter.FLAG_EN_PASSANT);
        int castlingMove = MoveAdapter.createMove(4, 6, MoveAdapter.FLAG_CASTLING);

        assertFalse(MoveAdapter.isEnPassant(normalMove), "Normal move should not be en passant");
        assertTrue(MoveAdapter.isEnPassant(enPassantMove), "En passant move should be en passant");
        assertFalse(MoveAdapter.isEnPassant(castlingMove), "Castling move should not be en passant");
    }

    @Test
    void testAllCornerSquares() {
        // Test moves from all corners to ensure square encoding works correctly
        Move a1h1 = new Move(Square.A1, Square.H1);
        Move a8h8 = new Move(Square.A8, Square.H8);
        Move h1a1 = new Move(Square.H1, Square.A1);
        Move h8a8 = new Move(Square.H8, Square.A8);

        int move1 = MoveAdapter.toBitboardMove(a1h1);
        int move2 = MoveAdapter.toBitboardMove(a8h8);
        int move3 = MoveAdapter.toBitboardMove(h1a1);
        int move4 = MoveAdapter.toBitboardMove(h8a8);

        assertEquals(0, MoveAdapter.getFromSquare(move1), "a1 should be 0");
        assertEquals(7, MoveAdapter.getToSquare(move1), "h1 should be 7");

        assertEquals(56, MoveAdapter.getFromSquare(move2), "a8 should be 56");
        assertEquals(63, MoveAdapter.getToSquare(move2), "h8 should be 63");

        assertEquals(7, MoveAdapter.getFromSquare(move3), "h1 should be 7");
        assertEquals(0, MoveAdapter.getToSquare(move3), "a1 should be 0");

        assertEquals(63, MoveAdapter.getFromSquare(move4), "h8 should be 63");
        assertEquals(56, MoveAdapter.getToSquare(move4), "a8 should be 56");
    }
}
