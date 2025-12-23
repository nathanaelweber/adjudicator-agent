package ch.adjudicator.agent.bitboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FastMove {
    public int destinationSquare;
    public int originSquare;
    public int pieceTypeToPromote;
    public boolean promotion;
    public boolean enPassant;
    public boolean castling;
}
