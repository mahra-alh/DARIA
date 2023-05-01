import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MultiThreadServer {

public static void main (String [] args) throws Exception {
    DatagramSocket serverSocket = new DatagramSocket(6666);
        while (true) {
            byte[] receiveData = new byte[1024];
            System.out.println("FTP Server starting at host: " + InetAddress.getLocalHost().getHostName() +
            		", waiting to be contacted for transferring files...");
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();
            System.out.println(clientAddress.getHostName()+ " has started the connection");
            // Create a new thread to handle the client connection
            Thread t = new Thread(new ClientHandler(serverSocket, clientAddress, clientPort));
            t.start();
        } //!!DO NOT TOUCH WHILE(TRUE)
    }//MAIN METHOD
 }//SERVER



