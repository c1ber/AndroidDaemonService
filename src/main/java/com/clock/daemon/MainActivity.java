package com.clock.daemon;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.clock.daemon.service.BackgroundService;
import com.clock.daemon.service.GrayService;
import com.clock.daemon.service.WhiteService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    /**
     * 黑色唤醒广播的action
     */
    private final static String BLACK_WAKE_ACTION = "com.wake.black";

    private ServiceConnection mGrayServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "GrayService->onServiceConnected");
            mBinderPool = BinderPool.Stub.asInterface(service);
            try {
                service.linkToDeath(mGrayBinderDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "GrayService->onServiceConnected");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) { //进程死亡或者Binder键发生断裂会产生回调
            Log.e(TAG, "GrayService->onServiceDisconnected");
            mBinderPool.asBinder().unlinkToDeath(mGrayBinderDeathRecipient, 0);
            mBinderPool = null;
        }
    };

    private IBinder.DeathRecipient mGrayBinderDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() { //进程死亡或者Binder键发生断裂会产生回调
            Log.e(TAG, "Gray ->binderDied");
        }
    };

    private BinderPool mBinderPool = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_white).setOnClickListener(this);
        findViewById(R.id.btn_gray).setOnClickListener(this);
        findViewById(R.id.btn_black).setOnClickListener(this);
        findViewById(R.id.btn_background_service).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.btn_white) { //系统正常的前台Service，白色保活手段
            Intent whiteIntent = new Intent(this, WhiteService.class);
            startService(whiteIntent);

        } else if (viewId == R.id.btn_gray) {//利用系统漏洞，灰色保活手段（API < 18 和 API >= 18 两种情况）
            Intent grayIntent = new Intent(this, GrayService.class);
            startService(grayIntent);
            bindService(grayIntent, mGrayServiceConnection, Context.BIND_AUTO_CREATE);

        } else if (viewId == R.id.btn_black) { //拉帮结派，黑色保活手段，利用广播唤醒队友
            Intent blackIntent = new Intent();
            blackIntent.setAction(BLACK_WAKE_ACTION);
            sendBroadcast(blackIntent);

            /*AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            PendingIntent operation = PendingIntent.getBroadcast(this, 123, blackIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis(), operation);*/

        } else if (viewId == R.id.btn_background_service) {//普通的后台进程
            Intent bgIntent = new Intent(this, BackgroundService.class);
            startService(bgIntent);

        }
    }
}
