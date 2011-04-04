package helma.extensions.helmagroups.debug;

import helma.extensions.helmagroups.Config;
import helma.extensions.helmagroups.Group;
import helma.extensions.helmagroups.GroupObject;
import helma.extensions.helmagroups.Util;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * class used benchmark a group configuration.
 * it starts several threads which fire requests against the group
 * and prints throughput stats to a PrintWriter. 
 */

public class Benchmarker {

    PrintWriter writer;
    Group g;

    // number of threads started
    int numOfThreads;

    // number of objects sent per thread
    int numOfObjects;

    // length of propertystring in sent objects 
    int sizeOfObjects;
    
    // GroupRequest.GET_ALL, GroupRequest.GET_FIRST etc
    int sendMode;
    
    // pause between requests
    int pause;
    
    long startTime;
    
    // interrupted by the last thread that finishes
    Thread mainThread;

    // list of threads
    Vector runners;

    // each created object is assigned a unique key so that we can find the objects in logfiles
    static int objectsPassedThrough = 0; 
    
    public Benchmarker(PrintWriter writer, Group g, int threads, int objects, int size, int pause, String sendMode) {
        this.writer         = writer;
        this.g              = g;
        this.numOfThreads   = threads;
        this.numOfObjects   = objects;
        this.sizeOfObjects  = size;
        this.pause          = pause;
        this.sendMode       = Util.sendModeToInt(sendMode);
    } 

    /*
     * runs the actual test, returns the elapsed millis
     */
    public long test() {
        mainThread = Thread.currentThread();
        prepare();
        startTime = System.currentTimeMillis();
        startTest();
        while(true) {
            // print current stats once every second until all threads are done 
            try {
                long diff = System.currentTimeMillis() - startTime;
                Thread.sleep(1000);
            } catch (Exception anything) {}
            int sum = 0;
            for (int i=0; i<runners.size(); i++) {
                BenchmarkRunner runner = (BenchmarkRunner)runners.get(i);
                sum += runner.objectsSent;
            }
            double millis = elapsed();
            writer.println("sent " + sum + " objects in " + (millis/1000) + " seconds (" + Math.floor(sum/(millis/1000)) + " avg/s)");
            writer.flush();
            if (checkRunning()==0) {
                break;
            }
        }
        writer.println("done");
        writer.flush();
        runners.removeAllElements();
        runners = null;
        return System.currentTimeMillis() - startTime;
    }

    /**
     * returns the number of threads still active
     */
    private int checkRunning() {
        int runningThreads = 0;
        for (int i=0; i<runners.size(); i++) {
            BenchmarkRunner runner = (BenchmarkRunner)runners.get(i);
            if (runner.running == true) {
                runningThreads++;
            }
        }
        return runningThreads;
    }

    /**
     * returns the elapsed millis since start of the test
     */
    private long elapsed() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * prepares the test threads and makes them create the objects
     */
    private void prepare() {
        runners = new Vector();
        for (int i=0; i<numOfThreads; i++) {
            BenchmarkRunner runner = new BenchmarkRunner();
            runners.add(runner);
            runner.prepare();
        }
    }
    
    /**
     * loops though all threads and makes them start the test
     */
    private void startTest() {
        for (int i=0; i<runners.size(); i++) {
            BenchmarkRunner runner = (BenchmarkRunner)runners.get(i);
            runner.start();
        }
    }

    private static char raw[] = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    /**
      * creates a random string of a given length 
      */
    private static String randomString(int len) {
        StringBuffer buf = new StringBuffer();
        for (int j=0; j<len; j++) {
            int rnd = (int) Math.floor(Math.random()*26);
            buf.append(raw[rnd]);
        }
        return buf.toString();
    }

    
    private class BenchmarkRunner extends Thread {
        int objectsSent;
        boolean running;
        Hashtable objects;
        
        public BenchmarkRunner() {
            running = false;
            objects = new Hashtable();
            objectsSent = 0;
        }
        
        public void run() {
            running = true;
            objectsSent = 0;
            for(Enumeration e = objects.keys(); e.hasMoreElements(); ) {
                String key = (String)e.nextElement();
                GroupObject obj = (GroupObject) objects.get(key);
                long beforePut = System.currentTimeMillis();
                String key2 = key + objectsPassedThrough++;
                g.getRoot().put(key2, obj, sendMode);
                if ((System.currentTimeMillis()-beforePut) > 500) {
                    writer.println("sending " + key2 + " took " + (System.currentTimeMillis()-beforePut) + "ms");
                }
                objectsSent++;
                long sleep = pause + ((long)Math.floor(Math.random()*100) - 50);
                if (sleep > 0) {
                    try {
                        Thread.sleep (sleep);
                    } catch (InterruptedException ie) {
                    }
                }
            }
            running = false;
            if (checkRunning()==0) {
                mainThread.interrupt();
            }
        }

        /**
         * prepares the GroupObjects 
         */
        private void prepare() {
            for (int i=0; i<numOfObjects; i++) {
                GroupObject obj = new GroupObject();
                obj.put("data", randomString(sizeOfObjects));
                objects.put(randomString(10), obj);
            }
        }
    }

    /**
     * for testing: creates group and runs a tiny benchmark test against it 
     */
    public static void main(String args[]) throws Exception {
        String props = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-props")) {
                props = args[++i];
                continue;
            }
        }
        Config config = Config.getConfig(props, null);
        Group g = Group.newInstance(config);
        g.getRoot().put("test", new GroupObject());

        System.out.println("testing...");
        PrintWriter writer = new PrintWriter(System.out);
        Benchmarker benchmarker = new Benchmarker(writer, g, 2, 100, 50, 1000, "all");
        long time = benchmarker.test();
        System.out.println("test took " + time + " millis");
    }
    

    
}
