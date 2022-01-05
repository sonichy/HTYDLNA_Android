// https://www.cnblogs.com/yxwkf/p/3942214.html
package com.hty.dlna;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    EditText editText;
    Button button_discover, button_select, button_response;
    TextView textView;
    InputMethodManager IMM;
    List<DLNAClient> list_client = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IMM = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        editText = (EditText) findViewById(R.id.editText);
        button_discover = (Button) findViewById(R.id.button_discover);
        button_discover.setOnClickListener(new MyOnClickListener());
        button_select = (Button) findViewById(R.id.button_select);
        button_select.setOnClickListener(new MyOnClickListener());
        button_response = (Button) findViewById(R.id.button_response);
        button_response.setOnClickListener(new MyOnClickListener());
        textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "发现");
        menu.add(0, 1, 1, "关于");
        menu.add(0, 2, 2, "退出");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();
        switch (item_id) {
            case 0:
                textView.setText("");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        discover();
                    }
                }).start();
                break;
            case 1:
                new AlertDialog.Builder(this)
                        .setIcon(R.mipmap.ic_launcher)
                        .setTitle("海天鹰投屏 V1.0")
                        .setMessage("基于DLNA的投屏，支持Macast、极光TV、奇异果TV。\n作者：海天鹰\nE-mail: sonichy@163.com\nQQ: 84429027\n参考：\nhttps://github.com/sonichy/HTYDLNA")
                        .setPositiveButton("确定", null)
                        .show();
                break;
            case 2:
                finish();
                break;
        }
        return true;
    }

    class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_discover:
                    IMM.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    textView.setText("");
                    list_client.clear();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            discover();
                        }
                    }).start();
                    break;
                case R.id.button_select:
                    IMM.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    String items[] = new String[list_client.size()];
                    for (int i=0; i<list_client.size(); i++) {
                        items[i] = list_client.get(i).friendlyName;
                    }
                    AlertDialog.Builder ADB = new AlertDialog.Builder(MainActivity.this);
                    ADB.setTitle("选择投屏设备");
                    ADB.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final DLNAClient client = list_client.get(which);
                            for (int i=0; i<client.list_service.size(); i++) {
                                Map map = client.list_service.get(i);
                                //Log.e(Thread.currentThread().getStackTrace()[2] + "", map.toString());
                                final String controlURL;
                                //Log.e(Thread.currentThread().getStackTrace()[2] + "", map.get("serviceType") + " , " + map.get("controlURL"));
                                if (map.get("serviceType").toString().equals("urn:schemas-upnp-org:service:AVTransport:1")) {
                                    controlURL = map.get("controlURL").toString();
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String s = client.uploadFileToPlay(controlURL, editText.getText().toString());
                                            Message message = new Message();
                                            message.what = 1;
                                            message.obj = "投屏响应：\n" + s;
                                            handler.sendMessage(message);
                                        }
                                    }).start();
                                    break;
                                }
                            }
                        }
                    });
                    ADB.show();
                    break;
                case R.id.button_response:
                    IMM.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    for (int i=0; i<list_client.size(); i++) {
                        textView.setText(list_client.get(i).responseData);
                    }
                    break;
            }
        }
    }

    void discover() {
        String s = "M-SEARCH * HTTP/1.1\r\n"
                + "HOST: 239.255.255.250:1900\r\n"
                + "MAN: \"ssdp:discover\"\r\n"
                + "ST: urn:schemas-upnp-org:service:AVTransport:1\r\n" //投屏
                + "MX: 3\r\n"
                + "\r\n";

        try {
            MulticastSocket socket = new MulticastSocket(5800);
            InetAddress address = InetAddress.getByName("239.255.255.250");
            socket.joinGroup(address);
            DatagramPacket DP = new DatagramPacket(s.getBytes(), s.length(), address, 1900);
            socket.send(DP);
            Message message = new Message();
            message.what = 0;
            message.obj = "发送：\n" + s;
            handler.sendMessage(message);

            while (true) {
                byte[] buf = new byte[1024];
                DP = new DatagramPacket(buf, buf.length);
                socket.receive(DP);
                String IP = DP.getAddress().toString();
                int port = DP.getPort();
                s = new String(DP.getData());
                DLNAClient client = new DLNAClient(s);
                list_client.add(client);
                message = new Message();
                message.what = 0;
                message.obj = "接收：" + IP + ":" + port + "\n" + s;
                handler.sendMessage(message);
            }
        } catch (Exception e) {
            Message message = new Message();
            message.what = 0;
            message.obj = e.toString();
            handler.sendMessage(message);
        }
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    String s = (String) msg.obj;
                    textView.append(s);
                    break;
                case 1:
                    s = (String) msg.obj;
                    textView.setText(s);
                    break;
            }
        }
    };

}