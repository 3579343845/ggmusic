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

    MediaPlayer mMediaPlayer;
    //标识前台通知的唯一ID
    private static final int ONGOING_NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "Music channel";
    NotificationManager mNotificationManager;
    private final IBinder mBinder = new MusicServiceBinder();
    //该实例通常向其他组件提供当前绑定服务对象实例的方法，从而使得其他组件可直接访问绑定服务对象的信息
    //IBinder是Android中用于实现跨进程通信（IPC）的接口，它是一个抽象类，定义了一些方法用于实现进程间的通信。
    //在Android中，一个进程无法直接调用另一个进程内的组件，例如Service或者Activity，因此需要通过IPC机制来实现跨进程通信。
    //而在Service组件中，我们通常需要将Service的一些功能暴露给其他组件使用，例如Activity等，这时就可以使用IBinder接口来实现跨进程通信。

    public MusicService() {
    }

    //停止音乐播放并释放资源
    //在服务销毁时调用。在这个方法中，停止音乐播放并释放相关资源。
    @Override
    public void onDestroy() {
        mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = null;
        super.onDestroy();
    }

    //在服务创建时调用。在这个方法中，创建一个新的 MediaPlayer 对象
    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = new MediaPlayer();
    }

    //这个方法在绑定服务时被调用，用于返回与服务通信的 IBinder 对象。在这里，将 mBinder 返回给客户端。
    @Override
    public IBinder onBind(Intent intent) {
        // 暂不使用绑定服务，返回 null
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return mBinder;
    }

    //暂停MediaPlayer音乐播放
    public void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }
    //继续MediaPlayer音乐播放
    public void play() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }
    //获取当前播放音乐总时长
    public int getDuration() {
        int duration = 0;
        if (mMediaPlayer != null) {
            duration = mMediaPlayer.getDuration();
        }
        return duration;
    }
    //获取当前音乐播放进度
    public int getCurrentPosition() {
        int position = 0;
        if (mMediaPlayer != null) {
            position = mMediaPlayer.getCurrentPosition();
        }
        return position;
    }
    //获取MediaPlayer音乐播放状态；
    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }
    //启动服务并初始化
    //在这个方法中，从 Intent 中获取音乐信息，设置 MediaPlayer 的数据源并开始播放音乐。
    //接下来，创建通知栏，并将服务设置为前台服务，显示通知。
    //Intent对象包含了启动服务时传递的数据
    //flags：表示启动请求的标志位，用于指示启动模式和启动标志。这些标志位可以帮助服务确定如何处理启动请求
    //startId：每个启动请求的唯一标识符。每次调用 startService() 方法启动服务时，都会分配一个新的 startId 值。
    // 可以使用这个值来区分不同的启动请求
    @Override
    public int onStartCommand(Intent intent,
                              int flags, int startId) {
        String title = intent.getStringExtra(MainActivity.TITLE);
        String artist = intent.getStringExtra(MainActivity.ARTIST);
        String data = intent.getStringExtra(
                MainActivity.DATA_URI);
        Uri dataUri = Uri.parse(data);

        if (mMediaPlayer != null) {
            try {
                //重置MediaPlayer对象
                mMediaPlayer.reset();
                //设置MediaPlayer的数据源为点击的音频文件的位置
                mMediaPlayer.setDataSource(
                        getApplicationContext(),
                        dataUri);
                //准备MediaPlayer以播放音频文件
                mMediaPlayer.prepare();
                //开始播放音频文件。
                mMediaPlayer.start();

                Intent musicStartIntent =
                        new Intent(MainActivity.ACTION_MUSIC_START);
                sendBroadcast(musicStartIntent);

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Music Channel", NotificationManager.IMPORTANCE_HIGH);

            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
        }
        //创建一个用于跳转到MainActivity的Intent对象。
        Intent notificationIntent =
                new Intent(getApplicationContext(),
                        MainActivity.class);
        //创建一个PendingIntent对象，用于表示当用户点击通知时，应该执行的操作。
        //在这里，我们将PendingIntent设置为跳转到MainActivity页面。
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        //创建一个用于构建通知的Builder对象
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        getApplicationContext(),
                        CHANNEL_ID);
        //设置通知的属性
        Notification notification = builder
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent).build();
        //调用startForeground()方法，将Service设置为前台运行状态，并传入ONGOING_NOTIFICATION_ID和notification参数。
        //这样，在Service运行时，通知将一直显示在状态栏中，提醒用户Service正在运行。
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        return super.onStartCommand(intent, flags, startId);
    }
    //提供绑定服务对象实例的方法,通过这个内部类，其他组件可以直接访问绑定服务对象的信息。
    public class MusicServiceBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
}