package com.my.p2p;
import java.io.IOException;
import java.net.*;

public class P2PClient {

    private static final int LOG_LEVEL = 2;

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
        this(host, port, name, -1);
    }

    public P2PClient(String host, int port, String name, int localPort) throws P2PClientCreateException {
        try
        {
            address = InetAddress.getByName(host);
            this.port = port;
            this.name = name;
            if(localPort < 0) {
                socket = new DatagramSocket();
            } else {
                socket = new DatagramSocket(localPort);
            }
            init();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
            throw new P2PClientCreateException("Create socket fail!");
        }
    }

    private void init() throws P2PClientCreateException {
        status = 0;
        log("Server_Address:  " + address);
        log("Server_Port:     " + port);
        log("Connecting to server...");
        sendMessageToServer("SYN");
        waitServer();
    }

    private void waitServer() throws P2PClientCreateException {
        while(true) {
            try {
                String message = receiveMessageFromServer(5000, 1);
                //log("--> " + message);
                receive(message);
            } catch (TimeOutException e) {
                //log("Time out!");
                timeOut();
            }
        }
    }

    private void receive(String data) throws P2PClientCreateException {
        log("--> " + data, 1);

        if("RETRY".equals(data)) {
            status = 0;
            log("<<< Lost!");
            timeOut();
            return;
        }
        if(status == 0) {
            if("ACK".equals(data)) {
                status = 1;
                timeOut();
            }
        } else if(status == 1) {
            if("ACCEPT".equals(data)) {
                status = 2;
                timeOut();
            }
        } else if(status == 2) {
            if("OK".equals(data)) {
                status = 3;
                log(">>> Login!");
            }
        } else if(status == 3) {
            toDo(data);
        }
    }

    private void timeOut() throws P2PClientCreateException {
        if(status == 0) {
            sendMessageToServer("SYN");
        } else if(status == 1) {
            sendMessageToServer("ACK" + name);
        } else if(status == 2) {
            sendMessageToServer("OK");
        } else if(status == 3) {
            sendMessageToServer("HB");
        }
    }

    private void toDo(String data) {

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
            log("<-- " + message, 1);
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

    private void log(String message) {
        log(message, 0);
    }

    private void log(String message, int p) {
        if(p < LOG_LEVEL) {
            System.out.println(message);
        }
    }

    public static void main(String[] args) {
        try {
            P2PClient client = new P2PClient("test.kaixinguo.site", 1234, "User-" + (int) (Math.random() * 1000));
        } catch (P2PClient.P2PClientCreateException e) {
            e.printStackTrace();
        }
    }
}
