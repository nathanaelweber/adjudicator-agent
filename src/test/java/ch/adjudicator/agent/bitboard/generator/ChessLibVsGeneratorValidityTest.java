package ch.adjudicator.agent.bitboard.generator;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2b: Validate BitboardGenerator moves against ChessLib
 * 
 * This test class validates that our bitboard-generated moves match
 * the legal moves provided by the ChessLib library in real chess positions.
 */
class ChessLibVsGeneratorValidityTest {

    /**
     * Helper method to convert ChessLib Square to bitboard square index (0-63)
     */
    private int squareToBitboardIndex(Square square) {
        return square.ordinal();
    }

    /**
     * Helper method to convert bitboard index to ChessLib Square
     */
    private Square bitboardIndexToSquare(int index) {
        return Square.values()[index];
    }

    /**
     * Helper method to get all destination squares from a bitboard
     */
    private long getDestinationBitboard(long attackBitboard) {
        return attackBitboard;
    }

    /**
     * Test knight moves from starting position
     */
    @Test
    void testKnightMovesFromStartingPosition() {
        Board board = new Board();
        
        // White knight on b1 (square index 1)
        Square b1 = Square.B1;
        int b1Index = squareToBitboardIndex(b1);
        
        // Get our generated knight attacks
        long knightAttacks = BitboardGenerator.KNIGHT_ATTACKS[b1Index];
        
        // Get legal moves from ChessLib for knight on b1
        List<Move> legalMoves = board.legalMoves();
        
        // Filter moves that originate from b1 (knight moves)
        int chesslibKnightMoveCount = 0;
        for (Move move : legalMoves) {
            if (move.getFrom() == b1) {
                chesslibKnightMoveCount++;
                
                // Verify the destination is in our attack bitboard
                int destIndex = squareToBitboardIndex(move.getTo());
                long destBit = 1L << destIndex;
                assertTrue((knightAttacks & destBit) != 0, 
                    "ChessLib move to " + move.getTo() + " not found in our knight attacks from " + b1);
            }
        }
        
        // In starting position, knight on b1 can move to a3 and c3
        assertEquals(2, chesslibKnightMoveCount, "Knight on b1 should have 2 legal moves in starting position");
        
        // Verify our bitboard has exactly those moves (accounting for blocking pieces)
        // Note: Our attack bitboard shows all attacked squares, not considering blocking
        assertTrue(BitboardGenerator.bitCount(knightAttacks) >= chesslibKnightMoveCount,
            "Our knight attacks should include at least the legal moves");
    }

    /**
     * Test knight moves from center of board with no obstructions
     */
    @Test
    void testKnightMovesFromCenterEmptyBoard() {
        Board board = new Board();
        board.loadFromFen("4k3/8/8/3N4/8/8/8/4K3 w - - 0 1"); // White knight on d5, kings on e8 and e1
        
        Square d5 = Square.D5;
        int d5Index = squareToBitboardIndex(d5);
        
        // Get our generated knight attacks
        long knightAttacks = BitboardGenerator.KNIGHT_ATTACKS[d5Index];
        
        // Get legal moves from ChessLib (knight moves only, not king moves)
        List<Move> legalMoves = board.legalMoves();
        
        // Filter knight moves from d5
        int knightMoveCount = 0;
        for (Move move : legalMoves) {
            if (move.getFrom() == d5) {
                knightMoveCount++;
                
                int destIndex = squareToBitboardIndex(move.getTo());
                long destBit = 1L << destIndex;
                assertTrue((knightAttacks & destBit) != 0,
                    "ChessLib move to " + move.getTo() + " not found in our knight attacks from " + d5);
            }
        }
        
        // Knight on d5 should have 8 legal moves
        assertEquals(8, knightMoveCount, "Knight on d5 should have 8 legal moves");
        
        // Verify our bitboard has exactly 8 attacks
        assertEquals(8, BitboardGenerator.bitCount(knightAttacks),
            "Knight on d5 should attack exactly 8 squares");
    }

    /**
     * Test knight moves from corner
     */
    @Test
    void testKnightMovesFromCorner() {
        Board board = new Board();
        board.loadFromFen("4k2N/8/8/8/8/8/8/4K3 w - - 0 1"); // White knight on h8, kings on e8 and e1
        
        Square h8 = Square.H8;
        int h8Index = squareToBitboardIndex(h8);
        
        // Get our generated knight attacks
        long knightAttacks = BitboardGenerator.KNIGHT_ATTACKS[h8Index];
        
        // Get legal moves from ChessLib
        List<Move> legalMoves = board.legalMoves();
        
        // Filter knight moves from h8
        int knightMoveCount = 0;
        for (Move move : legalMoves) {
            if (move.getFrom() == h8) {
                knightMoveCount++;
                
                int destIndex = squareToBitboardIndex(move.getTo());
                long destBit = 1L << destIndex;
                assertTrue((knightAttacks & destBit) != 0,
                    "ChessLib move to " + move.getTo() + " not found in our knight attacks from " + h8);
            }
        }
        
        // Knight on h8 (corner) should have 2 legal moves
        assertEquals(2, knightMoveCount, "Knight on h8 should have 2 legal moves");
        
        // Verify our bitboard has exactly 2 attacks
        assertEquals(2, BitboardGenerator.bitCount(knightAttacks),
            "Knight on h8 should attack exactly 2 squares");
    }

    /**
     * Test king moves from starting position
     */
    @Test
    void testKingMovesFromStartingPosition() {
        Board board = new Board();
        
        // In starting position, the king cannot move (blocked by pieces)
        Square e1 = Square.E1;
        int e1Index = squareToBitboardIndex(e1);
        
        // Get our generated king attacks
        long kingAttacks = BitboardGenerator.KING_ATTACKS[e1Index];
        
        // Get legal moves from ChessLib
        List<Move> legalMoves = board.legalMoves();
        
        // Filter king moves
        int chesslibKingMoveCount = 0;
        for (Move move : legalMoves) {
            if (move.getFrom() == e1) {
                chesslibKingMoveCount++;
            }
        }
        
        // King has no legal moves in starting position (all squares blocked)
        assertEquals(0, chesslibKingMoveCount, "King should have no legal moves in starting position");
        
        // But our attack bitboard shows potential attacks (5 directions: e1 is on rank 1 edge)
        // King on e1 can attack: d1, f1, d2, e2, f2 (no squares below rank 1)
        assertEquals(5, BitboardGenerator.bitCount(kingAttacks),
            "King attack pattern from e1 should show 5 squares");
    }

    /**
     * Test king moves from center of empty board
     */
    @Test
    void testKingMovesFromCenterEmptyBoard() {
        Board board = new Board();
        board.loadFromFen("8/8/8/3K4/8/8/8/8 w - - 0 1"); // White king on d5
        
        Square d5 = Square.D5;
        int d5Index = squareToBitboardIndex(d5);
        
        // Get our generated king attacks
        long kingAttacks = BitboardGenerator.KING_ATTACKS[d5Index];
        
        // Get legal moves from ChessLib
        List<Move> legalMoves = board.legalMoves();
        
        // King on d5 (empty board) should have 8 legal moves
        assertEquals(8, legalMoves.size(), "King on d5 should have 8 legal moves");
        
        // Verify all ChessLib moves are in our attack bitboard
        for (Move move : legalMoves) {
            int destIndex = squareToBitboardIndex(move.getTo());
            long destBit = 1L << destIndex;
            assertTrue((kingAttacks & destBit) != 0,
                "ChessLib move to " + move.getTo() + " not found in our king attacks from " + d5);
        }
        
        // Verify our bitboard has exactly 8 attacks
        assertEquals(8, BitboardGenerator.bitCount(kingAttacks),
            "King on d5 should attack exactly 8 squares");
    }

    /**
     * Test king moves from corner
     */
    @Test
    void testKingMovesFromCorner() {
        Board board = new Board();
        board.loadFromFen("K7/8/8/8/8/8/8/8 w - - 0 1"); // White king on a8
        
        Square a8 = Square.A8;
        int a8Index = squareToBitboardIndex(a8);
        
        // Get our generated king attacks
        long kingAttacks = BitboardGenerator.KING_ATTACKS[a8Index];
        
        // Get legal moves from ChessLib
        List<Move> legalMoves = board.legalMoves();
        
        // King on a8 (corner) should have 3 legal moves
        assertEquals(3, legalMoves.size(), "King on a8 should have 3 legal moves");
        
        // Verify all ChessLib moves are in our attack bitboard
        for (Move move : legalMoves) {
            int destIndex = squareToBitboardIndex(move.getTo());
            long destBit = 1L << destIndex;
            assertTrue((kingAttacks & destBit) != 0,
                "ChessLib move to " + move.getTo() + " not found in our king attacks from " + a8);
        }
        
        // Verify our bitboard has exactly 3 attacks
        assertEquals(3, BitboardGenerator.bitCount(kingAttacks),
            "King on a8 should attack exactly 3 squares");
    }

    /**
     * Test king moves from edge (not corner)
     */
    @Test
    void testKingMovesFromEdge() {
        Board board = new Board();
        board.loadFromFen("8/8/8/K7/8/8/8/8 w - - 0 1"); // White king on a5
        
        Square a5 = Square.A5;
        int a5Index = squareToBitboardIndex(a5);
        
        // Get our generated king attacks
        long kingAttacks = BitboardGenerator.KING_ATTACKS[a5Index];
        
        // Get legal moves from ChessLib
        List<Move> legalMoves = board.legalMoves();
        
        // King on a5 (edge) should have 5 legal moves
        assertEquals(5, legalMoves.size(), "King on a5 should have 5 legal moves");
        
        // Verify all ChessLib moves are in our attack bitboard
        for (Move move : legalMoves) {
            int destIndex = squareToBitboardIndex(move.getTo());
            long destBit = 1L << destIndex;
            assertTrue((kingAttacks & destBit) != 0,
                "ChessLib move to " + move.getTo() + " not found in our king attacks from " + a5);
        }
        
        // Verify our bitboard has exactly 5 attacks
        assertEquals(5, BitboardGenerator.bitCount(kingAttacks),
            "King on a5 should attack exactly 5 squares");
    }

    /**
     * Test knight with captures available
     */
    @Test
    void testKnightWithCaptures() {
        Board board = new Board();
        // White knight on e4, black pawns on potential target squares, kings on a1 and a8
        board.loadFromFen("k7/8/3p1p2/2p3p1/4N3/2p3p1/3p1p2/K7 w - - 0 1");
        
        Square e4 = Square.E4;
        int e4Index = squareToBitboardIndex(e4);
        
        // Get our generated knight attacks
        long knightAttacks = BitboardGenerator.KNIGHT_ATTACKS[e4Index];
        
        // Get legal moves from ChessLib
        List<Move> legalMoves = board.legalMoves();
        
        // Filter knight moves from e4
        int knightMoveCount = 0;
        for (Move move : legalMoves) {
            if (move.getFrom() == e4) {
                knightMoveCount++;
                
                int destIndex = squareToBitboardIndex(move.getTo());
                long destBit = 1L << destIndex;
                assertTrue((knightAttacks & destBit) != 0,
                    "ChessLib capture to " + move.getTo() + " not found in our knight attacks");
            }
        }
        
        // All 8 squares around the knight have black pawns, so all 8 moves are captures
        assertEquals(8, knightMoveCount, "Knight should have 8 legal capture moves");
    }

    /**
     * Test that attack bitboards don't allow pieces to attack their own square
     */
    @Test
    void testPiecesDoNotAttackTheirOwnSquare() {
        // Test all 64 squares for knight
        for (int square = 0; square < 64; square++) {
            long knightAttacks = BitboardGenerator.KNIGHT_ATTACKS[square];
            long selfBit = 1L << square;
            
            assertFalse((knightAttacks & selfBit) != 0,
                "Knight on square " + square + " should not attack its own square");
        }
        
        // Test all 64 squares for king
        for (int square = 0; square < 64; square++) {
            long kingAttacks = BitboardGenerator.KING_ATTACKS[square];
            long selfBit = 1L << square;
            
            assertFalse((kingAttacks & selfBit) != 0,
                "King on square " + square + " should not attack its own square");
        }
    }

    /**
     * Test knight moves match ChessLib expectations for multiple positions
     */
    @Test
    void testMultipleKnightPositions() {
        // Test several knight positions with both kings present
        String[] fens = {
            "4k3/8/8/8/8/2N5/8/4K3 w - - 0 1",  // c3: 8 moves
            "N3k3/8/8/8/8/8/8/4K3 w - - 0 1",     // a8: 2 moves
            "4k2N/8/8/8/8/8/8/4K3 w - - 0 1",     // h8: 2 moves
            "4k3/8/8/8/8/8/8/N3K3 w - - 0 1",     // a1: 2 moves
            "4k3/8/8/8/8/8/8/4K2N w - - 0 1",     // h1: 2 moves
            "4k3/8/8/8/4N3/8/8/4K3 w - - 0 1"     // e4: 8 moves
        };
        
        int[] expectedMoves = {8, 2, 2, 2, 2, 8};
        
        for (int i = 0; i < fens.length; i++) {
            Board board = new Board();
            board.loadFromFen(fens[i]);
            
            List<Move> legalMoves = board.legalMoves();
            
            // Filter knight moves only
            int knightMoveCount = 0;
            Square knightSquare = null;
            
            // Find the knight square from FEN
            for (Move move : legalMoves) {
                // Check if this is a knight move by testing our knight attack table
                int fromIndex = squareToBitboardIndex(move.getFrom());
                long knightAttacks = BitboardGenerator.KNIGHT_ATTACKS[fromIndex];
                int toIndex = squareToBitboardIndex(move.getTo());
                long destBit = 1L << toIndex;
                
                // If the move destination is in knight attacks from source, it's a knight move
                if ((knightAttacks & destBit) != 0 && knightSquare == null) {
                    knightSquare = move.getFrom();
                }
                
                if (knightSquare != null && move.getFrom() == knightSquare) {
                    knightMoveCount++;
                    assertTrue((knightAttacks & destBit) != 0,
                        "Legal move " + move + " not found in our knight attacks");
                }
            }
            
            assertEquals(expectedMoves[i], knightMoveCount,
                "FEN " + i + " should have " + expectedMoves[i] + " knight moves");
        }
    }
}
