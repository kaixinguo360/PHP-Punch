package com.my.p2p;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class P2PClient implements Runnable {

    private static final int LOG_LEVEL = 1;
    private static final int DATA_LEN = 1024;

    private InetAddress address;
    private int port;
    private DatagramSocket socket;

    private String name;

    private final byte[] inBuff = new byte[DATA_LEN];
    private final DatagramPacket inPacket = new DatagramPacket(inBuff, inBuff.length, address, port);
    private final ArrayList<String> sendQueue = new ArrayList<>();
    private final ArrayList<String> receiveQueue = new ArrayList<>();
    private final ArrayList<String> reqQueue = new ArrayList<>();

    private String targetName = "";
    private int selfPort = -1;

    private String targetAddress = "";
    private int targetPort = -1;

    private int updateFlag = 0;
    private int status = -2;

    private boolean useRelay = false;

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

    public void reLogin() {
        if(thread.isAlive()) {
            status = 0;
        }
    }

    public void close() {
        if(thread.isAlive()) {
            status = -2;
        }
    }

    public List<String> list() throws P2PClientException {
        return list(5000);
    }

    public List<String>  list(int time) throws P2PClientException {
        if(status != 2) {
            throw new P2PClientException("Not Connect to server!");
        }
        sendQueue.clear();
        receiveQueue.clear();
        sendQueue.add("LIST");
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
        throw new P2PClientException("List Failed!");
    }

    public P2PSocket getSocketTo(String target) throws SocketException, P2PClientException {
        return  getSocketTo(target, 10000);
    }

    public P2PSocket getSocketTo(String target, int time) throws SocketException, P2PClientException {
        return  getSocketTo(target, time, 1);
    }

    public P2PSocket getSocketTo(String target, int time, int retry) throws SocketException, P2PClientException {

        P2PSocket p2pSocket = new P2PSocket();

        connectTo(target, p2pSocket.selfPort);
        log("< < " + targetName);

        int slice = 500;
        int times = time / slice;
        int i = 0;
        int trytime = 0;
        while(++i <= times) {
            if(status == 2) {
                if(++trytime < retry) {
                    connectTo(target, p2pSocket.selfPort);
                    log("< < " + targetName + "Retry...", 1);
                } else {
                    closeConnect();
                    throw new P2PClientException("Retry time out!");
                }
            } else if(status == 5 || status == 7) {
                try {
                    p2pSocket.setTarget(targetAddress, targetPort, targetName);
                } catch (UnknownHostException e) {
                    closeConnect();
                    throw new P2PClientException("Set Socket Address Failed!");
                }
                int j = 0;
                while(++j <= 10) {
                    try {
                        p2pSocket.send("Hello");
                        String hello = p2pSocket.receive(500);
                        if("Hello".equals(hello)) {
                            p2pSocket.send("Hello");
                            log("<=< Connect to " + targetName + " Success!");
                            closeConnect();
                            return p2pSocket;
                        }
                    } catch (TimeOutException ignored) {}
                }
                if(status == 5) {
                    if(useRelay) {
                        useRelay();
                        log("P2P Connect Failed, Retry With Relay...");
                    } else {
                        closeConnect();
                        throw new P2PClientException("Create Socket Time Out!");
                    }
                } else if(status == 7) {
                    closeConnect();
                    throw new P2PClientException("Create Socket With Relay Time Out!");
                }
            }
            try { Thread.sleep(slice);} catch (InterruptedException ignored) {}
        }
        closeConnect();
        throw new P2PClientException("Connect To User Failed!");
    }

    public int getStatus() {
        return this.status;
    }

    public List<String> getReq() {
        return reqQueue;
    }

    public P2PSocket agreeReq(int index, int time) throws P2PClientException, SocketException {
        if(index < reqQueue.size()) {
            String name = reqQueue.get(index);
            reqQueue.remove(index);
            return getSocketTo(name, time);
        }
        throw new P2PClientException("No That Request!");
    }

    public String getName() {
        return name;
    }

    public void setUseRelay(boolean useRelay) {
        this.useRelay = useRelay;
    }

    public void run() {
        status = 0;
        log("User_Name:       " + name);
        log("Server_Address:  " + address);
        log("Server_Port:     " + port);
        log("Connecting to server...");
        try {
            mainLoop();
        } catch (P2PClientException e) {
            e.printStackTrace();
        }
        log("Connect Closed");
    }


    /** --------------------------------- Private --------------------------------- **/
    private void mainLoop() throws P2PClientException {
        try {
            socket.setSoTimeout(500);
        } catch (SocketException e) {
            throw new P2PClientException("Set time out fail!");
        }

        try {sendMessageToServer("SYN");} catch (SendMessageException ignored) {}

        int i = 0;

        while(status > -2) {
            try {
                try {
                    socket.receive(inPacket);
                    String message = new String(inPacket.getData(), 0, inPacket.getLength());
                    //log("--> " + message);
                    receive(message);
                } catch (IOException e) {
                    if(++i >= 4 || updateFlag != 0) {
                        updateFlag = 0;
                        i = 0;
                        timeOut();
                    }
                }
                if(status == 2) {
                    toSend();
                }
            } catch (SendMessageException e) {
                log(e.message);
            }
        }
    }

    private void toSend() throws SendMessageException {
        if(!sendQueue.isEmpty()) {
            String data = sendQueue.get(0);
            sendQueue.remove(0);
            sendMessageToServer(data);
        }
    }

    private void toReceive(String data) {
        if(data.length() > 4 && "LIST".equals(data.substring(0,4))) {
            receiveQueue.add(data.substring(4));
        } else if(data.length() > 3 && "REQ".equals(data.substring(0,3))) {
            String name = data.substring(3);
            log("> > " + name);
            reqQueue.add(name);
        }
    }

    private void receive(String data) throws SendMessageException {
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
        } else if(status >= 3) {
            receiveP2P(data);
        }
    }

    private int lastHB = 0;
    private void timeOut() throws SendMessageException {
        if(status >= 2) {
            if(++lastHB >= 3) {
                lastHB = 0;
                sendMessageToServer("HB");
            }
        }
        if(status == 0) {
            sendMessageToServer("SYN");
        } else if(status == 1) {
            sendMessageToServer("ACK" + name);
        } else if(status >= 3) {
            timeOutP2P();
        }
    }

    private void receiveP2P(String data) throws SendMessageException {
        if("NON".equals(data)) {
            status = 2;
            targetName = "";
            selfPort = -1;
            log("=X= Connect Close!");
            return;
        }
        if(status == 3) {
            if("CNTACCEPT".equals(data.substring(0, 9))) {
                String[] infos = data.substring(9).split(":");
                if(infos.length == 4) {
                    targetAddress = infos[2];
                    status = 4;
                    timeOut();
                } else {
                    status = 3;
                    log("<X< Info Error -- Retry...", 1);
                }
            }
        } else if(status == 4) {
            if("CNTOK".equals(data.substring(0, 5))) {
                targetPort = Integer.valueOf(data.substring(5));
                status = 5;
                log("<-< Prepared! -- Begin to build socket...");
                log("    Self:   Name     -- " + name, 1);
                log("            Port     -- " + selfPort, 1);
                log("    Target: Name     -- " + targetName, 1);
                log("            Port     -- " + targetPort, 1);
                log("            Address  -- " + targetAddress, 1);
                timeOut();
            }
        } else if(status == 6) {
            if("RELAY".equals(data)) {
                targetAddress = address.getHostAddress();
                targetPort = port;
                status = 7;
                log("<_< Relay! -- Begin to build socket...");
                log("    Self:   Name     -- " + name, 1);
                log("            Port     -- " + selfPort, 1);
                log("    Target: Name     -- " + targetName, 1);
                log("            Port     -- " + targetPort, 1);
                log("            Address  -- " + targetAddress, 1);
                timeOut();
            }
        }
    }

    private void timeOutP2P() throws SendMessageException {
        if(status == 3) {
            if(!"".equals(targetName)) {
                sendMessageToServer("CNT" + targetName);
            } else {
                closeConnect();
            }
        } else if(status == 4) {
            sendMessageToServer("CNTPORT" + selfPort);
        } else if(status == 6) {
            sendMessageToServer("CNTFAILED");
        } else if(status == 10) {
            sendMessageToServer("NON");
        }
    }

    private void connectTo(String target, int selfPort) {
        status = 3;
        targetName = target;
        this.selfPort = selfPort;
        updateFlag = 1;
    }

    private void closeConnect() {
        status = 10;
        targetName = "";
        selfPort = -1;
    }

    private void useRelay() {
        status = 6;
    }

    private void sendMessageToServer(String message) throws SendMessageException {
        byte[] bytes = message.getBytes();

        DatagramPacket outPacket = new DatagramPacket(bytes, bytes.length, address, port);
        outPacket.setData(bytes);
        outPacket.setLength(bytes.length);
        outPacket.setAddress(address);
        outPacket.setPort(port);

        try {
            socket.send(outPacket);
            log("<-- " + message, 1);
        } catch (IOException e) {
            throw new SendMessageException("Send Message to Server fail");
        }
    }

    private void log(String message) {
        log(message, 0);
    }

    private void log(String message, int p) {
        if(p < LOG_LEVEL) {
            System.out.println(name + " " + message);
        }
    }


    /** --------------------------------- Inner Class --------------------------------- **/
    public class P2PClientException extends Exception {

        final String message;

        public P2PClientException(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    public class SendMessageException extends Exception {

        final String message;

        public SendMessageException(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    public class TimeOutException extends Exception {

        final String message;

        public TimeOutException(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    public class P2PSocket {

        private String targetName;
        private InetAddress targetAddress;
        private int targetPort;
        private int selfPort = -1;

        private final byte[] inBuff = new byte[DATA_LEN];
        private DatagramPacket inPacket;
        private DatagramSocket socket;

        private P2PSocket() throws SocketException, P2PClientException {
            socket = new DatagramSocket();
            inPacket = new DatagramPacket(inBuff, inBuff.length, address, port);
            this.targetAddress = address;
            this.targetPort = port;
            this.selfPort = getSelfPort();
        }

        private int getSelfPort() throws P2PClientException {
            if(selfPort != -1) {
                return selfPort;
            }

            int i = 0;
            while(++i < 10) {
                try {
                    send("WHOAMI");
                    String selfInfo = receive(500);
                    String[] infos = selfInfo.split(":");
                    if(infos.length == 2) {
                        int port = Integer.valueOf(infos[1]);
                        log("Self Port Is " + port, 1);
                        return port;
                    }
                } catch (TimeOutException ignored) {}
            }
            throw new P2PClientException("Time Out!");
        }

        private void setTarget(String targetAddress, int targetPort, String targetName) throws UnknownHostException {
            this.targetAddress = InetAddress.getByName(targetAddress);
            this.targetPort = targetPort;
            this.targetName = targetName;

        }


        /** --------------------------------- Getter --------------------------------- **/
        public String getTargetName() {
            return targetName;
        }

        public InetAddress getTargetAddress() {
            return targetAddress;
        }

        public int getTargetPort() {
            return targetPort;
        }

        public DatagramSocket getSocket() {
            return socket;
        }

        /** --------------------------------- Public --------------------------------- **/
        public String receive(int time) throws TimeOutException {
            try {
                socket.setSoTimeout(time);
            } catch (SocketException e) {
                e.printStackTrace();
            }

            try {
                socket.receive(inPacket);
                String message = new String(inPacket.getData(), 0, inPacket.getLength());
                log("---> " + message, 1);
                return message;
            } catch (IOException ignored) {}

            throw new TimeOutException("Time Out");
        }

        public void send(String message) throws P2PClientException {
            byte[] bytes = message.getBytes();

            DatagramPacket outPacket = new DatagramPacket(bytes, bytes.length, targetAddress, targetPort);
            outPacket.setData(bytes);
            outPacket.setLength(bytes.length);
            outPacket.setAddress(targetAddress);
            outPacket.setPort(targetPort);

            try {
                socket.send(outPacket);
                log("<--- " + message, 1);
            } catch (IOException e) {
                e.printStackTrace();
                throw new P2PClientException("Send Message to " + targetAddress + " fail");
            }
        }
    }


    /** --------------------------------- Test --------------------------------- **/
    public static void main(String[] args) throws UnknownHostException {
        P2PClient p2pClient;

        try {
            p2pClient = new P2PClient("test.kaixinguo.site", 2018, "User-" + (int) (Math.random() * 1000));
            p2pClient.setUseRelay(true);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        P2PSocket p2pSocket = null;

        p2pClient.connect();
        int i = 0;
        while(i++ < 100) {
            try { Thread.sleep(3000);} catch (InterruptedException ignored) {}

            if(p2pClient.getStatus() == 2) {
                if(p2pSocket != null) {
                    System.exit(0);
                }

                List reqs = p2pClient.getReq();
                if(!reqs.isEmpty()) {
                    System.out.println("- Receive Request: " + reqs.get(0));
                    try {
                        p2pSocket = p2pClient.agreeReq(0, 40000);
                        System.out.println("- Create P2PSocket Success! -- " + p2pSocket);
                    } catch (P2PClientException | SocketException e) {
                        System.out.println("- Connect Fail...");
                    }
                    continue;
                }

                List<String> names = null;
                try {
                    System.out.println("- List...");
                    names = p2pClient.list();
                } catch (P2PClientException ignored) {
                    System.out.println("- List Fail");
                }
                String target = null;
                if(names != null){
                    int j = 0;
                    System.out.println("- List Of User(" + names.size() + "): ");
                    for(String name : names) {
                        if(p2pClient.name.equals(name)) {
                            System.out.println("-     [" + ++j + "] " + name + " [Self]");
                        } else {
                            target = name;
                            System.out.println("-     [" + ++j + "] " + name);
                        }
                    }
                }
                if(target != null) {
                    try {
                        System.out.println("- Connect to " + target + "...");
                        p2pSocket = p2pClient.getSocketTo(target, 40000);
                        System.out.println("- Create P2PSocket Success! -- " + p2pSocket);
                    } catch (P2PClientException | SocketException e) {
                        System.out.println("- Connect Fail...");
                    }
                }
            }
        }
        //p2pClient.close();
        System.out.println("EXIT");
    }
}