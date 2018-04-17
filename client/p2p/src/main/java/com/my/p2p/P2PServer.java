package com.my.p2p;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class P2PServer {

    public static int LOG_LEVEL = 1;

    private final String serverAddress;
    private final int serverPort;
    private final String selfName;
    private boolean isAutoConnect;

    private P2PClient p2pClient;

    private boolean isRun = false;
    private boolean isOnline = false;
    private boolean useRelay = true;
    private String connectName = null;

    private Set<String> newRequests = new HashSet<>();
    private List<P2PClient.P2PSocket> newConnects = new ArrayList<>();

    private final Thread thread = new Thread() {
        @Override
        public void run() {
            new Thread() {
                @Override
                public void run() {
                    try {
                        if(p2pClient == null) {
                            try {
                                p2pClient = new P2PClient(serverAddress, serverPort, selfName);
                            } catch (UnknownHostException | SocketException e) {
                                e.printStackTrace();
                                log("P2PClient创建失败!");
                                return;
                            }
                        }

                        isRun = true;
                        while(isRun) {
                            //启动P2P客户端
                            if(p2pClient.getStatus() == -2) {
                                p2pClient.connect();
                            }

                            p2pClient.setUseRelay(useRelay);

                            //检查连接状况
                            if(p2pClient.getStatus() < 2) {
                                if(isOnline) {
                                    log("从服务器断开!");
                                }
                                isOnline = false;
                            } else {
                                if(!isOnline) {
                                    log("连接到服务器!");
                                }
                                isOnline = true;

                                //检查连接请求
                                List<String> reqs = p2pClient.getReq();
                                if(!reqs.isEmpty()) {
                                    int size = newRequests.size();
                                    newRequests.addAll(reqs);
                                    reqs.clear();
                                    if(newRequests.size() != size) {
                                        log("收到新的连接请求!");
                                    }
                                }

                                if(isAutoConnect && connectName == null && !newRequests.isEmpty()) {
                                    agreeRequest((String) newRequests.toArray()[0]);
                                }

                                //连接到指定用户
                                if(connectName != null) {
                                    String name = connectName;
                                    connectName = null;
                                    try {
                                        log("正在连接到指定用户(" + name + ")...");
                                        P2PClient.P2PSocket p2pSocket = p2pClient.getSocketTo(name);
                                        newConnects.add(p2pSocket);
                                        log("连接到指定用户(" + name + ")成功!");
                                    } catch (SocketException | P2PClient.P2PClientException e) {
                                        log("连接到指定用户(" + name + ")失败!");
                                    }
                                }

                                try { Thread.sleep(100);} catch (InterruptedException ignored) {}
                            }
                        }
                    } finally {
                        isRun = false;
                        System.out.println("EXIT");
                    }
                }
            }.start();
        }
    };

    public P2PServer(String host, int serverPort, String selfName) {
        this(host, serverPort, selfName, true);
    }

    public P2PServer(String serverAddress, int serverPort, String selfName, boolean isAutoConnect) {
        P2PClient.LOG_LEVEL = 0;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.selfName = selfName;
        this.isAutoConnect = isAutoConnect;
        thread.start();
    }

    public Set<String> getNewRequests() {
        return newRequests;
    }

    public void agreeRequest(String name) {
        newRequests.remove(name);
        connectName = name;
    }

    public List<P2PClient.P2PSocket> getNewConnects() {
        return newConnects;
    }

    private void log(String message) {
        log(message, 0);
    }

    private void log(String message, int p) {
        if(p < LOG_LEVEL) {
            System.out.println(selfName + " " + message);
        }
    }

    public static void main(String[] args) {
        new P2PServer("test.kaixinguo.site", 2018, "Server-" + (int) (Math.random() * 1000));
    }
}
