package ch.adjudicator.agent.positionevaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ResultingScoreAndBounds {
    int score;
    int alpha;
    int beta;
    int ply;
}
