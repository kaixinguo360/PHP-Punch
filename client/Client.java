import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class UDPSocket {
    public static void main (String[] asdf) {
    
        try {
    
            String host = "test.kaixinguo.site";
            int port = 1234;
    
            byte[] message1 = "Java Source and Support".getBytes();
            byte[] message2 = "Java Source and Support".getBytes();
            byte[] message3 = "Java Source and Support".getBytes();
    
            // Get the internet address of the specified host
            InetAddress address = InetAddress.getByName(host);
            System.out.println("Remote_Address: " + address);
    
            // Initialize a datagram packet with data and address
            DatagramPacket packet = new DatagramPacket(message1, message1.length,
                    address, port);
            System.out.println("Remote_Port: " + port);
            
            // Create a datagram socket, send the packet through it, close it.
            DatagramSocket dsocket = new DatagramSocket(2005);
            System.out.println("Local_Socket: " + dsocket);
            boolean receive = false;
            int i = 0;
            dsocket.setSoTimeout(3000);
            while(!receive) {
                i++;
                try{
                    System.out.println("Send[" + i + "]...");
                    dsocket.send(packet);
                    dsocket.receive(packet);
                    System.out.println("Receive Packet: " + packet);
                } catch (SocketTimeoutException e) {
                    System.out.println("Time Out!");
                }
            }
            dsocket.close();
            System.out.println("Closed!");
    
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
