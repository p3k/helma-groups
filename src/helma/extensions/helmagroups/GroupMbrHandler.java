package helma.extensions.helmagroups;

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.jgroups.Address;
import org.jgroups.MembershipListener;
import org.jgroups.View;

public class GroupMbrHandler implements MembershipListener {

    private Group g          = null;
    protected Vector members = null;
    
    public GroupMbrHandler(Group g) {
        this.g = g;
        this.members = new Vector();
    }

    public void shutdown() {
        g = null;
    }

    
    /**
     * From interface MembershipListener. Tells us when members left or joined
     * the group. <br/><br/>If this instance is the coordinator it expects to
     * be asked for the state by the joining member and stores the serialized
     * state at the time of the view change in a self-cleaning hashtable. (in
     * case of a MERGE event state would be stored too although it will never be
     * retrieved).
     */
    public void viewAccepted(View new_view) {
        Log logger = g.logger;
        if(logger.isDebugEnabled()) logger.debug("Group.viewAccepted() membership changed: " + new_view.getMembers().toString());
        // look for members that joined:
        boolean newMemberEntered = false;
        Vector newMembers = new_view.getMembers();
        for (int i = 0; i < newMembers.size(); i++) {
            if (!members.contains(newMembers.elementAt(i))) {
                logger.info("new member joined: " + newMembers.elementAt(i));
                newMemberEntered = true;
            }
        }
        // look for members that left:
        for (int i = 0; i < members.size(); i++) {
            if (!newMembers.contains(members.elementAt(i))) {
                logger.info("member left: " + members.elementAt(i));
            }
        }
        // store new members as current members
        members = newMembers;
    }

    public void suspect(Address suspected_mbr) {
        if(g.logger != null && g.logger.isDebugEnabled()) g.logger.debug("GroupMbrHandler.suspect() with member " + suspected_mbr);
    }

    public void block() {
        if(g.logger != null && g.logger.isDebugEnabled()) g.logger.debug("GroupMbrHandler.block() called");
    }

}
