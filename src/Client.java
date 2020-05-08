import java.util.Random;

public class Client extends Thread {
    private final int id;
    private boolean isRunning;
    private final LamportLock lock;
    private final Random random;

    Client(int id, Network network) {
        this.id = id;
        this.isRunning = true;
        this.lock = new LamportLock(id, network);
        this.random = new Random(id);
    }

    @Override
    public void run() {
        while (this.isRunning()) {
            this.doSomethingNotCritical();
            this.lock.acquire();
            this.doSomethingInCriticalBlock();
            this.lock.release();
        }
    }

    // Simulate doing something outside of the critical block
    private void doSomethingNotCritical() {
        this.lock.increaseLocalTime();

        try {
            long timeout = (long) (this.random.nextDouble() * 1000);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Simulate doing something in the critical block
    private void doSomethingInCriticalBlock() {
        this.lock.increaseLocalTime();
        System.out.println("Client " + this.id + " ENTERED critical block.");
        try {
            long timeout = (long) (this.random.nextDouble() * 1000);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Client " + this.id + " LEFT critical block.");
    }

    synchronized void stopRunning() {
        this.isRunning = false;
    }

    synchronized boolean isRunning() {
        return this.isRunning;
    }
}
