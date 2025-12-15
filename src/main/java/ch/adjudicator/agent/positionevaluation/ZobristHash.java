package ch.adjudicator.agent.positionevaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

import java.util.Random;

/**
 * Zobrist Hashing for position identification
 */
public class ZobristHash {
        private long[][][] pieceKeys; // [piece_type][color][square]
        private long[] castlingKeys; // 4 castling rights
        private long[] enPassantKeys; // 8 files
        private long sideToMoveKey;
        private Random random;

        public ZobristHash() {
            random = new Random(123456789L); // Fixed seed for reproducibility
            pieceKeys = new long[6][2][64];
            castlingKeys = new long[4];
            enPassantKeys = new long[8];

            // Initialize piece keys
            for (int piece = 0; piece < 6; piece++) {
                for (int color = 0; color < 2; color++) {
                    for (int square = 0; square < 64; square++) {
                        pieceKeys[piece][color][square] = random.nextLong();
                    }
                }
            }

            // Initialize castling keys
            for (int i = 0; i < 4; i++) {
                castlingKeys[i] = random.nextLong();
            }

            // Initialize en passant keys
            for (int i = 0; i < 8; i++) {
                enPassantKeys[i] = random.nextLong();
            }

            sideToMoveKey = random.nextLong();
        }

        public long computeHash(Board board) {
            long hash = 0L;

            // Hash pieces
            for (Square square : Square.values()) {
                if (square == Square.NONE) continue;

                Piece piece = board.getPiece(square);
                if (piece == Piece.NONE) continue;

                int pieceType = getPieceTypeIndex(piece);
                int color = piece.getPieceSide() == Side.WHITE ? 0 : 1;
                int squareIndex = square.ordinal();

                hash ^= pieceKeys[pieceType][color][squareIndex];
            }

            // Hash castling rights
            String castleStr = board.getCastleRight(Side.WHITE).toString() +
                    board.getCastleRight(Side.BLACK).toString();
            if (castleStr.contains("K")) {
                hash ^= castlingKeys[0];
            }
            if (castleStr.contains("Q")) {
                hash ^= castlingKeys[1];
            }
            if (castleStr.contains("k")) {
                hash ^= castlingKeys[2];
            }
            if (castleStr.contains("q")) {
                hash ^= castlingKeys[3];
            }

            // Hash en passant
            if (board.getEnPassant() != Square.NONE) {
                int file = board.getEnPassant().getFile().ordinal();
                hash ^= enPassantKeys[file];
            }

            // Hash side to move
            if (board.getSideToMove() == Side.BLACK) {
                hash ^= sideToMoveKey;
            }

            return hash;
        }

        private int getPieceTypeIndex(Piece piece) {
            switch (piece.getPieceType()) {
                case PAWN: return 0;
                case KNIGHT: return 1;
                case BISHOP: return 2;
                case ROOK: return 3;
                case QUEEN: return 4;
                case KING: return 5;
                default: return 0;
            }
        }
}
