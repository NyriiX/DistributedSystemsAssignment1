import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Network extends Thread {
    private final LinkedBlockingQueue<Message> messageInbox;
    private final HashMap<Integer, LamportLock> clientList;
    private final Object clientListAccess = new Object();
    private boolean isRunning;

    Network() {
        this.messageInbox = new LinkedBlockingQueue<>();
        this.clientList = new HashMap<>();
        this.isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning()) {
            try {
                Message receivedMessage = this.messageInbox.take();
                synchronized (clientListAccess) {
                    // transmit incoming messages based on their class
                    // unicasts are send to the specified receiver, multicasts to all registered clients
                    if (receivedMessage instanceof UnicastMessage) {
                        int rid = ((UnicastMessage) receivedMessage).getReceiverID();

                        if (clientList.containsKey(rid)) {
                            clientList.get((rid)).sendToThisClient(receivedMessage);
                        } else {
                            System.out.println("Unknown receiver: " + rid);
                        }

                    } else if (receivedMessage instanceof MulticastMessage) {
                        for (Integer id : this.clientList.keySet()) {

                            if (id != receivedMessage.getSenderID()) {
                                clientList.get(id).sendToThisClient(receivedMessage);
                            }
                        }
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void sendMessage(Message message) {
        try {
            this.messageInbox.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void register(LamportLock client, Integer ID) {
        synchronized (this.clientListAccess) {
            this.clientList.put(ID, client);
            System.out.println("Network: Registered client with ID " + ID);
        }
    }

    synchronized void stopRunning() {
        this.isRunning = false;
    }

    synchronized boolean isRunning() {
        return this.isRunning;
    }

}
