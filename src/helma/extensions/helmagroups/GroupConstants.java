package helma.extensions.helmagroups;

import org.jgroups.blocks.GroupRequest;

public interface GroupConstants {

    public static final String VERSION = "0.11";

    /**
     * state of the group.
     */
    public static final int STATE_DISCONNECTED = 0;

    public static final int STATE_CONNECTED = 1;

    /**
     * Rights management to exclude applications from modifying or seeing the
     * tree.
     */
    public static final int MAY_READ = 1;

    public static final int MAY_WRITE = 2;

    public static final int MAY_CONNECT = 4;

    public static final int MAY_DISCONNECT = 8;

    /**
     * The default millis the waitFor()-method is going to wait for an object to
     * appear in the tree.
     */
    public static final long DEFAULT_WAIT_TIMEOUT = 2000;

    /**
     * timeout for put/remove/save/wrap operations
     */
    public static final long DEFAULT_SEND_TIMEOUT = 5000;
    
    /**
     * timeout for execute operations
     */
    public static final long DEFAULT_EXECUTE_TIMEOUT = 30000;
    
    /**
     * the string to separate pathelements in the hashtable.
     */
    public static final String SEPARATOR = "/";

    /**
     * The name of the group if no name is supplied.
     */
    public static final String DEFAULT_GROUPNAME = "helma";

    public static final String DEFAULT_LOCALNAME = "helma";
    
    public static final int DEFAULT_SEND_MODE = GroupRequest.GET_ALL;

    /**
     * The constants used in RemoteAction
     */
    public static final int REQUEST_PUT = 1;

    public static final int REQUEST_REMOVE = 2;

    public static final int REQUEST_SAVE = 3;

    public static final int REQUEST_WRAP = 4;

    public static final int REQUEST_EXECUTE = 5;

    // 6 was REQUEST_STATE

    // public static final int REQUEST_RESET = 7;

    // public static final int REQUEST_DESTROY = 8;

    public static final int REQUEST_INTERNAL = 9;
    
    /**
     * GroupObject: when in state LOCAL changes will only be made locally.
     */
    public static final int LOCAL = 0;

    /**
     * * GroupObject: when in state REPLICATED any change (add/modify/delete) of
     * a property will immediately be replicated to all members of the group.
     */
    public static final int REPLICATED = 1;

}

