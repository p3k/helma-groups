package helma.extensions.helmagroups;

public class Scheduler implements Runnable {

    public static final int DESTROY = 1;
    public static final int RESTART = 2;
    
    Group g;
    int todo;
    int multiplier;

    public Scheduler(Group g, int todo, int multiplier) {
        this.g = g;
        this.todo = todo;
        this.multiplier = multiplier;
    }

    public void run() {
        if (todo == DESTROY) {
            try {
                Thread.sleep(3000);
                g.disconnect();
            } catch (InterruptedException i) {
            }
        } else if (todo == RESTART) {
            try {
                Thread.sleep(3000);
                g.disconnect();
            } catch (InterruptedException i) {
            }
            try {
                Thread.sleep(10000 + (multiplier * 5000));
                g.connect();
            } catch (InterruptedException i) {
            }
        }
    }

}
