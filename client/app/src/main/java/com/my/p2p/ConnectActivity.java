package com.my.p2p;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import p2p.com.my.com.R;

public class ConnectActivity extends AppCompatActivity {

    private P2PClient.P2PSocket socket;
    private boolean isRun = true;

    private String textToSend = null;
    private StringBuilder strBuilder = new StringBuilder();
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Thread thread = new Thread() {
        @Override
        public void run() {
            while(isRun) {
                if(textToSend != null) {
                    try {
                        socket.send(textToSend);
                        strBuilder.append("<== ");
                        strBuilder.append(df.format(new Date()));
                        strBuilder.append("\n");
                        strBuilder.append(textToSend);
                        strBuilder.append("\n\n");
                        connectHandler.sendEmptyMessage(MSG_UPDATE_HISTORY);
                        textToSend = null;
                    } catch (P2PClient.P2PClientException e) {
                        e.printStackTrace();
                        System.out.println("发送失败!");
                    }
                }
                try {
                    String receive = socket.receive(100);
                    strBuilder.append("==> ");
                    strBuilder.append(df.format(new Date()));
                    strBuilder.append("\n");
                    strBuilder.append(receive);
                    strBuilder.append("\n\n");
                    connectHandler.sendEmptyMessage(MSG_UPDATE_HISTORY);
                } catch (P2PClient.TimeOutException ignored) {}
            }
        }
    };

    private TextView textView;
    private EditText editText;
    private ScrollView scrollView;

    private final static int MSG_UPDATE_HISTORY = 1;
    private ConnectHandler connectHandler = new ConnectHandler(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connect);

        textView = (TextView) findViewById(R.id.textView_history);
        editText = (EditText) findViewById(R.id.editText_send);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        String name = getIntent().getStringExtra("name");
        for (Map<String, Object> map : ((P2PApplication) getApplication()).connects ) {
            if(name.equals(map.get("name"))) {
                socket = (P2PClient.P2PSocket) map.get("socket");
                break;
            }
            finish();
        }

        thread.start();
    }

    private void updateTextView() {
        textView.setText(strBuilder.toString());
        connectHandler.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public void send(View view) {
        textToSend = editText.getText().toString();
        editText.setText("");
    }

    private static class ConnectHandler extends Handler {

        private final WeakReference<ConnectActivity> mOuter;

        public ConnectHandler(ConnectActivity connectActivity) {
            mOuter = new WeakReference<>(connectActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            ConnectActivity connectActivity = mOuter.get();
            switch(msg.what) {
                case MSG_UPDATE_HISTORY:
                    connectActivity.updateTextView();
                    break;
                default:
                    break;
            }
        }
    }
}
