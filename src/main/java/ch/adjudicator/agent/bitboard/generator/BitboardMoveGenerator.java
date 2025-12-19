package ch.adjudicator.agent.bitboard.generator;

import ch.adjudicator.agent.bitboard.model.BoardState;
import ch.adjudicator.agent.bitboard.model.FastMove;

import java.util.ArrayList;
import java.util.List;

public class BitboardMoveGenerator {
    public static List<FastMove> generateMoves(BoardState boardState, FastMove lastMove) {
        List<FastMove> fastMoves;
        if(boardState.isWhiteToMove()) {
            fastMoves = generateWhiteMoves(boardState);
        } else {
            fastMoves = generateBlackMoves(boardState);
        }
        return filterPlayerInCheckMoves(boardState, fastMoves, lastMove);
    }

    private static List<FastMove> filterPlayerInCheckMoves(BoardState boardState, List<FastMove> fastMoves, FastMove lastMove) {
        List<FastMove> legalMoves = new ArrayList<>();
        boolean isWhite = boardState.isWhiteToMove();
        
        for (FastMove move : fastMoves) {
            // Make the move on a temporary board state
            BoardState tempState = makeMove(boardState, move, isWhite);
            
            // Check if the player's own king is in check after the move
            if (!isKingInCheck(tempState, isWhite)) {
                legalMoves.add(move);
            }
        }
        
        return legalMoves;
    }
    
    /**
     * Makes a move on a copy of the board state and returns the new state.
     */
    private static BoardState makeMove(BoardState boardState, FastMove move, boolean isWhite) {
        BoardState newState = copyBoardState(boardState);
        
        int from = move.originSquare;
        int to = move.destinationSquare;
        long fromBit = 1L << from;
        long toBit = 1L << to;
        
        // Find which piece is moving
        int pieceType = -1;
        if (isWhite) {
            for (int i = 0; i < 6; i++) {
                if ((newState.whitePieces[i] & fromBit) != 0) {
                    pieceType = i;
                    break;
                }
            }
            
            if (pieceType != -1) {
                // Remove piece from origin
                newState.whitePieces[pieceType] &= ~fromBit;
                
                // Handle captures - remove enemy piece at destination
                for (int i = 0; i < 6; i++) {
                    newState.blackPieces[i] &= ~toBit;
                }
                
                // Handle special moves
                if (move.castling) {
                    // Move the king
                    newState.whitePieces[pieceType] |= toBit;
                    // Move the rook
                    if (to == 6) { // Kingside castling
                        newState.whitePieces[BoardState.INDEX_ROOK] &= ~(1L << 7);
                        newState.whitePieces[BoardState.INDEX_ROOK] |= (1L << 5);
                    } else if (to == 2) { // Queenside castling
                        newState.whitePieces[BoardState.INDEX_ROOK] &= ~(1L << 0);
                        newState.whitePieces[BoardState.INDEX_ROOK] |= (1L << 3);
                    }
                } else if (move.enPassant) {
                    // Move pawn to destination
                    newState.whitePieces[pieceType] |= toBit;
                    // Remove captured pawn (one rank below destination)
                    int capturedPawnSquare = to - 8;
                    newState.blackPieces[BoardState.INDEX_PAWN] &= ~(1L << capturedPawnSquare);
                } else if (move.promotion) {
                    // Place promoted piece at destination
                    newState.whitePieces[move.pieceTypeToPromote] |= toBit;
                } else {
                    // Normal move
                    newState.whitePieces[pieceType] |= toBit;
                }
            }
        } else {
            // Black's move
            for (int i = 0; i < 6; i++) {
                if ((newState.blackPieces[i] & fromBit) != 0) {
                    pieceType = i;
                    break;
                }
            }
            
            if (pieceType != -1) {
                // Remove piece from origin
                newState.blackPieces[pieceType] &= ~fromBit;
                
                // Handle captures - remove enemy piece at destination
                for (int i = 0; i < 6; i++) {
                    newState.whitePieces[i] &= ~toBit;
                }
                
                // Handle special moves
                if (move.castling) {
                    // Move the king
                    newState.blackPieces[pieceType] |= toBit;
                    // Move the rook
                    if (to == 62) { // Kingside castling
                        newState.blackPieces[BoardState.INDEX_ROOK] &= ~(1L << 63);
                        newState.blackPieces[BoardState.INDEX_ROOK] |= (1L << 61);
                    } else if (to == 58) { // Queenside castling
                        newState.blackPieces[BoardState.INDEX_ROOK] &= ~(1L << 56);
                        newState.blackPieces[BoardState.INDEX_ROOK] |= (1L << 59);
                    }
                } else if (move.enPassant) {
                    // Move pawn to destination
                    newState.blackPieces[pieceType] |= toBit;
                    // Remove captured pawn (one rank above destination)
                    int capturedPawnSquare = to + 8;
                    newState.whitePieces[BoardState.INDEX_PAWN] &= ~(1L << capturedPawnSquare);
                } else if (move.promotion) {
                    // Place promoted piece at destination
                    newState.blackPieces[move.pieceTypeToPromote] |= toBit;
                } else {
                    // Normal move
                    newState.blackPieces[pieceType] |= toBit;
                }
            }
        }
        
        return newState;
    }
    
    /**
     * Copies a board state.
     */
    public static BoardState copyBoardState(BoardState state) {
        BoardState copy = new BoardState();
        for (int i = 0; i < 6; i++) {
            copy.whitePieces[i] = state.whitePieces[i];
            copy.blackPieces[i] = state.blackPieces[i];
        }
        copy.bitAuxiliaries = state.bitAuxiliaries;
        return copy;
    }

    public static boolean isCurrentPlayerInCheck(BoardState boardState) {
        if(boardState.isWhiteToMove()) {
            return isKingInCheck(boardState, true);
        }
        return isKingInCheck(boardState, false);
    }

    /**
     * Checks if the specified side's king is in check.
     */
    private static boolean isKingInCheck(BoardState boardState, boolean whiteKing) {
        long kingBitboard = whiteKing ? boardState.whitePieces[BoardState.INDEX_KING] : boardState.blackPieces[BoardState.INDEX_KING];
        
        if (kingBitboard == 0) {
            return false; // No king (shouldn't happen in valid position)
        }
        
        int kingSquare = Long.numberOfTrailingZeros(kingBitboard);
        
        return whiteKing ? isSquareAttackedByBlack(kingSquare, boardState) : isSquareAttackedByWhite(kingSquare, boardState);
    }
    
    /**
     * Checks if a square is attacked by white pieces.
     */
    private static boolean isSquareAttackedByWhite(int square, BoardState boardState) {
        long whiteOccupied = getWhiteOccupied(boardState);
        long blackOccupied = getBlackOccupied(boardState);
        long allOccupied = whiteOccupied | blackOccupied;
        
        // Check for pawn attacks
        int rank = square / 8;
        int file = square % 8;
        if (rank > 0) {
            if (file > 0) {
                long leftPawnAttack = 1L << (square - 9);
                if ((boardState.whitePieces[BoardState.INDEX_PAWN] & leftPawnAttack) != 0) {
                    return true;
                }
            }
            if (file < 7) {
                long rightPawnAttack = 1L << (square - 7);
                if ((boardState.whitePieces[BoardState.INDEX_PAWN] & rightPawnAttack) != 0) {
                    return true;
                }
            }
        }
        
        // Check for knight attacks
        long knightAttacks = BitboardGenerator.KNIGHT_ATTACKS[square];
        if ((knightAttacks & boardState.whitePieces[BoardState.INDEX_KNIGHT]) != 0) {
            return true;
        }
        
        // Check for king attacks
        long kingAttacks = BitboardGenerator.KING_ATTACKS[square];
        if ((kingAttacks & boardState.whitePieces[BoardState.INDEX_KING]) != 0) {
            return true;
        }
        
        // Check for bishop/queen attacks (diagonal)
        long bishopAttacks = BitboardGenerator.getBishopAttacks(square, allOccupied);
        if ((bishopAttacks & (boardState.whitePieces[BoardState.INDEX_BISHOP] | boardState.whitePieces[BoardState.INDEX_QUEEN])) != 0) {
            return true;
        }
        
        // Check for rook/queen attacks (straight)
        long rookAttacks = BitboardGenerator.getRookAttacks(square, allOccupied);
        if ((rookAttacks & (boardState.whitePieces[BoardState.INDEX_ROOK] | boardState.whitePieces[BoardState.INDEX_QUEEN])) != 0) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a square is attacked by black pieces.
     */
    private static boolean isSquareAttackedByBlack(int square, BoardState boardState) {
        long whiteOccupied = getWhiteOccupied(boardState);
        long blackOccupied = getBlackOccupied(boardState);
        long allOccupied = whiteOccupied | blackOccupied;
        
        // Check for pawn attacks
        int rank = square / 8;
        int file = square % 8;
        if (rank < 7) {
            if (file > 0) {
                long leftPawnAttack = 1L << (square + 7);
                if ((boardState.blackPieces[BoardState.INDEX_PAWN] & leftPawnAttack) != 0) {
                    return true;
                }
            }
            if (file < 7) {
                long rightPawnAttack = 1L << (square + 9);
                if ((boardState.blackPieces[BoardState.INDEX_PAWN] & rightPawnAttack) != 0) {
                    return true;
                }
            }
        }
        
        // Check for knight attacks
        long knightAttacks = BitboardGenerator.KNIGHT_ATTACKS[square];
        if ((knightAttacks & boardState.blackPieces[BoardState.INDEX_KNIGHT]) != 0) {
            return true;
        }
        
        // Check for king attacks
        long kingAttacks = BitboardGenerator.KING_ATTACKS[square];
        if ((kingAttacks & boardState.blackPieces[BoardState.INDEX_KING]) != 0) {
            return true;
        }
        
        // Check for bishop/queen attacks (diagonal)
        long bishopAttacks = BitboardGenerator.getBishopAttacks(square, allOccupied);
        if ((bishopAttacks & (boardState.blackPieces[BoardState.INDEX_BISHOP] | boardState.blackPieces[BoardState.INDEX_QUEEN])) != 0) {
            return true;
        }
        
        // Check for rook/queen attacks (straight)
        long rookAttacks = BitboardGenerator.getRookAttacks(square, allOccupied);
        if ((rookAttacks & (boardState.blackPieces[BoardState.INDEX_ROOK] | boardState.blackPieces[BoardState.INDEX_QUEEN])) != 0) {
            return true;
        }
        
        return false;
    }

    private static List<FastMove> generateWhiteMoves(BoardState boardState) {
        List<FastMove> moves = new ArrayList<>();
        
        // Calculate occupancy bitboards
        long whiteOccupied = getWhiteOccupied(boardState);
        long blackOccupied = getBlackOccupied(boardState);
        long allOccupied = whiteOccupied | blackOccupied;
        
        // Generate moves for each piece type
        generateWhitePawnMoves(boardState, moves, whiteOccupied, blackOccupied, allOccupied);
        generateWhiteKnightMoves(boardState, moves, whiteOccupied, blackOccupied);
        generateWhiteBishopMoves(boardState, moves, whiteOccupied, blackOccupied, allOccupied);
        generateWhiteRookMoves(boardState, moves, whiteOccupied, blackOccupied, allOccupied);
        generateWhiteQueenMoves(boardState, moves, whiteOccupied, blackOccupied, allOccupied);
        generateWhiteKingMoves(boardState, moves, whiteOccupied, blackOccupied);
        
        return moves;
    }

    private static List<FastMove> generateBlackMoves(BoardState boardState) {
        List<FastMove> moves = new ArrayList<>();
        
        // Calculate occupancy bitboards
        long whiteOccupied = getWhiteOccupied(boardState);
        long blackOccupied = getBlackOccupied(boardState);
        long allOccupied = whiteOccupied | blackOccupied;
        
        // Generate moves for each piece type
        generateBlackPawnMoves(boardState, moves, whiteOccupied, blackOccupied, allOccupied);
        generateBlackKnightMoves(boardState, moves, whiteOccupied, blackOccupied);
        generateBlackBishopMoves(boardState, moves, whiteOccupied, blackOccupied, allOccupied);
        generateBlackRookMoves(boardState, moves, whiteOccupied, blackOccupied, allOccupied);
        generateBlackQueenMoves(boardState, moves, whiteOccupied, blackOccupied, allOccupied);
        generateBlackKingMoves(boardState, moves, whiteOccupied, blackOccupied);
        
        return moves;
    }

    // Helper methods for occupancy
    private static long getWhiteOccupied(BoardState boardState) {
        return boardState.whitePieces[BoardState.INDEX_PAWN] |
               boardState.whitePieces[BoardState.INDEX_KNIGHT] |
               boardState.whitePieces[BoardState.INDEX_BISHOP] |
               boardState.whitePieces[BoardState.INDEX_ROOK] |
               boardState.whitePieces[BoardState.INDEX_QUEEN] |
               boardState.whitePieces[BoardState.INDEX_KING];
    }

    private static long getBlackOccupied(BoardState boardState) {
        return boardState.blackPieces[BoardState.INDEX_PAWN] |
               boardState.blackPieces[BoardState.INDEX_KNIGHT] |
               boardState.blackPieces[BoardState.INDEX_BISHOP] |
               boardState.blackPieces[BoardState.INDEX_ROOK] |
               boardState.blackPieces[BoardState.INDEX_QUEEN] |
               boardState.blackPieces[BoardState.INDEX_KING];
    }

    // White pawn move generation
    private static void generateWhitePawnMoves(BoardState boardState, List<FastMove> moves, 
                                               long whiteOccupied, long blackOccupied, long allOccupied) {
        long pawns = boardState.whitePieces[BoardState.INDEX_PAWN];
        
        while (pawns != 0) {
            int from = Long.numberOfTrailingZeros(pawns);
            long fromBit = 1L << from;
            pawns &= pawns - 1; // Clear the least significant bit
            
            int rank = from / 8;
            int file = from % 8;
            
            // Single push
            int singlePush = from + 8;
            if (singlePush < 64 && (allOccupied & (1L << singlePush)) == 0) {
                if (rank == 6) { // Promotion rank
                    addPromotionMoves(moves, from, singlePush);
                } else {
                    addMove(moves, from, singlePush, false, false, false);
                }
                
                // Double push from starting rank
                if (rank == 1) {
                    int doublePush = from + 16;
                    if ((allOccupied & (1L << doublePush)) == 0) {
                        addMove(moves, from, doublePush, false, false, false);
                    }
                }
            }
            
            // Captures
            // Left capture
            if (file > 0) {
                int leftCapture = from + 7;
                if (leftCapture < 64 && (blackOccupied & (1L << leftCapture)) != 0) {
                    if (rank == 6) {
                        addPromotionMoves(moves, from, leftCapture);
                    } else {
                        addMove(moves, from, leftCapture, false, false, false);
                    }
                }
            }
            
            // Right capture
            if (file < 7) {
                int rightCapture = from + 9;
                if (rightCapture < 64 && (blackOccupied & (1L << rightCapture)) != 0) {
                    if (rank == 6) {
                        addPromotionMoves(moves, from, rightCapture);
                    } else {
                        addMove(moves, from, rightCapture, false, false, false);
                    }
                }
            }
            
            // En passant
            int epSquare = boardState.getEnPassantSquare();
            if (epSquare != -1 && rank == 4) {
                int epFile = epSquare % 8;
                if (Math.abs(file - epFile) == 1 && epSquare == from + 8 + (epFile - file)) {
                    addMove(moves, from, epSquare, false, true, false);
                }
            }
        }
    }

    // Black pawn move generation
    private static void generateBlackPawnMoves(BoardState boardState, List<FastMove> moves,
                                               long whiteOccupied, long blackOccupied, long allOccupied) {
        long pawns = boardState.blackPieces[BoardState.INDEX_PAWN];
        
        while (pawns != 0) {
            int from = Long.numberOfTrailingZeros(pawns);
            long fromBit = 1L << from;
            pawns &= pawns - 1;
            
            int rank = from / 8;
            int file = from % 8;
            
            // Single push (down)
            int singlePush = from - 8;
            if (singlePush >= 0 && (allOccupied & (1L << singlePush)) == 0) {
                if (rank == 1) { // Promotion rank
                    addPromotionMoves(moves, from, singlePush);
                } else {
                    addMove(moves, from, singlePush, false, false, false);
                }
                
                // Double push from starting rank
                if (rank == 6) {
                    int doublePush = from - 16;
                    if ((allOccupied & (1L << doublePush)) == 0) {
                        addMove(moves, from, doublePush, false, false, false);
                    }
                }
            }
            
            // Captures
            // Left capture (from black's perspective)
            if (file > 0) {
                int leftCapture = from - 9;
                if (leftCapture >= 0 && (whiteOccupied & (1L << leftCapture)) != 0) {
                    if (rank == 1) {
                        addPromotionMoves(moves, from, leftCapture);
                    } else {
                        addMove(moves, from, leftCapture, false, false, false);
                    }
                }
            }
            
            // Right capture
            if (file < 7) {
                int rightCapture = from - 7;
                if (rightCapture >= 0 && (whiteOccupied & (1L << rightCapture)) != 0) {
                    if (rank == 1) {
                        addPromotionMoves(moves, from, rightCapture);
                    } else {
                        addMove(moves, from, rightCapture, false, false, false);
                    }
                }
            }
            
            // En passant
            int epSquare = boardState.getEnPassantSquare();
            if (epSquare != -1 && rank == 3) {
                int epFile = epSquare % 8;
                if (Math.abs(file - epFile) == 1 && epSquare == from - 8 + (epFile - file)) {
                    addMove(moves, from, epSquare, false, true, false);
                }
            }
        }
    }

    // Knight move generation
    private static void generateWhiteKnightMoves(BoardState boardState, List<FastMove> moves,
                                                 long whiteOccupied, long blackOccupied) {
        long knights = boardState.whitePieces[BoardState.INDEX_KNIGHT];
        
        while (knights != 0) {
            int from = Long.numberOfTrailingZeros(knights);
            knights &= knights - 1;
            
            long attacks = BitboardGenerator.KNIGHT_ATTACKS[from];
            attacks &= ~whiteOccupied; // Remove friendly pieces
            
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                addMove(moves, from, to, false, false, false);
            }
        }
    }

    private static void generateBlackKnightMoves(BoardState boardState, List<FastMove> moves,
                                                 long whiteOccupied, long blackOccupied) {
        long knights = boardState.blackPieces[BoardState.INDEX_KNIGHT];
        
        while (knights != 0) {
            int from = Long.numberOfTrailingZeros(knights);
            knights &= knights - 1;
            
            long attacks = BitboardGenerator.KNIGHT_ATTACKS[from];
            attacks &= ~blackOccupied; // Remove friendly pieces
            
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                addMove(moves, from, to, false, false, false);
            }
        }
    }

    // Bishop move generation
    private static void generateWhiteBishopMoves(BoardState boardState, List<FastMove> moves,
                                                 long whiteOccupied, long blackOccupied, long allOccupied) {
        long bishops = boardState.whitePieces[BoardState.INDEX_BISHOP];
        
        while (bishops != 0) {
            int from = Long.numberOfTrailingZeros(bishops);
            bishops &= bishops - 1;
            
            long attacks = BitboardGenerator.getBishopAttacks(from, allOccupied);
            attacks &= ~whiteOccupied;
            
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                addMove(moves, from, to, false, false, false);
            }
        }
    }

    private static void generateBlackBishopMoves(BoardState boardState, List<FastMove> moves,
                                                 long whiteOccupied, long blackOccupied, long allOccupied) {
        long bishops = boardState.blackPieces[BoardState.INDEX_BISHOP];
        
        while (bishops != 0) {
            int from = Long.numberOfTrailingZeros(bishops);
            bishops &= bishops - 1;
            
            long attacks = BitboardGenerator.getBishopAttacks(from, allOccupied);
            attacks &= ~blackOccupied;
            
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                addMove(moves, from, to, false, false, false);
            }
        }
    }

    // Rook move generation
    private static void generateWhiteRookMoves(BoardState boardState, List<FastMove> moves,
                                               long whiteOccupied, long blackOccupied, long allOccupied) {
        long rooks = boardState.whitePieces[BoardState.INDEX_ROOK];
        
        while (rooks != 0) {
            int from = Long.numberOfTrailingZeros(rooks);
            rooks &= rooks - 1;
            
            long attacks = BitboardGenerator.getRookAttacks(from, allOccupied);
            attacks &= ~whiteOccupied;
            
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                addMove(moves, from, to, false, false, false);
            }
        }
    }

    private static void generateBlackRookMoves(BoardState boardState, List<FastMove> moves,
                                               long whiteOccupied, long blackOccupied, long allOccupied) {
        long rooks = boardState.blackPieces[BoardState.INDEX_ROOK];
        
        while (rooks != 0) {
            int from = Long.numberOfTrailingZeros(rooks);
            rooks &= rooks - 1;
            
            long attacks = BitboardGenerator.getRookAttacks(from, allOccupied);
            attacks &= ~blackOccupied;
            
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                addMove(moves, from, to, false, false, false);
            }
        }
    }

    // Queen move generation
    private static void generateWhiteQueenMoves(BoardState boardState, List<FastMove> moves,
                                                long whiteOccupied, long blackOccupied, long allOccupied) {
        long queens = boardState.whitePieces[BoardState.INDEX_QUEEN];
        
        while (queens != 0) {
            int from = Long.numberOfTrailingZeros(queens);
            queens &= queens - 1;
            
            long attacks = BitboardGenerator.getBishopAttacks(from, allOccupied) | BitboardGenerator.getRookAttacks(from, allOccupied);
            attacks &= ~whiteOccupied;
            
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                addMove(moves, from, to, false, false, false);
            }
        }
    }

    private static void generateBlackQueenMoves(BoardState boardState, List<FastMove> moves,
                                                long whiteOccupied, long blackOccupied, long allOccupied) {
        long queens = boardState.blackPieces[BoardState.INDEX_QUEEN];
        
        while (queens != 0) {
            int from = Long.numberOfTrailingZeros(queens);
            queens &= queens - 1;
            
            long attacks = BitboardGenerator.getBishopAttacks(from, allOccupied) | BitboardGenerator.getRookAttacks(from, allOccupied);
            attacks &= ~blackOccupied;
            
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                addMove(moves, from, to, false, false, false);
            }
        }
    }

    // King move generation
    private static void generateWhiteKingMoves(BoardState boardState, List<FastMove> moves,
                                               long whiteOccupied, long blackOccupied) {
        long king = boardState.whitePieces[BoardState.INDEX_KING];
        
        if (king != 0) {
            int from = Long.numberOfTrailingZeros(king);
            
            long attacks = BitboardGenerator.KING_ATTACKS[from];
            attacks &= ~whiteOccupied;
            
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                addMove(moves, from, to, false, false, false);
            }
            
            // Castling
            long allOccupied = whiteOccupied | blackOccupied;
            
            // Kingside castling
            if (boardState.isWhiteKingsideCastling() && from == 4) {
                if ((allOccupied & 0x60L) == 0) { // f1 and g1 empty
                    // Check that king is not in check and doesn't pass through check
                    if (!isSquareAttackedByBlack(4, boardState) && 
                        !isSquareAttackedByBlack(5, boardState) && 
                        !isSquareAttackedByBlack(6, boardState)) {
                        addMove(moves, from, 6, false, false, true);
                    }
                }
            }
            
            // Queenside castling
            if (boardState.isWhiteQueensideCastling() && from == 4) {
                if ((allOccupied & 0x0EL) == 0) { // b1, c1, d1 empty
                    // Check that king is not in check and doesn't pass through check
                    if (!isSquareAttackedByBlack(4, boardState) && 
                        !isSquareAttackedByBlack(3, boardState) && 
                        !isSquareAttackedByBlack(2, boardState)) {
                        addMove(moves, from, 2, false, false, true);
                    }
                }
            }
        }
    }

    private static void generateBlackKingMoves(BoardState boardState, List<FastMove> moves,
                                               long whiteOccupied, long blackOccupied) {
        long king = boardState.blackPieces[BoardState.INDEX_KING];
        
        if (king != 0) {
            int from = Long.numberOfTrailingZeros(king);
            
            long attacks = BitboardGenerator.KING_ATTACKS[from];
            attacks &= ~blackOccupied;
            
            while (attacks != 0) {
                int to = Long.numberOfTrailingZeros(attacks);
                attacks &= attacks - 1;
                addMove(moves, from, to, false, false, false);
            }
            
            // Castling
            long allOccupied = whiteOccupied | blackOccupied;
            
            // Kingside castling
            if (boardState.isBlackKingsideCastling() && from == 60) {
                if ((allOccupied & 0x6000000000000000L) == 0) { // f8 and g8 empty
                    // Check that king is not in check and doesn't pass through check
                    if (!isSquareAttackedByWhite(60, boardState) && 
                        !isSquareAttackedByWhite(61, boardState) && 
                        !isSquareAttackedByWhite(62, boardState)) {
                        addMove(moves, from, 62, false, false, true);
                    }
                }
            }
            
            // Queenside castling
            if (boardState.isBlackQueensideCastling() && from == 60) {
                if ((allOccupied & 0x0E00000000000000L) == 0) { // b8, c8, d8 empty
                    // Check that king is not in check and doesn't pass through check
                    if (!isSquareAttackedByWhite(60, boardState) && 
                        !isSquareAttackedByWhite(59, boardState) && 
                        !isSquareAttackedByWhite(58, boardState)) {
                        addMove(moves, from, 58, false, false, true);
                    }
                }
            }
        }
    }

    // Move creation helpers
    private static void addMove(List<FastMove> moves, int from, int to, boolean promotion, boolean enPassant, boolean castling) {
        FastMove move = new FastMove();
        move.originSquare = from;
        move.destinationSquare = to;
        move.promotion = promotion;
        move.enPassant = enPassant;
        move.castling = castling;
        moves.add(move);
    }

    private static void addPromotionMoves(List<FastMove> moves, int from, int to) {
        // Queen, Rook, Bishop, Knight promotions
        int[] promotionPieces = {BoardState.INDEX_QUEEN, BoardState.INDEX_ROOK, BoardState.INDEX_BISHOP, BoardState.INDEX_KNIGHT};
        
        for (int piece : promotionPieces) {
            FastMove move = new FastMove();
            move.originSquare = from;
            move.destinationSquare = to;
            move.promotion = true;
            move.pieceTypeToPromote = piece;
            moves.add(move);
        }
    }
}
