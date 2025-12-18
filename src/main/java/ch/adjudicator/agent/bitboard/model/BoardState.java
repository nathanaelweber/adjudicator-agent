package ch.adjudicator.agent.bitboard.model;

public class BoardState {
    public static final int INDEX_PAWN = 0;
    public static final int INDEX_KNIGHT = 1;
    public static final int INDEX_BISHOP = 2;
    public static final int INDEX_ROOK = 3;
    public static final int INDEX_QUEEN = 4;
    public static final int INDEX_KING = 5;
    public long[] whitePieces = new long[6];
    public long[] blackPieces = new long[6];
    
    // Additional FEN state
    public static final int INDEX_WHITE_TO_MOVE = 15;
    public static final int INDEX_WHITE_KINGSIDE_CASTLING_RIGHT_INTACT = 14;
    public static final int INDEX_WHITE_QUEENSIDE_CASTLING_RIGHT_INTACT = 13;
    public static final int INDEX_BLACK_KINGSIDE_CASTLING_RIGHT_INTACT = 12;
    public static final int INDEX_BLACK_QUEENSIDE_CASTLING_RIGHT_INTACT = 11;
    public static final int INDEX_EN_PASSANT_SQUARE_ACTIVE = 6;
    public static final long EN_PASSANT_SQUARE_MASK = 0x3FL; // using 6 bits thus the en passant square mask
    public static final long EN_PASSANT_CLEAR_MASK = 0x7F;

    public long bitAuxiliaries;
    
    // Bitwise getters
    public boolean isWhiteToMove() {
        return (bitAuxiliaries & (1L << INDEX_WHITE_TO_MOVE)) != 0;
    }
    
    public boolean isWhiteKingsideCastling() {
        return (bitAuxiliaries & (1L << INDEX_WHITE_KINGSIDE_CASTLING_RIGHT_INTACT)) != 0;
    }
    
    public boolean isWhiteQueensideCastling() {
        return (bitAuxiliaries & (1L << INDEX_WHITE_QUEENSIDE_CASTLING_RIGHT_INTACT)) != 0;
    }
    
    public boolean isBlackKingsideCastling() {
        return (bitAuxiliaries & (1L << INDEX_BLACK_KINGSIDE_CASTLING_RIGHT_INTACT)) != 0;
    }
    
    public boolean isBlackQueensideCastling() {
        return (bitAuxiliaries & (1L << INDEX_BLACK_QUEENSIDE_CASTLING_RIGHT_INTACT)) != 0;
    }
    
    public int getEnPassantSquare() {
        boolean active = (bitAuxiliaries & (1L << INDEX_EN_PASSANT_SQUARE_ACTIVE)) != 0;
        if (!active) {
            return -1;
        }
        // Extract bits 5-0 for the en passant square value
        return (int) (bitAuxiliaries & 0x3F);
    }
    
    // Bitwise setters
    public void setWhiteToMove(boolean value) {
        if (value) {
            bitAuxiliaries |= (1L << INDEX_WHITE_TO_MOVE);
        } else {
            bitAuxiliaries &= ~(1L << INDEX_WHITE_TO_MOVE);
        }
    }
    
    public void setWhiteKingsideCastling(boolean value) {
        if (value) {
            bitAuxiliaries |= (1L << INDEX_WHITE_KINGSIDE_CASTLING_RIGHT_INTACT);
        } else {
            bitAuxiliaries &= ~(1L << INDEX_WHITE_KINGSIDE_CASTLING_RIGHT_INTACT);
        }
    }
    
    public void setWhiteQueensideCastling(boolean value) {
        if (value) {
            bitAuxiliaries |= (1L << INDEX_WHITE_QUEENSIDE_CASTLING_RIGHT_INTACT);
        } else {
            bitAuxiliaries &= ~(1L << INDEX_WHITE_QUEENSIDE_CASTLING_RIGHT_INTACT);
        }
    }
    
    public void setBlackKingsideCastling(boolean value) {
        if (value) {
            bitAuxiliaries |= (1L << INDEX_BLACK_KINGSIDE_CASTLING_RIGHT_INTACT);
        } else {
            bitAuxiliaries &= ~(1L << INDEX_BLACK_KINGSIDE_CASTLING_RIGHT_INTACT);
        }
    }
    
    public void setBlackQueensideCastling(boolean value) {
        if (value) {
            bitAuxiliaries |= (1L << INDEX_BLACK_QUEENSIDE_CASTLING_RIGHT_INTACT);
        } else {
            bitAuxiliaries &= ~(1L << INDEX_BLACK_QUEENSIDE_CASTLING_RIGHT_INTACT);
        }
    }
    
    public void setEnPassantSquare(int square) {
        // Clear existing en passant bits (0-6)
        bitAuxiliaries &= ~EN_PASSANT_CLEAR_MASK; // Clear bits 0-6
        
        if (square == -1) {
            // No en passant - active bit already cleared
        } else {
            // Set the square value (bits 0-7) and active flag (bit 8)
            bitAuxiliaries |= (square & EN_PASSANT_SQUARE_MASK);
            bitAuxiliaries |= (1L << INDEX_EN_PASSANT_SQUARE_ACTIVE);
        }
    }
}
