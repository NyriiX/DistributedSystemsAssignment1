import java.util.ArrayList;

public class Simulator {
    final static int NUMBER_CLIENTS = 2;
    private final static int RUN_TIME_IN_SECS = 10;
    private final ArrayList<Client> clients;

    Simulator() {
        this.clients = new ArrayList<>();
    }

    void run() {

        // Initialise and start clients as well as the network thread
        Network network = new Network();

        for (int i = 0; i < NUMBER_CLIENTS; i++) {
            Client client = new Client(i, network);
            clients.add(client);
        }

        network.start();
        for (Client client : this.clients) {
            client.start();
        }

        // Let the simulation run for the configured duration, then stop the clients and the network threads
        try {
            Thread.sleep(RUN_TIME_IN_SECS * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Stopping communication now ...");
        for (Client client : this.clients) {
            client.stopRunning();
        }

        network.stopRunning();
    }

}


