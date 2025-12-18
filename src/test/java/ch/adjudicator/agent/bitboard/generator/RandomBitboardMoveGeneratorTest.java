package ch.adjudicator.agent.bitboard.generator;

import ch.adjudicator.agent.bitboard.adapter.ChessLibAdapter;
import ch.adjudicator.agent.bitboard.model.BoardState;
import ch.adjudicator.agent.bitboard.model.FastMove;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomBitboardMoveGeneratorTest {
    @Test
    void testDo15_randomMoves_shouldAlwaysRespondWithTheSamePossibleMovesAsInChesslib() {
        String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        BoardState state = ChessLibAdapter.fenToBoardState(startFen);

        FastMove lastMove = null;

        for (int i = 0; i < 15; i++) {
            List<FastMove> fastMoves = BitboardMoveGenerator.generateMoves(state, lastMove);
            Board board = new Board();
            board.loadFromFen(ChessLibAdapter.boardStateToFen(state));
            for(Move legalMovesExpected : board.legalMoves()) {
                board.doMove(legalMovesExpected);
                boolean foundLegalMove = false;
                for(FastMove fastMove : fastMoves) {
                    if(ChessLibAdapter.convertFastMoveToChessLibMove(fastMove).equals(legalMovesExpected)) {
                        foundLegalMove = true;
                        break;
                    }
                }
                assertTrue(foundLegalMove, "Move " + legalMovesExpected + " not found in generated moves. Board: " + board.getFen() + " actualFastMovesFound: " + fastMoves);
                board.undoMove();
            }

            FastMove fastMove = fastMoves.get(new Random().nextInt(fastMoves.size()));
            state = state.applyMove(fastMove);
            lastMove = fastMove;
            board.doMove(ChessLibAdapter.convertFastMoveToChessLibMove(fastMove));
        }
    }
}
