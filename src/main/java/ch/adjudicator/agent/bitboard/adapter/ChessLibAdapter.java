package ch.adjudicator.agent.bitboard.adapter;

import ch.adjudicator.agent.bitboard.model.BoardState;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Adapter to convert between ChessLib representations and bitboard representations.
 * Phase 7: Conversion adapter implementation.
 */
public class ChessLibAdapter {

    /**
     * Converts a FEN string to a BoardState bitboard representation.
     * The bit representation matches the position and representation of ch.adjudicator.agent.bitboard.generator.
     * 
     * @param fen FEN string representing the chess position
     * @return BoardState with bitboards populated according to the FEN
     */
    public static BoardState fenToBoardState(String fen) {
        Board board = new Board();
        board.loadFromFen(fen);
        return boardToBoardState(board);
    }

    /**
     * Converts a ChessLib Board to a BoardState bitboard representation.
     * 
     * @param board ChessLib Board instance
     * @return BoardState with bitboards populated
     */
    public static BoardState boardToBoardState(Board board) {
        BoardState state = new BoardState();
        
        // Initialize all bitboards to 0
        for (int i = 0; i < 6; i++) {
            state.whitePieces[i] = 0L;
            state.blackPieces[i] = 0L;
        }
        
        // Populate piece bitboards
        for (Square square : Square.values()) {
            if (square == Square.NONE) continue;
            
            Piece piece = board.getPiece(square);
            if (piece == Piece.NONE) continue;
            
            int squareIndex = square.ordinal();
            long squareBit = 1L << squareIndex;
            
            // Determine piece type and color
            int pieceIndex = getPieceIndex(piece);
            if (pieceIndex == -1) continue; // Skip if invalid piece
            
            if (piece.getPieceSide() == Side.WHITE) {
                state.whitePieces[pieceIndex] |= squareBit;
            } else {
                state.blackPieces[pieceIndex] |= squareBit;
            }
        }
        
        // Set side to move
        state.setWhiteToMove(board.getSideToMove() == Side.WHITE);
        
        // Set castling rights - parse from FEN string
        String fen = board.getFen();
        String[] fenParts = fen.split(" ");
        String castlingRights = fenParts.length > 2 ? fenParts[2] : "-";
        state.setWhiteKingsideCastling(castlingRights.contains("K"));
        state.setWhiteQueensideCastling(castlingRights.contains("Q"));
        state.setBlackKingsideCastling(castlingRights.contains("k"));
        state.setBlackQueensideCastling(castlingRights.contains("q"));
        
        // Set en passant square
        Square epSquare = board.getEnPassant();
        if (epSquare != null && epSquare != Square.NONE) {
            state.setEnPassantSquare(epSquare.ordinal());
        } else {
            state.setEnPassantSquare(-1);
        }
        
        return state;
    }

    /**
     * Helper method to map ChessLib Piece to BoardState piece index.
     * 
     * @param piece ChessLib Piece
     * @return piece index (0-5) or -1 if invalid
     */
    private static int getPieceIndex(Piece piece) {
        switch (piece.getPieceType()) {
            case PAWN:
                return BoardState.INDEX_PAWN;
            case KNIGHT:
                return BoardState.INDEX_KNIGHT;
            case BISHOP:
                return BoardState.INDEX_BISHOP;
            case ROOK:
                return BoardState.INDEX_ROOK;
            case QUEEN:
                return BoardState.INDEX_QUEEN;
            case KING:
                return BoardState.INDEX_KING;
            default:
                return -1;
        }
    }

    /**
     * Converts a BoardState bitboard representation to a FEN string.
     * 
     * @param state BoardState with bitboards
     * @return FEN string representing the position
     */
    public static String boardStateToFen(BoardState state) {
        StringBuilder fen = new StringBuilder();
        
        // Build piece placement (first part of FEN)
        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;
            for (int file = 0; file < 8; file++) {
                int square = rank * 8 + file;
                long squareBit = 1L << square;
                
                char pieceChar = getPieceCharAt(state, squareBit);
                
                if (pieceChar == '.') {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(pieceChar);
                }
            }
            
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            
            if (rank > 0) {
                fen.append('/');
            }
        }
        
        // Active color
        fen.append(' ');
        fen.append(state.isWhiteToMove() ? 'w' : 'b');
        
        // Castling rights
        fen.append(' ');
        StringBuilder castling = new StringBuilder();
        if (state.isWhiteKingsideCastling()) castling.append('K');
        if (state.isWhiteQueensideCastling()) castling.append('Q');
        if (state.isBlackKingsideCastling()) castling.append('k');
        if (state.isBlackQueensideCastling()) castling.append('q');
        if (castling.isEmpty()) {
            fen.append('-');
        } else {
            fen.append(castling);
        }
        
        // En passant square
        fen.append(' ');
        int epSquareIndex = state.getEnPassantSquare();
        if (epSquareIndex == -1) {
            fen.append('-');
        } else {
            Square epSquare = Square.values()[epSquareIndex];
            fen.append(epSquare.toString().toLowerCase());
        }

        // for optimization of search, we don't care about move counter at this point
        fen.append(' ');
        fen.append(0);
        fen.append(' ');
        fen.append(1);

        return fen.toString();
    }

    /**
     * Helper method to get the piece character at a specific square bit.
     * 
     * @param state BoardState
     * @param squareBit bit representing the square
     * @return piece character (uppercase for white, lowercase for black, '.' for empty)
     */
    private static char getPieceCharAt(BoardState state, long squareBit) {
        // Check white pieces
        if ((state.whitePieces[BoardState.INDEX_PAWN] & squareBit) != 0) return 'P';
        if ((state.whitePieces[BoardState.INDEX_KNIGHT] & squareBit) != 0) return 'N';
        if ((state.whitePieces[BoardState.INDEX_BISHOP] & squareBit) != 0) return 'B';
        if ((state.whitePieces[BoardState.INDEX_ROOK] & squareBit) != 0) return 'R';
        if ((state.whitePieces[BoardState.INDEX_QUEEN] & squareBit) != 0) return 'Q';
        if ((state.whitePieces[BoardState.INDEX_KING] & squareBit) != 0) return 'K';
        
        // Check black pieces
        if ((state.blackPieces[BoardState.INDEX_PAWN] & squareBit) != 0) return 'p';
        if ((state.blackPieces[BoardState.INDEX_KNIGHT] & squareBit) != 0) return 'n';
        if ((state.blackPieces[BoardState.INDEX_BISHOP] & squareBit) != 0) return 'b';
        if ((state.blackPieces[BoardState.INDEX_ROOK] & squareBit) != 0) return 'r';
        if ((state.blackPieces[BoardState.INDEX_QUEEN] & squareBit) != 0) return 'q';
        if ((state.blackPieces[BoardState.INDEX_KING] & squareBit) != 0) return 'k';
        
        return '.';
    }
}
