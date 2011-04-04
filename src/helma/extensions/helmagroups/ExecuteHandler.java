package helma.extensions.helmagroups;

import java.util.Vector;

/**
 * defines an interface that handles method calls received by a Group
 */

public interface ExecuteHandler {

    public Result execute(String funcName, Vector args);

}

