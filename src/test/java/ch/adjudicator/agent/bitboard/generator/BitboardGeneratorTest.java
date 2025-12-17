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
}
