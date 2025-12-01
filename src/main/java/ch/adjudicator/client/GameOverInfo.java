package ch.adjudicator.client;

import lombok.Value;

/**
 * Information about a completed game.
 */
@Value
public class GameOverInfo {
    /**
     * Result from the agent's perspective (WIN, LOSS, or DRAW).
     */
    GameResult result;

    /**
     * Reason for game end (e.g., "CHECKMATE", "TIMEOUT", "ILLEGAL_MOVE").
     */
    String reason;

    /**
     * Complete game in PGN format.
     */
    String finalPgn;
}
