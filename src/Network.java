import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
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
        System.out.println("Network waiting for messages to forward ...");
        while (isRunning()) {
            try {
                Message receivedMessage = this.messageInbox.take();
                System.out.println("Forwarding: " + receivedMessage.toString());
                synchronized (clientListAccess) {
                    if (receivedMessage instanceof UnicastMessage) {
                        int rid = ((UnicastMessage) receivedMessage).id_receiver;

                        if (clientList.containsKey(rid)) {
                            clientList.get((rid)).sendToThisClient(receivedMessage);
                        } else {
                            System.out.println("Unknown receiver: " + rid);
                        }


                    } else if (receivedMessage instanceof MulticastMessage) {
                        for (Integer id : this.clientList.keySet()) {

                            if (id != receivedMessage.id_sender) {
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
            System.out.println("Registered client with id" + ID);
        }
    }

    synchronized void stopRunning() {
        this.isRunning = false;
    }

    synchronized boolean isRunning() {
        return this.isRunning;
    }

}
