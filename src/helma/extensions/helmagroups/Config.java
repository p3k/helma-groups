package helma.extensions.helmagroups;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jgroups.Address;
import org.jgroups.conf.ProtocolData;
import org.jgroups.conf.ProtocolParameter;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.conf.XmlConfigurator;
import org.jgroups.stack.IpAddress;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Config implements GroupConstants {

    public String groupName = null;
    
    public String localName = null;
    
    public XmlConfigurator xmlConfig = null;

    /**
     * method creating a config object with XmlConfigurator etc
     * 
     * @param configValue String is interpreted as an URL, as a full filepath or
     *        as a file in some directory
     * @param hint File object denoting a directory where config files are
     *        placed.
     * @return Config object with parsed XmlConfigurator, groupName and localName
     */
    public static Config getConfig(String configValue, File hint) throws Exception {
        Config config = new Config();
        config.xmlConfig = null;
        String groupname = null;
        if (configValue.startsWith("http://") || configValue.startsWith("file://")) {
            URL url = new URL(configValue);
            config.xmlConfig = XmlConfigurator.getInstance(url);
            config.groupName = getGroupnameFromXmlStream(url.openStream());
        } else {
            // first try if value is a full file path:
            File f = new File(configValue);
            if (!f.exists()) {
                // if value doesn't exist, prefix it with file hint (helma home directory etc)
                f = new File(hint, configValue);
            }
            config.xmlConfig = XmlConfigurator.getInstance(new FileInputStream(f));
            config.groupName = getGroupnameFromXmlStream(new FileInputStream(f));
        }
        config.localName = guessLocalName(null);
        return config;
    }

    /**
     * tries to get the local name of this group either from JVM system property
     * helmagroup.localname or from the same property in helma.main.Server
     */
    public static String guessLocalName(String def) {
        String t = System.getProperty("helmagroup.localname");
        if (t != null && !"".equals(t.trim())) {
            return t;
        }
        try {
            Class cl = Class.forName("helma.main.Server");
            Method m = cl.getMethod("getServer", new Class[0]);
            Object obj = m.invoke(null, new Object[0]);
            if (obj != null) {
                helma.main.Server srv = (helma.main.Server) obj;
                t = srv.getProperty("helmagroup.localname");
                if (t != null && !"".equals(t.trim())) {
                    return t;
                }
            }
        } catch (Exception e) {
            // class not found
        } catch (NoClassDefFoundError err) {
            // missing a library
        }
        return def;
    }
    
    /**
     * method constructing an identifier from a configurator object:
     * [localName@][groupName]-[protocol UDP|TCP]-[ipaddress]:[port]
     */
    public static String getIdentifier(Config config) {
        String transport = getTransport(config.xmlConfig);
        return renderIdentifier(config.localName, config.groupName, transport);
    }

    /**
     * method rendering an identifier from given values:
     * [localName@][groupName]-[transport]
     */
    public static String renderIdentifier(String localName, String groupName, String transport) {
        return ((localName != null) ? localName + "@" : "") + groupName + "-" + transport;
    }


    /**
     * renders an Address object to a string 
     */
    public static String addressToString(Address addr) {
        if (addr instanceof IpAddress) {
            IpAddress ipAddr = (IpAddress) addr;
            String reval = ipAddr.getIpAddress().getHostName() + ":" + ipAddr.getPort();
            if (ipAddr.getAdditionalData() != null) {
                byte[] addBytes = ipAddr.getAdditionalData();
                String instanceName = null;
                try {
                    instanceName = (String) org.jgroups.util.Util.objectFromByteBuffer(addBytes);
                } catch (Exception anything) {}
                if (instanceName != null) {
                    reval = instanceName + ":" + reval;
                }
            }
            return reval;
        } else if (addr != null) {
            return addr.toString();
        } else {
            return "";
        }
    }

    
    /**
     * extracts the transport string from the given protocol stack
     */
    public static String getTransport(ProtocolStackConfigurator config) {
        // find the most important values of the transport config:
        ProtocolData[] proto = config.getProtocolStack();
        for (int i = 0; i < proto.length; i++) {
            if (proto[i].getClassName().endsWith("UDP")) {
                String ip = (String) ((ProtocolParameter) proto[i].getParameters().get("mcast_addr")).getValue();
                String port = (String) ((ProtocolParameter) proto[i].getParameters().get("mcast_port")).getValue();
                return "UDP-" + ip + ":" + port;
            } else if (proto[i].getClassName().endsWith("TCP")) {
                String port = (String) ((ProtocolParameter) proto[i].getParameters().get("start_port")).getValue();
                try {
                    InetAddress local = InetAddress.getLocalHost();
                    return "TCP-" + local.getHostAddress() + ":" + port;
                } catch (UnknownHostException unknown) {
                    System.err.println("couldn't fetch local host");
                    unknown.printStackTrace();
                    return "TCP-127.0.0.1:" + port;
                }
            }
        }
        return "no-transport";
    }

    /**
     * method parsing xml inputstream and extracting groupname from root config element
     */
    private static String getGroupnameFromXmlStream(InputStream in) {
        try {
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
            factory.setValidating(false); //for now
            DocumentBuilder builder=factory.newDocumentBuilder();
            Document document=builder.parse(in);
            Element configElement = document.getDocumentElement();
            return configElement.getAttribute("groupname");
        } catch(Exception x) {
            // return default value
            return DEFAULT_GROUPNAME;
        }
    }
    
 
    public Config() {
        groupName = DEFAULT_GROUPNAME;
        localName = "helma";
    }
    
    public boolean isValid() {
        return (xmlConfig != null);
    }

    public String toString() {
        return "[Config " + localName + "/" + groupName + "/" + xmlConfig + "]";
    }
}
