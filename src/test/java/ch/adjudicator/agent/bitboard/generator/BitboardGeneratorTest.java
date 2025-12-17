package ch.adjudicator.agent.bitboard.generator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for BitboardGenerator covering Phase 1 and Phase 2 functionality.
 */
class BitboardGeneratorTest {

    // ============================================================
    // Phase 1 Tests: File Masks
    // ============================================================

    @Test
    void testFileAMask() {
        // FILE_A should have bits set only on the A-file (leftmost column)
        // Squares: 0, 8, 16, 24, 32, 40, 48, 56
        assertEquals(0x0101010101010101L, BitboardGenerator.FILE_A);
        
        // Verify it has exactly 8 bits set
        assertEquals(8, BitboardGenerator.bitCount(BitboardGenerator.FILE_A));
    }

    @Test
    void testFileHMask() {
        // FILE_H should have bits set only on the H-file (rightmost column)
        // Squares: 7, 15, 23, 31, 39, 47, 55, 63
        assertEquals(0x8080808080808080L, BitboardGenerator.FILE_H);
        
        // Verify it has exactly 8 bits set
        assertEquals(8, BitboardGenerator.bitCount(BitboardGenerator.FILE_H));
    }

    @Test
    void testFileABMask() {
        // FILE_AB should cover both A and B files
        // 16 bits total (8 on A-file + 8 on B-file)
        assertEquals(16, BitboardGenerator.bitCount(BitboardGenerator.FILE_AB));
        
        // Should include FILE_A
        assertEquals(BitboardGenerator.FILE_A, BitboardGenerator.FILE_A & BitboardGenerator.FILE_AB);
    }

    @Test
    void testFileGHMask() {
        // FILE_GH should cover both G and H files
        // 16 bits total (8 on G-file + 8 on H-file)
        assertEquals(16, BitboardGenerator.bitCount(BitboardGenerator.FILE_GH));
        
        // Should include FILE_H
        assertEquals(BitboardGenerator.FILE_H, BitboardGenerator.FILE_H & BitboardGenerator.FILE_GH);
    }

    // ============================================================
    // Phase 1 Tests: Utility Methods
    // ============================================================

    @Test
    void testGetLSB() {
        // Single bit set
        assertEquals(0, BitboardGenerator.getLSB(1L));
        assertEquals(5, BitboardGenerator.getLSB(1L << 5));
        assertEquals(63, BitboardGenerator.getLSB(1L << 63));
        
        // Multiple bits set - should return the lowest one
        assertEquals(0, BitboardGenerator.getLSB(0b1010101L));
        assertEquals(3, BitboardGenerator.getLSB(0b11111000L));
        
        // Empty bitboard
        assertEquals(64, BitboardGenerator.getLSB(0L));
    }

    @Test
    void testBitCount() {
        // Empty board
        assertEquals(0, BitboardGenerator.bitCount(0L));
        
        // Single bit
        assertEquals(1, BitboardGenerator.bitCount(1L));
        assertEquals(1, BitboardGenerator.bitCount(1L << 32));
        
        // Multiple bits
        assertEquals(8, BitboardGenerator.bitCount(0xFFL));
        assertEquals(64, BitboardGenerator.bitCount(-1L)); // All bits set
        
        // Specific patterns
        assertEquals(4, BitboardGenerator.bitCount(0b1111L));
        assertEquals(3, BitboardGenerator.bitCount(0b10101L));
    }

    // ============================================================
    // Phase 2 Tests: Knight Attacks
    // ============================================================

    @Test
    void testKnightAttacksCornerA1() {
        // Knight on a1 (square 0) can move to: b3 (17), c2 (10)
        long attacks = BitboardGenerator.KNIGHT_ATTACKS[0];
        assertEquals(2, BitboardGenerator.bitCount(attacks));
        
        // Verify specific squares
        assertTrue((attacks & (1L << 17)) != 0); // b3
        assertTrue((attacks & (1L << 10)) != 0); // c2
    }

    @Test
    void testKnightAttacksCornerH1() {
        // Knight on h1 (square 7) can move to: f2 (13), g3 (22)
        long attacks = BitboardGenerator.KNIGHT_ATTACKS[7];
        assertEquals(2, BitboardGenerator.bitCount(attacks));
        
        // Verify specific squares
        assertTrue((attacks & (1L << 13)) != 0); // f2
        assertTrue((attacks & (1L << 22)) != 0); // g3
    }

    @Test
    void testKnightAttacksCornerA8() {
        // Knight on a8 (square 56) can move to: b6 (41), c7 (50)
        long attacks = BitboardGenerator.KNIGHT_ATTACKS[56];
        assertEquals(2, BitboardGenerator.bitCount(attacks));
        
        // Verify specific squares
        assertTrue((attacks & (1L << 41)) != 0); // b6
        assertTrue((attacks & (1L << 50)) != 0); // c7
    }

    @Test
    void testKnightAttacksCornerH8() {
        // Knight on h8 (square 63) can move to: f7 (53), g6 (46)
        long attacks = BitboardGenerator.KNIGHT_ATTACKS[63];
        assertEquals(2, BitboardGenerator.bitCount(attacks));
        
        // Verify specific squares
        assertTrue((attacks & (1L << 53)) != 0); // f7
        assertTrue((attacks & (1L << 46)) != 0); // g6
    }

    @Test
    void testKnightAttacksEdge() {
        // Knight on e1 (square 4) - on edge but not corner
        long attacks = BitboardGenerator.KNIGHT_ATTACKS[4];
        assertEquals(4, BitboardGenerator.bitCount(attacks));
    }

    @Test
    void testKnightAttacksCenter() {
        // Knight on e4 (square 28) - center of board, should have all 8 moves
        long attacks = BitboardGenerator.KNIGHT_ATTACKS[28];
        assertEquals(8, BitboardGenerator.bitCount(attacks));
        
        // Verify all 8 possible knight moves from e4
        assertTrue((attacks & (1L << 45)) != 0); // f6 (2 up, 1 right)
        assertTrue((attacks & (1L << 38)) != 0); // g5 (1 up, 2 right)
        assertTrue((attacks & (1L << 22)) != 0); // g3 (1 down, 2 right)
        assertTrue((attacks & (1L << 13)) != 0); // f2 (2 down, 1 right)
        assertTrue((attacks & (1L << 11)) != 0); // d2 (2 down, 1 left)
        assertTrue((attacks & (1L << 18)) != 0); // c3 (1 down, 2 left)
        assertTrue((attacks & (1L << 34)) != 0); // c5 (1 up, 2 left)
        assertTrue((attacks & (1L << 43)) != 0); // d6 (2 up, 1 left)
    }

    @Test
    void testKnightAttacksD4() {
        // Knight on d4 (square 27) - also should have all 8 moves
        long attacks = BitboardGenerator.KNIGHT_ATTACKS[27];
        assertEquals(8, BitboardGenerator.bitCount(attacks));
    }

    // ============================================================
    // Phase 2 Tests: King Attacks
    // ============================================================

    @Test
    void testKingAttacksCornerA1() {
        // King on a1 (square 0) can move to: a2 (8), b1 (1), b2 (9)
        long attacks = BitboardGenerator.KING_ATTACKS[0];
        assertEquals(3, BitboardGenerator.bitCount(attacks));
        
        // Verify specific squares
        assertTrue((attacks & (1L << 8)) != 0);  // a2 (up)
        assertTrue((attacks & (1L << 1)) != 0);  // b1 (right)
        assertTrue((attacks & (1L << 9)) != 0);  // b2 (up-right)
    }

    @Test
    void testKingAttacksCornerH1() {
        // King on h1 (square 7) can move to: h2 (15), g1 (6), g2 (14)
        long attacks = BitboardGenerator.KING_ATTACKS[7];
        assertEquals(3, BitboardGenerator.bitCount(attacks));
        
        // Verify specific squares
        assertTrue((attacks & (1L << 15)) != 0); // h2 (up)
        assertTrue((attacks & (1L << 6)) != 0);  // g1 (left)
        assertTrue((attacks & (1L << 14)) != 0); // g2 (up-left)
    }

    @Test
    void testKingAttacksCornerA8() {
        // King on a8 (square 56) can move to: a7 (48), b8 (57), b7 (49)
        long attacks = BitboardGenerator.KING_ATTACKS[56];
        assertEquals(3, BitboardGenerator.bitCount(attacks));
        
        // Verify specific squares
        assertTrue((attacks & (1L << 48)) != 0); // a7 (down)
        assertTrue((attacks & (1L << 57)) != 0); // b8 (right)
        assertTrue((attacks & (1L << 49)) != 0); // b7 (down-right)
    }

    @Test
    void testKingAttacksCornerH8() {
        // King on h8 (square 63) can move to: h7 (55), g8 (62), g7 (54)
        long attacks = BitboardGenerator.KING_ATTACKS[63];
        assertEquals(3, BitboardGenerator.bitCount(attacks));
        
        // Verify specific squares
        assertTrue((attacks & (1L << 55)) != 0); // h7 (down)
        assertTrue((attacks & (1L << 62)) != 0); // g8 (left)
        assertTrue((attacks & (1L << 54)) != 0); // g7 (down-left)
    }

    @Test
    void testKingAttacksEdge() {
        // King on e1 (square 4) - on edge but not corner
        long attacks = BitboardGenerator.KING_ATTACKS[4];
        assertEquals(5, BitboardGenerator.bitCount(attacks));
    }

    @Test
    void testKingAttacksCenter() {
        // King on e4 (square 28) - center of board, should have all 8 moves
        long attacks = BitboardGenerator.KING_ATTACKS[28];
        assertEquals(8, BitboardGenerator.bitCount(attacks));
        
        // Verify all 8 possible king moves from e4
        assertTrue((attacks & (1L << 36)) != 0); // e5 (up)
        assertTrue((attacks & (1L << 20)) != 0); // e3 (down)
        assertTrue((attacks & (1L << 29)) != 0); // f4 (right)
        assertTrue((attacks & (1L << 27)) != 0); // d4 (left)
        assertTrue((attacks & (1L << 37)) != 0); // f5 (up-right)
        assertTrue((attacks & (1L << 35)) != 0); // d5 (up-left)
        assertTrue((attacks & (1L << 21)) != 0); // f3 (down-right)
        assertTrue((attacks & (1L << 19)) != 0); // d3 (down-left)
    }

    @Test
    void testKingAttacksD4() {
        // King on d4 (square 27) - also should have all 8 moves
        long attacks = BitboardGenerator.KING_ATTACKS[27];
        assertEquals(8, BitboardGenerator.bitCount(attacks));
    }

    // ============================================================
    // Edge Case Tests
    // ============================================================

    @Test
    void testAllSquaresHaveKnightAttacks() {
        // Every square should have at least 2 knight attacks (corners)
        // and at most 8 (center squares)
        for (int square = 0; square < 64; square++) {
            long attacks = BitboardGenerator.KNIGHT_ATTACKS[square];
            int count = BitboardGenerator.bitCount(attacks);
            assertTrue(count >= 2 && count <= 8, 
                "Square " + square + " has invalid knight attack count: " + count);
        }
    }

    @Test
    void testAllSquaresHaveKingAttacks() {
        // Every square should have at least 3 king attacks (corners)
        // and at most 8 (center/near-center squares)
        for (int square = 0; square < 64; square++) {
            long attacks = BitboardGenerator.KING_ATTACKS[square];
            int count = BitboardGenerator.bitCount(attacks);
            assertTrue(count >= 3 && count <= 8, 
                "Square " + square + " has invalid king attack count: " + count);
        }
    }

    @Test
    void testNoSelfAttack() {
        // Knight and King should never attack their own square
        for (int square = 0; square < 64; square++) {
            long knightAttacks = BitboardGenerator.KNIGHT_ATTACKS[square];
            long kingAttacks = BitboardGenerator.KING_ATTACKS[square];
            long squareBit = 1L << square;
            
            assertEquals(0, knightAttacks & squareBit, 
                "Knight on square " + square + " attacks itself");
            assertEquals(0, kingAttacks & squareBit, 
                "King on square " + square + " attacks itself");
        }
    }

    // ============================================================
    // Phase 3 Tests: White Pawn Moves
    // ============================================================

    @Test
    void testWhitePawnSinglePush() {
        // Pawns on e2 (12) and d2 (11), empty board
        long pawns = (1L << 12) | (1L << 11);
        long empty = ~0L; // All squares empty
        
        long moves = BitboardGenerator.getWhitePawnSinglePush(pawns, empty);
        
        // Should move to e3 (20) and d3 (19)
        assertEquals(2, BitboardGenerator.bitCount(moves));
        assertTrue((moves & (1L << 20)) != 0); // e3
        assertTrue((moves & (1L << 19)) != 0); // d3
    }

    @Test
    void testWhitePawnSinglePushBlocked() {
        // Pawn on e2 (12), square e3 (20) is blocked
        long pawns = 1L << 12;
        long empty = ~(1L << 20); // e3 is not empty
        
        long moves = BitboardGenerator.getWhitePawnSinglePush(pawns, empty);
        
        // Should have no moves
        assertEquals(0, BitboardGenerator.bitCount(moves));
    }

    @Test
    void testWhitePawnDoublePush() {
        // Pawns on e2 (12) and d2 (11), empty board
        long pawns = (1L << 12) | (1L << 11);
        long empty = ~0L; // All squares empty
        
        long moves = BitboardGenerator.getWhitePawnDoublePush(pawns, empty);
        
        // Should move to e4 (28) and d4 (27)
        assertEquals(2, BitboardGenerator.bitCount(moves));
        assertTrue((moves & (1L << 28)) != 0); // e4
        assertTrue((moves & (1L << 27)) != 0); // d4
    }

    @Test
    void testWhitePawnDoublePushBlockedFirstSquare() {
        // Pawn on e2 (12), square e3 (20) is blocked
        long pawns = 1L << 12;
        long empty = ~(1L << 20); // e3 is not empty
        
        long moves = BitboardGenerator.getWhitePawnDoublePush(pawns, empty);
        
        // Should have no moves
        assertEquals(0, BitboardGenerator.bitCount(moves));
    }

    @Test
    void testWhitePawnDoublePushBlockedSecondSquare() {
        // Pawn on e2 (12), square e4 (28) is blocked but e3 (20) is empty
        long pawns = 1L << 12;
        long empty = ~(1L << 28); // e4 is not empty, but e3 is empty
        
        long moves = BitboardGenerator.getWhitePawnDoublePush(pawns, empty);
        
        // Should have no moves
        assertEquals(0, BitboardGenerator.bitCount(moves));
    }

    @Test
    void testWhitePawnDoublePushFromWrongRank() {
        // Pawn on e4 (28), not on starting rank
        long pawns = 1L << 28;
        long empty = ~0L;
        
        long moves = BitboardGenerator.getWhitePawnDoublePush(pawns, empty);
        
        // Should have no double push moves from e4
        assertEquals(0, BitboardGenerator.bitCount(moves));
    }

    @Test
    void testWhitePawnCaptureRight() {
        // Pawn on e4 (28), black pieces on f5 (37) and d5 (35)
        long pawns = 1L << 28;
        long blackPieces = (1L << 37) | (1L << 35);
        
        long moves = BitboardGenerator.getWhitePawnCaptureRight(pawns, blackPieces);
        
        // Should capture on f5 (37)
        assertEquals(1, BitboardGenerator.bitCount(moves));
        assertTrue((moves & (1L << 37)) != 0); // f5
    }

    @Test
    void testWhitePawnCaptureLeft() {
        // Pawn on e4 (28), black pieces on f5 (37) and d5 (35)
        long pawns = 1L << 28;
        long blackPieces = (1L << 37) | (1L << 35);
        
        long moves = BitboardGenerator.getWhitePawnCaptureLeft(pawns, blackPieces);
        
        // Should capture on d5 (35)
        assertEquals(1, BitboardGenerator.bitCount(moves));
        assertTrue((moves & (1L << 35)) != 0); // d5
    }

    @Test
    void testWhitePawnCaptureNoWrapAround() {
        // Pawn on a4 (24), should not wrap around to h-file
        long pawns = 1L << 24;
        long blackPieces = (1L << 33) | (1L << 31); // b5 and h5
        
        long movesLeft = BitboardGenerator.getWhitePawnCaptureLeft(pawns, blackPieces);
        
        // Should have no left captures (would wrap to h-file)
        assertEquals(0, BitboardGenerator.bitCount(movesLeft));
        
        // Pawn on h4 (31), should not wrap around to a-file
        pawns = 1L << 31;
        blackPieces = (1L << 40) | (1L << 38); // a5 and g5
        
        long movesRight = BitboardGenerator.getWhitePawnCaptureRight(pawns, blackPieces);
        
        // Should have no right captures (would wrap to a-file)
        assertEquals(0, BitboardGenerator.bitCount(movesRight));
    }

    // ============================================================
    // Phase 3 Tests: Black Pawn Moves
    // ============================================================

    @Test
    void testBlackPawnSinglePush() {
        // Pawns on e7 (52) and d7 (51), empty board
        long pawns = (1L << 52) | (1L << 51);
        long empty = ~0L; // All squares empty
        
        long moves = BitboardGenerator.getBlackPawnSinglePush(pawns, empty);
        
        // Should move to e6 (44) and d6 (43)
        assertEquals(2, BitboardGenerator.bitCount(moves));
        assertTrue((moves & (1L << 44)) != 0); // e6
        assertTrue((moves & (1L << 43)) != 0); // d6
    }

    @Test
    void testBlackPawnSinglePushBlocked() {
        // Pawn on e7 (52), square e6 (44) is blocked
        long pawns = 1L << 52;
        long empty = ~(1L << 44); // e6 is not empty
        
        long moves = BitboardGenerator.getBlackPawnSinglePush(pawns, empty);
        
        // Should have no moves
        assertEquals(0, BitboardGenerator.bitCount(moves));
    }

    @Test
    void testBlackPawnDoublePush() {
        // Pawns on e7 (52) and d7 (51), empty board
        long pawns = (1L << 52) | (1L << 51);
        long empty = ~0L; // All squares empty
        
        long moves = BitboardGenerator.getBlackPawnDoublePush(pawns, empty);
        
        // Should move to e5 (36) and d5 (35)
        assertEquals(2, BitboardGenerator.bitCount(moves));
        assertTrue((moves & (1L << 36)) != 0); // e5
        assertTrue((moves & (1L << 35)) != 0); // d5
    }

    @Test
    void testBlackPawnDoublePushBlockedFirstSquare() {
        // Pawn on e7 (52), square e6 (44) is blocked
        long pawns = 1L << 52;
        long empty = ~(1L << 44); // e6 is not empty
        
        long moves = BitboardGenerator.getBlackPawnDoublePush(pawns, empty);
        
        // Should have no moves
        assertEquals(0, BitboardGenerator.bitCount(moves));
    }

    @Test
    void testBlackPawnDoublePushBlockedSecondSquare() {
        // Pawn on e7 (52), square e5 (36) is blocked but e6 (44) is empty
        long pawns = 1L << 52;
        long empty = ~(1L << 36); // e5 is not empty, but e6 is empty
        
        long moves = BitboardGenerator.getBlackPawnDoublePush(pawns, empty);
        
        // Should have no moves
        assertEquals(0, BitboardGenerator.bitCount(moves));
    }

    @Test
    void testBlackPawnDoublePushFromWrongRank() {
        // Pawn on e5 (36), not on starting rank
        long pawns = 1L << 36;
        long empty = ~0L;
        
        long moves = BitboardGenerator.getBlackPawnDoublePush(pawns, empty);
        
        // Should have no double push moves from e5
        assertEquals(0, BitboardGenerator.bitCount(moves));
    }

    @Test
    void testBlackPawnCaptureRight() {
        // Pawn on e5 (36), white pieces on f4 (29) and d4 (27)
        long pawns = 1L << 36;
        long whitePieces = (1L << 29) | (1L << 27);
        
        long moves = BitboardGenerator.getBlackPawnCaptureRight(pawns, whitePieces);
        
        // Should capture on f4 (29)
        assertEquals(1, BitboardGenerator.bitCount(moves));
        assertTrue((moves & (1L << 29)) != 0); // f4
    }

    @Test
    void testBlackPawnCaptureLeft() {
        // Pawn on e5 (36), white pieces on f4 (29) and d4 (27)
        long pawns = 1L << 36;
        long whitePieces = (1L << 29) | (1L << 27);
        
        long moves = BitboardGenerator.getBlackPawnCaptureLeft(pawns, whitePieces);
        
        // Should capture on d4 (27)
        assertEquals(1, BitboardGenerator.bitCount(moves));
        assertTrue((moves & (1L << 27)) != 0); // d4
    }

    @Test
    void testBlackPawnCaptureNoWrapAround() {
        // Pawn on a5 (32), should not wrap around to h-file
        long pawns = 1L << 32;
        long whitePieces = (1L << 25) | (1L << 23); // b4 and h4
        
        long movesLeft = BitboardGenerator.getBlackPawnCaptureLeft(pawns, whitePieces);
        
        // Should have no left captures (would wrap to h-file)
        assertEquals(0, BitboardGenerator.bitCount(movesLeft));
        
        // Pawn on h5 (39), should not wrap around to a-file
        pawns = 1L << 39;
        whitePieces = (1L << 32) | (1L << 30); // a4 and g4
        
        long movesRight = BitboardGenerator.getBlackPawnCaptureRight(pawns, whitePieces);
        
        // Should have no right captures (would wrap to a-file)
        assertEquals(0, BitboardGenerator.bitCount(movesRight));
    }

    // ============================================================
    // Phase 3 Tests: Rank Masks
    // ============================================================

    @Test
    void testRank4Mask() {
        // RANK_4 should have bits set only on the 4th rank
        // Squares: 24-31
        assertEquals(0x00000000FF000000L, BitboardGenerator.RANK_4);
        assertEquals(8, BitboardGenerator.bitCount(BitboardGenerator.RANK_4));
    }

    @Test
    void testRank5Mask() {
        // RANK_5 should have bits set only on the 5th rank
        // Squares: 32-39
        assertEquals(0x000000FF00000000L, BitboardGenerator.RANK_5);
        assertEquals(8, BitboardGenerator.bitCount(BitboardGenerator.RANK_5));
    }

    // ============================================================
    // Phase 4 Tests: Magic Bitboards - Rook Attacks
    // ============================================================

    @Test
    void testRookAttacksEmptyBoard() {
        // Rook on e4 (square 28), empty board
        long occupancy = 0L;
        long attacks = BitboardGenerator.getRookAttacks(28, occupancy);
        
        // Should attack entire rank and file (14 squares: 7 on rank + 7 on file)
        assertEquals(14, BitboardGenerator.bitCount(attacks));
        
        // Verify attacks along rank 4 (squares 24-31 except 28)
        for (int sq = 24; sq <= 31; sq++) {
            if (sq != 28) {
                assertTrue((attacks & (1L << sq)) != 0, "Should attack square " + sq);
            }
        }
        
        // Verify attacks along file e (squares 4, 12, 20, 36, 44, 52, 60)
        int[] fileSquares = {4, 12, 20, 36, 44, 52, 60};
        for (int sq : fileSquares) {
            assertTrue((attacks & (1L << sq)) != 0, "Should attack square " + sq);
        }
    }

    @Test
    void testRookAttacksBlocked() {
        // Rook on e4 (square 28), blocked by pieces on e6 (44) and g4 (30)
        long occupancy = (1L << 44) | (1L << 30);
        long attacks = BitboardGenerator.getRookAttacks(28, occupancy);
        
        // Should attack up to blockers (including blocker squares)
        assertTrue((attacks & (1L << 36)) != 0); // e5
        assertTrue((attacks & (1L << 44)) != 0); // e6 (blocker)
        assertFalse((attacks & (1L << 52)) != 0); // e7 (beyond blocker)
        
        assertTrue((attacks & (1L << 29)) != 0); // f4
        assertTrue((attacks & (1L << 30)) != 0); // g4 (blocker)
        assertFalse((attacks & (1L << 31)) != 0); // h4 (beyond blocker)
    }

    @Test
    void testRookAttacksCorner() {
        // Rook on a1 (square 0), empty board
        long occupancy = 0L;
        long attacks = BitboardGenerator.getRookAttacks(0, occupancy);
        
        // Should attack entire rank 1 and file a (14 squares)
        assertEquals(14, BitboardGenerator.bitCount(attacks));
        
        // Should NOT attack the rook's own square
        assertFalse((attacks & 1L) != 0);
    }

    @Test
    void testRookAttacksH8() {
        // Rook on h8 (square 63), empty board
        long occupancy = 0L;
        long attacks = BitboardGenerator.getRookAttacks(63, occupancy);
        
        // Should attack entire rank 8 and file h (14 squares)
        assertEquals(14, BitboardGenerator.bitCount(attacks));
        
        // Should NOT attack the rook's own square
        assertFalse((attacks & (1L << 63)) != 0);
    }

    @Test
    void testRookAttacksComplexOccupancy() {
        // Rook on d4 (square 27), multiple blockers
        long occupancy = (1L << 35) | (1L << 19) | (1L << 26) | (1L << 29);
        // Blockers: d5 (35), d3 (19), c4 (26), f4 (29)
        long attacks = BitboardGenerator.getRookAttacks(27, occupancy);
        
        // Should include blocker squares but not squares beyond them
        assertTrue((attacks & (1L << 35)) != 0); // d5 (blocker)
        assertFalse((attacks & (1L << 43)) != 0); // d6 (beyond blocker)
        
        assertTrue((attacks & (1L << 19)) != 0); // d3 (blocker)
        assertFalse((attacks & (1L << 11)) != 0); // d2 (beyond blocker)
        
        assertTrue((attacks & (1L << 26)) != 0); // c4 (blocker)
        assertFalse((attacks & (1L << 25)) != 0); // b4 (beyond blocker)
        
        assertTrue((attacks & (1L << 28)) != 0); // e4
        assertTrue((attacks & (1L << 29)) != 0); // f4 (blocker)
        assertFalse((attacks & (1L << 30)) != 0); // g4 (beyond blocker)
    }

    // ============================================================
    // Phase 4 Tests: Magic Bitboards - Bishop Attacks
    // ============================================================

    @Test
    void testBishopAttacksEmptyBoard() {
        // Bishop on e4 (square 28), empty board
        long occupancy = 0L;
        long attacks = BitboardGenerator.getBishopAttacks(28, occupancy);
        
        // Should attack all diagonals (13 squares total)
        assertEquals(13, BitboardGenerator.bitCount(attacks));
        
        // Verify some diagonal squares
        assertTrue((attacks & (1L << 19)) != 0); // d3
        assertTrue((attacks & (1L << 10)) != 0); // c2
        assertTrue((attacks & (1L << 1)) != 0);  // b1
        assertTrue((attacks & (1L << 37)) != 0); // f5
        assertTrue((attacks & (1L << 46)) != 0); // g6
        assertTrue((attacks & (1L << 55)) != 0); // h7
    }

    @Test
    void testBishopAttacksBlocked() {
        // Bishop on e4 (square 28), blocked by pieces on g6 (46) and c2 (10)
        long occupancy = (1L << 46) | (1L << 10);
        long attacks = BitboardGenerator.getBishopAttacks(28, occupancy);
        
        // Should attack up to blockers (including blocker squares)
        assertTrue((attacks & (1L << 37)) != 0); // f5
        assertTrue((attacks & (1L << 46)) != 0); // g6 (blocker)
        assertFalse((attacks & (1L << 55)) != 0); // h7 (beyond blocker)
        
        assertTrue((attacks & (1L << 19)) != 0); // d3
        assertTrue((attacks & (1L << 10)) != 0); // c2 (blocker)
        assertFalse((attacks & (1L << 1)) != 0); // b1 (beyond blocker)
    }

    @Test
    void testBishopAttacksCorner() {
        // Bishop on a1 (square 0), empty board
        long occupancy = 0L;
        long attacks = BitboardGenerator.getBishopAttacks(0, occupancy);
        
        // Should attack one diagonal only (7 squares: b2, c3, d4, e5, f6, g7, h8)
        assertEquals(7, BitboardGenerator.bitCount(attacks));
        
        // Verify diagonal
        assertTrue((attacks & (1L << 9)) != 0);  // b2
        assertTrue((attacks & (1L << 18)) != 0); // c3
        assertTrue((attacks & (1L << 27)) != 0); // d4
        assertTrue((attacks & (1L << 36)) != 0); // e5
        assertTrue((attacks & (1L << 45)) != 0); // f6
        assertTrue((attacks & (1L << 54)) != 0); // g7
        assertTrue((attacks & (1L << 63)) != 0); // h8
    }

    @Test
    void testBishopAttacksH8() {
        // Bishop on h8 (square 63), empty board
        long occupancy = 0L;
        long attacks = BitboardGenerator.getBishopAttacks(63, occupancy);
        
        // Should attack one diagonal only (7 squares: g7, f6, e5, d4, c3, b2, a1)
        assertEquals(7, BitboardGenerator.bitCount(attacks));
        
        // Verify diagonal
        assertTrue((attacks & (1L << 54)) != 0); // g7
        assertTrue((attacks & (1L << 45)) != 0); // f6
        assertTrue((attacks & (1L << 36)) != 0); // e5
        assertTrue((attacks & (1L << 27)) != 0); // d4
        assertTrue((attacks & (1L << 18)) != 0); // c3
        assertTrue((attacks & (1L << 9)) != 0);  // b2
        assertTrue((attacks & (1L << 0)) != 0);  // a1
    }

    @Test
    void testBishopAttacksCenterWithOccupancy() {
        // Bishop on d4 (square 27), multiple blockers
        long occupancy = (1L << 36) | (1L << 18) | (1L << 20) | (1L << 34);
        // Blockers: e5 (36), c3 (18), e3 (20), c5 (34)
        long attacks = BitboardGenerator.getBishopAttacks(27, occupancy);
        
        // Should include blocker squares but not squares beyond them
        assertTrue((attacks & (1L << 36)) != 0); // e5 (blocker)
        assertFalse((attacks & (1L << 45)) != 0); // f6 (beyond blocker)
        
        assertTrue((attacks & (1L << 18)) != 0); // c3 (blocker)
        assertFalse((attacks & (1L << 9)) != 0); // b2 (beyond blocker)
        
        assertTrue((attacks & (1L << 20)) != 0); // e3 (blocker)
        assertFalse((attacks & (1L << 13)) != 0); // f2 (beyond blocker)
        
        assertTrue((attacks & (1L << 34)) != 0); // c5 (blocker)
        assertFalse((attacks & (1L << 41)) != 0); // b6 (beyond blocker)
    }

    @Test
    void testBishopAttacksEdge() {
        // Bishop on d1 (square 3), empty board
        long occupancy = 0L;
        long attacks = BitboardGenerator.getBishopAttacks(3, occupancy);
        
        // Should attack both diagonals from edge position
        assertTrue(BitboardGenerator.bitCount(attacks) > 0);
        
        // Verify some squares
        assertTrue((attacks & (1L << 10)) != 0); // c2
        assertTrue((attacks & (1L << 12)) != 0); // e2
    }

    // ============================================================
    // Phase 4 Tests: Edge Cases
    // ============================================================

    @Test
    void testRookDoesNotAttackOwnSquare() {
        // Verify rooks never attack their own square
        for (int square = 0; square < 64; square++) {
            long attacks = BitboardGenerator.getRookAttacks(square, 0L);
            long squareBit = 1L << square;
            assertEquals(0, attacks & squareBit, 
                "Rook on square " + square + " attacks itself");
        }
    }

    @Test
    void testBishopDoesNotAttackOwnSquare() {
        // Verify bishops never attack their own square
        for (int square = 0; square < 64; square++) {
            long attacks = BitboardGenerator.getBishopAttacks(square, 0L);
            long squareBit = 1L << square;
            assertEquals(0, attacks & squareBit, 
                "Bishop on square " + square + " attacks itself");
        }
    }

    @Test
    void testRookAttacksAllSquares() {
        // Verify rook attacks can be generated for all 64 squares without error
        for (int square = 0; square < 64; square++) {
            long attacks = BitboardGenerator.getRookAttacks(square, 0L);
            assertTrue(BitboardGenerator.bitCount(attacks) >= 14, 
                "Rook on square " + square + " has invalid attack count on empty board");
        }
    }

    @Test
    void testBishopAttacksAllSquares() {
        // Verify bishop attacks can be generated for all 64 squares without error
        for (int square = 0; square < 64; square++) {
            long attacks = BitboardGenerator.getBishopAttacks(square, 0L);
            assertTrue(BitboardGenerator.bitCount(attacks) >= 7, 
                "Bishop on square " + square + " has too few attacks on empty board");
            assertTrue(BitboardGenerator.bitCount(attacks) <= 13,
                "Bishop on square " + square + " has too many attacks on empty board");
        }
    }
}
