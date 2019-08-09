package org.springframework.transaction;

@SuppressWarnings("serial")
public class HeuristicCompletionException extends TransactionException {

    public static final int STATE_UNKNOWN = 0;

    public static final int STATE_COMMITTED = 1;

    public static final int STATE_ROLLED_BACK = 2;

    public static final int STATE_MIXED = 3;

    public static String getStateString(int state) {
        switch (state) {
            case STATE_COMMITTED:
                return "committed";
            case STATE_ROLLED_BACK:
                return "rolled back";
            case STATE_MIXED:
                return "mixed";
            default:
                return "unknown";
        }
    }

    private final int outcomeState;

    public HeuristicCompletionException(int outcomeState, Throwable cause) {
        super("Heuristic completion: outcome state is " + getStateString(outcomeState), cause);
        this.outcomeState = outcomeState;
    }

    public int getOutcomeState() {
        return this.outcomeState;
    }

}
