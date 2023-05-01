import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

public class ClientHandler implements Runnable {
private DatagramSocket serverSocket;
private InetAddress clientAddress;
private int clientPort;

   public ClientHandler(DatagramSocket serverSocket, InetAddress clientAddress, int clientPort) throws Exception {
       this.clientAddress = clientAddress;
       this.clientPort = clientPort;
       this.serverSocket = getServerSocket();;
   }

public DatagramSocket getServerSocket() throws SocketException {
   serverSocket = new DatagramSocket();
return serverSocket;
}
public void setServerSocket(DatagramSocket serverSocket) {
this.serverSocket = serverSocket;
}

public void run() {
       try {
        byte [] rData = new byte [1024];
        byte [] sData = new byte [1024];
        Frame sACK = new Frame(clientAddress,
        		"ACK", //msgType
        		"", //filename
        		0, //seqNo
        		0, //length
        		null);
           //send initial ACK to confirm the connection
           String ACK = "ACK";
           byte[] ackBytes = ACK.getBytes();
           sACK.setBody(ackBytes);
           sACK.setLength(ackBytes.length);
           int SEQ = sACK.getRandomSeq(0);
           sACK.setSequenceNo(SEQ);
          // System.out.println(sACK.getSequenceNo());
         //  System.out.println(sACK.getMsgType());
           byte [] ackFrame = Frame.serialize(sACK);
           DatagramPacket sPacket = new DatagramPacket(ackFrame,ackFrame.length,clientAddress, clientPort);
           serverSocket.send(sPacket);
           System.out.println("ACK sent waiting for client ....");
           
           //get ACKACK
           DatagramPacket rPacket = new DatagramPacket(rData,rData.length);
           serverSocket.receive(rPacket);
           byte [] cACK = rPacket.getData();
           Frame cAckFrame = Frame.deserialize(cACK);
           if(cAckFrame.getMsgType().equals("ACK")&& cAckFrame.getSequenceNo()== sACK.getSequenceNo()+1) {
           byte [] cACKACK = cAckFrame.getBody();
           String ACKACK = new String(cACKACK);
           System.out.println("Acknowledgement from client: " + ACKACK);}
           else
           {
        	   System.out.println("Acknowledgement not receieved");
           }
          
           
           //getting the type of transfer
           rData = new byte [1024];
           rPacket = new DatagramPacket(rData,rData.length);
           serverSocket.receive(rPacket);
          // System.out.println("got the packet");
           byte [] fData = rPacket.getData();
           Frame rFrame = Frame.deserialize(fData);
         //  System.out.println("extracted it");
           String transfer = rFrame.getMsgType();
           if(transfer.equals("GET")) {
       	        String fileName = rFrame.getFileName();
       	        while(!fileName.equals("quit")) {
       	        	sendFile(fileName);
       	        	rData = new byte[1024];
       	            rPacket = new DatagramPacket(rData, rData.length);
       	            serverSocket.receive(rPacket);
       	            fData = rPacket.getData();
       	            rFrame = Frame.deserialize(fData);
       	            fileName = rFrame.getFileName();
       	        }
       	        System.out.println("Client has terminated their connection !");
       	       
      }//GET
           else if (transfer.equals("PUT")) {
        	   
           }//PUT
           
           else {
        	   String message = "ERROR: invalid transfer type";
               byte[] msgbytes = message.getBytes();
               Frame sframe = new Frame(clientAddress,
                       "ERROR", //transfer type
                       "", //file name
                       0,   //sequence no.
                       msgbytes.length,  //length
                       msgbytes); //body
               sframe.getRandomSeq(0);
               sframe.setSequenceNo(SEQ);
               //convert to bytes for sending
               byte[] sFrame = Frame.serialize(sframe);
               sPacket = new DatagramPacket(sFrame, sFrame.length, clientAddress, clientPort);
               serverSocket.send(sPacket); //GET frame sent
        	   System.out.println("ERROR: client requested for an invalid transfer type");
        	   
           }//ERROR        
       }//TRY
       catch (Exception e) {
           e.printStackTrace();
       }
       
   }//void run()


private void sendFile(String fileName) throws IOException, ClassNotFoundException {
    File file = new File(fileName);
    if (!file.exists()) {
        System.out.println("ERROR: Client requested a file that does not exist");
        String message = "ERROR: the file does not exist in our server";
        byte[] msgbytes = message.getBytes();
        Frame sframe = new Frame(clientAddress,
                "ERROR", //transfer type
                "", //file name
                0,   //sequence no.
                msgbytes.length,  //length
                msgbytes); //body
        int SEQ = 0;
        sframe.setSequenceNo(Frame.getRandomSeq(SEQ));
        //convert to bytes for sending
        byte[] sFrame = Frame.serialize(sframe);
        DatagramPacket sPacket = new DatagramPacket(sFrame, sFrame.length, clientAddress, clientPort);
        serverSocket.send(sPacket); //ERROR frame sent
        System.out.println("frame sent");
    	} else 
    		{   
   	        System.out.println("The file name is " + fileName);
			long fileSize = file.length();
			String fileSizeString = Long.toString(fileSize);
			byte[] fileSizeBytes = fileSizeString.getBytes();
			//sending to the client the file size
			Frame sizeframe = new Frame(clientAddress,
	                "RESPONSE", //transfer type
	                "", //file name
	                0,   //sequence no.
	                fileSizeBytes.length,  //length
	                fileSizeBytes); //body
		    byte[] sizeFrame = Frame.serialize(sizeframe);
	        DatagramPacket sPacket = new DatagramPacket(sizeFrame, sizeFrame.length, clientAddress, clientPort);
	        serverSocket.send(sPacket); //FileSize sent
	        System.out.println("the file size is : " + fileSize +".Sent file size to the client");
	        
	        if (fileSize <= 1024) {
	            // Implement the code for less than buffer
	            byte[] fileData = Files.readAllBytes(file.toPath());
	            Frame sFrame = new Frame(clientAddress,
	                                     "DATA", //transfer type
	                                     fileName, //file name
	                                     0,   //sequence no.
	                                     fileData.length,  //length
	                                     fileData); //body
	            int SEQ = 0;
	            sFrame.setSequenceNo(Frame.getRandomSeq(SEQ));
	            SEQ = sFrame.getSequenceNo();
	            System.out.println("seq = " + SEQ);
	            byte[] sFrameBytes = Frame.serialize(sFrame);
	            sPacket = new DatagramPacket(sFrameBytes, sFrameBytes.length, clientAddress, clientPort);
	            serverSocket.send(sPacket); //sending file data
	            //System.out.println("file sent ... ");
	            
	            //ACK frame
	            int ackNo;
	            Random ran = new Random();
	            ackNo = ran.nextInt(100);
	            String ackNoString = String.valueOf(ackNo);
	            byte [] ackNoBytes = ackNoString.getBytes();
	            //sendACKframe
	            Frame sAckFrame = new Frame(clientAddress,"ACK","",SEQ,ackNoBytes.length,ackNoBytes);
	            System.out.println("the seq for the ackFrame" + sAckFrame.getSequenceNo());
	            byte [] sAck = Frame.serialize(sAckFrame);
	            DatagramPacket ackPacket = new DatagramPacket(sAck,sAck.length,clientAddress,clientPort);
	            serverSocket.send(ackPacket);//ACK frame sent
	            System.out.println("Packet sent) Seq = " + SEQ +",ACK = " + ackNo );
	            System.out.println("Awaiting ACK ...");
	            
	            //ACKfromClient
	            byte [] ackData = new byte [1024];
	            DatagramPacket ackpkt = new DatagramPacket(ackData,ackData.length);
	            serverSocket.receive(ackpkt);
	            byte [] fData = ackpkt.getData();
	            Frame ackFrame = Frame.deserialize(fData);
	            byte [] body  = ackFrame.getBody();
	            String bodyString = new String(body);
	            int ackClient = Integer.parseInt(bodyString);
	            if(ackFrame.getMsgType().equals("ACK")&& ackFrame.getSequenceNo()== ackNo && ackClient == (SEQ+1)) {
	            	System.out.println("ACK from client receieved");
	            	
	            }
	            else {
	            	System.out.println("msg type " + ackFrame.getMsgType());
	            	System.out.println("seq " + ackFrame.getSequenceNo());
	            	System.out.println("ack " + ackClient);

	            }
	            	
	        } else if (fileSize > 1024){
	            // Implement the code for greater than buffer
	        	byte[] fileData = Files.readAllBytes(file.toPath());
	        	fileSize = fileData.length;
	        	int SEQ = Frame.getRandomSeq(0);
	        	int numPackets = (int) Math.ceil((double) fileSize / 1024);
	        	for (int i = 0; i < numPackets; i++) {
	        	    int startIdx = i * 1024;
	        	    int endIdx = (int) Math.min((i + 1) * 1024, fileSize);
	        	    byte[] payload = Arrays.copyOfRange(fileData, startIdx, endIdx);

	        	    Frame sFrame = new Frame(clientAddress,
	        	                             "DATA", //transfer type
	        	                             fileName, //file name
	        	                             SEQ,   //sequence no.
	        	                             payload.length,  //length
	        	                             payload); //body
	        	    //serialize the Frame instance into a byte array
	        	    byte[] sFrameBytes = Frame.serialize(sFrame);
	        	    sPacket = new DatagramPacket(sFrameBytes, sFrameBytes.length, clientAddress, clientPort);
	        	    serverSocket.send(sPacket); //sending file data
	        	    int count = 1;
	        	    System.out.println("packet " + count + " sent");
	        	    count++;
	        	    System.out.println("waiting for ACK");

	        	    // Wait for the ACK from the client before sending the next packet
	        	    boolean receivedACK = false;
	        	    while (!receivedACK) {
	        	        // Wait for the ACK from the client
	        	        byte[] ackData = new byte[1024];
	        	        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
	        	        serverSocket.receive(ackPacket);
	        	        byte[] fData = ackPacket.getData();
	        	        Frame ackFrame = Frame.deserialize(fData);
	        	        byte[] body = ackFrame.getBody();
	        	        String bodyString = new String(body);
	        	        int ackClient = Integer.parseInt(bodyString);
	        	        if (ackFrame.getMsgType().equals("ACK") && ackClient == (SEQ + 1)) {
	        	            // ACK received from client for this packet
	        	        	System.out.println("ACK received");
	        	            receivedACK = true;
	        	        }
	        	        else {
	        	        	System.out.println("ACK not received :( ");
	        	        }
	        	    }//while!rACK
	        	    SEQ++;
	        	}//keep sending

	        }//exceeding buffer
    	}
}//sendFile method

} //class