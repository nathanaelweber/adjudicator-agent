package ch.adjudicator.client;

import lombok.Value;

/**
 * Request for the agent to make a move.
 */
@Value
public class MoveRequest {
    /**
     * The opponent's last move in Long Algebraic Notation.
     * Empty string if it's the first move of the game (and you are White).
     */
    String opponentMove;

    /**
     * Your remaining time in milliseconds.
     */
    int yourTimeMs;

    /**
     * Opponent's remaining time in milliseconds.
     */
    int opponentTimeMs;
}
