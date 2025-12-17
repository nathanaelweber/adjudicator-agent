package ch.adjudicator.agent.positionevaluation;

import com.github.bhlangonijr.chesslib.move.Move;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ScoreAndMove {
    ResultingScoreAndBounds score;
    Move move;
}
