import java.util.Random;

public class Client extends Thread {
    private final int id;
    private boolean isRunning;
    final LamportLock lock;
    private final Random random;

    Client(int id, Network network) {
        this.id = id;
        this.isRunning = true;
        this.lock = new LamportLock(id, network);
        this.random = new Random(id);
    }

    // TODO Add back nonCritical
    @Override
    public void run() {
        while (this.isRunning()) {
            //this.doSomethingNotCritical();
            this.lock.acquire();
            this.doSomethingInCriticalBlock();
            this.lock.release();
        }
    }

    // Simulate doing something outside of the critical block
    void doSomethingNotCritical() {
        try {
            long timeout = (long) (this.random.nextDouble() * 3000);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.lock.increaseLocalTime();
    }

    // Simulate doing something in the critical block
    void doSomethingInCriticalBlock() {
        System.out.println(this.id + " Client " + this.id + " ENTERED critical block at " + lock.getCurrentLocalTime());
        try {
            long timeout = (long) (this.random.nextDouble() * 1000);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.lock.increaseLocalTime();
        System.out.println(this.id + " Client " + this.id + " LEFT critical block at " + lock.getCurrentLocalTime());
    }

    synchronized void stopRunning() {
        this.isRunning = false;
    }

    synchronized boolean isRunning() {
        return this.isRunning;
    }
}
