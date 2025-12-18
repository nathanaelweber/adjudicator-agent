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

    public BoardState applyMove(FastMove fastMove) {
        BoardState newState = new BoardState();
        
        // Copy piece bitboards
        for (int i = 0; i < 6; i++) {
            newState.whitePieces[i] = this.whitePieces[i];
            newState.blackPieces[i] = this.blackPieces[i];
        }
        
        // Copy auxiliary bits
        newState.bitAuxiliaries = this.bitAuxiliaries;
        
        int from = fastMove.originSquare;
        int to = fastMove.destinationSquare;
        long fromBit = 1L << from;
        long toBit = 1L << to;
        
        boolean isWhite = this.isWhiteToMove();
        
        // Find which piece is moving
        int pieceType = -1;
        if (isWhite) {
            for (int i = 0; i < 6; i++) {
                if ((this.whitePieces[i] & fromBit) != 0) {
                    pieceType = i;
                    break;
                }
            }
            
            if (pieceType != -1) {
                // Remove piece from origin
                newState.whitePieces[pieceType] &= ~fromBit;
                
                // Handle captures - remove enemy piece at destination
                for (int i = 0; i < 6; i++) {
                    newState.blackPieces[i] &= ~toBit;
                }
                
                // Handle special moves
                if (fastMove.castling) {
                    // Move the king
                    newState.whitePieces[pieceType] |= toBit;
                    // Move the rook
                    if (to == 6) { // Kingside castling
                        newState.whitePieces[INDEX_ROOK] &= ~(1L << 7);
                        newState.whitePieces[INDEX_ROOK] |= (1L << 5);
                    } else if (to == 2) { // Queenside castling
                        newState.whitePieces[INDEX_ROOK] &= ~(1L << 0);
                        newState.whitePieces[INDEX_ROOK] |= (1L << 3);
                    }
                } else if (fastMove.enPassant) {
                    // Move pawn to destination
                    newState.whitePieces[pieceType] |= toBit;
                    // Remove captured pawn (one rank below destination)
                    int capturedPawnSquare = to - 8;
                    newState.blackPieces[INDEX_PAWN] &= ~(1L << capturedPawnSquare);
                } else if (fastMove.promotion) {
                    // Place promoted piece at destination
                    newState.whitePieces[fastMove.pieceTypeToPromote] |= toBit;
                } else {
                    // Normal move
                    newState.whitePieces[pieceType] |= toBit;
                }
                
                // Update castling rights
                if (pieceType == INDEX_KING) {
                    newState.setWhiteKingsideCastling(false);
                    newState.setWhiteQueensideCastling(false);
                } else if (pieceType == INDEX_ROOK) {
                    if (from == 0) { // a1 rook moved
                        newState.setWhiteQueensideCastling(false);
                    } else if (from == 7) { // h1 rook moved
                        newState.setWhiteKingsideCastling(false);
                    }
                }
                
                // Clear castling rights if opponent's rook is captured
                if (to == 56) { // a8 rook captured
                    newState.setBlackQueensideCastling(false);
                } else if (to == 63) { // h8 rook captured
                    newState.setBlackKingsideCastling(false);
                }
                
                // Update en passant square
                if (pieceType == INDEX_PAWN && Math.abs(to - from) == 16) {
                    // Double pawn push - set en passant square
                    int epSquare = from + 8; // Square behind the pawn
                    newState.setEnPassantSquare(epSquare);
                } else {
                    // Clear en passant square
                    newState.setEnPassantSquare(-1);
                }
            }
        } else {
            // Black's move
            for (int i = 0; i < 6; i++) {
                if ((this.blackPieces[i] & fromBit) != 0) {
                    pieceType = i;
                    break;
                }
            }
            
            if (pieceType != -1) {
                // Remove piece from origin
                newState.blackPieces[pieceType] &= ~fromBit;
                
                // Handle captures - remove enemy piece at destination
                for (int i = 0; i < 6; i++) {
                    newState.whitePieces[i] &= ~toBit;
                }
                
                // Handle special moves
                if (fastMove.castling) {
                    // Move the king
                    newState.blackPieces[pieceType] |= toBit;
                    // Move the rook
                    if (to == 62) { // Kingside castling
                        newState.blackPieces[INDEX_ROOK] &= ~(1L << 63);
                        newState.blackPieces[INDEX_ROOK] |= (1L << 61);
                    } else if (to == 58) { // Queenside castling
                        newState.blackPieces[INDEX_ROOK] &= ~(1L << 56);
                        newState.blackPieces[INDEX_ROOK] |= (1L << 59);
                    }
                } else if (fastMove.enPassant) {
                    // Move pawn to destination
                    newState.blackPieces[pieceType] |= toBit;
                    // Remove captured pawn (one rank above destination)
                    int capturedPawnSquare = to + 8;
                    newState.whitePieces[INDEX_PAWN] &= ~(1L << capturedPawnSquare);
                } else if (fastMove.promotion) {
                    // Place promoted piece at destination
                    newState.blackPieces[fastMove.pieceTypeToPromote] |= toBit;
                } else {
                    // Normal move
                    newState.blackPieces[pieceType] |= toBit;
                }
                
                // Update castling rights
                if (pieceType == INDEX_KING) {
                    newState.setBlackKingsideCastling(false);
                    newState.setBlackQueensideCastling(false);
                } else if (pieceType == INDEX_ROOK) {
                    if (from == 56) { // a8 rook moved
                        newState.setBlackQueensideCastling(false);
                    } else if (from == 63) { // h8 rook moved
                        newState.setBlackKingsideCastling(false);
                    }
                }
                
                // Clear castling rights if opponent's rook is captured
                if (to == 0) { // a1 rook captured
                    newState.setWhiteQueensideCastling(false);
                } else if (to == 7) { // h1 rook captured
                    newState.setWhiteKingsideCastling(false);
                }
                
                // Update en passant square
                if (pieceType == INDEX_PAWN && Math.abs(to - from) == 16) {
                    // Double pawn push - set en passant square
                    int epSquare = from - 8; // Square behind the pawn
                    newState.setEnPassantSquare(epSquare);
                } else {
                    // Clear en passant square
                    newState.setEnPassantSquare(-1);
                }
            }
        }
        
        // Toggle side to move
        newState.setWhiteToMove(!isWhite);
        
        return newState;
    }

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
