package ch.adjudicator.client;

/**
 * Request for the agent to make a move.
 */
public class MoveRequest {
    private final String opponentMove;
    private final int yourTimeMs;
    private final int opponentTimeMs;
    
    /**
     * Create a move request.
     * 
     * @param opponentMove The opponent's last move (empty string for first move)
     * @param yourTimeMs Your remaining time in milliseconds
     * @param opponentTimeMs Opponent's remaining time in milliseconds
     */
    public MoveRequest(String opponentMove, int yourTimeMs, int opponentTimeMs) {
        this.opponentMove = opponentMove;
        this.yourTimeMs = yourTimeMs;
        this.opponentTimeMs = opponentTimeMs;
    }
    
    /**
     * Get the opponent's last move.
     * 
     * @return Move in Long Algebraic Notation, or empty string for first move
     */
    public String getOpponentMove() {
        return opponentMove;
    }
    
    /**
     * Get your remaining time.
     * 
     * @return Remaining time in milliseconds
     */
    public int getYourTimeMs() {
        return yourTimeMs;
    }
    
    /**
     * Get opponent's remaining time.
     * 
     * @return Remaining time in milliseconds
     */
    public int getOpponentTimeMs() {
        return opponentTimeMs;
    }
    
    @Override
    public String toString() {
        return String.format("MoveRequest{opponentMove='%s', yourTime=%dms, opponentTime=%dms}",
                opponentMove, yourTimeMs, opponentTimeMs);
    }
}
