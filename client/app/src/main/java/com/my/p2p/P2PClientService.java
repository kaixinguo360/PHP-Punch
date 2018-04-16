package com.my.p2p;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.my.p2p.MainActivity.MSG_CONNECT_SUCCESS;
import static com.my.p2p.MainActivity.MSG_CONNECT_TO_SERVER;
import static com.my.p2p.MainActivity.MSG_LOST_CONNECT_TO_SERVER;
import static com.my.p2p.MainActivity.MSG_RECEIVE_REQUEST;
import static com.my.p2p.MainActivity.MSG_SEND_MESSAGE;
import static com.my.p2p.MainActivity.MSG_UPDATE_LIST;
import static com.my.p2p.MainActivity.MSG_UPDATE_LIST_FAILED;
import static com.my.p2p.P2PApplication.ACTION_START_SERVICE;

public class P2PClientService extends IntentService {

    private P2PClient p2pClient;

    private String name = null;
    private String address = null;
    private int port= -1;

    private boolean isRun = false;
    private boolean isGetList = false;
    private boolean isConnect = false;
    private boolean useRelay = true;

    private String connectName = null;

    private Set<String> reqs;
    private List<Map<String, Object>> users;
    private List<Map<String, Object>> requests;

    private Handler mainHander;

    private final Thread p2pThread = new Thread() {
        @Override
        public void run() {
            new Thread() {
                @Override
                public void run() {
                    try {
                        if(p2pClient == null) {
                            try {
                                p2pClient = new P2PClient("test.kaixinguo.site", 2018, name);
                            } catch (UnknownHostException | SocketException e) {
                                e.printStackTrace();
                                P2PClientService.this.notify("P2PClient创建失败!");
                                return;
                            }
                        }

                        int timeToGetList = 0;
                        int timeToRetryGetList = 0;
                        isRun = true;
                        while(isRun) {
                            //启动P2P客户端
                            if(p2pClient.getStatus() == -2) {
                                p2pClient.connect();
                            }

                            p2pClient.setUseRelay(useRelay);

                            //检查连接状况
                            if(p2pClient.getStatus() < 2) {
                                if(((P2PApplication) getApplication()).isOnline) {
                                    sendEmptyMessage(MSG_LOST_CONNECT_TO_SERVER);
                                }
                                ((P2PApplication) getApplication()).isOnline = false;
                            } else {
                                if(!((P2PApplication) getApplication()).isOnline) {
                                    sendEmptyMessage(MSG_CONNECT_TO_SERVER);
                                }
                                ((P2PApplication) getApplication()).isOnline = true;

                                //刷新在线用户列表
                                if(++timeToGetList > 30) {
                                    timeToGetList = 0;
                                    isGetList = true;
                                }
                                if(isGetList) {
                                    isGetList = false;

                                    List<String> list;
                                    try {
                                        list = p2pClient.list();

                                        users.clear();
                                        for(String name : list) {
                                            if(!p2pClient.getName().equals(name)) {
                                                HashMap<String, Object> map = new HashMap<>();
                                                map.put("name", name);
                                                users.add(map);
                                            }
                                        }

                                        sendEmptyMessage(MSG_UPDATE_LIST);
                                        timeToRetryGetList = 0;
                                    } catch (P2PClient.P2PClientException e) {
                                        //e.printStackTrace();
                                        if(++timeToRetryGetList < 3) {
                                            //P2PClientService.this.notify("获取在线用户列表失败!");
                                            sendEmptyMessage(MSG_UPDATE_LIST_FAILED);
                                        } else {
                                            //P2PClientService.this.notify("与服务器失去联系!");
                                            p2pClient.reLogin();
                                        }
                                    }
                                }

                                //连接到指定用户
                                if(isConnect && connectName != null) {
                                    String name = connectName;
                                    isConnect = false;
                                    connectName = null;
                                    try {
                                        P2PClientService.this.notify("正在连接到指定用户..");
                                        P2PClient.P2PSocket p2pSocket = p2pClient.getSocketTo(name);
                                        Message msg = new Message();
                                        msg.what = MSG_CONNECT_SUCCESS;
                                        msg.obj = p2pSocket;
                                        sendMessage(msg);
                                    } catch (SocketException | P2PClient.P2PClientException e) {
                                        e.printStackTrace();
                                        P2PClientService.this.notify("连接到指定用户失败!");
                                    }
                                }

                                //检查连接请求
                                List<String> reqs = p2pClient.getReq();
                                if(!reqs.isEmpty()) {
                                    int size = P2PClientService.this.reqs.size();
                                    P2PClientService.this.reqs.addAll(reqs);
                                    if(P2PClientService.this.reqs.size() != size) {
                                        requests.clear();
                                        for(String name : P2PClientService.this.reqs) {
                                            Map<String, Object> map = new HashMap<>();
                                            map.put("name", name);
                                            requests.add(map);
                                        }
                                        sendEmptyMessage(MSG_RECEIVE_REQUEST);
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

    public P2PClientService() {
        super("P2PClientService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if(intent != null) {
            String action = intent.getAction();

            if(ACTION_START_SERVICE.equals(action) && !isRun) {

                name = intent.getStringExtra("name");
                address = intent.getStringExtra("server");
                port = intent.getIntExtra("port", 2018);

                reqs = ((P2PApplication) getApplication()).reqs;
                users = ((P2PApplication) getApplication()).users;
                requests = ((P2PApplication) getApplication()).requests;

                p2pThread.start();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public void notify(String text) {
        if(mainHander != null) {
            Message message = new Message();
            message.what = MSG_SEND_MESSAGE;
            message.obj = text;
            sendMessage(message);
        }
    }

    public void sendEmptyMessage(int what) {
        if(mainHander != null) {
            mainHander.sendEmptyMessage(what);
        }
    }

    public void sendMessage(Message message) {
        if(mainHander != null) {
            mainHander.sendMessage(message);
        }
    }

    public class Binder extends android.os.Binder {
        public String getName() {
            return name;
        }

        public void updateUserList() {
            if(p2pClient.getStatus() >= 2) {
                isGetList = true;
            } else {
                sendEmptyMessage(MSG_UPDATE_LIST_FAILED);
            }
        }

        public void connectToUser(String name) {
            if(p2pClient.getStatus() >= 2) {
                isConnect = true;
                connectName = name;
            } else {
                P2PClientService.this.notify("未连接到服务器!");
            }
        }

        public void setHandler(Handler handler) {
            mainHander = handler;
        }
    }
}
