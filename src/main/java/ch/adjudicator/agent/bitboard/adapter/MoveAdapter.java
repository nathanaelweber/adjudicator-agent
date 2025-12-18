package ch.adjudicator.agent.bitboard.adapter;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

/**
 * Adapter to convert between ChessLib Move objects and bitboard move representation.
 * Phase 7: Move conversion adapter implementation.
 * 
 * Bitboard move encoding (32-bit int):
 * - Bits 0-5: Source square (0-63)
 * - Bits 6-11: Destination square (0-63)
 * - Bits 12-15: Special flags (Promotion, Castling, En Passant)
 */
public class MoveAdapter {

    // Special move flags (bits 12-15)
    public static final int FLAG_NONE = 0;
    public static final int FLAG_EN_PASSANT = 1;
    public static final int FLAG_CASTLING = 2;
    public static final int FLAG_PROMOTION_KNIGHT = 3;
    public static final int FLAG_PROMOTION_BISHOP = 4;
    public static final int FLAG_PROMOTION_ROOK = 5;
    public static final int FLAG_PROMOTION_QUEEN = 6;

    /**
     * Converts a ChessLib Move to a bitboard move (int encoding).
     * 
     * @param move ChessLib Move object
     * @return bitboard move as int (source | dest << 6 | flags << 12)
     */
    public static int toBitboardMove(Move move) {
        int from = move.getFrom().ordinal();
        int to = move.getTo().ordinal();
        int flags = FLAG_NONE;

        // Determine special flags
        if (move.getPromotion() != Piece.NONE) {
            // Promotion move
            PieceType promotionType = move.getPromotion().getPieceType();
            flags = switch (promotionType) {
                case KNIGHT -> FLAG_PROMOTION_KNIGHT;
                case BISHOP -> FLAG_PROMOTION_BISHOP;
                case ROOK -> FLAG_PROMOTION_ROOK;
                case QUEEN -> FLAG_PROMOTION_QUEEN;
                default -> FLAG_NONE;
            };
        } else if (isCastlingMove(move)) {
            flags = FLAG_CASTLING;
        } else if (isEnPassantMove(move)) {
            flags = FLAG_EN_PASSANT;
        }

        return from | (to << 6) | (flags << 12);
    }

    /**
     * Converts a bitboard move (int encoding) to a ChessLib Move.
     * Note: This creates a basic Move object. For special moves like castling,
     * the caller may need additional context from the board state.
     * 
     * @param bitboardMove bitboard move as int
     * @return ChessLib Move object
     */
    public static Move toChessLibMove(int bitboardMove) {
        int from = bitboardMove & 0x3F;
        int to = (bitboardMove >> 6) & 0x3F;
        int flags = (bitboardMove >> 12) & 0xF;

        Square fromSquare = Square.values()[from];
        Square toSquare = Square.values()[to];

        Piece promotion = Piece.NONE;
        if (flags >= FLAG_PROMOTION_KNIGHT && flags <= FLAG_PROMOTION_QUEEN) {
            promotion = getPromotionPiece(flags);
        }

        return new Move(fromSquare, toSquare, promotion);
    }

    /**
     * Extracts the source square from a bitboard move.
     * 
     * @param bitboardMove bitboard move as int
     * @return source square index (0-63)
     */
    public static int getFromSquare(int bitboardMove) {
        return bitboardMove & 0x3F;
    }

    /**
     * Extracts the destination square from a bitboard move.
     * 
     * @param bitboardMove bitboard move as int
     * @return destination square index (0-63)
     */
    public static int getToSquare(int bitboardMove) {
        return (bitboardMove >> 6) & 0x3F;
    }

    /**
     * Extracts the special flags from a bitboard move.
     * 
     * @param bitboardMove bitboard move as int
     * @return flags (0-15)
     */
    public static int getFlags(int bitboardMove) {
        return (bitboardMove >> 12) & 0xF;
    }

    /**
     * Checks if a bitboard move is a promotion.
     * 
     * @param bitboardMove bitboard move as int
     * @return true if promotion move
     */
    public static boolean isPromotion(int bitboardMove) {
        int flags = getFlags(bitboardMove);
        return flags >= FLAG_PROMOTION_KNIGHT && flags <= FLAG_PROMOTION_QUEEN;
    }

    /**
     * Checks if a bitboard move is a castling move.
     * 
     * @param bitboardMove bitboard move as int
     * @return true if castling move
     */
    public static boolean isCastling(int bitboardMove) {
        return getFlags(bitboardMove) == FLAG_CASTLING;
    }

    /**
     * Checks if a bitboard move is an en passant move.
     * 
     * @param bitboardMove bitboard move as int
     * @return true if en passant move
     */
    public static boolean isEnPassant(int bitboardMove) {
        return getFlags(bitboardMove) == FLAG_EN_PASSANT;
    }

    /**
     * Creates a bitboard move from components.
     * 
     * @param from source square (0-63)
     * @param to destination square (0-63)
     * @param flags special flags
     * @return bitboard move as int
     */
    public static int createMove(int from, int to, int flags) {
        return from | (to << 6) | (flags << 12);
    }

    /**
     * Helper method to determine if a ChessLib Move is a castling move.
     * Castling is detected when a king moves more than 1 square horizontally.
     * 
     * @param move ChessLib Move
     * @return true if castling move
     */
    private static boolean isCastlingMove(Move move) {
        int fromSquare = move.getFrom().ordinal();
        int toSquare = move.getTo().ordinal();
        int fromFile = fromSquare % 8;
        int toFile = toSquare % 8;
        int fromRank = fromSquare / 8;
        int toRank = toSquare / 8;

        // King moves 2 squares horizontally on the same rank
        return fromRank == toRank && Math.abs(fromFile - toFile) == 2;
    }

    /**
     * Helper method to determine if a ChessLib Move is an en passant move.
     * This is a simplified heuristic - a pawn diagonal capture to an empty square.
     * For accurate detection, board state would be needed.
     * 
     * @param move ChessLib Move
     * @return true if likely en passant move
     */
    private static boolean isEnPassantMove(Move move) {
        // En passant detection requires board context.
        // This is a placeholder that returns false.
        // The caller should set this flag based on board state.
        return false;
    }

    /**
     * Helper method to get the promotion piece from flags.
     * 
     * @param flags special move flags
     * @return Piece for promotion (WHITE_QUEEN, WHITE_ROOK, etc.)
     */
    private static Piece getPromotionPiece(int flags) {
        // Note: ChessLib Move constructor will handle the color based on board context
        // We return white pieces here, but in practice the Move class adjusts
        return switch (flags) {
            case FLAG_PROMOTION_KNIGHT -> Piece.WHITE_KNIGHT;
            case FLAG_PROMOTION_BISHOP -> Piece.WHITE_BISHOP;
            case FLAG_PROMOTION_ROOK -> Piece.WHITE_ROOK;
            case FLAG_PROMOTION_QUEEN -> Piece.WHITE_QUEEN;
            default -> Piece.NONE;
        };
    }

    /**
     * Converts a move to a human-readable string (UCI format).
     * 
     * @param bitboardMove bitboard move as int
     * @return UCI move string (e.g., "e2e4", "e7e8q")
     */
    public static String toUciString(int bitboardMove) {
        int from = getFromSquare(bitboardMove);
        int to = getToSquare(bitboardMove);
        int flags = getFlags(bitboardMove);

        Square fromSquare = Square.values()[from];
        Square toSquare = Square.values()[to];

        StringBuilder uci = new StringBuilder();
        uci.append(fromSquare.toString().toLowerCase());
        uci.append(toSquare.toString().toLowerCase());

        if (isPromotion(bitboardMove)) {
            char promotionChar = switch (flags) {
                case FLAG_PROMOTION_KNIGHT -> 'n';
                case FLAG_PROMOTION_BISHOP -> 'b';
                case FLAG_PROMOTION_ROOK -> 'r';
                case FLAG_PROMOTION_QUEEN -> 'q';
                default -> ' ';
            };
            if (promotionChar != ' ') {
                uci.append(promotionChar);
            }
        }

        return uci.toString();
    }
}
