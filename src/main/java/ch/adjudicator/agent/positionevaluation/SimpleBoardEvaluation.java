package ch.adjudicator.agent.positionevaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

public class SimpleBoardEvaluation {
    // Material values in centipawns
    private static final int PAWN_VALUE = 100;
    private static final int KNIGHT_VALUE = 300;
    private static final int BISHOP_VALUE = 300;
    private static final int ROOK_VALUE = 500;
    private static final int QUEEN_VALUE = 900;
    private static final int KING_VALUE = 20000;
    /**
     * Evaluate the board position from the perspective of the side to move.
     * Positive scores favor the side to move, negative scores favor the opponent.
     */
    public static int evaluate(Board board) {
        int score = 0;
        Side sideToMove = board.getSideToMove();

        // Count material for both sides
        for (Square square : Square.values()) {
            if (square == Square.NONE) continue;

            Piece piece = board.getPiece(square);
            if (piece == Piece.NONE) continue;

            int pieceValue = getPieceValue(piece, square);

            // Evaluate the board from the perspective of the one who just has done the move.
            if (piece.getPieceSide() == sideToMove) {
                score += pieceValue;
            } else {
                score -= pieceValue;
            }
        }

        return score;
    }

    /**
     * Get the material value of a piece in centipawns, including positional bonuses.
     * Positional bonuses are based on piece-square tables from chess theory.
     *
     * @param piece  The piece to evaluate
     * @param square The square where the piece is located
     * @return The value of the piece including positional adjustment
     */
    private static int getPieceValue(Piece piece, Square square) {
        int baseValue;
        switch (piece.getPieceType()) {
            case PAWN:
                baseValue = PAWN_VALUE;
                break;
            case KNIGHT:
                baseValue = KNIGHT_VALUE;
                break;
            case BISHOP:
                baseValue = BISHOP_VALUE;
                break;
            case ROOK:
                baseValue = ROOK_VALUE;
                break;
            case QUEEN:
                baseValue = QUEEN_VALUE;
                break;
            case KING:
                baseValue = KING_VALUE;
                break;
            default:
                return 0;
        }

        // Add positional bonus based on piece type and square
        int positionalBonus = getPositionalBonus(piece, square);
        return baseValue + positionalBonus;
    }

    /**
     * Get positional bonus for a piece on a given square.
     * Based on standard chess piece-square tables.
     * Positive values indicate better squares for the piece.
     *
     * @param piece  The piece
     * @param square The square
     * @return Positional bonus in centipawns
     */
    public static int getPositionalBonus(Piece piece, Square square) {
        if (square == Square.NONE) return 0;

        int rank = square.getRank().ordinal(); // 0 = Rank 1, 7 = Rank 8
        int file = square.getFile().ordinal(); // 0 = File A, 7 = File H

        // Adjust rank based on piece color (White prefers advancing, Black prefers back ranks)
        Side side = piece.getPieceSide();
        int adjustedRank = (side == Side.WHITE) ? rank : (7 - rank);

        switch (piece.getPieceType()) {
            case PAWN:
                // Pawns gain value as they advance toward promotion
                // Central pawns are slightly more valuable
                int pawnBonus = adjustedRank * 10; // 0 to 70 centipawns
                // Center files (d,e = files 3,4) get small bonus
                if (file >= 2 && file <= 5) {
                    pawnBonus += 5;
                }
                return pawnBonus;

            case KNIGHT:
                // Knights prefer central squares and lose value on edges
                // Better in middlegame, so central control is key
                int knightBonus = 0;
                // Penalize edge files
                if (file == 0 || file == 7) knightBonus -= 10;
                // Penalize back rank
                if (adjustedRank == 0) knightBonus -= 10;
                // Bonus for central squares (files c-f, ranks 3-6)
                if (file >= 2 && file <= 5 && adjustedRank >= 2 && adjustedRank <= 5) {
                    knightBonus += 10;
                }
                return knightBonus;

            case BISHOP:
                // Bishops prefer central activity and open diagonals
                int bishopBonus = 0;
                // Slight bonus for central squares
                if (file >= 2 && file <= 5 && adjustedRank >= 2 && adjustedRank <= 5) {
                    bishopBonus += 5;
                }
                return bishopBonus;

            case ROOK:
                // Rooks prefer 7th rank (attacking pawns) and open files
                int rookBonus = 0;
                if (adjustedRank == 6) { // 7th rank from piece's perspective
                    rookBonus += 10;
                }
                return rookBonus;

            case QUEEN:
                // Queen doesn't have strong positional preferences
                // Slight penalty for early development
                if (adjustedRank == 0) return -5;
                return 0;

            case KING:
                // King safety: prefer back rank in opening/middlegame
                // This is simplified; real engines consider pawn shield, etc.
                int kingBonus = 0;
                if (adjustedRank <= 1) { // Back two ranks
                    kingBonus += 10;
                }
                // Prefer corners/edges for safety (castled position)
                if (file <= 2 || file >= 5) {
                    kingBonus += 5;
                }
                return kingBonus;

            default:
                return 0;
        }
    }

    /**
     * Get a heuristic score for move ordering (MVV-LVA).
     */
    public static int getMoveScore(Board board, Move move) {
        int score = 0;

        // Capture value: victim value - attacker value / 10
        Piece capturedPiece = board.getPiece(move.getTo());
        if (capturedPiece != Piece.NONE) {
            score += getPieceValue(capturedPiece, move.getTo()) * 10;
            Piece attacker = board.getPiece(move.getFrom());
            if (attacker != Piece.NONE) {
                score -= getPieceValue(attacker, move.getFrom());
            }
        }

        // Promotion bonus
        if (move.getPromotion() != Piece.NONE) {
            score += getPieceValue(move.getPromotion(), move.getTo()) * 10;
        }

        return score;
    }
}
