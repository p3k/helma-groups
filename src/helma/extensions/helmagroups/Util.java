package helma.extensions.helmagroups;

import java.io.Serializable;
import java.util.Vector;

import org.jgroups.blocks.GroupRequest;

public class Util implements GroupConstants {

    /**
     * return a very simple stop watch
     */
    public static Watch getWatch() {
        return new Watch();
    }

    static class Watch {
        private long starttime;

        public Watch() {
            starttime = System.currentTimeMillis();
        }

        public void log() {
            System.out.println(elapsed());
        }

        public String elapsed() {
            long diff = System.currentTimeMillis() - starttime;
            return diff + "ms";
        }
    }



  

    /**
     * utility for converting property value to GroupRequest sendmode integer
     * 
     * @param val may be all | first | majority | none
     * @returns specified integer, GET_ALL by default
     */
    public static int sendModeToInt(String val) {
        if (val == null) {
            return GroupRequest.GET_ALL;
        }
        val = val.toLowerCase();
        if (val.equals("all")) {
            return GroupRequest.GET_ALL;
        } else if (val.equals("first")) {
            return GroupRequest.GET_FIRST;
        } else if (val.equals("majority")) {
            return GroupRequest.GET_MAJORITY;
        } else if (val.equals("none")) {
            return GroupRequest.GET_NONE;
        } else {
            return GroupRequest.GET_ALL;
        }
    }

    /**
     * utility for converting property value to GroupRequest sendmode integer
     * 
     * @param val may be all | first | majority | none
     * @returns specified integer, GET_ALL by default
     */
    public static String sendModeToString(int val) {
        switch (val) {
        case GroupRequest.GET_ALL:
            return "all";
        case GroupRequest.GET_FIRST:
            return "first";
        case GroupRequest.GET_MAJORITY:
            return "majority";
        case GroupRequest.GET_NONE:
            return "none";
        default:
            return "not specified";
        }
    }

    public static Object getFromVector(Serializable ser, int idx) {
        if (ser == null) {
            return null;
        }
        if (ser instanceof Vector) {
            Vector vec = (Vector) ser;
            if (idx < vec.size()) {
                return vec.get(idx);
            }   
        }
        return null;
    }
 
    public static Vector wrapInVector(Object arg1) {
        Vector vec = new Vector();
        vec.add(arg1);
        return vec;
    }
    
}

