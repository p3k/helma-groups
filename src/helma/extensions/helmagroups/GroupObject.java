package helma.extensions.helmagroups;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * An Object that can be stored in a Group and will update itself when its
 * properties are changed. And it holds two Hashtables with primitive properties
 * and references to other GroupObjects that are children of this object. <br>
 * 
 * The put/remove-methods of this class decide: <br>
 * 
 * <li>wheter changes need to be passed on to the other members of the group
 * (if state is REPLICATED) or if they can remain locally because the
 * GroupObject has LOCAL state.
 * 
 * <li>wheter references in children-vector or elements in the properties table
 * need to be deleted after an update. <br>
 * <br>
 * 
 * Note that all methods with a leading _underscore should only be used by the
 * callback functions from {@link Group Group}to avoid loops or missing
 * replication.
 */

public class GroupObject implements Serializable, Cloneable, GroupConstants {

    // used by interface Serializable
    static final long serialVersionUID = 8256692038042602998L;

    private String path = null;

    // Hashtable containing the key/value pairs of primitive properties
    // the properties table always exists.
    private Hashtable properties = null;

    // Vector containing the keys of the children
    // full-path-to-child = this.path + SEPARATOR + key
    private Vector children = null;

    private transient int state;

    private transient Group group = null;

    private boolean isRoot = false;

    /**
     * Constructs an empty GroupObject
     */
    public GroupObject() {
        this(null, null);
    }

    /**
     * Constructs a GroupObject at a given position
     * 
     * @param group the Group this object belongs to.
     * @param string the full path to the object
     */
    public GroupObject(Group group, String path) {
        this.group = group;
        this.path = path;
        this.state = LOCAL;
        properties = new Hashtable();
        children   = new Vector();
    }

    /**
     * Returns the Group this GroupObject belongs to.
     * 
     * @return the Group. May be null if the object is still LOCAL or for root
     *         objects if the Group is not connected.
     */
    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setAsRoot(boolean tf) {
        isRoot = tf;
    }

    // ****************************************************
    // modification methods ...
    // leading _underscore -> callback-method used by Group
    // ****************************************************

    /**
     * Saves the GroupObject to the Group. <b>This may only be used to
     * initialize the root object. </b> Otherwise child objects might be
     * dereferenced in the children-table but remain in Group's objects-table.
     */
    public void save() {
        save(0);
    }

    /**
     * Saves the GroupObject to the Group. <b>This may only be used to
     * initialize the root object. </b> (Otherwise child objects might be
     * dereferenced in the children-table but remain in Group's objects-table.
     */
    public void save(int sendMode) {
        if (this.isRoot != true) {
            throw new GroupException("only a root GroupObject may be sent using save()");
        }
        group.save(path, this, sendMode);
        this.state = REPLICATED;
    }

    /**
     * Stores all primitive properties of newObject in this object, but leaves
     * the children intact.
     */
    public void wrap(GroupObject object) {
        group.wrap(path, object, 0);
    }

    public void wrap(GroupObject object, int sendMode) {
        group.wrap(path, object, sendMode);
    }

    public void put(Object key, Object value) throws GroupException {
        put(key, value, 0);
    }

    /**
     * Adds the key-value pair to this GroupObject after various checks. If
     * value is a GroupObject it will be assigned a path and replicated to the
     * Group, if value is a primitive property the change will be replicated
     * according to the state of this object.
     */
    public void put(Object key, Object value, int sendMode) throws GroupException {
        // do the checks:
        if (value instanceof GroupObject) {
            if (state == LOCAL)
                throw new GroupException("GroupObject can only be added when the parent object is replicated.");
            if (((GroupObject) value).getState() == REPLICATED)
                throw new GroupException("GroupObjects may be added to the tree only once.");
            // if the same value already exists at the correct position, don't
            // do anything
            if (((GroupObject) value).equals(getChild((String) key)))
                return;
        } else {
            // if the hashtable already contains the same value we don't do
            // anything
            if (value.equals(properties.get(key)))
                return;
        }
        // if we're going to add a GroupObject, we assign its path:
        if (value instanceof GroupObject) {
            GroupObject object = (GroupObject) value;
            object.setPath(path + GroupExtension.SEPARATOR + key);
            // and the transient fields
            object.setState(REPLICATED);
            object.setGroup(group);
            // if we come so far, we're always REPLICATED, so we do the
            // transmission ....
            group.put(path, (String) key, object, sendMode);
            return;
        } else {
            if (state == REPLICATED) {
                // replicate the new property to the group:
                try {
                    group.put(path, (String) key, (Serializable) value, sendMode);
                } catch (ClassCastException e) {
                    throw new GroupException("Object not serializable: " + value.getClass().toString());
                }
            } else {
                // just do the change locally ...
                properties.put(key, value);
            }
        }
    }

    /**
     * Put the value into the local Hashtable. This should only be used by
     * callback methods of {@link Group}.
     */
    protected Object _put(Object key, Object value) {
        return properties.put(key, value);
    }

    public Object remove(Object key) {
        return remove(key, 0);
    }
    
    /**
     * If key points to a primitive property it is removed and this change
     * replicated to the group. If key points to a child object the remove
     * operation will delete the whole branch below that object.
     */
    public Object remove(Object key, int sendMode) {
        Object reval = properties.get(key);
        if (reval == null && state == REPLICATED && group.get(path, (String) key) == null) {
            // no value in the property-table, no child object available,
            // nothing to do.
            return null;
        }
        if (state == REPLICATED) {
            group.remove(path, (String) key, sendMode);
        } else {
            properties.remove(key);
        }
        return reval;
    }

    // ****************************************************
    // object retrieval methods
    // ****************************************************

    /**
     * Checks if a key is set in the properties table
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * Deletes the properties table
     */
    public void clearProperties() {
        properties.clear();
    }

    /**
     * Returns the keys of the local properties
     */
    public Enumeration properties() {
        return properties.keys();
    }

    /**
     * Gets a property value
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * removes a property value
     */
    protected Object removeProperty(String key) {
        return properties.remove(key);
    }
    
    /**
     * Returns the whole properties table
     */
    public Hashtable getProperties() {
        return properties;
    }

    /**
     * Sets the whole properties table.
     */
    public void setProperties(Hashtable properties) {
        this.properties = properties;
    }

    /**
     * Checks if this object has a child with a given key
     */
    public boolean hasChild(String key) {
        return children.contains(key);
    }

    /**
     * Gets a child with a given key
     */
    public GroupObject getChild(String key) {
        return group.get(path + GroupExtension.SEPARATOR + key);
    }

    /**
     * Returns the keys of the children
     */
    public Enumeration children() {
        return children.elements();
    }

    /**
     * Returns the whole children table
     */
    public Vector getChildren() {
        return children;
    }

    /**
     * Sets the whole children table.
     */
    public void setChildren(Vector children) {
        this.children = children;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean equals(Object checkObject) {
        if (checkObject == null)
            return false;
        GroupObject groupobject = null;
        try {
            groupobject = (GroupObject) checkObject;
        } catch (ClassCastException cce) {
            return false;
        }
        boolean propsEqual = properties.equals(groupobject.getProperties());
        if (propsEqual == false)
            return false;
        // the property-hashtables are equal, now compare children-hashtables
        Vector checkChildren = groupobject.getChildren();
        if (children == null) {
            return (checkChildren == null) ? true : false;
        } else {
            return children.equals(checkChildren);
        }
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    /**
     * Adds a key to the list of childnames. Should only be called by the
     * callback functions of Group.
     * 
     * @param key the name of the child (relative to the parent object)
     */
    protected void addChildReference(String key) {
        synchronized(children) {
            if (!children.contains(key)) {
                children.add(key);
            }
        }
    }

    /**
     * Remove a key to the list of childnames. Should only be called by the
     * callback functions of Group.
     * 
     * @param key the name of the child (relative to the parent object)
     */
    protected void removeChildReference(String key) {
        children.remove(key);
    }

    public String toString() {
        return "[" + toStringBasic() + "]";
    }

    /**
     * Prints the whole object including path, properties and children.
     */
    public String toStringFull() {
        return "[" + toStringBasic() + ", properties=" + properties.toString() + ", children="
                + ((children != null) ? children.toString() : "null") + "]";
    }

    public String toStringBasic() {
        if (path == null) {
            return "GroupObject " + this.hashCode();
        }
        String firstPart = null;
        if (isRoot == true) {
            return "GroupObject " + this.hashCode() + ":root";
        } else {
            return "GroupObject " + this.hashCode() + ":" + path;
        }
    }

    /**
     * Prints just the properties-table.
     */
    public String toStringHashtable() {
        return properties.toString();
    }

}

