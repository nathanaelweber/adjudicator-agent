package ch.adjudicator.agent.bitboard.model;

public class FastMove {
    public int destinationSquare;
    public int originSquare;
    public int pieceTypeToPromote;
    public boolean promotion;
    public boolean enPassant;
    public boolean castling;
}
