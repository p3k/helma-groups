package helma.extensions.helmagroups;

import helma.extensions.helmagroups.debug.TelnetDebugger;

import java.io.*;
import java.util.*;

import org.apache.commons.logging.Log;

import org.jgroups.*;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.util.RspList;

/**
 * This class builds a replicated tree of GroupObjects and stores them flat in a
 * Hashtable. Updates are replicated to other members of the group.
 */

public class Group implements MessageListener, RequestHandler, GroupConstants {

    // the state of the Group
    protected int state = STATE_DISCONNECTED;

    // the name of the Group (ie. the JGroups-Channel). This has to be the same
    // for all members of a group)
    protected String groupName;

    // the name of this instance of the Group. Should be different for each
    // member so that they can be distinguished.
    protected String localName;

    // basically only used for logging. name of the group member that sent us
    // the current state.
    private String stateSource;

    // in case state transfer throws an exception it is stored here and
    // rethrown when initThread is up again.
    private Exception stateTransferException;

    // the identifier of the Group as it is shown in logfiles etc
    // this is computed automatically and stored for quicker access
    protected String identifier;

    // the current jgroups-config string
    protected String config;

    // the actual jgroups-channel
    protected JChannel channel = null;

    // the jgroups block to handle messages
    private MessageDispatcher disp = null;

    // the object storage
    protected Hashtable objects = null;

    // list of local applications using this group (key=appname,
    // value=executeHandler on this app)
    private Hashtable localClients = null;

    // the thread that starts the group. this is notified after the state was received 
    private Thread initThread = null;
    
    protected Log logger;

    // timeout for org.jgroups.blocks.MessageDispatcher.castMessage
    protected long timeout     = DEFAULT_SEND_TIMEOUT;

    // a copy of the JGroups option AUTO_RECONNECT
    protected boolean autoReconnect = false;

    // where we got the config from (if available). used in reconnect() to reload the config
    private String configValue;
    private File configHint;
    
    // timestamp at connect
    protected long connectTime = 0;
    
    private TelnetDebugger debugger;

    public GroupInfo info;
    
    protected GroupMbrHandler membershipMgr;
    
    /**
     * Construct a new Group instance and connect it.
     * 
     * @param config the string to configure a JGroups channel
     * @param localName a string tagging the local instance within the group
     * @param groupName the name of the groupname
     * @param transport a string used for logging
     */
    public static Group newInstance(String config, String localName, String groupName, String transport) throws Exception {
        Group group = new Group();
        group.init(config, localName, groupName, transport);
        group.connect();
        return group;
    }

    /**
     * Construct a new Group instance and connect it.
     * 
     * @param config a configured XmlConfigurator
     * @param localName a string tagging the local instance within the group. if
     *        not set this value is guessed from the system properties
     */
    public static Group newInstance(Config config) throws Exception {
        if (!config.isValid()) {
            throw new RuntimeException("incomplete config, can't start group");
        }
        String transport = Config.getTransport(config.xmlConfig);
        return newInstance(config.xmlConfig.getProtocolStackString(), config.localName, config.groupName, transport);
    }

    /**
     * constructor kept private, use newInstance for construction
     */
    private Group() {
        localClients = new Hashtable();
        info = new GroupInfo(this);
        membershipMgr = new GroupMbrHandler(this);
    }

    /**
     * Inits some vars
     * 
     * @param config the properties string to configure JGroups.
     * @param localName
     * @param groupName
     * @param transport
     * @see Group#newInstance(String, String, String, String)
     */
    private void init(String config, String localName, String groupName, String transport) throws GroupException {
        this.config = config;
        this.groupName = (groupName != null) ? groupName : DEFAULT_GROUPNAME;
        this.localName = (localName != null) ? localName : DEFAULT_LOCALNAME;
        identifier = Config.renderIdentifier(localName, this.groupName, transport);
        logger = GroupExtension.getLogger(groupName);
    }

    /**
     * Creates a JGroups-Channel, contact the coordinator of an existing group
     * to retrieve the current state or create a new group if necessary.
     */
    public void connect() throws GroupException {
        logger.info("Trying to connect to the group ...");
        if (channel != null) {
            logger.info("... already connected.");
            return;
        }
        try {

            channel = new JChannel(config);
            if (autoReconnect == true) {
                channel.setOpt(Channel.AUTO_RECONNECT, Boolean.TRUE);
                channel.setOpt(Channel.AUTO_GETSTATE, Boolean.TRUE);
            }
            channel.setOpt(Channel.GET_STATE_EVENTS, Boolean.TRUE); // FIXME: not needed with current JGroups

            // before connecting send config event down the jgroups-stack
            // and set additional_data to localname
            Event configEvent = new Event(Event.CONFIG);
            HashMap map = new HashMap();
            map.put("additional_data", org.jgroups.util.Util.objectToByteBuffer(localName));
            configEvent.setArg(map);
            channel.down(configEvent);

            disp = new org.jgroups.blocks.MessageDispatcher(channel, this, membershipMgr, this);

            channel.connect(groupName);

            long starttime = System.currentTimeMillis();
            
            synchronized (this) {
                // keep reference to the current thread to wait for complete state-transfere
                this.initThread = Thread.currentThread();
            }
            boolean rc = channel.getState(null, 30000);
            logger.info("====================================================================");
            if (rc) {
                logger.info("joining member. retrieving state...");
                synchronized(this) {
                    try {
                        for (int i = 0 ; i < 6 && initThread != null; i++) {
                            if(logger.isDebugEnabled()) logger.debug(Thread.currentThread() + " waiting for 5 sec");
                            this.wait(5000);
                        }
                    } catch (InterruptedException interrupted) {
                        if(logger.isDebugEnabled()) logger.debug("initThread interrupted");
                    }
                }
                // if state transfer thread caught an exception, rethrow it here and
                // exit connect
                if (stateTransferException != null) {
                    throw (stateTransferException);
                }
                
                if (initThread != null) {
                    logger.warn("WARNING: state-transfer failed or incomplete!");
                } else {
                    logger.info("current state was successfully retrieved from " + stateSource + "!");
                    logger.info("transfer took " + (System.currentTimeMillis() - starttime) + " ms");
                }
            } else {
                logger.info("state could not be retrieved (first member)");
                objects = new Hashtable();
            }
            logger.info("====================================================================");

            // mark group as connected
            state = STATE_CONNECTED;
            // as soon as we're fully connected we can make sure
            // there is a root object ... if we're the first member,
            // root object is created by getRoot()
            getRoot();
            connectTime = System.currentTimeMillis();
            logger.info("... connected to the group " + info.getConnection());

        } catch (Exception problem) {
            logger.error(problem.toString());
            problem.printStackTrace();
            try {
                disconnect();
            } catch (Exception ignore) {
            }
            throw new GroupException("Error connecting to the group.", problem);
        }
    }

    /**
     * Reconnects to the group and resets the object-cache.
     */
    public void reconnect() {
        disconnect();
        try {
            // try to re-read the config
            Config config = Config.getConfig(configValue, configHint);
            String id = Config.getIdentifier(config);
            String transport = Config.getTransport(config.xmlConfig);
            init(config.xmlConfig.getProtocolStackString(), config.localName, config.groupName, transport);
            logger.info("reconnect loaded the configuration from " + configValue);
        } catch (Exception anything) {
            logger.error("reconnect couldn't reload the configuration from " + configValue + ", leaving all values as they are now");
        }
        connect();
    }

    /**
     * Disconnects from the group and deletes the object-cache.
     */
    public void disconnect() {
        logger.info("Trying to disconnect from the group " + info.getConnection() + " ...");
        state = STATE_DISCONNECTED;
        if (channel != null) {
            channel.close();
            disp.stop();
            disp.setMembershipListener(null);
            disp.setMessageListener(null);
            disp.setRequestHandler(null);
        }
        channel = null;
        disp = null;
        objects = new Hashtable();
        logger.info("... disconnected from the group.");
    }

    public void stop() {
        stopDebugger();
        info.stop();
        info = null;
    }
    
    public boolean startDebugger(int port, String allowedIps) {
        if (debugger == null) {
            debugger = new TelnetDebugger(this, port);
            try {
                debugger.addAddresses(allowedIps);
                debugger.init();
                debugger.start();
            } catch (IOException io) {
                logger.error("couldn't start debugger on port " + port + ": " + io.toString());
                return false;
            }
            return true;
        } else {
            logger.info("didn't start debugger on port " + port + " because it's already start on port " + debugger.getPort());
            return false;
        }
    }
    
    public boolean stopDebugger() {
        if (debugger != null) {
            debugger.shutdown();
            debugger = null;
            return true;
        }
        return false;
    }
    
    public long getTimeout() {
        return timeout;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public boolean getAutoReconnect() {
        return autoReconnect;
    }
    
    public void setAutoReconnect(boolean autoReconnect) {
        if (this.autoReconnect == autoReconnect) {
            // nothing to change
            return;
        } else {
            this.autoReconnect = autoReconnect;
            if (channel != null) {
                channel.setOpt(Channel.AUTO_RECONNECT, Boolean.valueOf(autoReconnect));
                channel.setOpt(Channel.AUTO_GETSTATE, Boolean.valueOf(autoReconnect));
            }
        }
    }

    public boolean isConnected() {
        return (state == STATE_CONNECTED);
    }

    public JChannel getChannel() {
        return channel;
    }
    
    public Log getLogger() {
        return logger;
    }
    
    public void setConfigSource(String configValue, File configHint) {
        this.configValue = configValue;
        this.configHint = configHint;
    }
    
    public String toString() {
        return info.getConnection();
    }

    /**
     * tell this instance that it is used by a local resource
     * 
     * @param clientName name of the local resource, ie. the name of the helma
     *        application
     * @param exec the object against which execute requests in the group are
     *        run
     */
    public Object addLocalClient(String clientName, ExecuteHandler exec) {
        return localClients.put(clientName, exec);
    }

    /**
     * remove a local resource from this group
     */
    public Object removeLocalClient(String clientName) {
        return localClients.remove(clientName);
    }

    /**
     * list local resources using this group
     */
    public Iterator getLocalClients() {
        return localClients.keySet().iterator();
    }

    /**
     * tells if this group is used by any local ressource
     */
    public boolean hasLocalClients() {
        return (localClients.size() == 0) ? false : true;
    }

    // ******************************
    // from MessageListener-interface:
    // ******************************

    public void receive(Message msg) {
        if(logger.isDebugEnabled()) logger.debug("received message " + msg);
    }

    public byte[] getState() {
        logger.info("called getState(), serializing " + objects.size() + " objects");
        byte[] retval = StateWrapper.wrap(localName, objects);
        if (retval == null) {
            logger.error("Group.getState() couldn't serialize " + objects);
            return null;
        } else {
            if(logger.isDebugEnabled()) logger.debug("getState() returning " + retval.length + " bytes");
            return retval;
        }
    }

    public void setState(byte[] input) {
        String logStr = "Group.setState() called with ";
        logStr += (input == null) ? "null" : (input.length + " bytes");
        logger.info(logStr);
        if(logger.isDebugEnabled()) logger.debug("initThread=" + initThread + " current Thread = " + Thread.currentThread());
        try {
            StateWrapper swp = StateWrapper.unwrap(input);
            if (swp != null) {
                objects = (Hashtable) swp.object;
                stateSource = swp.localName;
                for (Iterator i=objects.values().iterator(); i.hasNext();) {
                    GroupObject currObj = (GroupObject) i.next();
                    currObj.setGroup(this);
                    currObj.setState(REPLICATED);
                }
            }
        } catch (Exception ex) {
            // store exception so that it can be rethrown by init/connect thread
            synchronized(this) {
                stateTransferException = ex;
            }
        }
        synchronized(this) {
            if (initThread != null) {
                if(logger.isDebugEnabled()) logger.debug("interrupting initThread");
                this.notify();
            }
            initThread = null;
        }
    }

    // ******************************
    // from RequestHandler-interface:
    // ******************************

    /**
     * Main callback method for MessageDispatcher. Processes messages sent to
     * this instance
     * 
     * @param msg the Message to process
     * @return the return value of the method call (if any)
     */
    public Object handle(Message msg) {
        try {
            if (state == STATE_CONNECTED) {
                // get the actual request attached to the message:
                Request req = (Request) msg.getObject();
                return invokeRequest(req);
            } else {
                if(logger.isDebugEnabled()) logger.debug("Group.handle() discarded message because group isn't yet connected.");
                return null;
            }
        } catch (Exception ex) {
            // catch all nullpointers, classcasts etc
            if(logger.isDebugEnabled()) logger.debug("Group.handle() exception: " + ex.toString() + ", msg=" + msg);
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * does the actual work of locally running a request. decides on the type
     * and calls the matching method (we do this without java.lang.reflect as it
     * is much faster this way.
     */
    private Object invokeRequest(Request req) {
        switch (req.type) {
        case REQUEST_PUT:
            _put(req.path, req.key, req.value);
            break;
        case REQUEST_REMOVE:
            _remove(req.path, req.key);
            break;
        case REQUEST_SAVE:
            _save(req.path, req.value);
            break;
        case REQUEST_WRAP:
            _wrap(req.path, req.value);
            break;
        case REQUEST_EXECUTE:
            return _execute(req.key, req.value);
        case REQUEST_INTERNAL:
            return _internal(req.key, req.value);
        default:
            logger.error("Request " + req + " as no or an unkown type: " + req.type);
        }
        return null;
    }



    // **************************************
    // method-handlers and callback-functions
    // **************************************

    /**
     * casts a request to all members of the group
     * 
     * @param req Request object
     * @param sendMode static values defined in org.jgroups.blocks.GroupRequest
     */
    private RspList castRequest(Request req, int sendMode) {
        sendMode = (sendMode == 0) ? GroupRequest.GET_ALL : sendMode;
        return disp.castMessage(null, req.getMessage(), sendMode, timeout);
    }

    public Object internal(String type, Vector args) {
        return internal(type, args, GroupRequest.GET_ALL);
    }
    
    /**
     * invokes any kind of internal request in all remote instances
     */
    public Vector internal(String type, Vector args, int sendMode) {
        Request req = new Request(REQUEST_INTERNAL, null, type, args);
        sendMode = (sendMode == 0) ? GroupRequest.GET_ALL : sendMode;
        Util.Watch w = Util.getWatch();
        RspList list = disp.castMessage(null, req.getMessage(), sendMode, timeout);
        if(logger.isDebugEnabled()) logger.debug(w.elapsed() + " Group.internal() type=" + type + ", args=" + args);
        // combine the result-lists from all instances
        Vector results = list.getResults();
        Vector allResults = new Vector();
        for (int i = 0; i < results.size(); i++) {
            allResults.addAll((Vector) results.get(i));
        }
        return allResults;
    }


    /**
     * callback for internal.
     */
    public Object _internal(String type, Serializable args) {
        if(logger.isDebugEnabled()) logger.debug("Group._internal() type=" + type + ", args=" + args);
        Vector reval = new Vector();
        String local = Config.addressToString(channel.getLocalAddress());
        if (type.equals("apps")) {
           // make a list of all apps using this instance
            for (Iterator i = localClients.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                reval.add(key + "@" + local);
            }
        } else if (type.equals("reset")) {
            logger.info("resetting group, request came from " + Util.getFromVector(args, 0));
            objects = new Hashtable();
            reval.add(Boolean.TRUE);
        } else if (type.equals("destroy")) {
            logger.info("destroying group, request came from " + Util.getFromVector(args, 0));
            new Thread(new Scheduler(this, Scheduler.DESTROY, 0)).start();
            reval.add(Boolean.TRUE);
        } else if (type.equals("restart")) {
            logger.info("restarting group, request came from " + Util.getFromVector(args, 0));
            Vector vec = (Vector)args;
            for (int i=1; i<vec.size(); i++) {
                if (channel.getLocalAddress().equals(vec.get(i))) {
                    new Thread(new Scheduler(this, Scheduler.RESTART, i)).start();
                    break;
                }
            }
            reval.add(Boolean.TRUE);
        }
        return reval;
    }

   public Vector execute(String funcName, Vector args) {
        return execute(funcName, args, GroupRequest.GET_ALL, DEFAULT_EXECUTE_TIMEOUT);
    }

    public Vector execute(String funcName, Vector args, long timeout) {
        return execute(funcName, args, GroupRequest.GET_ALL, timeout);
    }

    /**
     * executes a function in all remote instances
     */
    public Vector execute(String funcName, Vector args, int sendMode, long timeout) {
        Request req = new Request(REQUEST_EXECUTE, null, funcName, args);
        sendMode = (sendMode == 0) ? GroupRequest.GET_ALL : sendMode;
        Util.Watch w = Util.getWatch();
        RspList list = disp.castMessage(null, req.getMessage(), sendMode, timeout);
        if(logger.isDebugEnabled()) logger.debug(w.elapsed() + " Group.execute() funcName=" + funcName + ", args=" + args);
        // combine the result-lists from all instances
        Vector results = list.getResults();
        Vector allResults = new Vector();
        for (int i = 0; i < results.size(); i++) {
            allResults.addAll((Vector) results.get(i));
        }
        return allResults;
    }

    /**
     * callback for execute. runs the request against all local clients
     */
    public Object _execute(String funcName, Serializable args) {
        if(logger.isDebugEnabled()) logger.debug("Group._execute() funcName=" + funcName + ", args=" + args);
        Vector reval = new Vector();
        Address local = channel.getLocalAddress();
        // execute in all apps using this group:
        for (Iterator i = localClients.values().iterator(); i.hasNext();) {
            ExecuteHandler ex = (ExecuteHandler) i.next();
            Result result = ex.execute(funcName, (Vector) args);
            result.host = local;
            result.member = Config.addressToString(local);
            reval.add(result);
        }
        return reval;
    }

    public void reset() {
        Util.Watch w = Util.getWatch();
        Vector apps = (Vector) internal("reset", Util.wrapInVector(info.getLocalAddress()), GroupRequest.GET_ALL);
        if(logger.isDebugEnabled()) logger.debug(w.elapsed() + " Group.reset()");
    }

    public void destroy() {
        Util.Watch w = Util.getWatch();
        Vector apps = (Vector) internal("destroy", Util.wrapInVector(info.getLocalAddress()), GroupRequest.GET_ALL);
        if(logger.isDebugEnabled()) logger.debug(w.elapsed() + " Group.destroy()");
    }

    public void restart() {
        Util.Watch w = Util.getWatch();
        Vector args = Util.wrapInVector(info.getLocalAddress());
        args.addAll(membershipMgr.members);
        Vector apps = (Vector) internal("restart", args, GroupRequest.GET_ALL);
        if(logger.isDebugEnabled()) logger.debug(w.elapsed() + " Group.restart()");
    }

    public void save(String path, Serializable value) {
        save(path, value, 0);
    }

    /**
     * Add an object to the group just by defining the path. No check is made
     * wheter this fits in the hierarchy or the children-array is initialized
     * correctly. This should only be used during startup for storing the root
     * object.
     */
    public void save(String path, Serializable value, int sendMode) {
        Request req = new Request(REQUEST_SAVE, path, null, value);
        Util.Watch w = Util.getWatch();
        castRequest(req, sendMode);
        if(logger.isDebugEnabled()) logger.debug(w.elapsed() + " Group.save() path=" + path + ", value=" + value);
    }

    /**
     * Callback method for save.
     * 
     * @see Group#save
     */
    public void _save(String path, Serializable value) {
        if(logger.isDebugEnabled()) logger.debug("Group._save() path=" + path + ", value=" + value);
        if (!(value instanceof GroupObject)) {
            logger.error("Group._save() tried to call _save at " + path + " with non-GroupObject " + value);
            return;
        }
        GroupObject currObj = (GroupObject) value;
        currObj.setState(GroupObject.REPLICATED);
        currObj.setGroup(this);
        objects.put(path, value);
    }

    public void wrap(String path, Serializable value) {
        wrap(path, value, 0);
    }
    
    /**
     * Stores all primitive properties of newObject in this object, but leaves
     * the children intact.
     */
    public void wrap(String path, Serializable value, int sendMode) {
        Request req = new Request(REQUEST_WRAP, path, null, value);
        Util.Watch w = Util.getWatch();
        castRequest(req, sendMode);
        if(logger.isDebugEnabled()) logger.debug(w.elapsed() + " Group.wrap() path=" + path + ", value=" + value);
    }

    /**
     * Callback method for wrap.
     * 
     * @see Group#wrap
     */
    public void _wrap(String path, Serializable value) {
        if(logger.isDebugEnabled()) logger.debug("Group._wrap() path=" + path + ", value=" + value);
        GroupObject currObj = (GroupObject) objects.get(path);
        GroupObject newObj = (GroupObject) value;
        currObj.setProperties(newObj.getProperties());
    }

    /**
     * Add a value to the object defined by path. The callback method will check
     * wether it is added to the child-objects or to the Hashtable of primitive
     * properties. A child of the same name will be deleted recursivly.
     */
    public void put(String path, String key, Serializable value) {
        put(path, key, value, GroupRequest.GET_ALL);
    }

    /**
     * Add a value to the object defined by path. The callback method will check
     * wether it is added to the child-objects or to the Hashtable of primitive
     * properties. A child of the same name will be deleted recursivly. Uses
     * sendMode value to set mode of GroupRequest.
     */
    public void put(String path, String key, Serializable value, int sendMode) {
        Request req = new Request(REQUEST_PUT, path, key, value);
        Util.Watch w = Util.getWatch();
        castRequest(req, sendMode);
        if(logger.isDebugEnabled()) logger.debug(w.elapsed() + " Group.put() path=" + path + ", key=" + key + ", value=" + value + ", sendmode="
                + Util.sendModeToString(sendMode));
    }

   
    /**
     * Callback method for put.
     * 
     * @see Group#put
     */
    public void _put(String path, String key, Serializable value) {
        if(logger.isDebugEnabled()) logger.debug("Group._put() path=" + path + ", key=" + key + ", value=" + value);
        GroupObject currObj = (GroupObject) objects.get(path);
        if (currObj == null) {
            // if the parent is not found log an inconstency and return
            logger.error("Group._put() inconsistency at " + path + "/" + key);
            return;
        }
        if (value instanceof GroupObject) {

            // remove a possible primitive property of that name
            currObj.removeProperty(key);
            // if this object had a child with that name before,
            // delete the whole tree below this object
            if (currObj.hasChild(key)) {
                removeChildren(path, key);
            }

            // set the transient fields of the new object 
            GroupObject newObj = (GroupObject) value;
            newObj.setState(GroupObject.REPLICATED);
            newObj.setGroup(this);

            // add the child to the object store
            objects.put(currObj.getPath() + SEPARATOR + key, newObj);

            // add a reference from the parent to the child
            currObj.addChildReference(key);

        } else {
            currObj._put(key, value);
            if (currObj.hasChild(key)) {
                removeChildren(path, key);
            }
        }
    }

    public void remove(String path, String key) {
        remove(path, key, GroupRequest.GET_ALL);
    }

    
    /**
     * Remove a property <code>key</code> of the object <code>path</code>.
     * Objects and children with that name will be deleted (we do both, for
     * safety reasons).
     */
    public void remove(String path, String key, int sendMode) {
        Request req = new Request(REQUEST_REMOVE, path, key, null);
        Util.Watch w = Util.getWatch();
        castRequest(req, 0);
        if(logger.isDebugEnabled()) logger.debug(w.elapsed() + " Group.remove() path=" + path + ", key=" + key);
    }

    /**
     * Callback method for remove.
     * 
     * @see Group#remove
     */
    public void _remove(String path, String key) {
        if(logger.isDebugEnabled()) logger.debug("Group._remove() path=" + path + ", key=" + key);
        GroupObject currObj = (GroupObject) objects.get(path);
        if (currObj == null) {
            // if the parent is not found log an inconstency and return
            logger.error("Group._remove() inconsistency at " + path + "/" + key);
            return;
        }
        if (currObj.hasProperty(key)) {
            currObj.removeProperty(key);
        } else {
            removeChildren(path, key);
        }
    }

    /**
     * Utility method that removes an object and all its children recursivly.
     */
    private void removeChildren(String path, String key) {
        if(logger.isDebugEnabled()) logger.debug("Group.removeChildren(), parentPath=" + path + ", propertyName=" + key);
        GroupObject currObj = (GroupObject) objects.get(path);
        if (currObj != null) {
            currObj.removeChildReference(key);
            Vector cacheKeys = collectRemoveKeys(new Vector(), path + SEPARATOR + key);
            for (Enumeration e = cacheKeys.elements(); e.hasMoreElements(); ) {
                String cacheKey = (String) e.nextElement();
                objects.remove(cacheKey);
            }
        }
    }

        
    private Vector collectRemoveKeys(Vector collector, String path) {
        if(logger.isDebugEnabled()) logger.debug("Group.removeDeep, path=" + path);
        GroupObject currObj = (GroupObject) objects.get(path);
        if (currObj == null) {
            return collector;
        } else {
            collector.add(path);
        }
        Enumeration e = currObj.children();
        while (e.hasMoreElements()) {
            collectRemoveKeys(collector, path + SEPARATOR + (String) e.nextElement());
        }
        return collector;
    }

    
    
    // ************************************
    // object retrieval functions
    // ************************************

    /**
     * @return the object storeage
     */
    public Hashtable getObjects() {
        return objects;
    }

    /**
     * @return the number of objects in the whole tree
     */
    public int size() {
        return objects.size();
    }

    /**
     * @return true if a GroupObject with the given path exists in the tree. -
     * the same as containsKey()
     */
    public boolean exists(String path) {
        return objects.containsKey(path);
    }

    /**
     * Gets a GroupObject from the tree and sets its transient fields
     * 
     * @param path path of the parent object
     * @param key name of the child
     * @return GroupObject or null if not found.
     */
    public GroupObject get(String path, String key) {
        return get(path + SEPARATOR + key);
    }

    /**
     * Gets a GroupObject from the tree and sets its transient fields
     * 
     * @param path path of the object
     * @return GroupObject or null if not found.
     */
    public GroupObject get(String path) {
        try {
            return (GroupObject) objects.get(path);
        } catch (ClassCastException e) {
            logger.error("Group.get(): illegal content in the group (" + path + "): " + e.toString());
            return null;
        }
    }

    /**
     * Gets the names of the children of a GroupObject specified by path.
     * 
     * @param path path to the object.
     * @return Enumeration with the names of the children
     */
    public Enumeration getChildrenNames(String path) {
        GroupObject object = (GroupObject) objects.get(path);
        return object.children();
    }

    /**
     * Gets the root object. Object will be created if necessary.
     */
    public GroupObject getRoot() {
        GroupObject root;
        if (!objects.containsKey("")) {
            root = new GroupObject(this, "");
            root.setAsRoot(true);
            root.save();
        } else {
            root = get("");
        }
        return root;
    }

    /**
     * for testing. starts a Group with a properties file
     */
    public static void main(String args[]) throws Exception {
        String props = null;
        int port = 0;
        String allowed = null;
        String todo = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-props")) {
                props = args[++i];
            } else if (args[i].equals("-port")) {
                port = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-allowed")) {
                allowed = args[++i];
            } else if (args[i].equals("-todo")) {
                todo = args[++i];
            }
        }
        if (props == null) {
            System.out.println("usage:");
            System.out.println("helma.extensions.helmagroups.Group -props <propfile> -port <debugPort> -allowed <debugAddresses>");
            System.exit(1);
        }
        Config config = Config.getConfig(props, null);
        Group g = Group.newInstance(config);
        if (port != 0 && allowed != null) {
            g.startDebugger(port, allowed);
        }
    }

}

