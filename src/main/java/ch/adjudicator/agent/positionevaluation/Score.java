package ch.adjudicator.agent.positionevaluation;

/**
 * Represents a chess position score, which can be either:
 * - A numeric score in centipawns
 * - A forced mate in N moves (e.g., +M1, +M3, -M2)
 */
public class Score implements Comparable<Score> {
    private final boolean isMate;
    private final int value; // For numeric scores, this is centipawns; for mate, this is moves to mate (positive = winning, negative = losing)

    // Special score values
    public static final int MATE_SCORE = 30000;
    public static final int MAX_MATE_DISTANCE = 1000; // Maximum ply distance for mate detection

    private Score(boolean isMate, int value) {
        this.isMate = isMate;
        this.value = value;
    }

    /**
     * Create a numeric score in centipawns
     */
    public static Score valueOf(int centipawns) {
        return new Score(false, centipawns);
    }

    /**
     * Create a mate score. Positive means we win, negative means we lose.
     * @param movesToMate Number of moves to mate (positive = we win, negative = we lose)
     */
    public static Score mateIn(int movesToMate) {
        return new Score(true, movesToMate);
    }

    /**
     * Create a mate score from ply distance
     * @param plyDistance Distance in ply from current position (always positive)
     * @param weAreWinning true if we are delivering mate, false if we are getting mated
     */
    public static Score mateInPly(int plyDistance, boolean weAreWinning) {
        int movesToMate = (plyDistance + 1) / 2; // Convert ply to moves
        return new Score(true, weAreWinning ? movesToMate : -movesToMate);
    }

    public boolean isMate() {
        return isMate;
    }

    public int getValue() {
        return value;
    }

    /**
     * Returns true if this is a winning mate score
     */
    public boolean isWinningMate() {
        return isMate && value > 0;
    }

    /**
     * Returns true if this is a losing mate score
     */
    public boolean isLosingMate() {
        return isMate && value < 0;
    }

    /**
     * Get the absolute number of moves to mate
     */
    public int getMovesToMate() {
        return isMate ? Math.abs(value) : 0;
    }

    /**
     * Negate the score (for switching perspective)
     */
    public Score negate() {
        return new Score(isMate, -value);
    }

    /**
     * Increment mate distance by one ply (used when propagating mate scores up the tree)
     */
    public Score incrementMateDistance() {
        if (!isMate) {
            return this;
        }
        // When propagating mate score up the tree, we add 1 ply
        // If it's a winning mate, we increase the distance (further from mate)
        // If it's a losing mate, we also increase the distance (further from getting mated)
        int newMovesToMate = value > 0 ? value + 1 : value - 1;
        return new Score(true, newMovesToMate);
    }

    @Override
    public int compareTo(Score other) {
        // Mate scores always beat non-mate scores
        if (this.isMate && !other.isMate) {
            return this.value > 0 ? 1 : -1;
        }
        if (!this.isMate && other.isMate) {
            return other.value > 0 ? -1 : 1;
        }

        // Both are mate scores
        if (this.isMate && other.isMate) {
            // Positive mate (we win): prefer shorter mate distance (smaller positive number)
            // Negative mate (we lose): prefer longer mate distance (larger negative number, closer to 0)
            if (this.value > 0 && other.value > 0) {
                return Integer.compare(other.value, this.value); // Shorter mate is better
            }
            if (this.value < 0 && other.value < 0) {
                return Integer.compare(other.value, this.value); // Longer delay is better
            }
            // One is winning, one is losing
            return Integer.compare(this.value, other.value);
        }

        // Both are numeric scores
        return Integer.compare(this.value, other.value);
    }

    @Override
    public String toString() {
        if (isMate) {
            if (value > 0) {
                return "+M" + value;
            } else {
                return "-M" + Math.abs(value);
            }
        }
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Score)) return false;
        Score other = (Score) obj;
        return this.isMate == other.isMate && this.value == other.value;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(isMate) * 31 + Integer.hashCode(value);
    }

    /**
     * Convert to internal integer representation for storage (e.g., in transposition table)
     */
    public int toInt() {
        if (isMate) {
            // Encode mate scores as large values
            // Positive mate: MATE_SCORE - movesToMate
            // Negative mate: -MATE_SCORE + movesToMate
            return value > 0 ? MATE_SCORE - value : -MATE_SCORE - value;
        }
        return value;
    }

    /**
     * Convert from internal integer representation
     */
    public static Score fromInt(int intValue) {
        if (intValue >= MATE_SCORE - MAX_MATE_DISTANCE) {
            // Winning mate
            int movesToMate = MATE_SCORE - intValue;
            return Score.mateIn(movesToMate);
        } else if (intValue <= -MATE_SCORE + MAX_MATE_DISTANCE) {
            // Losing mate
            int movesToMate = -MATE_SCORE - intValue;
            return Score.mateIn(movesToMate);
        }
        return Score.valueOf(intValue);
    }
}
