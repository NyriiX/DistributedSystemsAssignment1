public class Message implements Comparable<Message> {
    public final int id_sender;
    public final long senderTimeStamp;
    public final MessageType messageType;
    public long localTimeStamp;

    Message(int id_sender, long senderTimeStamp, MessageType messageType) {
        this.id_sender = id_sender;
        this.senderTimeStamp = senderTimeStamp;
        this.messageType = messageType;
        this.localTimeStamp = -999;
    }

    // TODO Remove check if not occured
    // TODO Evtl. hier erweiterte Lamportzeit implementieren falls notwendig
    @Override
    public int compareTo(Message o) {

        if (this.localTimeStamp == -999 || o.localTimeStamp == -999) {
            System.out.println("LOCAL TIME NOT SET FOR MESSAGE");
            System.exit(-1);
        }

        return Long.compare(this.localTimeStamp, o.localTimeStamp);
    }


    @Override
    public String toString() {
        return "Message{" +
                "id_sender=" + id_sender +
                ", senderTimeStamp=" + senderTimeStamp +
                ", messageType=" + messageType +
                ", localTimeStamp=" + localTimeStamp +
                '}';
    }
}

class UnicastMessage extends Message {
    public final int id_receiver;

    UnicastMessage(int id_sender, long timeStampSender, MessageType messageType, int id_receiver) {
        super(id_sender, timeStampSender, messageType);
        this.id_receiver = id_receiver;
    }
}

class MulticastMessage extends Message {

    MulticastMessage(int id_sender, long timeStampSender, MessageType messageType) {
        super(id_sender, timeStampSender, messageType);
    }
}


enum MessageType {
    REQUEST, ACK, RELEASE
}