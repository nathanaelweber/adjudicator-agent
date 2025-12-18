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
    public boolean whiteToMove = true;
    public boolean whiteKingsideCastling = true;
    public boolean whiteQueensideCastling = true;
    public boolean blackKingsideCastling = true;
    public boolean blackQueensideCastling = true;
    public int enPassantSquare = -1; // -1 means no en passant, otherwise 0-63
    public int halfmoveClock = 0;
    public int fullmoveNumber = 1;
}
