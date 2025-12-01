package ch.adjudicator.client;

/**
 * Information about a started game.
 */
public class GameInfo {
    private final String gameId;
    private final Color color;
    private final int initialTimeMs;
    private final int incrementMs;
    
    public GameInfo(String gameId, Color color, int initialTimeMs, int incrementMs) {
        this.gameId = gameId;
        this.color = color;
        this.initialTimeMs = initialTimeMs;
        this.incrementMs = incrementMs;
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public Color getColor() {
        return color;
    }
    
    public int getInitialTimeMs() {
        return initialTimeMs;
    }
    
    public int getIncrementMs() {
        return incrementMs;
    }
    
    @Override
    public String toString() {
        return String.format("GameInfo{gameId='%s', color=%s, initialTime=%dms, increment=%dms}",
                gameId, color, initialTimeMs, incrementMs);
    }
}
