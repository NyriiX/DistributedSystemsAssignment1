import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public class LamportLock {
    private final int id;
    private final Network network;
    private final LinkedBlockingQueue<Message> messageQueue;
    private final LinkedList<Message> requestQueue;
    private long local_lamport_time;
    private boolean[] acksReceived;

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

                // Handle message based on type corresponding to the lamport algorithm
                if (message.getMessageType() == MessageType.REQUEST) {
                    this.handleIncomingRequest(message);
                } else if (message.getMessageType() == MessageType.RELEASE) {
                    this.removeRequest(message.getSenderID());
                } else if (message.getMessageType() == MessageType.ACK) {

                    // Only shut down message contains a negative sender id
                    if (message.getSenderID() < 0) {
                        return;
                    }

                    this.acksReceived[message.getSenderID()] = true;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendRequest() {
        // Reset the array where the received acks are saved
        this.acksReceived = new boolean[Simulator.NUMBER_CLIENTS];
        this.acksReceived[this.id] = true;

        // Create request, append to own and send to other clients via a multicast message
        this.increaseLocalTime();
        MulticastMessage request = new MulticastMessage(this.id, this.getCurrentLocalTime(), MessageType.REQUEST);
        this.addRequest(request);
        this.logState("Sending request: " + request);
        this.network.sendMessage(request);
    }

    void addRequest(Message request) {
        // Add new request to queue and sort using the message comparator
        // which implements the extended lamport time
        this.requestQueue.add(request);
        Collections.sort(this.requestQueue);
    }


    // checks if client is allowed to access (all acks received + first in request queue)
    boolean accessGranted() {
        // peak instead of pop, so we dont remove the element from the queue
        Message currentFirstRequest = this.requestQueue.peekFirst();

        // peak may return null if request queue is empty
        if (currentFirstRequest == null) {
            return false;
        }

        boolean myRequestIsFirst = currentFirstRequest.getSenderID() == this.id;
        boolean allAcksReceived = allTrueInArray(this.acksReceived);

        this.logState("Current access state: allAcksReceived=" + allAcksReceived
                + "\tmyRequestIsFirst=" + myRequestIsFirst + "\tFirst: " + currentFirstRequest);
        return (myRequestIsFirst && allAcksReceived);
    }

    private static boolean allTrueInArray(boolean[] array) {
        for (boolean b : array) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    private void handleIncomingRequest(Message request) {
        this.addRequest(request);

        // Send event resulting from the request must increase the local time again
        this.increaseLocalTime();
        Message ack = new UnicastMessage(this.id, this.getCurrentLocalTime(), MessageType.ACK, request.getSenderID());
        this.logState("Sending request: " + request);
        this.network.sendMessage(ack);
    }


    void release() {
        // Lock was released by the main application
        // Remove own request from the request queue and send a release message to other clients
        this.removeRequest(this.id);
        this.sendRelease();
    }

    private void sendRelease() {
        this.increaseLocalTime();
        MulticastMessage release = new MulticastMessage(this.id, this.getCurrentLocalTime(), MessageType.RELEASE);
        this.logState("Sending release: " + release);
        this.network.sendMessage(release);
    }

    private void removeRequest(int requester_id) {
        for (Message request : this.requestQueue) {
            // Break after first so only first is removed, not possible future requests from the same source
            if (request.getSenderID() == requester_id) {
                this.logState("Removing: " + request);
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

    // adjustment of the local time to a timestamp received via a message
    private void matchAndIncLocalTime(long transmitted_timestamp) {
        this.local_lamport_time = Math.max(this.getCurrentLocalTime(), transmitted_timestamp) + 1;
    }

    void increaseLocalTime() {
        this.local_lamport_time++;
    }

    long getCurrentLocalTime() {
        return this.local_lamport_time;
    }

    private void logState(String logEntry) {
        if (Simulator.DEBUGGING_OUTPUT_ENABLED) {
            System.out.println("Client " + this.id + " @ " + this.getCurrentLocalTime() + ": " + logEntry);
        }
    }
}
