public class Message implements Comparable<Message> {
    private final int senderID;
    private long timeStamp;
    private final MessageType messageType;

    Message(int senderID, long timeStamp, MessageType messageType) {
        this.senderID = senderID;
        this.timeStamp = timeStamp;
        this.messageType = messageType;
    }

    // Comparator for messages using the extended lamport time
    // Primarily sort by timestamp, if equal compare sender ids
    @Override
    public int compareTo(Message o) {

        if (this.timeStamp < o.timeStamp) {
            return -1;
        } else if (this.timeStamp > o.timeStamp) {
            return 1;
        } else {
            if (this.senderID < o.senderID) {
                return -1;
            } else if (this.senderID > o.senderID) {
                return 1;
            } else {
                return 1;
            }
        }
    }

    public int getSenderID() {
        return senderID;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public String toString() {
        return "M{senderID=" + this.getSenderID() + ", timeStamp=" + this.getTimeStamp() +
                ", messageType=" + this.getMessageType() + '}';
    }
}

class UnicastMessage extends Message {
    private final int receiverID;

    UnicastMessage(int id_sender, long timeStamp, MessageType messageType, int id_receiver) {
        super(id_sender, timeStamp, messageType);
        this.receiverID = id_receiver;
    }

    public int getReceiverID() {
        return receiverID;
    }

    @Override
    public String toString() {
        return "M{senderID=" + this.getSenderID() + ", timeStamp=" + this.getTimeStamp() +
                ", messageType=" + this.getMessageType() + ", receiverID=" + this.getMessageType() + '}';
    }
}

class MulticastMessage extends Message {

    MulticastMessage(int id_sender, long timeStamp, MessageType messageType) {
        super(id_sender, timeStamp, messageType);
    }
}


enum MessageType {
    REQUEST, ACK, RELEASE
}