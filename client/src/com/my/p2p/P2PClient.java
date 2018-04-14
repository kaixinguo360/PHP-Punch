package com.my.p2p;
import java.io.IOException;
import java.net.*;

public class P2PClient implements Runnable {

    private static int LOG_LEVEL = 1;
    private static int DATA_LEN = 1024;

    private InetAddress address;
    private int port;
    private DatagramSocket socket;
    private String name;

    private byte[] inBuff = new byte[DATA_LEN];
    private DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length, address, port);
    private DatagramPacket outPacket;

    private int status = -2;

    private Thread thread;

    /** --------------------------------- Constructor --------------------------------- **/
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
            thread = new Thread(this, "P2PClient");
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
            throw new P2PClientCreateException("Create socket fail!");
        }
    }


    /** --------------------------------- Public --------------------------------- **/
    public void connect() throws P2PClientCreateException {
        if(!thread.isAlive()) {
            status = 0;
            thread.start();
        }
    }

    public void close() throws P2PClientCreateException {
        if(thread.isAlive()) {
            status = -2;
        }
    }

    public int getStatus() {
        return this.status;
    }

    public void run() {
        status = 0;
        log("User_Name:       " + name);
        log("Server_Address:  " + address);
        log("Server_Port:     " + port);
        log("Connecting to server...");
        try {
            sendMessageToServer("SYN");
            mainLoop();
        } catch (P2PClientCreateException e) {
            e.printStackTrace();
        }
        log("Connect Closed");
    }


    /** --------------------------------- Private --------------------------------- **/
    private void mainLoop() throws P2PClientCreateException {
        while(status > -2) {
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
                log(">>> Login!");
            }
        } else if(status == 2) {
            toDo(data);
        }
    }

    private void timeOut() throws P2PClientCreateException {
        if(status == 0) {
            sendMessageToServer("SYN");
        } else if(status == 1) {
            sendMessageToServer("ACK" + name);
        } else if(status == 2) {
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

    private void log(String message) {
        log(message, 0);
    }

    private void log(String message, int p) {
        if(p < LOG_LEVEL) {
            System.out.println(message);
        }
    }


    /** --------------------------------- Inner Class --------------------------------- **/
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
            P2PClient p2pClient = new P2PClient("test.kaixinguo.site", 1234, "User-" + (int) (Math.random() * 1000));
            p2pClient.connect();
            Thread.sleep(5000);
            p2pClient.close();
        } catch (P2PClientCreateException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
