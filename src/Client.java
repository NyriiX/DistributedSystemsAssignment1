import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class Client extends Thread {
    private int id;
    private Network network;
    private LinkedList<HashMap> message_queue;
    private boolean isRunning;
    private Random random;

    Client(int id, Network network) {
        this.id = id;
        this.network = network;
        this.message_queue = new LinkedList<>();
        this.isRunning = true;

        this.random = new Random();
    }

    @Override
    public void run() {
        while (this.isRunning()) {
            this.doSomethingInCriticalBlock();

            this.doSomethingNotCritical();
        }
    }

    void doSomethingNotCritical() {
        // Simulate doing something outside of the critical block
        try {
            long timeout = (long) (this.random.nextDouble() * 1000);
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void doSomethingInCriticalBlock() {
        System.out.println("Client " + this.id + " ENTERED critical block.");

        // Simulate doing something in the critical block
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
