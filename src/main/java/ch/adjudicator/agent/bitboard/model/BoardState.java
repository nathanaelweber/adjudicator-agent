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
    public static final int INDEX_EN_PASSANT_SQUARE_ACTIVE = 8;
    public static final int INDEX_EN_PASSANT_SQUARE_MSB = 7;
    public static final int INDEX_EN_PASSANT_SQUARE_LSB = 0;
    public long bitAuxiliaries;
    public boolean whiteToMove = true;
    public boolean whiteKingsideCastling = true;
    public boolean whiteQueensideCastling = true;
    public boolean blackKingsideCastling = true;
    public boolean blackQueensideCastling = true;
    public int enPassantSquare = -1; // -1 means no en passant, otherwise 0-63
}
