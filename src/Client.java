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

            // Only enter critical block if program wasn't stopped while waiting for the lock acquisition
            if (this.isRunning()) {
                this.doSomethingInCriticalBlock();
            }

            this.lock.release();
        }
    }

    // Simulate doing something outside of the critical block
    void doSomethingNotCritical() {
        try {
            long timeout = (long) (this.random.nextDouble() * Simulator.MAX_BLOCK_DURATION_IN_SECS * 1000);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.lock.increaseLocalTime();
    }

    // Simulate doing something in the critical block
    void doSomethingInCriticalBlock() {
        System.out.println("Client " + this.id + " @ " + this.lock.getCurrentLocalTime() + ": ENTERED critical block");

        try {
            long timeout = (long) (this.random.nextDouble() * Simulator.MAX_BLOCK_DURATION_IN_SECS * 1000);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.lock.increaseLocalTime();
        System.out.println("Client " + this.id + " @ " + this.lock.getCurrentLocalTime() + ": LEFT critical block");
    }

    synchronized void stopRunning() {
        this.isRunning = false;
        // Send message to lock to ensure it won't stall waiting for a message after shutdown
        this.lock.sendToThisClient(new Message(-999, -999, MessageType.ACK));
    }

    synchronized boolean isRunning() {
        return this.isRunning;
    }
}
