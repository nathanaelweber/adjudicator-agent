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
        state.whiteToMove = (board.getSideToMove() == Side.WHITE);
        
        // Set castling rights - parse from FEN string
        String fen = board.getFen();
        String[] fenParts = fen.split(" ");
        String castlingRights = fenParts.length > 2 ? fenParts[2] : "-";
        state.whiteKingsideCastling = castlingRights.contains("K");
        state.whiteQueensideCastling = castlingRights.contains("Q");
        state.blackKingsideCastling = castlingRights.contains("k");
        state.blackQueensideCastling = castlingRights.contains("q");
        
        // Set en passant square
        Square epSquare = board.getEnPassant();
        if (epSquare != null && epSquare != Square.NONE) {
            state.enPassantSquare = epSquare.ordinal();
        } else {
            state.enPassantSquare = -1;
        }
        
        // Set move counters
        state.halfmoveClock = board.getHalfMoveCounter();
        state.fullmoveNumber = board.getMoveCounter();
        
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
        fen.append(state.whiteToMove ? 'w' : 'b');
        
        // Castling rights
        fen.append(' ');
        StringBuilder castling = new StringBuilder();
        if (state.whiteKingsideCastling) castling.append('K');
        if (state.whiteQueensideCastling) castling.append('Q');
        if (state.blackKingsideCastling) castling.append('k');
        if (state.blackQueensideCastling) castling.append('q');
        if (castling.length() == 0) {
            fen.append('-');
        } else {
            fen.append(castling);
        }
        
        // En passant square
        fen.append(' ');
        if (state.enPassantSquare == -1) {
            fen.append('-');
        } else {
            Square epSquare = Square.values()[state.enPassantSquare];
            fen.append(epSquare.toString().toLowerCase());
        }
        
        // Halfmove clock and fullmove number
        fen.append(' ');
        fen.append(state.halfmoveClock);
        fen.append(' ');
        fen.append(state.fullmoveNumber);
        
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
