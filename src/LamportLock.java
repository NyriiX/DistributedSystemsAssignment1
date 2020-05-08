import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public class LamportLock {
    private final int id;
    private final Network network;
    final LinkedBlockingQueue<Message> messageQueue;
    final LinkedList<Message> requestQueue;
    long local_lamport_time;
    boolean[] acksReceived;

    LamportLock(int id, Network network) {
        this.id = id;
        this.network = network;
        this.local_lamport_time = 0;
        this.requestQueue = new LinkedList<>();

        /*
        BlockedLinkedList is used as an abstraction of a background process,
        which receives messages in parallel over the network and
        stores them temporarily until they are processed.
         */
        this.messageQueue = new LinkedBlockingQueue<>();

        this.network.register(this, id);
    }

    public void acquire() {
        this.sendRequest();

        while (!accessGranted()) {
            try {
                Message message = this.messageQueue.take();

                // Receiving event --> Adjustment of local time
                this.matchAndIncLocalTime(message.getTimeStamp());

                if (message.getMessageType() == MessageType.REQUEST) {
                    this.handleIncomingRequest(message);
                } else if (message.getMessageType() == MessageType.RELEASE) {
                    this.removeRequest(message.getSenderID());
                } else if (message.getMessageType() == MessageType.ACK) {
                    this.acksReceived[message.getSenderID()] = true;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // TODO Find a better way to get the number of clients in the network
    void sendRequest() {
        this.acksReceived = new boolean[Simulator.NUMBER_CLIENTS];
        this.acksReceived[this.id] = true;
        this.increaseLocalTime();
        MulticastMessage request = new MulticastMessage(this.id, this.getCurrentLocalTime(), MessageType.REQUEST);
        this.addRequest(request);

        //System.out.println(id + " sending request: " + request);
        this.network.sendMessage(request);

        //System.out.println(id + " Sending request now at " + this.getCurrentLocalTime());
    }

    void addRequest(Message request) {
        this.requestQueue.add(request);
        Collections.sort(this.requestQueue);
    }


    // checks if client is allowed to access (all acks received + first in request queue)
    boolean accessGranted() {
        // peak instead of pop, so we dont remove the element from the queue
        Message currentFirstRequest = this.requestQueue.peekFirst();

        if (currentFirstRequest == null) {
            return false;
        }

        boolean myRequestIsFirst = currentFirstRequest.getSenderID() == this.id;
        boolean allAcksReceived = areAllTrue(this.acksReceived);

        //System.out.println(id + " current access state: " + allAcksReceived + " " + currentFirstRequest + " at: " + this.getCurrentLocalTime());
        /*
        for (Message r : this.requestQueue) {
            System.out.println("\t" + id + " :" + r.toString());
        }
        System.out.println("\t" + id + " :" + "----------------------");
        */
        return (myRequestIsFirst && allAcksReceived);
    }

    static boolean areAllTrue(boolean[] array) {
        for (boolean b : array) if (!b) return false;
        return true;
    }

    void handleIncomingRequest(Message request) {
        this.addRequest(request);

        // Send event resulting from the request must increase the local time again
        this.increaseLocalTime();
        Message ack = new UnicastMessage(this.id, this.getCurrentLocalTime(), MessageType.ACK, request.getSenderID());
        //System.out.println(id + " sending ack: " + ack + " at: " + this.getCurrentLocalTime());
        this.network.sendMessage(ack);
    }


    void release() {
        // Lock was released by the main application
        // Remove own request from the request queue and send a release message to other clients
        this.removeRequest(this.id);
        this.sendRelease();
        //System.out.println(id + " lock was released");
    }

    void sendRelease() {
        this.increaseLocalTime();
        MulticastMessage release = new MulticastMessage(this.id, this.getCurrentLocalTime(), MessageType.RELEASE);
        //System.out.println(id + " sending release: " + release + " at: " + this.getCurrentLocalTime());
        this.network.sendMessage(release);
    }

    void removeRequest(int requester_id) {
        for (Message request : this.requestQueue) {
            // Break after first so only first is remove, not possible future requests from the same source
            if (request.getSenderID() == requester_id) {
                //System.out.println(id + " removing: " + request + " at: " + this.getCurrentLocalTime());
                this.requestQueue.remove(request);
                break;
            }
        }
    }

    // method simulation that a new message was send to this client via the network
    void sendToThisClient(Message message) {
        try {
            this.messageQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void increaseLocalTime() {
        this.local_lamport_time++;
    }

    void matchAndIncLocalTime(long transmitted_timestamp) {
        this.local_lamport_time = Math.max(this.getCurrentLocalTime(), transmitted_timestamp) + 1;
    }

    long getCurrentLocalTime() {
        return this.local_lamport_time;
    }
}
