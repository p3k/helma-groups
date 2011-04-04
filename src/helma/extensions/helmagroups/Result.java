package helma.extensions.helmagroups;

import java.io.Serializable;
import org.jgroups.Address;

public class Result implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * a simple class wrapping a return value or error from a function execution
     * that went to the whole group.
     */

    public String app;

    public Address host;
    
    public String member;

    public Object result;

    public String error;

    /**
     * create a new result in case of success
     */
    public static Result newResult(Object result, String app) {
        Result r = new Result(app);
        r.result = result;
        return r;
    }

    /**
     * create a new error object
     */
    public static Result newError(String error, String app) {
        Result r = new Result(app);
        r.error = error;
        return r;
    }

    private Result() {
    }

    private Result(String app) {
        this.app = app;
        this.result = null;
        this.error = null;
        this.host = null;
    }

    public String toString() {
        if (result != null) {
            return "[" + app + "@" + host.toString() + ", result=" + result.toString() + "]";
        } else {
            return "[" + app + "@" + host.toString() + ", error=" + error + "]";
        }
    }

}