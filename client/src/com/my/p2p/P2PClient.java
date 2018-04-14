package com.my.p2p;
import java.io.IOException;
import java.net.*;

public class P2PClient {

    private InetAddress address;
    private int port;
    private DatagramSocket socket;

    private byte[] bytes = new byte[]{};
    private DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
    private int status = 0;

    public P2PClient(String host, int port) throws P2PClientCreateException {
        try
        {
            address = InetAddress.getByName(host);
            this.port = port;
            socket = new DatagramSocket();
            System.out.println("Remote_Address: " + address);
            System.out.println("Remote_Port: " + port);
            init();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
            throw new P2PClientCreateException("Create socket fail!");
        }
    }

    private void init() throws P2PClientCreateException {
        setStatus(1);
        sendMessageToServer("SYN");
        setStatus(2);
        receiveMessageToServer(1);
    }

    private void sendMessageToServer(String message) throws P2PClientCreateException {
        byte[] bytes = message.getBytes();
        
        packet.setData(bytes);
        packet.setLength(bytes.length);
        packet.setAddress(address);
        packet.setPort(port);

        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
            throw new P2PClientCreateException("Send Message to Server fail");
        }
    }
    
    private String receiveMessageToServer(int outTime) throws P2PClientCreateException {
        socket.setSoTimeout(outTime);

        boolean receive = false;
        int i = 0;
        while(!receive) {
            socket.receive(packet);
            return packet.getData().toString();
        }
        throw new P2PClientCreateException("");
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
}
