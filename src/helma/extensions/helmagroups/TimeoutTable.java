package helma.extensions.helmagroups;

import java.util.*;

public class TimeoutTable extends Hashtable {

    private static final long serialVersionUID = 1L;

    /**
     * an extended Hashtable that removes objects automatically after a given
     * period. Removal is done with all get/put/remove operations
     */

    private long timeout;

    private Hashtable createTimes;

    public TimeoutTable(long timeout) {
        this.timeout = timeout;
        createTimes = new Hashtable();
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Object put(Object key, Object value) {
        Object retval = super.put(key, value);
        createTimes.put(key, new Long(System.currentTimeMillis()));
        checkTimeouts();
        return retval;
    }

    public void putAll(Map t) {
        for (Iterator i = t.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            createTimes.put(key, new Long(System.currentTimeMillis()));
            super.put(key, t.get(key));
        }
        checkTimeouts();
    }

    public Object remove(Object key) {
        Object retval = super.remove(key);
        createTimes.remove(key);
        checkTimeouts();
        return retval;
    }

    public boolean contains(Object value) {
        checkTimeouts();
        return super.contains(value);
    }

    public boolean containsKey(Object key) {
        checkTimeouts();
        return super.containsKey(key);
    }

    public boolean containsValue(Object value) {
        checkTimeouts();
        return super.containsValue(value);
    }

    public Enumeration elements() {
        checkTimeouts();
        return super.elements();
    }

    public Set entrySet() {
        checkTimeouts();
        return super.entrySet();
    }

    public Object get(Object key) {
        checkTimeouts();
        return super.get(key);
    }

    public boolean equals(Object o) {
        checkTimeouts();
        return super.equals(o);
    }

    public int hashCode() {
        checkTimeouts();
        return super.hashCode();
    }

    public boolean isEmpty() {
        checkTimeouts();
        return super.isEmpty();
    }

    public Enumeration keys() {
        checkTimeouts();
        return super.keys();
    }

    public Set keySet() {
        checkTimeouts();
        return super.keySet();
    }

    public int size() {
        checkTimeouts();
        return super.size();
    }

    public String toString() {
        checkTimeouts();
        return super.toString();
    }

    public Collection values() {
        checkTimeouts();
        return super.values();
    }

    private void checkTimeouts() {
        long limit = System.currentTimeMillis() - timeout;
        for (Enumeration e = createTimes.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            Long l = (Long) createTimes.get(key);
            if (l.longValue() < limit) {
                createTimes.remove(key);
                super.remove(key);
            }
        }
    }

}