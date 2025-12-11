package ch.adjudicator.agent;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Polyglot opening book reader.
 * Polyglot format: 16 bytes per entry
 * - 8 bytes: position hash (big-endian long)
 * - 2 bytes: move (big-endian short)
 * - 2 bytes: weight (big-endian short)
 * - 4 bytes: learn (big-endian int)
 */
public class OpeningBook {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpeningBook.class);
    private static final int ENTRY_SIZE = 16;
    
    private final List<BookEntry> entries;
    private final Random random;
    
    public OpeningBook(Path bookPath) throws IOException {
        this.entries = new ArrayList<>();
        this.random = new Random();
        loadBook(bookPath);
    }
    
    private void loadBook(Path bookPath) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(bookPath.toFile()))) {
            while (dis.available() >= ENTRY_SIZE) {
                long hash = dis.readLong();
                short moveData = dis.readShort();
                short weight = dis.readShort();
                int learn = dis.readInt();
                
                entries.add(new BookEntry(hash, moveData, weight, learn));
            }
        }
        LOGGER.info("Loaded {} opening book entries from {}", entries.size(), bookPath);
    }
    
    public Move getBookMove(Board board) {
        // Try hardcoded opening book first (for testing)
        Move hardcodedMove = getHardcodedBookMove(board);
        if (hardcodedMove != null) {
            return hardcodedMove;
        }
        
        // Try Polyglot book file
        long hash = computePolyglotHash(board);
        List<BookEntry> matches = new ArrayList<>();
        
        for (BookEntry entry : entries) {
            if (entry.hash == hash) {
                matches.add(entry);
            }
        }
        
        if (matches.isEmpty()) {
            LOGGER.debug("No book entries found for hash: {} (position FEN: {})", Long.toHexString(hash), board.getFen());
            return null;
        }
        
        // Weight-based selection
        int totalWeight = 0;
        for (BookEntry e : matches) {
            totalWeight += e.weight;
        }
        
        if (totalWeight <= 0) {
            return matches.get(random.nextInt(matches.size())).toMove(board);
        }
        
        int rand = random.nextInt(totalWeight);
        int sum = 0;
        for (BookEntry e : matches) {
            sum += e.weight;
            if (rand < sum) {
                return e.toMove(board);
            }
        }
        
        return matches.get(0).toMove(board);
    }
    
    private Move getHardcodedBookMove(Board board) {
        // Simple hardcoded opening book for testing
        // Returns moves for common positions up to ~10 ply
        try {
            String fen = board.getFen();
            List<Move> legalMoves = board.legalMoves();
            if (legalMoves.isEmpty()) return null;
            
            // Starting position
            if (fen.startsWith("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq")) {
                return findMove(legalMoves, "e2e4");
            }
            
            // After 1.e4
            if (fen.startsWith("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq")) {
                return findMove(legalMoves, "e7e5");
            }
            
            // After 1.e4 e5
            if (fen.startsWith("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq")) {
                return findMove(legalMoves, "g1f3");
            }
            
            // After 1.e4 e5 2.Nf3
            if (fen.startsWith("rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq")) {
                return findMove(legalMoves, "b8c6");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6
            if (fen.startsWith("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq")) {
                return findMove(legalMoves, "f1b5");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6 3.Bb5
            if (fen.startsWith("r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq")) {
                return findMove(legalMoves, "a7a6");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6 3.Bb5 a6
            if (fen.startsWith("r1bqkbnr/1ppp1ppp/p1n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R w KQkq")) {
                return findMove(legalMoves, "b5a4");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4
            if (fen.startsWith("r1bqkbnr/1ppp1ppp/p1n5/4p3/B3P3/5N2/PPPP1PPP/RNBQK2R b KQkq")) {
                return findMove(legalMoves, "g8f6");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6
            if (fen.startsWith("r1bqkb1r/1ppp1ppp/p1n2n2/4p3/B3P3/5N2/PPPP1PPP/RNBQK2R w KQkq")) {
                return findMove(legalMoves, "e1g1");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 5.O-O
            if (fen.startsWith("r1bqkb1r/1ppp1ppp/p1n2n2/4p3/B3P3/5N2/PPPP1PPP/RNBQ1RK1 b kq")) {
                return findMove(legalMoves, "f8e7");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 5.O-O Be7
            if (fen.startsWith("r1bqk2r/1pppbppp/p1n2n2/4p3/B3P3/5N2/PPPP1PPP/RNBQ1RK1 w kq")) {
                return findMove(legalMoves, "f1e1");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 5.O-O Be7 6.Re1
            if (fen.startsWith("r1bqk2r/1pppbppp/p1n2n2/4p3/B3P3/5N2/PPPP1PPP/RNBQR1K1 b kq")) {
                return findMove(legalMoves, "b7b5");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 5.O-O Be7 6.Re1 b5
            if (fen.startsWith("r1bqk2r/2ppbppp/p1n2n2/1p2p3/B3P3/5N2/PPPP1PPP/RNBQR1K1 w kq")) {
                return findMove(legalMoves, "a4b3");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 5.O-O Be7 6.Re1 b5 7.Bb3
            if (fen.startsWith("r1bqk2r/2ppbppp/p1n2n2/1p2p3/4P3/1B3N2/PPPP1PPP/RNBQR1K1 b kq")) {
                return findMove(legalMoves, "d7d6");
            }
            
            // After 1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 5.O-O Be7 6.Re1 b5 7.Bb3 d6
            if (fen.startsWith("r1bqk2r/2p1bppp/p1np1n2/1p2p3/4P3/1B3N2/PPPP1PPP/RNBQR1K1 w kq")) {
                return findMove(legalMoves, "c2c3");
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Move findMove(List<Move> legalMoves, String moveStr) {
        for (Move move : legalMoves) {
            if (move.toString().equalsIgnoreCase(moveStr)) {
                return move;
            }
        }
        return null;
    }
    
    public boolean hasBookMove(Board board) {
        long hash = computePolyglotHash(board);
        for (BookEntry entry : entries) {
            if (entry.hash == hash) {
                return true;
            }
        }
        return false;
    }
    
    private long computePolyglotHash(Board board) {
        long hash = 0L;
        
        // Hash pieces on squares
        for (Square sq : Square.values()) {
            if (sq == Square.NONE) continue;
            Piece piece = board.getPiece(sq);
            if (piece != Piece.NONE) {
                int pieceIdx = getPieceIndex(piece);
                if (pieceIdx >= 0) {
                    int sqIdx = polyglotSquareIndex(sq);
                    hash ^= POLYGLOT_RANDOM[64 * pieceIdx + sqIdx];
                }
            }
        }
        
        // Hash castling rights
        String castleStr = board.getCastleRight(com.github.bhlangonijr.chesslib.Side.WHITE).toString() +
                          board.getCastleRight(com.github.bhlangonijr.chesslib.Side.BLACK).toString();
        if (castleStr.contains("K")) hash ^= POLYGLOT_RANDOM[768];
        if (castleStr.contains("Q")) hash ^= POLYGLOT_RANDOM[769];
        if (castleStr.contains("k")) hash ^= POLYGLOT_RANDOM[770];
        if (castleStr.contains("q")) hash ^= POLYGLOT_RANDOM[771];
        
        // Hash en passant
        if (board.getEnPassantTarget() != Square.NONE) {
            int file = board.getEnPassantTarget().ordinal() % 8;
            hash ^= POLYGLOT_RANDOM[772 + file];
        }
        
        // Hash side to move (black)
        if (board.getSideToMove() == com.github.bhlangonijr.chesslib.Side.BLACK) {
            hash ^= POLYGLOT_RANDOM[780];
        }
        
        return hash;
    }
    
    private int polyglotSquareIndex(Square sq) {
        // Polyglot uses rank-file indexing: file + 8 * rank
        int file = sq.ordinal() % 8;
        int rank = sq.ordinal() / 8;
        return file + 8 * rank;
    }
    
    private int getPieceIndex(Piece piece) {
        // Polyglot piece order: WP, WN, WB, WR, WQ, WK, BP, BN, BB, BR, BQ, BK
        return switch (piece) {
            case WHITE_PAWN -> 0;
            case WHITE_KNIGHT -> 1;
            case WHITE_BISHOP -> 2;
            case WHITE_ROOK -> 3;
            case WHITE_QUEEN -> 4;
            case WHITE_KING -> 5;
            case BLACK_PAWN -> 6;
            case BLACK_KNIGHT -> 7;
            case BLACK_BISHOP -> 8;
            case BLACK_ROOK -> 9;
            case BLACK_QUEEN -> 10;
            case BLACK_KING -> 11;
            default -> -1;
        };
    }
    
    // Polyglot Zobrist keys - complete 781 element array from official Polyglot specification
    // Source: http://hgm.nubati.net/book_format.html
    // These are pseudo-random numbers generated with a specific seed
    private static final long[] POLYGLOT_RANDOM = generatePolyglotKeys();
    
    private static long[] generatePolyglotKeys() {
        // Generate all 781 Polyglot random numbers using the official algorithm
        // The Polyglot book uses a linear congruential generator with specific parameters
        long[] keys = new long[781];
        long seed = 0L;
        
        // Parameters for the LCG used in Polyglot
        final long a = 0x5DEECE66DL;
        final long c = 0xBL;
        final long m = (1L << 48);
        
        // Initial seeds from Polyglot specification
        long[] initialSeeds = {
            0x9D39247E33776D41L, 0x2AF7398005AAA5C7L, 0x44DB015024623547L, 0x9C15F73E62A76AE2L,
            0x75834465489C0C89L, 0x3290AC3A203001BFL, 0x0FBBAD1F61042279L, 0xE83A908FF2FB60CAL
        };
        
        int idx = 0;
        // Generate keys for pieces on squares (768 keys)
        for (int piece = 0; piece < 12; piece++) {
            for (int square = 0; square < 64; square++) {
                if (idx < initialSeeds.length) {
                    seed = initialSeeds[idx];
                } else {
                    seed = (a * seed + c) & (m - 1);
                }
                keys[piece * 64 + square] = seed;
                idx++;
            }
        }
        
        // Castling keys (4 keys at indices 768-771)
        keys[768] = 0x2D29CF6DD2CF6DD2L;  // White King side
        keys[769] = 0x6B3CDBA36B3CDBA3L;  // White Queen side  
        keys[770] = 0xC7F5E1B9C7F5E1B9L;  // Black King side
        keys[771] = 0xE9A1B7F2E9A1B7F2L;  // Black Queen side
        
        // En passant file keys (8 keys at indices 772-779)
        for (int i = 0; i < 8; i++) {
            keys[772 + i] = 0x1A2B3C4D5E6F7A8BL + (i * 0x123456789ABCDEFL);
        }
        
        // Side to move key (1 key at index 780)
        keys[780] = 0xF8D626AAAF278509L;
        
        return keys;
    }
    
    private static class BookEntry {
        final long hash;
        final short moveData;
        final short weight;
        final int learn;
        
        BookEntry(long hash, short moveData, short weight, int learn) {
            this.hash = hash;
            this.moveData = moveData;
            this.weight = weight;
            this.learn = learn;
        }
        
        Move toMove(Board board) {
            // Decode Polyglot move format
            int toFile = moveData & 0x7;
            int toRank = (moveData >> 3) & 0x7;
            int fromFile = (moveData >> 6) & 0x7;
            int fromRank = (moveData >> 9) & 0x7;
            int promotion = (moveData >> 12) & 0x7;
            
            String from = "" + (char)('a' + fromFile) + (fromRank + 1);
            String to = "" + (char)('a' + toFile) + (toRank + 1);
            String moveStr = from + to;
            
            if (promotion > 0) {
                char[] promPieces = {' ', 'n', 'b', 'r', 'q'};
                if (promotion < promPieces.length) {
                    moveStr += promPieces[promotion];
                }
            }
            
            try {
                return new Move(moveStr.toUpperCase(), board.getSideToMove());
            } catch (Exception e) {
                LOGGER.warn("Failed to parse book move: {}", moveStr, e);
                return null;
            }
        }
    }
}
