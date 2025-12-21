package ch.adjudicator.agent.bitboard.generator;

import ch.adjudicator.agent.bitboard.adapter.ChessLibAdapter;
import ch.adjudicator.agent.bitboard.model.BoardState;
import ch.adjudicator.agent.bitboard.model.FastMove;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomBitboardMoveGeneratorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomBitboardMoveGeneratorTest.class);

    @Test
    void testDo15_randomMoves_shouldAlwaysRespondWithTheSamePossibleMovesAsInChesslib() {
        for(int testBatch = 0; testBatch < 40; testBatch++) {
            String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
            BoardState state = ChessLibAdapter.fenToBoardState(startFen);

            FastMove lastMove = null;

            for (int i = 0; i < 40; i++) {
                String fen = ChessLibAdapter.boardStateToFen(state);
                LOGGER.info("Current fen={}", fen);
                List<FastMove> fastMoves = BitboardMoveGenerator.generateMoves(state, lastMove);
                Board board = new Board();
                board.loadFromFen(ChessLibAdapter.boardStateToFen(state));
                for(Move legalMoveExpected : board.legalMoves()) {
                    board.doMove(legalMoveExpected);
                    boolean foundLegalMove = false;
                    for(FastMove fastMove : fastMoves) {
                        if(ChessLibAdapter.convertFastMoveToChessLibMove(fastMove).equals(legalMoveExpected)) {
                            foundLegalMove = true;
                            break;
                        }
                    }
                    assertTrue(foundLegalMove, "Move " + legalMoveExpected + " not found in generated moves. Board: " + board.getFen() + " actualFastMovesFound: " + fastMoves);
                    board.undoMove();
                }

                assertEquals(board.legalMoves().size(), fastMoves.size());

                FastMove fastMove = fastMoves.get(new Random().nextInt(fastMoves.size()));
                state = state.applyMove(fastMove);
                lastMove = fastMove;
                board.doMove(ChessLibAdapter.convertFastMoveToChessLibMove(fastMove));
            }
        }
    }
}
