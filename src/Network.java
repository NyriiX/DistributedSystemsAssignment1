public class Network extends Thread {
    private boolean isRunning;

    Network() {

    }

    @Override
    public void run() {

    }

    synchronized void stopRunning() {
        this.isRunning = false;
    }

    synchronized boolean isRunning() {
        return this.isRunning;
    }

}
