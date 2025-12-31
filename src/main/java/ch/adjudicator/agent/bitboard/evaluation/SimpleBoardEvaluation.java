package ch.adjudicator.agent.bitboard.evaluation;

import ch.adjudicator.agent.bitboard.model.BoardState;

public class SimpleBoardEvaluation {
    static int[] piece_value = { 100, 300, 300, 500, 900,  0};

    public static int evaluate(BoardState boardState) {

        int score = 0;

        boolean whiteToMove = boardState.isWhiteToMove();

        int signWhite = whiteToMove ? 1 : -1;
        int signBlack = whiteToMove ? -1 : 1;

        for (int pieceType = 0; pieceType < 5; pieceType++) {
            for(int square = 0; square < 64; square++) {
                if((boardState.whitePieces[pieceType] & (1L << square)) != 0) {
                    score += signWhite * piece_value[pieceType];
                }
                if((boardState.blackPieces[pieceType] & (1L << square)) != 0) {
                    score += signBlack * piece_value[pieceType];
                }
            }
        }
        return score;
    }
}
