package helma.extensions.helmagroups;

import java.util.HashMap;
import helma.scripting.ScriptingEngine;
import helma.extensions.*;

public abstract class ScriptingExtension implements GroupConstants {

    /**
     * base class for all scripting extensions
     */

    public abstract HashMap init(ApplicationLink appLink, ScriptingEngine engine) throws ConfigurationException;

}