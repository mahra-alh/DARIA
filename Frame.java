import java.io.*;
import java.net.*;
import java.util.Objects;
import java.util.Random;

public class Frame implements Serializable {
    private int hostIP;
    private String msgType; //the one we are inputting
    private MessageType messageType; // using enum instead of string for message type
    private String fileName;
    private int sequenceNo = 0;
    private int length;
    private byte[] body;

    public Frame(InetAddress hostIP, String msgType, String fileName, int sequenceNo, int length, byte[] body) {
        this.hostIP = convertIPAddressToInt(hostIP);
        this.msgType = Objects.requireNonNull(msgType);
        this.fileName = fileName;
        this.sequenceNo = sequenceNo;
        this.length = length;
        this.body = body;
        setMessageType(msgType);
    }

    //convert an InetAddress to an int
    private static int convertIPAddressToInt(InetAddress address) {
        byte[] bytes = address.getAddress();
        int result = 0;
        for (byte b : bytes) {
            result = result << 8 | (b & 0xFF);
        }
        return result;
    }

    //convert an int to an InetAddress
    public static InetAddress convertIntToIPAddress(int address) throws UnknownHostException {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (address >>> 24);
        bytes[1] = (byte) (address >>> 16);
        bytes[2] = (byte) (address >>> 8);
        bytes[3] = (byte) (address);
        return InetAddress.getByAddress(bytes);
    }

    public InetAddress getHostIP() throws UnknownHostException {
        return convertIntToIPAddress(hostIP);
    }

    public String getMsgType() {
        return msgType;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getFileName() {
        return fileName;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public int getLength() {
        return length;
    }

    public byte[] getBody() {
        return body;
    }


    public void setHostIP(InetAddress hostIP) {
        this.hostIP = convertIPAddressToInt(hostIP);
    }
    public void setBody(byte [] body) {
    this.body = body;
    }
    public void setLength(int length) {
    this.length = length;
    }

    public void setMsgType(String msgType) {
    this.msgType = Objects.requireNonNull(msgType, "msgType cannot be null");
        setMessageType(msgType); // update the messageType field
    }

    private void setMessageType(String msgType) {
            this.messageType = MessageType.valueOf(msgType);
    }
   
    public void setSequenceNo(int sequenceNo) {
    this.sequenceNo = sequenceNo;
    }
    public void setFileName(String fileName) {
    this.fileName = fileName;
    }
   
    public static int getRandomSeq(int SEQ) {
    if(SEQ == 0) {
    Random rand = new Random();
    SEQ = rand.nextInt(1000); //random SEQNO btwn 0-999
    return SEQ;
    }
    return SEQ;
    }


    // enum for message types
    enum MessageType {
        GET(1),
        PUT(2),
        ACK(3),
        DATA(4),
        ERROR(5),
        RESPONSE(6),
        UNKNOWN(0);
        private int messageTypeInt;
        //kinda like a constructor
        MessageType(int messageTypeInt) {
            this.messageTypeInt = messageTypeInt;
        }
        public int getMessageTypeInt() {
            return messageTypeInt;
        }
    }
   
    public static String getMessageTypeString(int messageTypeInt) {
        MessageType messageType = MessageType.UNKNOWN; // default to UNKNOWN
        for (MessageType type : MessageType.values()) {
            if (type.getMessageTypeInt() == messageTypeInt) {
                messageType = type;
                break;
            }
        }
        return messageType.toString();
    }

    public static byte[] serialize(Frame frame) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(convertIPAddressToInt(frame.getHostIP()));
        dos.writeInt(frame.getMessageType().getMessageTypeInt());
        String fileName = frame.getFileName();
        byte[] fileNameBytes;
        if (fileName.length() == 0) {
            fileNameBytes = new byte[0];
        } else {
            fileNameBytes = frame.getFileName().getBytes();
        }
        dos.writeInt(fileNameBytes.length);
        dos.write(fileNameBytes);
        dos.writeInt(frame.getSequenceNo());
        dos.writeInt(frame.getLength());

        // Write payload
        byte[] body = frame.getBody();
        if (body == null) {
            body = new byte[0];
            System.out.println("the body is empty BTW");
        }
        if (body.length <= 1024) {
            dos.write(body);
            System.out.println("there is something in the body");
        } else {
            throw new IOException("Payload is too long");
        }
        dos.flush();
        dos.close();
        return baos.toByteArray();
    }

    public static Frame deserialize(byte[] bytes) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);
        int hostIP = dis.readInt();
        int messageTypeInt = dis.readInt();
        int fileNameLength = dis.readInt();
        byte[] fileNameBytes = new byte[fileNameLength];
        dis.readFully(fileNameBytes);
        String fileName = new String(fileNameBytes);
        int sequenceNo = dis.readInt();
        int length = dis.readInt();
        // Check payload length
        if (length > 1024) {
            dis.close();
            throw new IOException("Payload is too long");
        }
        byte [] body;
        try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            body = new byte[8 * 1024];
            while ((length = dis.read(body)) != -1) {
                result.write(body, 0, length);
            }            
        }
        Frame frame = new Frame(convertIntToIPAddress(hostIP), getMessageTypeString(messageTypeInt), fileName, sequenceNo, length, body);
        return frame;
    }

 }