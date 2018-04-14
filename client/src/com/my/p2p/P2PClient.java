package com.my.p2p;
import java.io.IOException;
import java.net.*;

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
        status = 0;
        sendMessageToServer("SYN");
        waitServer();
    }

    private void waitServer() throws P2PClientCreateException {
        while(true) {
            try {
                String message = receiveMessageFromServer(3000, 1);
                //log("--> " + message);
                receive(message);
            } catch (TimeOutException e) {
                //log("Time out!");
                timeOut();
            }
        }
    }

    private void receive(String data) throws P2PClientCreateException {
        if(status == 0) {
            if("ACK".equals(data)) {
                log("--> ACK");
                status = 1;
                timeOut();
            }
        } else if(status == 1) {
            if("ACCEPT".equals(data)) {
                log("--> ACCEPT");
                status = 2;
                timeOut();
            }
        } else if(status == 2) {
            if("OK".equals(data)) {
                log("--> OK");
                status = 3;
            }
        } else if(status == 3) {
            log("--> " + data);
        }
    }

    private void timeOut() throws P2PClientCreateException {
        if(status == 0) {
            sendMessageToServer("SYN");
            log("<-- SYN");
        } else if(status == 1) {
            sendMessageToServer("ACK" + name);
            log("<-- ACK" + name);
        } else if(status == 2) {
            sendMessageToServer("OK");
            log("<-- OK");
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

    private String receiveMessageFromServer(int outTime, int times) throws P2PClientCreateException, TimeOutException {
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
                return new String(inPacket.getData(), 0, inPacket.getLength());
            } catch (IOException e) {

            }
        }
        throw new TimeOutException("Receive message from server time out!");
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
