package com.example.administrator.smstest;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private TextView sender;
    private TextView content;
    private IntentFilter receiveFilter;
    private MessageReceiver messageReceiver;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sender = (TextView) findViewById(R.id.sender);
        content = (TextView) findViewById(R.id.content);

        queryAuthority();
        //registerReceiver();
    }

    protected void onDestroy(){
        super.onDestroy();
        //取消注册
        unregisterReceiver(messageReceiver);
    }

    //查询权限
    private void queryAuthority() {
        int hasReadContactsPermission = 0;
        //Android Marshmallow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasReadContactsPermission = checkSelfPermission(Manifest.permission.RECEIVE_SMS);
        }

        if (hasReadContactsPermission != PackageManager.PERMISSION_GRANTED) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                请求授权对话框
                requestPermissions(new String[] {Manifest.permission.RECEIVE_SMS},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
            return;
        }
        //执行查询操作
        registerReceiver();
    }
    //为短信监听器注册（动态）
    public void registerReceiver(){
        receiveFilter = new IntentFilter();
        receiveFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        messageReceiver = new MessageReceiver();
        registerReceiver(messageReceiver,receiveFilter);
    }

    //回调函数，不论用户在授权对话框同意还是拒绝，activity的onRequestPermissionsResult会被回调来通知结果（通过第三个参数）
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    queryAuthority();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "READ_MESSAGE Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            Object []pdus = (Object[]) bundle.get("pdus");
            String format = intent.getStringExtra("format");
            SmsMessage[]messages = new SmsMessage[pdus.length];
            Log.d(TAG, "onReceive format is "+format+" !!!!!! ");
            for(int i=0;i<messages.length;i++){
//                createFromPdu(byte []pdu)方法已被废弃
//                上面的SmsMessage.createFromPdu方法已经被官方文档说明即将被废弃
//                原因是为了同时支持3GPP和3GPP2
//                他们是移动系统通信标准的拟定组织分别拟定了GSM/UMTS/LTE标准和CDMA/LTE标准
//                因此推荐是用的方法是createFromPdu(byte[] pdu, String format)
//                其中fotmat可以是SmsConstants.FORMAT_3GPP或者SmsConstants.FORMAT_3GPP2
                byte[] sms = (byte[])pdus[i];
                messages[i]  = SmsMessage.createFromPdu(sms,format);
            }
            //获取发送方手机号码
            String address = messages[0].getOriginatingAddress();
            String fullMessage = "";
            for(SmsMessage message:messages){
                //获取短信内容（短信内容太长会被分段）
                fullMessage += message.getMessageBody();
            }
            content.setText(fullMessage);
            sender.setText(address);
        }
    }
}
