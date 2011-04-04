package helma.extensions.helmagroups.debug;

import helma.extensions.helmagroups.Config;
import helma.extensions.helmagroups.GroupObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import org.jgroups.Address;

public class ConnectionHandler extends Thread {

    TelnetDebugger debugger;
    Socket socket;
    BufferedReader reader;
    PrintWriter writer;
    
    boolean ended = false;
    
    public ConnectionHandler(TelnetDebugger debugger, Socket socket) throws IOException {
        this.debugger = debugger;
        this.socket = socket;
        InputStream in = socket.getInputStream();
        reader = new BufferedReader(new InputStreamReader(in));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    public void run() {
        debugger.g.getLogger().info("new connection to debugger from " + socket.getInetAddress().toString());
        try {
            status();
            prompt();
            String line = "";
            while (line != null) {
                handleInput(line);
                line = reader.readLine();
            }
        } catch (IOException ex) {
        }
        if (!ended) {
            shutdown();
        }
    }

    public void shutdown() {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (Exception ex) {
        }
        ended = true;
        interrupt();
    }
    
    void handleInput(String rawline) throws IOException {
        rawline = rawline.trim().toLowerCase();
        if (rawline.equals("")) {
            return;
        }
        StringBuffer cleanLine = new StringBuffer();
        for (int i=0; i<rawline.length(); i++) {
            int x = Character.getNumericValue(rawline.charAt(i));
            if (x >= 10 && x<=35) {
                cleanLine.append(rawline.charAt(i));
            }
        }
        String line = cleanLine.toString();
        if (line.equals("exit")) {
            socket.close();
            socket = null;
            interrupt();
        } else if(line.equals("members")) {
            members();
            writer.println("");
        } else if(line.equals("apps")) {
            String[] apps = debugger.g.info.listMemberApps();
            for (int i=0; i<apps.length; i++) {
                writer.println(apps[i]);
            }
            writer.println("");
        } else if(line.equals("connect")) {
            debugger.g.connect();
            writer.println("connected\n");
        } else if(line.equals("disconnect")) {
            debugger.g.disconnect();
            writer.println("disconnected\n");
        } else if(line.equals("reconnect")) {
            debugger.g.reconnect();
            writer.println("disconnected and connected again\n");
        } else if(line.equals("reset")) {
            debugger.g.reset();
            writer.println("reset group\n");
        } else if(line.equals("destroy")) {
            debugger.g.destroy();
            writer.println("destroyed group\n");
        } else if(line.equals("restart")) {
            debugger.g.restart();
            writer.println("restarted group\n");
        } else if(line.equals("config")) {
            String str = debugger.g.info.getConfig();
            String lines[] = str.split(":");
            for (int i=0; i<lines.length; i++) {
                writer.println(lines[i]);
            }
            writer.println("");
        } else if(line.equals("size")) {
            size();
        } else if(line.equals("content")) {
            String str = debugger.g.info.print();
            writer.println(str);
            writer.println("");
        } else if(line.equals("details")) {
            String str = debugger.g.info.printFull();
            writer.println(str);
            writer.println("");
        } else if(line.equals("info")) {
            String str = debugger.g.info.getConnection();
            writer.println(str);
        } else if (line.startsWith("execute")) {
            StringTokenizer tok = new StringTokenizer(rawline);
            try {            
                tok.nextToken();    // consume the execute command
                String funcName = tok.nextToken();
                Vector args = new Vector();
                while (tok.hasMoreTokens()==true) {
                    args.add(tok.nextToken());
                }
                Vector results = debugger.g.execute(funcName, args);
                for (Enumeration e = results.elements(); e.hasMoreElements(); ) {
                    writer.println(e.nextElement().toString());
                    writer.println("");
                }
            } catch (NoSuchElementException nsee) {
                writer.println("Syntax of execute command is:"); 
                writer.println("execute <functionName> <arg1> <arg2> <arg3> ..."); 
                writer.println("e.g. execute echo message1"); 
            }
        } else if(line.startsWith("remove")) {
            StringTokenizer tok = new StringTokenizer(rawline);
            try {
                tok.nextToken();    // consume the remove command
                String path = tok.nextToken();
                GroupObject parent = resolvePath(path);
                parent.remove(resolvePropName(path));
            } catch (NoSuchElementException nsee) {
                writer.println("Syntax of remove command is:"); 
                writer.println("remove <path.propertyname>"); 
                writer.println("e.g. remove test.test1"); 
            }
        } else if(line.startsWith("add")) {
            add(rawline);
        } else if(line.startsWith("test")) {
            add("add test test:test");
        } else if(line.startsWith("benchmark")) {
            benchmark(rawline);
        } else if(line.equals("help")) {
            writer.println("valid commands: connect disconnect reconnect reset destroy restart");
            writer.println("                info config members apps size content details execute");
            writer.println("                add remove help status test benchmark exit");
            writer.println("");
        } else if(line.equals("status") || line.trim().equals("")) {
            status();
        } else {
            writer.println("command not understood. type <help> for help.");
        }
        prompt();
    }

    private String resolvePropName(String path) {
        int idx = path.lastIndexOf(".");
        return path.substring(idx + 1);
    }
    
    private GroupObject resolvePath(String path) {
        GroupObject obj = debugger.g.getRoot();
        if (path.indexOf(".")>0) {
            StringTokenizer tok = new StringTokenizer (path.substring(0, path.lastIndexOf(".")), ".");
            while (tok.hasMoreTokens()==true) {
                String key = tok.nextToken();
                obj = obj.getChild(key);
                if (obj == null) {
                    return null;
                }
            }
            return obj;
        } else {
            return obj;
        }
    }

    private void benchmark(String rawline) {
        StringTokenizer tok = new StringTokenizer(rawline);
        int numOfThreads = 0;
        int numOfObjects = 0;
        int sizeOfObjects = 0;
        int pause = 0;
        String sendMode = null;
        try {
            tok.nextToken();
            numOfThreads = Integer.parseInt(tok.nextToken());
            numOfObjects = Integer.parseInt(tok.nextToken());
            sizeOfObjects = Integer.parseInt(tok.nextToken());
            pause = Integer.parseInt(tok.nextToken());
            sendMode = tok.nextToken();
        } catch(Exception anything) {
            writer.println("Syntax of benchmark command is:");
            writer.println("benchmark <numberOfThreads> <numberOfObjectsPerThread> <sizeOfObjects> <pause> <sendMode>");
            writer.println("Will try to send these objects as fast as possible shows the time spent.");
            writer.println("For accuracy check the other instances of the group.");
            return;
        }
        Benchmarker benchmarker = new Benchmarker(writer, debugger.g, numOfThreads, numOfObjects, sizeOfObjects, pause, sendMode);
        benchmarker.test();
    }
    
    private void add(String rawline) {
        StringTokenizer tok = new StringTokenizer(rawline);
        try {
            tok.nextToken();    // consume the add command
            String path = tok.nextToken();
            GroupObject parent = resolvePath(path);
            GroupObject obj = new GroupObject();
            while (tok.hasMoreTokens()) {
                String pair = tok.nextToken();
                StringTokenizer tok2 = new StringTokenizer (pair, ":");  
                String key = tok2.nextToken();
                String value = tok2.nextToken();
                obj.put(key, value);
            }
            parent.put(resolvePropName(path), obj);
        } catch (NoSuchElementException nsee) {
            writer.println("Syntax of add command is:"); 
            writer.println("add <path.propertyname> <key:value>"); 
            writer.println("This is only a very simple implementation - no spaces in key-value pairs are allowed!"); 
            writer.println("e.g. add test"); 
            writer.println("     add test.test1 key1:value1 key2:value2"); 
        }
    }

    private void size() {
        writer.println(debugger.g.size() + " object(s) in the group\n");
    }

    private void members() {
        Address[] members = debugger.g.info.listMembers();
        for (int i=0; i<members.length; i++) {
            writer.println(Config.addressToString(members[i]));
        }
    }

    private void prompt() {
        writer.print(debugger.g.info.getGroupName() + "> ");
        writer.flush();
    }
    
    private void status() {
        long connectTime = debugger.g.info.getConnectTime();
        Date connectDate = new Date(connectTime);
        writer.println("Connected to Group " + debugger.g.info.getGroupName() + " since " + connectDate);
        writer.println(debugger.g.info.getConnection());
        size();
        writer.println("Current members:");
        members();
    }
    
}
