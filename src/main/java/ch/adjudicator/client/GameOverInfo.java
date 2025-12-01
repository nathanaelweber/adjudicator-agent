package ch.adjudicator.client;

/**
 * Information about a completed game.
 */
public class GameOverInfo {
    private final GameResult result;
    private final String reason;
    private final String finalPgn;
    
    /**
     * Create game over information.
     * 
     * @param result Game result from the agent's perspective
     * @param reason Reason for game end (e.g., "CHECKMATE", "TIMEOUT", "ILLEGAL_MOVE")
     * @param finalPgn Complete game in PGN format
     */
    public GameOverInfo(GameResult result, String reason, String finalPgn) {
        this.result = result;
        this.reason = reason;
        this.finalPgn = finalPgn;
    }
    
    /**
     * Get the game result.
     * 
     * @return Result from the agent's perspective (WIN, LOSS, or DRAW)
     */
    public GameResult getResult() {
        return result;
    }
    
    /**
     * Get the reason for game end.
     * 
     * @return Reason string (e.g., "CHECKMATE", "TIMEOUT", "STALEMATE")
     */
    public String getReason() {
        return reason;
    }
    
    /**
     * Get the final game in PGN format.
     * 
     * @return Complete game notation
     */
    public String getFinalPgn() {
        return finalPgn;
    }
    
    @Override
    public String toString() {
        return String.format("GameOverInfo{result=%s, reason='%s'}",
                result, reason);
    }
}
