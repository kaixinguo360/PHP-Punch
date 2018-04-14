package com.my.p2p;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private ArrayList<String> sendQueue = new ArrayList<>();
    private ArrayList<String> receiveQueue = new ArrayList<>();

    private int status = -2;

    private Thread thread;

    /** --------------------------------- Constructor --------------------------------- **/
    public P2PClient(String host, int port, String name) throws UnknownHostException, SocketException {
        this(host, port, name, -1);
    }

    public P2PClient(String host, int port, String name, int localPort) throws UnknownHostException, SocketException {
        address = InetAddress.getByName(host);
        this.port = port;
        this.name = name;
        if(localPort < 0) {
            socket = new DatagramSocket();
        } else {
            socket = new DatagramSocket(localPort);
        }
        thread = new Thread(this, "P2PClient");
    }


    /** --------------------------------- Public --------------------------------- **/
    public void connect() {
        if(!thread.isAlive()) {
            status = 0;
            thread.start();
        }
    }

    public void close() {
        if(thread.isAlive()) {
            status = -2;
        }
    }

    public List<String> list() throws P2PClientCreateException {
        return list(5000);
    }

    public List<String>  list(int time) throws P2PClientCreateException {
        if(status != 2) {
            throw new P2PClientCreateException("Not Connect to server!");
        }
        sendQueue.add("LIST");
        receiveQueue.clear();
        int slice = 500;
        int times = time / slice;
        int i = 0;
        while(++i <= times) {
            if(!receiveQueue.isEmpty()) {
                String receive = receiveQueue.get(0);
                receiveQueue.remove(0);
                return Arrays.asList(receive.split("\n"));
            }
            try { Thread.sleep(slice);} catch (InterruptedException ignored) {}
        }
        throw new P2PClientCreateException("List Failed!");
    }

    public DatagramSocket getSocketTo(String target) {
        return null;
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
        try {
            socket.setSoTimeout(500);
        } catch (SocketException e) {
            throw new P2PClientCreateException("Set out time fail!");
        }

        int i = 0;

        while(status > -2) {
            try {
                socket.receive(inPacket);
                String message = new String(inPacket.getData(), 0, inPacket.getLength());
                //log("--> " + message);
                receive(message);
            } catch (IOException e) {
                if(++i >= 10) {
                    i = 0;
                    timeOut();
                }
            }
            if(status == 2) {
                toSend();
            }
        }
    }

    private void toSend() throws P2PClientCreateException {
        if(!sendQueue.isEmpty()) {
            String data = sendQueue.get(0);
            sendQueue.remove(0);
            log("<-- " + data, 1);
            sendMessageToServer(data);
        }
    }

    private void toReceive(String data) {
        log("--> " + data, 1);
        receiveQueue.add(data);
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
            toReceive(data);
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

    public static void main(String[] args) throws UnknownHostException {
        try {
            P2PClient p2pClient = new P2PClient("test.kaixinguo.site", 1234, "User-" + (int) (Math.random() * 1000));
            p2pClient.connect();
            int i = 0;
            while(i++ < 100) {
                Thread.sleep(3000);
                try {
                    System.out.println("List...");
                    List<String> names = p2pClient.list();
                    int j = 1;
                    for(String name : names) {
                        System.out.println("User[" + j++ + "]: " + name);
                    }
                } catch (P2PClientCreateException ignored) {
                    System.out.println("Fail");
                }
            }
            //p2pClient.close();
        } catch (InterruptedException | SocketException e) {
            e.printStackTrace();
        }
    }
}
