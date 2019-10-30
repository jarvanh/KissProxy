package com.jarvanh.kissproxy_sample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

public class ProxySettings extends AppCompatActivity implements ServiceConnection,
        OnCheckedChangeListener {
    public static final String TAG = "ProxySettings";

    protected static final String KEY_PREFS = "proxy_pref";
    protected static final String KEY_ENABALE = "proxy_enable";

    private static int NOTIFICATION_ID = 1;

    private IProxyControl proxyControl = null;

    private TextView tvInfo;
    private CheckBox cbEnable;

    private NotificationManager mNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.proxy_settings);

        tvInfo = (TextView) findViewById(R.id.tv_info);
        cbEnable = (CheckBox) findViewById(R.id.cb_enable);
        cbEnable.setOnCheckedChangeListener(this);

        Intent intent = new Intent(this, ProxyService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onServiceConnected(ComponentName cn, IBinder binder) {
        proxyControl = (IProxyControl) binder;
        if (proxyControl != null) {
            updateProxy();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName cn) {
        proxyControl = null;
    }

    @Override
    protected void onDestroy() {
        unbindService(this);
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences sp = getSharedPreferences(KEY_PREFS, MODE_PRIVATE);
        sp.edit().putBoolean(KEY_ENABALE, isChecked).commit();
        updateProxy();
    }

    private void updateProxy() {
        if (proxyControl == null) {
            return;
        }

        boolean isRunning = false;
        try {
            isRunning = proxyControl.isRunning();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        boolean shouldRun = getSharedPreferences(KEY_PREFS, MODE_PRIVATE)
                .getBoolean(KEY_ENABALE, false);
        if (shouldRun && !isRunning) {
            startProxy();
        } else if (!shouldRun && isRunning) {
            stopProxy();
        }

        try {
            isRunning = proxyControl.isRunning();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (isRunning) {
            tvInfo.setText(R.string.proxy_on);
            cbEnable.setChecked(true);
        } else {
            tvInfo.setText(R.string.proxy_off);
            cbEnable.setChecked(false);
        }
    }

    private void startProxy() {
        boolean started = false;
        try {
            started = proxyControl.start();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (!started) {
            return;
        }

        runningNotification();

        Toast.makeText(this, getResources().getString(R.string.proxy_started),
                Toast.LENGTH_SHORT).show();
    }

    //running notification
    private void runningNotification() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //设置通知消息的跳转  -->   Intend 和PendingIntent 的使用
        Intent resultIntent = new Intent(this, ProxySettings.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, resultIntent, 0);
        //实例化通知栏构造器
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        //设置通知频道,适配android 8.0 版本
        NotificationChannel running;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            running = new NotificationChannel("running", getString(R.string.proxy_started), NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(running);
            mBuilder.setChannelId("running");
        }
        //设置Builder
        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_text))
//                .setColor(Color.parseColor("#E75A6C"))
                //Shows the time to send a notification
                .setShowWhen(true)
                //Set the notification priority
                .setPriority(NotificationCompat.PRIORITY_MAX)
                //Can not slide to delete
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setContentIntent(pi);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ProxySettings.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        Notification notification = mBuilder.build();
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void stopProxy() {
        boolean stopped = false;

        try {
            stopped = proxyControl.stop();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (!stopped) {
            return;
        }

        tvInfo.setText(R.string.proxy_off);

        //cancel notification
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);

        Toast.makeText(this, getResources().getString(R.string.proxy_stopped),
                Toast.LENGTH_SHORT).show();
    }
}