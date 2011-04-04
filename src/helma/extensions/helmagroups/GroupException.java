package helma.extensions.helmagroups;

/**
 * This exception is thrown when the Group can't be initialized while connecting
 * or retrieving the state.
 */

public class GroupException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GroupException(String msg) {
        super(msg);
    }

    public GroupException(Throwable t) {
        super(t.toString());
    }

    public GroupException(String msg, Throwable t) {
        super(msg + " " + t.toString());
    }

}

