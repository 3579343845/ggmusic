package com.example.ggmusic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class MusicService extends Service {

    MediaPlayer mMediaPlayer;//标识前台通知的唯一ID
    private static final int ONGOING_NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "Music channel";
    NotificationManager mNotificationManager;
    private final IBinder mBinder = new MusicServiceBinder();

    public MusicService() {
    }

    @Override
    public void onDestroy() {//停止音乐播放并释放相关资源
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = new MediaPlayer();
    }

    @Override
    public IBinder onBind(Intent intent) {//这个方法在绑定服务时被调用，用于返回与服务通信的IBinder对象。在这里，将 mBinder 返回给客户端。
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    public void pause() {//暂停音乐播放
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    public void play() {//继续音乐播放
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }

    public int getDuration() {//获取当前播放音乐总时长
        int duration = 0;
        if (mMediaPlayer != null) {
            duration = mMediaPlayer.getDuration();
        }
        return duration;
    }

    public int getCurrentPosition() {//获取当前音乐播放进度
        int position = 0;
        if (mMediaPlayer != null) {
            position = mMediaPlayer.getCurrentPosition();
        }
        return position;
    }

    public boolean isPlaying() {//获取MediaPlayer音乐播放状态
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    //启动服务的信息及传递给服务的数据
    @Override
    public int onStartCommand(Intent intent,
                              int flags, int startId) {
        String title = intent.getStringExtra(MainActivity.TITLE);
        String artist = intent.getStringExtra(MainActivity.ARTIST);
        String data = intent.getStringExtra(
                MainActivity.DATA_URI);
        Uri dataUri = Uri.parse(data);

        if (mMediaPlayer != null) {//是否已经实例化
            try {
                mMediaPlayer.reset();//重置MediaPlayer对象
                mMediaPlayer.setDataSource(//设置MediaPlayer的数据源为点击的音频文件的位置
                        getApplicationContext(),
                        dataUri);

                mMediaPlayer.prepare();//准备MediaPlayer以播放音频文件
                mMediaPlayer.start();//开始播放音频文件

                Intent musicStartIntent = //创建一个广播Intent，并发送广播，通知音乐已开始播放
                        new Intent(MainActivity.ACTION_MUSIC_START);
                sendBroadcast(musicStartIntent);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {// 创建通知渠道
            mNotificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Music Channel", NotificationManager.IMPORTANCE_HIGH);

            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
        }

        Intent notificationIntent = //创建一个用于跳转到MainActivity的Intent对象。
                new Intent(getApplicationContext(),
                        MainActivity.class);

        PendingIntent pendingIntent = //创建一个PendingIntent对象，用于表示当用户点击通知时，应该执行的操作,跳转到MainActivity页面
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = //创建一个用于构建通知的Builder对象
                new NotificationCompat.Builder(
                        getApplicationContext(),
                        CHANNEL_ID);

        Notification notification = builder//设置通知的属性
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent).build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);//将Service设置为前台运行状态，在Service运行时，通知将一直显示在状态栏中，提醒用户Service正在运行。

        return super.onStartCommand(intent, flags, startId);
    }

    public class MusicServiceBinder extends Binder {//提供绑定服务对象实例的方法,通过这个内部类，其他组件可以直接访问绑定服务对象的信息。
        MusicService getService() {
            return MusicService.this;
        }
    }
}