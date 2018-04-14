package com.my.p2p;
import java.io.IOException;
import java.net.*;
import com.my.p2p.P2PClient.*;

public class P2PClient {

    private InetAddress address;
    private int port;
    private DatagramSocket socket;
    private String name;
    
    private static int DATA_LEN = 1024;
    
    private byte[] inBuff = new byte[DATA_LEN];
    private DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length, address, port);
    private DatagramPacket outPacket;
    
    private int status = 0;

    public P2PClient(String host, int port, String name) throws P2PClientCreateException {
        try
        {
            address = InetAddress.getByName(host);
            this.port = port;
            this.name = name;
            socket = new DatagramSocket(2005);
            log("Remote_Address: " + address);
            log("Remote_Port: " + port);
            init();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
            throw new P2PClientCreateException("Create socket fail!");
        }
    }

    private void init() throws P2PClientCreateException {
        boolean receive = true;
        while (receive) {
            setStatus(1);
            sendMessageToServer("SYN");
            setStatus(2);
            try {
                String message = receiveMessageToServer(3000, 1);
                log(message);
                if(message == "ACK") {
                    receive = false;
                    log("Connected to server!");
                }
                
            } catch (TimeOutException e) {
                log("Time out!");
            }
        }
        
        receive = true;
        while (receive) {
            setStatus(3);
            sendMessageToServer("ACK" + name);
            setStatus(4);
            try {
                String message = receiveMessageToServer(3000, 1);
                if(message == "ACCEPT" + name) {
                    
                }
            } catch (TimeOutException e) {
                log("Time out!");
            }
        }
    }

    private void sendMessageToServer(String message) throws P2PClientCreateException {
        byte[] bytes = message.getBytes();
        
        outPacket = new DatagramPacket(bytes, bytes.length, address, port);
        outPacket.setData(bytes);
        outPacket.setLength(bytes.length);
        outPacket.setAddress(address);
        outPacket.setPort(port);

        try {
            socket.send(outPacket);
        } catch (IOException e) {
            e.printStackTrace();
            throw new P2PClientCreateException("Send Message to Server fail");
        }
    }
    
    private String receiveMessageToServer(int outTime, int times) throws P2PClientCreateException, TimeOutException {
        try {
            socket.setSoTimeout(outTime);
        } catch (SocketException e) {
            throw new P2PClientCreateException("Set out time fail!");
        }

        boolean receive = false;
        int i = 0;
        while(!receive && ++i <= times) {
            try {
                socket.receive(inPacket);
                log("Receive Packet (" + inPacket.getLength() + ") !");
                return new String(inPacket.getData(), 0, inPacket.getLength());
            } catch (IOException e) {
                
            }
        }
        throw new TimeOutException("Receive message from server time out!");
    }

    private void setStatus(int status) {
        this.status = status;
    }

    public class P2PClientCreateException extends Exception {
        
        String message;
        
        public P2PClientCreateException(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }
    
    public class TimeOutException extends Exception {

        String message;

        public TimeOutException(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }
    
    public static void main(String[] args) {
        try {
            P2PClient client = new P2PClient("test.kaixinguo.site", 1234, "Kaixinguo");
        } catch (P2PClient.P2PClientCreateException e) {
            e.printStackTrace();
        }
    }
    
    private void log(String message) {
        System.out.println(message);
    }
}
