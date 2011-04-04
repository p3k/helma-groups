package helma.extensions.helmagroups;

import java.util.*;

/**
 * A class to store information on which application mounts which group and has
 * which rights on it. <br/><br/>All modification methods of the Hashtable are
 * overridden and trigger the same modification on a private Map holding the
 * access rights.
 */

public class ApplicationLink extends Hashtable implements GroupConstants {

    private static final long serialVersionUID = 1L;

    private String appName = null;

    // the table in which the right objects are stored
    private Map rights = null;

    /**
     * construct a new hashtable-storage, one for each app, defined through the
     * name of the app (and not the app itself).
     */
    public ApplicationLink(String appName) {
        this.appName = appName;
        this.rights = new Hashtable();
    }

    // a tiny class that holds the rights
    class Rights {
        boolean writable = false;

        boolean readable = true;
        // future extension
    }

    /**
     * sets write-rights for a given group
     */
    public void setWritable(Group g, boolean val) {
        Rights r = (Rights) rights.get(g);
        if (r != null)
            r.writable = val;
    }

    /**
     * get the write-rights for given group
     */
    public boolean isWritable(Group g) {
        Rights r = (Rights) rights.get(g);
        if (r == null) {
            // can only happen if the group hasn't been registered properly
            GroupExtension.getLogger().error("error: ApplicationLink.isWritable() no rights for " + g);
            return false;
        } else {
            return r.writable;
        }
    }

    public void clear() {
        rights.clear();
        super.clear();
    }

    public Object put(Object key, Object value) {
        rights.put(value, new Rights());
        return super.put(key, value);
    }

    public void putAll(Map t) {
        for (Iterator i = t.values().iterator(); i.hasNext();) {
            rights.put(i.next(), new Rights());
        }
        super.putAll(t);
    }

    public Object remove(Object key) {
        Object obj = super.remove(key);
        rights.remove(obj);
        return obj;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String toString() {
        return "[" + appName + " with groups " + super.toString() + "]";
    }

}

