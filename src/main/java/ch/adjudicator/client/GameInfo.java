package ch.adjudicator.client;

import lombok.Value;

/**
 * Information about a started game.
 */
@Value
public class GameInfo {
    /**
     * The unique ID of the game.
     */
    String gameId;

    /**
     * The color assigned to the agent.
     */
    Color color;

    /**
     * The initial time on the clock in milliseconds.
     */
    int initialTimeMs;

    /**
     * The time increment per move in milliseconds.
     */
    int incrementMs;
}
