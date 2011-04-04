package helma.extensions.helmagroups;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import org.jgroups.Address;

public class GroupInfo implements GroupConstants {

    private Group g;
    
    public GroupInfo(Group g) {
        this.g = g;
    }

    public void stop() {
        this.g = null;
    }
    
    public String getConfig() {
        return g.config;
    }

    public String getGroupName() {
        return g.groupName;
    }

    public long getConnectTime() {
        return g.connectTime;
    }

    
    /**
     * for introspection: returns the protocol, the ip(s) and the port number
     * this channel is connected to.
     */
    public String getConnection() {
        if (g.channel != null && g.channel.isConnected()) {
            return "[" + g.identifier + "]";
        } else {
            return "[" + g.identifier + " NOT CONNECTED]";
        }
    }

    public String getId() {
        return g.identifier;
    }

    /**
     * for introspection: returns the local ip/name and port number of this
     * instance
     */
    public String getLocalAddress() {
        if (g.channel != null && g.channel.isConnected())
            return g.channel.getLocalAddress().toString();
        else
            return "[channel not connected]";
    }

    /**
     * return a list of the members of this group
     * 
     * @return Vector containing Address objects
     */
    public Address[] listMembers() {
        return (org.jgroups.Address[]) g.membershipMgr.members.toArray(new Address[g.membershipMgr.members.size()]);
    }

    public String[] listMemberApps() {
        if (g.isConnected()) {
            Vector apps = (Vector) g.internal("apps", null);
            return (String[]) apps.toArray(new String[apps.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * Prints all the keys of this tree to a string.
     */
    public String print() {
        if (g.state != STATE_CONNECTED)
            return "group is not connected";
        StringBuffer sb = new StringBuffer();
        synchronized (g.objects) {
            TreeMap tree = new TreeMap(g.objects);
            for (Iterator i = tree.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                sb.append(key);
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * Prints all the objects of this tree including their content to a string.
     */
    public String printFull() {
        if (g.state != STATE_CONNECTED)
            return "group is not connected";
        StringBuffer sb = new StringBuffer();
        TreeMap tree = new TreeMap(g.objects);
        for (Iterator i = tree.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            GroupObject object = (GroupObject) g.objects.get(key);
            if (object != null) {
                sb.append("[GroupObject ");
                sb.append(object.getPath());
                sb.append(" ");
                sb.append(object.toStringHashtable());
                sb.append("]\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * for introspection: returns a list of protocols used in this channel
     * 
     * @param include_properties includes properties if set to true
     */
    public String printStack(boolean include_properties) {
        if (g.channel != null && g.channel.isConnected())
            return g.channel.printProtocolSpec(include_properties);
        else
            return "[channel not connected]";
    }



}
