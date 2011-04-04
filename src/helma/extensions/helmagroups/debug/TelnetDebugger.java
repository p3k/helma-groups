package helma.extensions.helmagroups.debug;

import helma.extensions.helmagroups.Group;
import helma.extensions.helmagroups.GroupExtension;
import helma.util.InetAddressFilter;
import helma.util.ParanoidServerSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * this class creates a debugger for a Group that can be accessed via telnet
 * and understands various commands, basically the same as in the webfrontend
 * in groupmgr.zip.
 * 
 * WARNING: this code is alpha quality and should not be activated in production
 * environments. and, as it is open to anyone connected, it should be hidden
 * behind a firewall!
 */

public class TelnetDebugger extends Thread {

    private int port;
    private ServerSocket listener;
    protected Group g;
    private Vector handlers = new Vector();
    private InetAddressFilter filter;
    
    public TelnetDebugger(Group g, int port) {
        setName("TelnetDebugger");
        this.g = g;
        this.port = port;
        this.filter = new InetAddressFilter();
    }

    public void addAddress(String address) throws IOException {
        filter.addAddress(address);
    }
    
    public void addAddresses(String addresses) throws IOException {
        if (addresses == null || addresses.trim().equals("")) {
            return;
        }
        StringTokenizer tok = new StringTokenizer(addresses, ",");
        while (tok.hasMoreTokens()==true) {
            String address = tok.nextToken().trim();
            filter.addAddress(address);
        }
    }
    
    public void init() throws IOException {
        listener = new ParanoidServerSocket(port, filter);
        GroupExtension.getLogger(g.info.getGroupName()).info("started TelnetDebugger on port " + port);
    }
    
    public void run() {
        try {
            while(true) {
                Socket socket = listener.accept();
                ConnectionHandler handler = new ConnectionHandler (this, socket);
                handler.start();
                handlers.add(handler);
            }
        } catch (SocketException ex) {
            // do nothing
        } catch (Exception anything) {
            GroupExtension.getLogger(g.info.getGroupName()).error("caught exception in TelnetDebugger: " + anything.toString());
        }
    }

    public synchronized void shutdown() {
        for (int i=0; i<handlers.size(); i++) {
            ConnectionHandler handler = (ConnectionHandler) handlers.get(i);
            handler.shutdown();
        }
        try {
            listener.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        listener = null;
    }
    
    public int getPort() {
        return port;
    }
    
}
