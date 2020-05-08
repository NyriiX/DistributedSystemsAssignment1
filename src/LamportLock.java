import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

public class LamportLock {
    private final int id;
    private final Network network;
    private final LinkedBlockingQueue<Message> messageQueue;
    private final LinkedList<Message> requestQueue;
    private long local_lamport_time;
    private Boolean[] acksReceived;

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
                this.matchAndIncLocalTime(message);

                if (message.messageType == MessageType.REQUEST) {
                    this.handleRequest(message);
                } else if (message.messageType == MessageType.RELEASE) {
                    this.removeRequest(message.id_sender);
                } else if (message.messageType == MessageType.ACK) {
                    this.acksReceived[message.id_sender] = true;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // TODO Find a better way to get the number of clients in the network
    private void sendRequest() {
        this.acksReceived = new Boolean[Simulator.NUMBER_CLIENTS];
        this.acksReceived[this.id] = true;
        this.increaseLocalTime();
        Message request = new MulticastMessage(this.id, this.getCurrentLocalTime(), MessageType.REQUEST);
        this.addRequest(request, true);
        this.network.sendMessage(request);
    }

    private void addRequest(Message request, boolean setLocalTime) {
        if (setLocalTime) {
            request.localTimeStamp = this.getCurrentLocalTime();
        }

        this.requestQueue.add(request);
        Collections.sort(this.requestQueue);
    }


    // checks if client is allowed to access (all acks received + first in request queue)
    private boolean accessGranted() {
        Message currentFirstRequest = this.requestQueue.peekFirst();

        if (currentFirstRequest == null) {
            return false;
        }

        boolean myRequestIsFirst = currentFirstRequest.id_sender == this.id;
        boolean allAcksReceived = Arrays.stream(this.acksReceived).allMatch(Boolean::valueOf);
        return (myRequestIsFirst && allAcksReceived);
    }

    private void handleRequest(Message request) {
        // local time in request is set before via matchAndIncLocalTime
        this.addRequest(request, false);
        Message ack = new UnicastMessage(this.id, this.getCurrentLocalTime(), MessageType.ACK, request.id_sender);
        this.network.sendMessage(ack);
    }


    public void release() {
        // Lock was released by the main application
        // Remove own request from the request queue and send a release message to other clients
        this.removeRequest(this.id);
        this.sendRelease();
    }

    private void sendRelease() {
        this.increaseLocalTime();
        Message release = new MulticastMessage(this.id, this.getCurrentLocalTime(), MessageType.RELEASE);
        this.network.sendMessage(release);
    }

    private void removeRequest(int requester_id) {
        for (Message request : this.requestQueue) {
            // Break after first so only first is remove, not possible future requests from the same source
            if (request.id_sender == requester_id) {
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

    // used when receiving any kind of message
    // adjusting the local time and save it in the message (only required for request messages)
    private void matchAndIncLocalTime(Message message) {
        this.local_lamport_time = Math.max(this.local_lamport_time, message.senderTimeStamp) + 1;
        message.localTimeStamp = this.local_lamport_time;
    }

    private long getCurrentLocalTime() {
        return this.local_lamport_time;
    }
}
