package helma.extensions.helmagroups;

import java.io.Serializable;

public class StateWrapper implements Serializable {

    private static final long serialVersionUID = 6443902769509263633L;

    public String localName;

    public Serializable object;

    /**
     * creates a new state wrapper object and returns it as serialized byte
     * array
     */
    public static byte[] wrap(String id, Serializable object) {
        StateWrapper swp = new StateWrapper(id, object);
        try {
            return org.jgroups.util.Util.objectToByteBuffer(swp);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * tries to re-create a StateWrapper object from a given byte array
     * 
     * @throws RuntimeException thrown in case the object is not a StateWrapper
     *         (presumably an older version of HelmaGroups provided the state in
     *         this case)
     */
    public static StateWrapper unwrap(byte[] raw) throws Exception {
        Object obj = org.jgroups.util.Util.objectFromByteBuffer(raw);
        if (obj != null && !(obj instanceof StateWrapper)) {
            throw new RuntimeException(
                    "got state from an earlier version of helmagroups (< 0.8). please update all instances to the same version!");
        }
        return (StateWrapper) obj;
    }

    public StateWrapper(String localName, Serializable object) {
        this.localName = localName;
        this.object = object;
    }

}