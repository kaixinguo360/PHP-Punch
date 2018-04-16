package com.my.p2p;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import p2p.com.my.com.R;

import static com.my.p2p.P2PApplication.ACTION_START_SERVICE;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public final static int MSG_UPDATE_LIST = 1;
    public final static int MSG_CONNECT_SUCCESS = 2;
    public final static int MSG_RECEIVE_REQUEST = 3;
    public final static int MSG_SEND_MESSAGE = 4;
    public final static int MSG_CONNECT_TO_SERVER = 5;
    public final static int MSG_LOST_CONNECT_TO_SERVER = 6;
    public final static int MSG_UPDATE_LIST_FAILED = 7;

    private String title = "在线用户";

    private final Map<String, SimpleAdapter> adapters = new HashMap<>();

    private FloatingActionButton fab;
    private ListView listView;
    private TextView textView_user_name;
    private Menu menu;

    private final Handler mainHander = new MainHandler(this);
    private P2PClientService.Binder binder;

    private boolean isUpdating = false;

    private Set<String> reqs;
    private List<Map<String, Object>> users;
    private List<Map<String, Object>> requests;
    private List<Map<String, Object>> connects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        reqs = ((P2PApplication) getApplication()).reqs;
        users = ((P2PApplication) getApplication()).users;
        requests = ((P2PApplication) getApplication()).requests;
        connects = ((P2PApplication) getApplication()).connects;

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                update();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        textView_user_name = (TextView) navigationView.getHeaderView(0).findViewById(R.id.text_view_user_name);
        menu = navigationView.getMenu();

        listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                String name = (String)((TextView)view.findViewById(R.id.text_view_name)).getText();

                if("已连接".equals(title)) {
                    openConnect(name);
                    return;
                }

                if("连接请求".equals(title)) {
                    removeRequest(name);
                    mainHander.sendEmptyMessage(MSG_UPDATE_LIST);
                }

                binder.connectToUser(name);
            }
        });

        adapters.put("在线用户",
                new SimpleAdapter(MainActivity.this, users, R.layout.list,
                        new String[]{"name"}, new int[]{R.id.text_view_name}));
        adapters.put("连接请求",
                new SimpleAdapter(MainActivity.this, requests, R.layout.list,
                        new String[]{"name"}, new int[]{R.id.text_view_name}));
        adapters.put("已连接",
                new SimpleAdapter(MainActivity.this, connects, R.layout.list,
                        new String[]{"name"}, new int[]{R.id.text_view_name}));

        //启动P2P服务
        Intent intent = new Intent(this, P2PClientService.class);
        intent.setAction(ACTION_START_SERVICE);
        intent.putExtra("name", "Android-" + (int) (Math.random() * 1000));
        intent.putExtra("server", "test.kaixinguo.site");
        intent.putExtra("port", 2018);
        startService(intent);

        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                binder = (P2PClientService.Binder) iBinder;
                binder.setHandler(mainHander);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                binder.setHandler(null);
            }
        }, Context.BIND_AUTO_CREATE);

        updateUI();

        update();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_update) {
            update();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_users) {
            this.title = "在线用户";
        } else if (id == R.id.nav_requests) {
            this.title = "连接请求";
        } else if (id == R.id.nav_connects) {
            this.title = "已连接";
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        updateUI();

        return true;
    }


    /** --------------------------------- Private --------------------------------- **/
    private void update() {
        if(binder != null) {
            isUpdating = true;
            notify("正在刷新...");
            binder.updateUserList();
        }
    }

    private void updateUI() {

        if(((P2PApplication) getApplication()).isOnline) {
            String name = "未登陆";
            if(binder != null) {
                name = binder.getName();
            }
            setTitle(name + " - " + title);
            textView_user_name.setText(name);
        } else {
            setTitle("未登录" + " - " + title);
            textView_user_name.setText("未登录");
        }

        String requestStr = "连接请求";
        int reqNum = requests.size();
        if(reqNum > 0) {
            requestStr += " (" + reqNum + ")";
        }
        menu.getItem(1).setTitle(requestStr);

        SimpleAdapter adapter = adapters.get(title);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void notify(String message) {
        Snackbar.make(fab, message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    private void openConnect(String name) {
        removeRequest(name);
        Intent intent = new Intent(MainActivity.this, ConnectActivity.class);
        intent.putExtra("name", name);
        startActivity(intent);
    }

    private void removeRequest(String name) {
        reqs.remove(name);
        Iterator<Map<String, Object>> it = requests.iterator();
        while(it.hasNext()) {
            if(name.equals(it.next().get("name"))) {
                it.remove();
                break;
            }
        }
    }

    private static class MainHandler extends Handler {

        private final WeakReference<MainActivity> mOuter;

        public MainHandler(MainActivity mainActivity) {
            mOuter = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mOuter.get();
            switch(msg.what) {
                case MSG_UPDATE_LIST:
                    if(mainActivity.isUpdating) {
                        mainActivity.isUpdating = false;
                        mainActivity.notify("刷新成功!");
                    }
                    mainActivity.updateUI();
                    break;
                case MSG_RECEIVE_REQUEST:
                    mainActivity.notify("收到新的连接请求(" + mainActivity.requests.size() + ") !");
                    mainActivity.updateUI();
                    break;
                case MSG_CONNECT_SUCCESS:
                    P2PClient.P2PSocket p2pSocket = (P2PClient.P2PSocket) msg.obj;
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", p2pSocket.getTargetName());
                    map.put("time", mainActivity.connects.size());
                    map.put("socket", p2pSocket);
                    mainActivity.connects.add(map);
                    mainActivity.notify("连接到 " + p2pSocket.getTargetName() + " 成功!");
                    mainActivity.removeRequest(p2pSocket.getTargetName());
                    if("已连接".equals(mainActivity.title)) {
                        mainActivity.updateUI();
                    }
                    mainActivity.openConnect(p2pSocket.getTargetName());
                    break;
                case MSG_SEND_MESSAGE:
                    mainActivity.notify((String) msg.obj);
                    break;
                case MSG_CONNECT_TO_SERVER:
                    mainActivity.notify("连接到服务器成功!");
                    mainActivity.updateUI();
                    break;
                case MSG_LOST_CONNECT_TO_SERVER:
                    mainActivity.notify("与服务器的连接断开!");
                    mainActivity.updateUI();
                    break;
                case MSG_UPDATE_LIST_FAILED:
                    if(mainActivity.isUpdating) {
                        mainActivity.isUpdating = false;
                        mainActivity.notify("刷新失败!");
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
