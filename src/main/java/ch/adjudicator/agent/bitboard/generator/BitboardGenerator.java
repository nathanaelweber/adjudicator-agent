package ch.adjudicator.agent.bitboard.generator;

/**
 * BitboardGenerator - Phases 1 & 2: Java Environment & Bitboard Basics + Precomputing Leaper Attacks
 * 
 * This class provides the fundamental bitboard utilities for a chess engine.
 * It uses Java's 64-bit long primitives to represent chess board positions
 * and provides essential bitwise operations and masks.
 */
public final class BitboardGenerator {

    // Phase 2: Precomputed Attack Tables
    
    /**
     * Precomputed knight attack bitboards for all 64 squares.
     * Index represents the square (0-63), value is the bitboard of all possible knight moves.
     */
    public static final long[] KNIGHT_ATTACKS = new long[64];
    
    /**
     * Precomputed king attack bitboards for all 64 squares.
     * Index represents the square (0-63), value is the bitboard of all possible king moves.
     */
    public static final long[] KING_ATTACKS = new long[64];
    
    // Static initializer to populate attack tables at class load time
    static {
        initializeKnightAttacks();
        initializeKingAttacks();
    }

    // Wrap-Around Masks to prevent pieces from "jumping" sides during shifts
    
    /**
     * FILE_A mask - represents the A-file (leftmost column) on the chess board.
     * Binary: 0000 0001 repeated 8 times vertically
     */
    public static final long FILE_A = 0x0101010101010101L;
    
    /**
     * FILE_H mask - represents the H-file (rightmost column) on the chess board.
     * Binary: 1000 0000 repeated 8 times vertically
     */
    public static final long FILE_H = 0x8080808080808080L;
    
    /**
     * FILE_AB mask - represents both A and B files.
     * Used to prevent knight/king moves from wrapping around the left edge.
     */
    public static final long FILE_AB = FILE_A | (FILE_A << 1);
    
    /**
     * FILE_GH mask - represents both G and H files.
     * Used to prevent knight/king moves from wrapping around the right edge.
     */
    public static final long FILE_GH = FILE_H | (FILE_H >>> 1);

    // Bitboard Utility Methods
    
    /**
     * Gets the index of the least significant bit (LSB) in the bitboard.
     * This is equivalent to finding the first occupied square.
     * 
     * @param bitboard the bitboard to analyze
     * @return the index (0-63) of the LSB, or 64 if the bitboard is empty
     */
    public static int getLSB(long bitboard) {
        return Long.numberOfTrailingZeros(bitboard);
    }
    
    /**
     * Counts the number of set bits in the bitboard.
     * This is useful for counting pieces and for evaluation purposes.
     * 
     * @param bitboard the bitboard to analyze
     * @return the number of set bits (population count)
     */
    public static int bitCount(long bitboard) {
        return Long.bitCount(bitboard);
    }

    // Phase 2: Leaper Attack Initialization Methods
    
    /**
     * Initializes the knight attack table for all 64 squares.
     * A knight moves in an "L" shape: 2 squares in one direction and 1 square perpendicular.
     * This results in 8 possible moves from any square (if not blocked by board edges).
     */
    private static void initializeKnightAttacks() {
        for (int square = 0; square < 64; square++) {
            long spot = 1L << square;
            long moves = 0L;
            
            // Knight moves: 8 directions in "L" shape
            // Up-Right moves
            moves |= (spot << 17) & ~FILE_A;        // 2 up, 1 right
            moves |= (spot << 10) & ~FILE_AB;       // 1 up, 2 right
            
            // Down-Right moves
            moves |= (spot >>> 6) & ~FILE_AB;       // 1 down, 2 right
            moves |= (spot >>> 15) & ~FILE_A;       // 2 down, 1 right
            
            // Down-Left moves
            moves |= (spot >>> 17) & ~FILE_H;       // 2 down, 1 left
            moves |= (spot >>> 10) & ~FILE_GH;      // 1 down, 2 left
            
            // Up-Left moves
            moves |= (spot << 6) & ~FILE_GH;        // 1 up, 2 left
            moves |= (spot << 15) & ~FILE_H;        // 2 up, 1 left
            
            KNIGHT_ATTACKS[square] = moves;
        }
    }
    
    /**
     * Initializes the king attack table for all 64 squares.
     * A king moves one square in any of the 8 directions: horizontal, vertical, or diagonal.
     */
    private static void initializeKingAttacks() {
        for (int square = 0; square < 64; square++) {
            long spot = 1L << square;
            long moves = 0L;
            
            // King moves: 8 directions, one square each
            // Vertical moves
            moves |= (spot << 8);                   // Up
            moves |= (spot >>> 8);                  // Down
            
            // Horizontal moves
            moves |= (spot << 1) & ~FILE_A;         // Right
            moves |= (spot >>> 1) & ~FILE_H;        // Left
            
            // Diagonal moves
            moves |= (spot << 9) & ~FILE_A;         // Up-Right
            moves |= (spot << 7) & ~FILE_H;         // Up-Left
            moves |= (spot >>> 7) & ~FILE_A;        // Down-Right
            moves |= (spot >>> 9) & ~FILE_H;        // Down-Left
            
            KING_ATTACKS[square] = moves;
        }
    }

    // Private constructor to prevent instantiation
    private BitboardGenerator() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}
