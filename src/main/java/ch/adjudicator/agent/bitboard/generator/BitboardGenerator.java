package ch.adjudicator.agent.bitboard.generator;

/**
 * BitboardGenerator - Phases 1-4: Java Environment & Bitboard Basics + Precomputing Leaper Attacks + Pawn Generation + Magic Bitboards
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
    
    // Phase 4: Magic Bitboard Tables for Sliding Pieces
    
    /**
     * Rook occupancy masks for all 64 squares.
     * Excludes edge squares (except the rook's square) to reduce table size.
     */
    private static final long[] ROOK_MASKS = new long[64];
    
    /**
     * Bishop occupancy masks for all 64 squares.
     * Excludes edge squares (except the bishop's square) to reduce table size.
     */
    private static final long[] BISHOP_MASKS = new long[64];
    
    /**
     * Precomputed magic numbers for rook attack generation.
     * These numbers are carefully chosen to provide perfect hashing.
     */
    private static final long[] ROOK_MAGICS = new long[64];
    
    /**
     * Precomputed magic numbers for bishop attack generation.
     * These numbers are carefully chosen to provide perfect hashing.
     */
    private static final long[] BISHOP_MAGICS = new long[64];
    
    /**
     * Shift amounts for rook magic hashing.
     * Determines the table size for each square (2^(64-shift)).
     */
    private static final int[] ROOK_SHIFTS = new int[64];
    
    /**
     * Shift amounts for bishop magic hashing.
     * Determines the table size for each square (2^(64-shift)).
     */
    private static final int[] BISHOP_SHIFTS = new int[64];
    
    /**
     * Precomputed rook attack tables.
     * First index is square, second index is magic hash of occupancy.
     */
    private static final long[][] ROOK_ATTACKS = new long[64][];
    
    /**
     * Precomputed bishop attack tables.
     * First index is square, second index is magic hash of occupancy.
     */
    private static final long[][] BISHOP_ATTACKS = new long[64][];
    
    // Static initializer to populate attack tables at class load time
    static {
        initializeKnightAttacks();
        initializeKingAttacks();
        initializeMagicBitboards();
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
    
    // Phase 3: Rank Masks for Pawn Double Pushes
    
    /**
     * RANK_4 mask - represents the 4th rank (row).
     * Used for white pawn double push destination validation.
     * Binary: 0x00000000FF000000L
     */
    public static final long RANK_4 = 0x00000000FF000000L;
    
    /**
     * RANK_5 mask - represents the 5th rank (row).
     * Used for black pawn double push destination validation.
     * Binary: 0x000000FF00000000L
     */
    public static final long RANK_5 = 0x000000FF00000000L;

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
    
    // Phase 3: Set-wise Pawn Generation Methods
    
    /**
     * Generates white pawn single push moves.
     * Pawns move one square forward (up) onto empty squares.
     * 
     * @param pawns bitboard of white pawns
     * @param empty bitboard of empty squares
     * @return bitboard of possible single push destinations
     */
    public static long getWhitePawnSinglePush(long pawns, long empty) {
        return (pawns << 8) & empty;
    }
    
    /**
     * Generates white pawn double push moves.
     * Pawns on the 2nd rank can move two squares forward if both squares are empty.
     * 
     * @param pawns bitboard of white pawns
     * @param empty bitboard of empty squares
     * @return bitboard of possible double push destinations (on rank 4)
     */
    public static long getWhitePawnDoublePush(long pawns, long empty) {
        long singlePush = (pawns << 8) & empty;
        return (singlePush << 8) & empty & RANK_4;
    }
    
    /**
     * Generates white pawn capture moves to the right (from white's perspective).
     * Pawns capture one square diagonally up-right onto black pieces.
     * 
     * @param pawns bitboard of white pawns
     * @param blackPieces bitboard of black pieces that can be captured
     * @return bitboard of possible right capture destinations
     */
    public static long getWhitePawnCaptureRight(long pawns, long blackPieces) {
        return (pawns << 9) & ~FILE_A & blackPieces;
    }
    
    /**
     * Generates white pawn capture moves to the left (from white's perspective).
     * Pawns capture one square diagonally up-left onto black pieces.
     * 
     * @param pawns bitboard of white pawns
     * @param blackPieces bitboard of black pieces that can be captured
     * @return bitboard of possible left capture destinations
     */
    public static long getWhitePawnCaptureLeft(long pawns, long blackPieces) {
        return (pawns << 7) & ~FILE_H & blackPieces;
    }
    
    /**
     * Generates black pawn single push moves.
     * Pawns move one square forward (down) onto empty squares.
     * 
     * @param pawns bitboard of black pawns
     * @param empty bitboard of empty squares
     * @return bitboard of possible single push destinations
     */
    public static long getBlackPawnSinglePush(long pawns, long empty) {
        return (pawns >>> 8) & empty;
    }
    
    /**
     * Generates black pawn double push moves.
     * Pawns on the 7th rank can move two squares forward if both squares are empty.
     * 
     * @param pawns bitboard of black pawns
     * @param empty bitboard of empty squares
     * @return bitboard of possible double push destinations (on rank 5)
     */
    public static long getBlackPawnDoublePush(long pawns, long empty) {
        long singlePush = (pawns >>> 8) & empty;
        return (singlePush >>> 8) & empty & RANK_5;
    }
    
    /**
     * Generates black pawn capture moves to the right (from black's perspective).
     * Pawns capture one square diagonally down-right onto white pieces.
     * 
     * @param pawns bitboard of black pawns
     * @param whitePieces bitboard of white pieces that can be captured
     * @return bitboard of possible right capture destinations
     */
    public static long getBlackPawnCaptureRight(long pawns, long whitePieces) {
        return (pawns >>> 7) & ~FILE_A & whitePieces;
    }
    
    /**
     * Generates black pawn capture moves to the left (from black's perspective).
     * Pawns capture one square diagonally down-left onto white pieces.
     * 
     * @param pawns bitboard of black pawns
     * @param whitePieces bitboard of white pieces that can be captured
     * @return bitboard of possible left capture destinations
     */
    public static long getBlackPawnCaptureLeft(long pawns, long whitePieces) {
        return (pawns >>> 9) & ~FILE_H & whitePieces;
    }
    
    // Phase 4: Magic Bitboard Methods
    
    /**
     * Gets rook attacks for a given square and occupancy.
     * Uses magic bitboard hashing for efficient lookup.
     * 
     * @param square the square (0-63) where the rook is located
     * @param occupancy the current board occupancy bitboard
     * @return bitboard of all squares the rook can attack
     */
    public static long getRookAttacks(int square, long occupancy) {
        occupancy &= ROOK_MASKS[square];
        int index = (int)((occupancy * ROOK_MAGICS[square]) >>> ROOK_SHIFTS[square]);
        return ROOK_ATTACKS[square][index];
    }
    
    /**
     * Gets bishop attacks for a given square and occupancy.
     * Uses magic bitboard hashing for efficient lookup.
     * 
     * @param square the square (0-63) where the bishop is located
     * @param occupancy the current board occupancy bitboard
     * @return bitboard of all squares the bishop can attack
     */
    public static long getBishopAttacks(int square, long occupancy) {
        occupancy &= BISHOP_MASKS[square];
        int index = (int)((occupancy * BISHOP_MAGICS[square]) >>> BISHOP_SHIFTS[square]);
        return BISHOP_ATTACKS[square][index];
    }
    
    /**
     * Initializes all magic bitboard tables.
     * This is called once at class load time.
     */
    private static void initializeMagicBitboards() {
        initializeRookMagics();
        initializeBishopMagics();
    }
    
    /**
     * Generates rook occupancy mask for a given square.
     * Includes all squares along ranks and files, excluding edges (unless it's the rook's square).
     */
    private static long generateRookMask(int square) {
        long mask = 0L;
        int rank = square / 8;
        int file = square % 8;
        
        // Generate vertical mask (file)
        for (int r = rank + 1; r <= 6; r++) {
            mask |= 1L << (r * 8 + file);
        }
        for (int r = rank - 1; r >= 1; r--) {
            mask |= 1L << (r * 8 + file);
        }
        
        // Generate horizontal mask (rank)
        for (int f = file + 1; f <= 6; f++) {
            mask |= 1L << (rank * 8 + f);
        }
        for (int f = file - 1; f >= 1; f--) {
            mask |= 1L << (rank * 8 + f);
        }
        
        return mask;
    }
    
    /**
     * Generates bishop occupancy mask for a given square.
     * Includes all squares along diagonals, excluding edges (unless it's the bishop's square).
     */
    private static long generateBishopMask(int square) {
        long mask = 0L;
        int rank = square / 8;
        int file = square % 8;
        
        // Up-right diagonal
        for (int r = rank + 1, f = file + 1; r <= 6 && f <= 6; r++, f++) {
            mask |= 1L << (r * 8 + f);
        }
        
        // Up-left diagonal
        for (int r = rank + 1, f = file - 1; r <= 6 && f >= 1; r++, f--) {
            mask |= 1L << (r * 8 + f);
        }
        
        // Down-right diagonal
        for (int r = rank - 1, f = file + 1; r >= 1 && f <= 6; r--, f++) {
            mask |= 1L << (r * 8 + f);
        }
        
        // Down-left diagonal
        for (int r = rank - 1, f = file - 1; r >= 1 && f >= 1; r--, f--) {
            mask |= 1L << (r * 8 + f);
        }
        
        return mask;
    }
    
    /**
     * Generates rook attacks from a square given an occupancy bitboard.
     * This is the "slow" version used during initialization.
     */
    private static long generateRookAttacks(int square, long occupancy) {
        long attacks = 0L;
        int rank = square / 8;
        int file = square % 8;
        
        // Up (increasing rank)
        for (int r = rank + 1; r <= 7; r++) {
            long sq = 1L << (r * 8 + file);
            attacks |= sq;
            if ((occupancy & sq) != 0) break;
        }
        
        // Down (decreasing rank)
        for (int r = rank - 1; r >= 0; r--) {
            long sq = 1L << (r * 8 + file);
            attacks |= sq;
            if ((occupancy & sq) != 0) break;
        }
        
        // Right (increasing file)
        for (int f = file + 1; f <= 7; f++) {
            long sq = 1L << (rank * 8 + f);
            attacks |= sq;
            if ((occupancy & sq) != 0) break;
        }
        
        // Left (decreasing file)
        for (int f = file - 1; f >= 0; f--) {
            long sq = 1L << (rank * 8 + f);
            attacks |= sq;
            if ((occupancy & sq) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Generates bishop attacks from a square given an occupancy bitboard.
     * This is the "slow" version used during initialization.
     */
    private static long generateBishopAttacks(int square, long occupancy) {
        long attacks = 0L;
        int rank = square / 8;
        int file = square % 8;
        
        // Up-right diagonal
        for (int r = rank + 1, f = file + 1; r <= 7 && f <= 7; r++, f++) {
            long sq = 1L << (r * 8 + f);
            attacks |= sq;
            if ((occupancy & sq) != 0) break;
        }
        
        // Up-left diagonal
        for (int r = rank + 1, f = file - 1; r <= 7 && f >= 0; r++, f--) {
            long sq = 1L << (r * 8 + f);
            attacks |= sq;
            if ((occupancy & sq) != 0) break;
        }
        
        // Down-right diagonal
        for (int r = rank - 1, f = file + 1; r >= 0 && f <= 7; r--, f++) {
            long sq = 1L << (r * 8 + f);
            attacks |= sq;
            if ((occupancy & sq) != 0) break;
        }
        
        // Down-left diagonal
        for (int r = rank - 1, f = file - 1; r >= 0 && f >= 0; r--, f--) {
            long sq = 1L << (r * 8 + f);
            attacks |= sq;
            if ((occupancy & sq) != 0) break;
        }
        
        return attacks;
    }
    
    /**
     * Generates all occupancy variations for a given mask.
     * Used during magic bitboard initialization.
     */
    private static long[] generateOccupancyVariations(long mask) {
        int bitCount = Long.bitCount(mask);
        int variationCount = 1 << bitCount;
        long[] variations = new long[variationCount];
        
        // Extract bit positions from mask
        int[] bitPositions = new int[bitCount];
        int index = 0;
        long tempMask = mask;
        while (tempMask != 0) {
            int pos = Long.numberOfTrailingZeros(tempMask);
            bitPositions[index++] = pos;
            tempMask &= tempMask - 1; // Clear LSB
        }
        
        // Generate all variations
        for (int i = 0; i < variationCount; i++) {
            long occupancy = 0L;
            for (int j = 0; j < bitCount; j++) {
                if ((i & (1 << j)) != 0) {
                    occupancy |= 1L << bitPositions[j];
                }
            }
            variations[i] = occupancy;
        }
        
        return variations;
    }
    
    /**
     * Initializes rook magic bitboards.
     * Uses verified magic numbers from Pradu Kannan / CPW.
     */
    private static void initializeRookMagics() {
        // Proven magic numbers for rooks (from Pradu Kannan/CPW - widely tested)
        long[] magics = {
            0x8a80104000800020L, 0x140002000100040L, 0x2801880a0017001L, 0x100081001000420L,
            0x200020010080420L, 0x3001c0002010008L, 0x8480008002000100L, 0x2080088004402900L,
            0x800098204000L, 0x2024401000200040L, 0x100802000801000L, 0x120800800801000L,
            0x208808088000400L, 0x2802200800400L, 0x2200800100020080L, 0x801000060821100L,
            0x80044006422000L, 0x100808020004000L, 0x12108a0010204200L, 0x140848010000802L,
            0x481828014002800L, 0x8094004002004100L, 0x4010040010010802L, 0x20008806104L,
            0x100400080208000L, 0x2040002120081000L, 0x21200680100081L, 0x20100080080080L,
            0x2000a00200410L, 0x20080800400L, 0x80088400100102L, 0x80004600042881L,
            0x4040008040800020L, 0x440003000200801L, 0x4200011004500L, 0x188020010100100L,
            0x14800401802800L, 0x2080040080800200L, 0x124080204001001L, 0x200046502000484L,
            0x480400080088020L, 0x1000422010034000L, 0x30200100110040L, 0x100021010009L,
            0x2002080100110004L, 0x202008004008002L, 0x20020004010100L, 0x2048440040820001L,
            0x101002200408200L, 0x40802000401080L, 0x4008142004410100L, 0x2060820c0120200L,
            0x1001004080100L, 0x20c020080040080L, 0x2935610830022400L, 0x44440041009200L,
            0x280001040802101L, 0x2100190040002085L, 0x80c0084100102001L, 0x4024081001000421L,
            0x20030a0244872L, 0x12001008414402L, 0x2006104900a0804L, 0x1004081002402L
        };
        
        for (int square = 0; square < 64; square++) {
            ROOK_MASKS[square] = generateRookMask(square);
            ROOK_MAGICS[square] = magics[square];
            
            int bitCount = Long.bitCount(ROOK_MASKS[square]);
            ROOK_SHIFTS[square] = 64 - bitCount;
            int tableSize = 1 << bitCount;
            ROOK_ATTACKS[square] = new long[tableSize];
            
            long[] occupancies = generateOccupancyVariations(ROOK_MASKS[square]);
            for (long occupancy : occupancies) {
                long attacks = generateRookAttacks(square, occupancy);
                int index = (int)((occupancy * ROOK_MAGICS[square]) >>> ROOK_SHIFTS[square]);
                ROOK_ATTACKS[square][index] = attacks;
            }
        }
    }
    
    /**
     * Initializes bishop magic bitboards.
     * Uses verified magic numbers from Pradu Kannan / CPW.
     */
    private static void initializeBishopMagics() {
        // Proven magic numbers for bishops (from Pradu Kannan/CPW - widely tested)
        long[] magics = {
            0x89a1121896040240L, 0x2004844802002010L, 0x2068080051921000L, 0x62880a0220200808L,
            0x4042004000000L, 0x100822020200011L, 0xc00444222012000aL, 0x28808801216001L,
            0x400492088408100L, 0x201c401040c0084L, 0x840800910a0010L, 0x82080240060L,
            0x2000840504006000L, 0x30010c4108405004L, 0x1008005410080802L, 0x8144042209100900L,
            0x208081020014400L, 0x4800201208ca00L, 0xf18140408012008L, 0x1004002802102001L,
            0x841000820080811L, 0x40200200a42008L, 0x800054042000L, 0x88010400410c9000L,
            0x520040470104290L, 0x1004040051500081L, 0x2002081833080021L, 0x400c00c010142L,
            0x941408200c002000L, 0x658810000806011L, 0x188071040440a00L, 0x4800404002011c00L,
            0x104442040404200L, 0x511080202091021L, 0x4022401120400L, 0x80c0040400080120L,
            0x8040010040820802L, 0x480810700020090L, 0x102008e00040242L, 0x809005202050100L,
            0x8002024220104080L, 0x431008804142000L, 0x19001802081400L, 0x200014208040080L,
            0x3308082008200100L, 0x41010500040c020L, 0x4012020c04210308L, 0x208220a202004080L,
            0x111040120082000L, 0x6803040141280a00L, 0x2101004202410000L, 0x8200000041108022L,
            0x21082088000L, 0x2410204010040L, 0x40100400809000L, 0x822088220820214L,
            0x40808090012004L, 0x910224040218c9L, 0x402814422015008L, 0x90014004842410L,
            0x1000042304105L, 0x10008830412a00L, 0x2520081090008908L, 0x40102000a0a60140L
        };
        
        for (int square = 0; square < 64; square++) {
            BISHOP_MASKS[square] = generateBishopMask(square);
            BISHOP_MAGICS[square] = magics[square];
            
            int bitCount = Long.bitCount(BISHOP_MASKS[square]);
            BISHOP_SHIFTS[square] = 64 - bitCount;
            int tableSize = 1 << bitCount;
            BISHOP_ATTACKS[square] = new long[tableSize];
            
            long[] occupancies = generateOccupancyVariations(BISHOP_MASKS[square]);
            for (long occupancy : occupancies) {
                long attacks = generateBishopAttacks(square, occupancy);
                int index = (int)((occupancy * BISHOP_MAGICS[square]) >>> BISHOP_SHIFTS[square]);
                BISHOP_ATTACKS[square][index] = attacks;
            }
        }
    }

    // Private constructor to prevent instantiation
    private BitboardGenerator() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}
