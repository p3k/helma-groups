package helma.extensions.helmagroups;

import java.io.Serializable;

import org.jgroups.Message;
import org.jgroups.Address;

public class Request implements Serializable, GroupConstants {

    private static final long serialVersionUID = 1L;

    /**
     * a class holding a request from one group instance to another/all others.
     * it exposes path, key and value as public properties and is assigned a
     * type (REQUEST_*).
     */

    public int type = 0;

    public String path = null;

    public String key = null;

    public Serializable value = null;

    public Request(int type) {
        this.type = type;
    }

    public Request(int type, String path, String key, Serializable value) {
        this.type = type;
        this.path = path;
        this.key = key;
        this.value = value;
    }

    /**
     * creates a JGroups Message object wrapping this request. addressed to
     * <b>all members </b>
     */
    public Message getMessage() {
        return getMessage(null);
    }

    /**
     * creates a JGroups Message object wrapping this request. addressed to <b>a
     * single given member </b>
     */
    public Message getMessage(Address dest) {
        return new Message(dest, null, this);
    }

}