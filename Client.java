import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.*;
import java.util.Scanner;

public class Client {
	public static InetAddress serverAddress;

	public static void main(String[] args) throws Exception {
        DatagramSocket client = new DatagramSocket(); //client Socket
        byte[] sData = new byte[1024];
        byte[] rData = new byte[1024];
        
        InetAddress serverAddress = null;
        String serverName = "";
        System.out.println(serverName);
        Scanner cin = new Scanner(System.in);
        while (serverAddress == null) {
            System.out.print("FTP Client starting on host: " + InetAddress.getLocalHost().getHostName() +
                    ". Type name of FTP server: ");
            serverName = cin.nextLine();
            try {
                serverAddress = InetAddress.getByName(serverName);
            }//TRY 
            catch (UnknownHostException e) {
                System.out.println("Unknown host, please try again.");
            }//CATCH
        }
        
       //just to establish a conn.
        byte[] conn = new byte[1024];
        String message = " ";
        conn = message.getBytes();
        DatagramPacket sPacket = new DatagramPacket(conn, conn.length, serverAddress, 6666);
        client.send(sPacket);
        System.out.println("connecting to the server...");
  
  //----------------------------- Three-way handshake------------------------------------------------------------------------
        DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
        client.receive(rPacket);
        byte [] sACK = rPacket.getData();
        Frame rFrame = Frame.deserialize(sACK);
        if(!rFrame.getMsgType().equals("ACK")) {
        	System.out.println("ERROR: Unexcepted Frame");
        	System.out.println(rFrame.getMsgType());
        }
        int serverPort = rPacket.getPort();
        byte [] sAck = rFrame.getBody();
        String sAckString = new String(sAck);
        System.out.println("Server acknowledgement: " + sAckString.trim());
       // System.out.println("seqNo server " + rFrame.getSequenceNo());
       
        // Client sends back ACKACK
        Frame cACKACK = new Frame(serverAddress,
        		"ACK", //type
        		"", //filename
        		0, 
        		0, 
        		null);
        String ClientACK = "ACKACK";
        sData = ClientACK.getBytes();
        cACKACK.setBody(sData);
        cACKACK.setLength(sData.length);
        cACKACK.setSequenceNo(rFrame.getSequenceNo()+1);
       // System.out.println("client seq No:" + cACKACK.getSequenceNo());
        byte [] fData = Frame.serialize(cACKACK);
        sPacket = new DatagramPacket(fData, fData.length, serverAddress, serverPort);
        client.send(sPacket); //ACKACK sent
        System.out.println("Connection is made successfully with " + serverName);
        
 
      //----------------------------- Three-way handshake END-------------------------------------------------------------------
           // Get or Put
              System.out.print("What's the type of transfer (get/put): ");
              Scanner s = new Scanner(System.in);
              String transfer = s.next();
              Frame sFrame = new Frame(serverAddress,
              		transfer.toUpperCase().trim(), //type
              		"", //filename
              		0, 
              		0, 
              		null);              
            		  
              if(transfer.toUpperCase().equals("GET")) {
                  get(client, serverAddress, serverPort);

              }//GET
              else if (transfer.toUpperCase().equals("PUT")) {
            	  
              }//PUT
              else {
            	  System.out.println("invalid transfer try again later");
              }//invalid transfer type
            
	}//MAIN METHOD

	private static void get(DatagramSocket client, InetAddress serverAddress, int serverPort) throws Exception {
	    Scanner s = new Scanner(System.in);
	    String filename;
	    System.out.println("Enter the name of the file to GET from the server (or type 'quit' to exit)");
	        while (true) {
	        filename = s.nextLine().trim();

	        if (filename.equals("quit")) {
		        Frame sFrame = new Frame(serverAddress, "GET", filename,0,0,null);
		        sFrame.setSequenceNo(Frame.getRandomSeq(0));
		        byte[] fData = Frame.serialize(sFrame);
		        DatagramPacket sPacket = new DatagramPacket(fData, fData.length, serverAddress, serverPort);
		        client.send(sPacket); // sent quit
		        System.out.println("good-bye :)");
	            break;
	        }

	        // Send GET request
	        Frame sFrame = new Frame(serverAddress, "GET", filename,0,0,null);
	        sFrame.setSequenceNo(Frame.getRandomSeq(0));
	        byte[] fData = Frame.serialize(sFrame);
	        DatagramPacket sPacket = new DatagramPacket(fData, fData.length, serverAddress, serverPort);
	        client.send(sPacket); // sent filename + transferType

	        // Receive file size
	        byte[] rData = new byte[1024];
	        DatagramPacket rPacket = new DatagramPacket(rData, rData.length);
	        client.receive(rPacket);
	        fData = rPacket.getData();
	        Frame rFrame = Frame.deserialize(fData);

	        if (rFrame.getMsgType().equals("ERROR")) {
	            byte[] bodymsgbytes = rFrame.getBody();
	            String msg = new String(bodymsgbytes);
	            System.out.println(msg);
	            continue;
	        } else if (!rFrame.getMsgType().equals("RESPONSE")) {
	            System.out.println("unexpected frame received");
	            continue;
	        }

	        byte[] fileSizeBytes = rFrame.getBody();
	        int fileSize = Integer.parseInt(new String(fileSizeBytes).trim());
	        System.out.println("the file size is " + fileSize);

	        //file storage
	        String FilePath = "C:\\Users\\mahra\\eclipse-workspace\\CN_Project";
            FileOutputStream fos = new FileOutputStream(FilePath + "\\" + filename);
            
	        if (fileSize <= 1024) {
	            // Receive file data when less than or equal to buffer size
	            rData = new byte[fileSize];
	            rPacket = new DatagramPacket(rData, rData.length);
	            client.receive(rPacket);
	            fData = rPacket.getData();
	            rFrame = Frame.deserialize(fData);
	            if (!rFrame.getMsgType().equals("DATA")) {
	                System.out.println("Unexpected frame received");
	                return;
	            }
	            byte[] fileData = rFrame.getBody();
	            //file storage
	            fos.write(fileData);
	            fos.close();
	           
	            //getACK
	            byte [] ackData = new byte [1024];
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length);
                client.receive(ackPacket);
                byte [] ackDataF = ackPacket.getData();
                Frame rACKFrame = Frame.deserialize(ackDataF); //accepting the ACK
                System.out.println(rACKFrame.getMsgType());
                
                if (rACKFrame.getMsgType().equals("ACK")) {
              	  byte [] ackNo = rACKFrame.getBody();
                    String ackNoString = new String(ackNo);
                    int ackNoInt = Integer.parseInt(ackNoString.trim());
                    //System.out.println("server ACKNo: " + ackNoInt);
					 System.out.println("server seq: " +rACKFrame.getSequenceNo());
					 System.out.println("server ack: " + ackNoInt);

                    //send back ACK
					  int clientSeq = ackNoInt;
					  int clientAck = rACKFrame.getSequenceNo() + 1; //this is in body of frame
					  String clientAckStr = Integer.toString(clientAck);
					  byte[] clientAckBytes = clientAckStr.getBytes();
					  Frame ackFrame = new Frame(serverAddress, "ACK", "", clientSeq, clientAckBytes.length, clientAckBytes);
					  ackFrame.setSequenceNo(clientSeq);
					  byte [] ACKframe = Frame.serialize(ackFrame);
					  sPacket = new DatagramPacket(ACKframe, ACKframe.length,serverAddress,rPacket.getPort());
                    client.send(sPacket); //sendACK frame
                    System.out.println("Packet Recieved) Seq = " + clientSeq +" ,ACK = " + clientAck );
                    System.out.println("File data received!");
                    
                }
                else {
                	System.out.println("unexpected frame");
                	System.out.println(rACKFrame.getMsgType());
                }   
                System.out.println("Enter file you would like to retrieve or quit to exit(you can also write the servername to exit)");       
                
	        } //if less than buffer
	        
	        else if (fileSize > 1024) {
	        	//implementation of greater than buffer
	            int totalBytesRead = 0;
	            ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            while (totalBytesRead < fileSize) {
	                rData = new byte[1024];
	                rPacket = new DatagramPacket(rData, rData.length);
	                client.receive(rPacket);
	                Frame dataFrame = Frame.deserialize(rPacket.getData());
	                if (dataFrame.getMsgType().equals("DATA")) {
	                    byte[] payload = dataFrame.getBody();
	                    baos.write(payload);
	                    totalBytesRead += payload.length;

	                    // Send ACK for this packet
	                    int sSEQ = dataFrame.getSequenceNo();
	                    String ackNo = String.valueOf(sSEQ + 1);
	                    byte[] ackNob = ackNo.getBytes();
	                    Frame ack = new Frame(serverAddress, "ACK", "", 0, ackNob.length, ackNob);
	                    byte[] ackFrame = Frame.serialize(ack);
	                    DatagramPacket AP = new DatagramPacket(ackFrame, ackFrame.length, serverAddress, serverPort);
	                    client.send(AP);
	                }
	            }
	            System.out.println("File received successfully");
	            byte[] contents = baos.toByteArray();
	            fos.write(contents);
	            fos.close();
	        } 
	    }//while(true)
	    client.close(); 
	}//get
	
}//CLASS