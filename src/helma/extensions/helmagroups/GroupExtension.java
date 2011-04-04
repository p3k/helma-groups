package helma.extensions.helmagroups;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import helma.extensions.HelmaExtension;
import helma.extensions.ConfigurationException;
import helma.framework.core.Application;
import helma.main.Server;
import helma.scripting.ScriptingEngine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jgroups.Version;

/**
 * This extension makes a groups available to the scripting environment: A
 * global object "group" is created which features different channels and to
 * which GroupObjects can be added.
 * 
 * <pre>
 *         group.sessions
 *         group.sessions.test = new GroupObject ();
 *         group.sessions.test.abc = &quot;abc&quot;;
 * </pre>
 * 
 * Each change is replicated to all other Helma servers that are in the same
 * group.
 */

public class GroupExtension extends HelmaExtension implements GroupConstants {

    public static GroupExtension self; 
    
    private static Server server;

    private Hashtable groups = new Hashtable();

    private Hashtable apps = new Hashtable();

    private Map scriptingExtensions = new WeakHashMap();
    
    private static Log logger = LogFactory.getLog("helmagroups");

    public GroupExtension() {
        self = this;
    }

    /**
     * basic initialization done once at server startup
     */
    public void init(Server server) throws ConfigurationException {
        // check wheter jgroups can be loaded
        try {
            Class check = Class.forName("org.jgroups.Version");
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "Couldn't init HelmaGroups: jgroups-library not in classpath. make sure jgroups-all.jar is included. get it from http://www.jgroups.org");
        }
        logger.info("loading HelmaGroups " + VERSION + ", using JGroups version " + org.jgroups.Version.version);
        GroupExtension.server = server;
    }

    /**
     * Inits an app and starts groups according to the app.properties
     */
    public synchronized void applicationStarted(Application app) throws ConfigurationException {
        updateGroups(app, app.getProperties());
    }

    /**
     * called each time an application updates its setup and creates/deletes
     * references to groups
     */
    public synchronized void applicationUpdated(Application app) {
        if (!apps.containsKey(app.getName())) {
            // we've never seen this app before, so it probably sent an update
            // event before it had initialized all extensions properly
            return;
        }
        updateGroups(app, app.getProperties());
    }

    /**
     * called when an application is going down.
     */
    public synchronized void applicationStopped(Application app) {
        // update with empty properties so that updateGroups() thinks all
        // references to groups have been deleted
        updateGroups(app, new Properties());
    }

    /**
     * Add the group to a scripting engine.
     */
    public synchronized HashMap initScripting(Application app, ScriptingEngine engine) throws ConfigurationException {
        return new HashMap();
    }

    public String getName() {
        return "HelmaGroupsExtension";
    }

    /**
     * parses properties and starts/stops groups accordingly
     */
    private void updateGroups(Application app, Properties props) throws ConfigurationException {
        // loop through app.properties:
        Vector names = new Vector();
        for (Enumeration e = props.keys(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            if (key.startsWith("group.") && key.lastIndexOf(".") == 5 && key.length() > 6) {
                names.add(key.substring(6));
            }
        }

        // loop through names
        synchronized (groups) {
            for (int i = 0; i < names.size(); i++) {
                String name = (String) names.get(i);
                String value = props.getProperty("group." + name);

                Group g;
                try {
                    g = checkGroup(value);
                } catch (FileNotFoundException fnfe) {
                    logger.error("couldn't read config file from " + value);
                    continue;
                } catch (GroupException ge) {
                    logger.error("couldn't start group: " + ge.toString());
                    ge.printStackTrace();
                    continue;
                } catch (Exception ex) {
                    logger.error("error starting group: " + ex.toString());
                    ex.printStackTrace();
                    continue;
                }
                // set auto-reconnect value
                String autoReconnect = props.getProperty("group." + name + ".autoreconnect", "false");
                if (autoReconnect.equalsIgnoreCase("true")) {
                    g.setAutoReconnect(true);
                } else {
                    g.setAutoReconnect(false);
                }
                // start debugger if wanted:
                String debuggerPort = props.getProperty("group." + name + ".debugger.port");
                if (debuggerPort != null) {
                    try {
                        int port = Integer.parseInt(debuggerPort);
                        g.startDebugger(port, props.getProperty("group." + name + ".debugger.allowed"));
                    } catch (NumberFormatException nfe) {
                        g.getLogger().error("couldn't start debugger on " + debuggerPort);
                    }
                }
                // check mountings
                ApplicationLink appLink = checkAppLink(app.getName());
                if (!appLink.containsKey(name)) {
                    g.addLocalClient(app.getName(), new Executor(app.getName()));
                    appLink.put(name, g);
                    g.getLogger().info("mounted as group." + name + " in " + app.getName());
                }
                // check write access
                if ("true".equalsIgnoreCase(props.getProperty("group." + name + ".writable"))) {
                    appLink.setWritable(g, true);
                } else if ("true".equalsIgnoreCase(props.getProperty("group." + name + ".writeable"))) {
                    appLink.setWritable(g, true);
                } else {
                    appLink.setWritable(g, false);
                }
            }
        }
        // loop through this app's groups and check if a group has to be removed
        ApplicationLink appLink = checkAppLink(app.getName());
        for (Enumeration e = appLink.keys(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            if (!names.contains(name)) {
                Group g = (Group) appLink.get(name);
                appLink.remove(name);
                if (!appLink.containsValue(g))
                    g.removeLocalClient(app.getName());
                app.logEvent("[HelmaGroups] removing " + g.info.getId());
            }
        }

        synchronized (groups) {
            // loop through groups and check if a group is not needed anymore
            for (Enumeration e=groups.keys(); e.hasMoreElements();) {
                Object key = e.nextElement();
                Group g = (Group) groups.get(key);
                if (g.hasLocalClients() == false) {
                    g.disconnect();
                    g.stopDebugger();
                    g.stop();
                    groups.remove(key);
                    if(logger.isDebugEnabled()) logger.debug("GroupExtension.updateGroups() removed group " + g + " because it's no longer needed");
                }
            }
        }
    }

    /**
     * private class doing the actual callback for the execute-system.
     */
    class Executor implements ExecuteHandler {
        String appName;

        public Executor(String appName) {
            this.appName = appName;
        }

        public Result execute(String funcName, Vector args) {
            Application app = server.getApplication(appName);
            try {
                Object result = app.executeXmlRpc(funcName, args);
                return Result.newResult(result, appName);
            } catch (Exception e) {
                logger.info("GroupExtension.execute(" + funcName + ") on " + app.getName() + " threw error: " + e.toString());
                return Result.newError(e.toString(), appName);
            }
        }
    }

    public ApplicationLink checkAppLink(String appName) {
        if (apps.containsKey(appName))
            return (ApplicationLink) apps.get(appName);
        ApplicationLink appLink = new ApplicationLink(appName);
        apps.put(appName, appLink);
        return appLink;
    }

    public boolean mayWrite(String appName, Group g) {
        return checkAppLink(appName).isWritable(g);
    }

    public static Log getLogger(String groupName) {
        return LogFactory.getLog("helmagroups." + groupName);
    }

    public static Log getLogger() {
        return logger;
    }
   

    /**
     * method fetching a group from the groups hashtable via the network
     * identifier. group is started if necessary.
     * 
     * @param configValue
     * @throws FileNotFoundException ... error reading config
     * @throws MalformedURLException ... error fetching config
     * @throws GroupException ... error starting group
     * @throws IOException .. error parsing config
     */
    private Group checkGroup(String configValue) throws Exception {
        File configHint = new File(server.getHopHome(), "helmagroups");
        Config config = Config.getConfig(configValue, configHint);
        String id = Config.getIdentifier(config);
        Group g = (Group) groups.get(id);
        if (g == null) {
            // start a new group
            logger.info("starting a new group with " + configValue);
            g = Group.newInstance(config);
            g.setConfigSource(configValue, configHint);
            groups.put(id, g);
        }
        return g;
    }

    /**
     * some options for testing
     */
    public static void main(String args[]) throws Exception {
        System.out.println("loading HelmaGroups " + VERSION + ", using JGroups version " + Version.version);
        if (args.length == 0) {
            System.out.println("usage: java helma.extensions.helmagroups.GroupExtension [-version] [-group config-file]");
        } else if (args[0].equals("-version")) {
            System.out.println("HelmaGroups " + VERSION);
        } else if (args[0].equals("-group")) {
            String filename = (args.length > 1) ? args[1] : "default.xml";
            Config config = Config.getConfig(filename, null);
            Group g = Group.newInstance(config);
            System.out.println("Group connected: " + g.info.getConnection());
            System.out.println("press RETURN to stop the group!");
            int t = System.in.read();
            System.out.println("");
            g.disconnect();
        }
    }

}

