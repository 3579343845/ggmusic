package com.example.ggmusic;

import static android.widget.Toast.LENGTH_SHORT;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity{

    private final static String TAG = "MyTest";
    private final int REQUEST_EXTERNAL_STORAGE = 1;//返回码  可以判断是否有权限
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE  //申请读写的权限
    };
    private ContentResolver mContentResolver;//进行一些操作 查询、插入、删除等
    //内容提供器  读取和操作数据或者创建自己的内容提供器给程序的数据
    private ListView mPlaylist;// 列表视图
    private MediaCursorAdapter mCursorAdapter;//适配器

    private final String SELECTION =
            MediaStore.Audio.Media.IS_MUSIC + " = ? " + " AND " +
                    MediaStore.Audio.Media.MIME_TYPE + " LIKE ? ";
    //定义了查询语句
    //IS_MUSIC表示音频文件是否属于音乐类型的元数据字段
    //MIME_TYPE表示音频文件的MIME类型，其中MP3文件对应的MIME类型为audio/mpeg
    private final String[] SELECTION_ARGS = {
            //指定音频文件是否属于音乐类型的元数据字段的值。
            //这里使用 "1" 表示选择音乐文件，因为音乐文件的元数据中的该字段通常为 1 表示是音乐文件。
            Integer.toString(1),
            //mp3文件
            "audio/mpeg"
    };
    private BottomNavigationView navigation;
    private TextView tvBottomTitle;
    private TextView tvBottomArtist;
    private ImageView ivAlbumThumbnail;
    private ImageView ivPlay;
    private MediaPlayer mMediaPlayer = null;
    private Boolean mPlayStatus = true;
    public static final String DATA_URI =
            "com.example.ggmusic.DATA_URI";
    public static final String TITLE =
            "com.example.ggmusic.TITLE";
    public static final String ARTIST =
            "com.example.ggmusic.ARTIST";
    //服务对象
    private MusicService mService;
    private boolean mBound = false;
    public static final int UPDATE_PROGRESS = 1;
    private ProgressBar pbProgress;
    public static final String ACTION_MUSIC_START = "com.example.ggmusic.ACTION_MUSIC_START";
    private static final String ACTION_MUSIC_STOP = "com.example.ggmusic.ACTION_MUSIC_STOP";
    private MusicReceiver musicReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "开始 ");

        mPlaylist = findViewById(R.id.lv_playlist);
        mContentResolver = getContentResolver();// 获取该类的实例
        mCursorAdapter = new MediaCursorAdapter(MainActivity.this);
        mPlaylist.setAdapter(mCursorAdapter);
        // 初始化UI元素
        navigation = findViewById(R.id.navigation);
        LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.bottom_media_toolbar,
                        navigation,
                        true);
        ivPlay = navigation.findViewById(R.id.iv_play);
        tvBottomTitle = navigation.findViewById(R.id.tv_bottom_title);
        tvBottomArtist = navigation.findViewById(R.id.tv_bottom_artist);
        ivAlbumThumbnail = navigation.findViewById(R.id.iv_thumbnail);
        navigation.setVisibility(View.GONE);
        //是否获得权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Log.d(TAG, "onCreate: 拒绝了权限请求");
                // 如果之前用户拒绝了权限请求，可以在这里给出相关解释
            }
            else {
                requestPermissions(PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                Log.d(TAG, "onCreate: 请求权限");
                // 否则，请求权限
            }
        } else {
            // 已有权限，初始化播放列表
            initPlaylist();
        }

        //给listview绑定点击事件
        mPlaylist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            adapterView：触发点击事件的适配器视图，即ListView。
//            view：被点击的列表项的视图。
//            i：被点击的列表项在适配器中的位置（索引）。
//            l：被点击的列表项的行ID。
            @Override
            public void onItemClick(AdapterView<?> adapterView,
                                    View view, int i, long l) {
                //保证每次点击都会转化成开始按钮
                ivPlay.setImageResource(R.drawable.baseline_pause_circle_outline_24);
                mPlayStatus = true;
                Log.d(TAG, "onItemClick: 切换成开始按钮");
                
                Cursor cursor = mCursorAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(i)) {

                    int titleIndex = cursor.getColumnIndex(
                            MediaStore.Audio.Media.TITLE);
                    int artistIndex = cursor.getColumnIndex(
                            MediaStore.Audio.Media.ARTIST);
                    int albumIdIndex = cursor.getColumnIndex(
                            MediaStore.Audio.Media.ALBUM_ID);
                    int dataIndex = cursor.getColumnIndex(
                            MediaStore.Audio.Media.DATA);

                    String title = cursor.getString(titleIndex);
                    String artist = cursor.getString(artistIndex);
                    Long albumId = cursor.getLong(albumIdIndex);
                    String data = cursor.getString(dataIndex);
                    //将字符串类型的data解析为Uri对象。这个Uri对象表示音频文件的位置。
                    Uri dataUri = Uri.parse(data);

                    Intent serviceIntent = new Intent(MainActivity.this,
                            MusicService.class);
                    serviceIntent.putExtra(MainActivity.DATA_URI, data);
                    serviceIntent.putExtra(MainActivity.TITLE, title);
                    serviceIntent.putExtra(MainActivity.ARTIST, artist);
                    startService(serviceIntent);

                    Log.d("mytest", "onItemClick: 歌曲"+title);


                    navigation.setVisibility(View.VISIBLE);

                    if (tvBottomTitle != null) {
                        tvBottomTitle.setText(title);
                    }
                    if (tvBottomArtist != null) {
                        tvBottomArtist.setText(artist);
                    }
                    loadingCover(data);

                }
            }
        });

        if (ivPlay != null) {
            ivPlay.setOnClickListener(v ->{
                Log.d(TAG, "onClick: iv_play");
                mPlayStatus = !mPlayStatus;//设置播放的状态  比如从播放到暂停   从暂停到播放
                if (mPlayStatus == true) {
                    Log.d(TAG, "正在播放....");
                    mService.play();
                    ivPlay.setImageResource(R.drawable.baseline_pause_circle_outline_24);
                } else {
                    //变为暂停状态

                    Log.d(TAG, "暂停播放....");
                    mService.pause();
                    ivPlay.setImageResource(R.drawable.baseline_play_circle_outline_24);
                }
            });
        }else {
            Log.d(TAG, "ivPlay null");
        }
        //广播接收器
        musicReceiver = new MusicReceiver();
        //指定要过滤的广播事件
        IntentFilter intentFilter = new IntentFilter();
        //开始播放（广播）
        intentFilter.addAction(ACTION_MUSIC_START);
        //暂停播放（广播）
        intentFilter.addAction(ACTION_MUSIC_STOP);
        //将 musicReceiver 注册为接收指定广播事件的接收器，使其能够接收到对应的广播。
        registerReceiver(musicReceiver, intentFilter);
    }
    //活动即将被销毁时调用此方法，它取消注册广播接收器并执行必要的清理操作
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: 销毁");
        unregisterReceiver(musicReceiver);
        super.onDestroy();
    }
    //当权限请求的结果返回时调用此方法。它根据请求码和授权结果进行处理，通常用于处理权限请求的回调逻辑。
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "开始获取数据");
                    initPlaylist();//获取权限后，初始化播放列表
                }
                break;
            default:
                break;
        }
    }
    //初始化播放列表。它使用内容解析器查询外部存储中的音频媒体文件，并将查询结果与适配器相关联，以便在列表视图中显示数据。
    private void initPlaylist(){
        //查询结果
        // 通过内容解析器查询外部存储中的音频媒体文件
        // 查询结果返回一个Cursor对象，该对象包含了查询结果集中的所有行和列
        Cursor mCursor = mContentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                //音乐类型
                SELECTION,
                SELECTION_ARGS,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        );
        // 将新的Cursor对象与适配器相关联，以便在列表视图中显示新的数据
        mCursorAdapter.swapCursor(mCursor);
        // 通知适配器数据已更改，以便它可以更新列表视图以显示新的数据
        mCursorAdapter.notifyDataSetChanged();
    }
    //与音乐播放后台服务进行绑定和解绑操作的接口
    //通过IBinder对象获取MusicService实例，并将其赋值给mService对象。
    //在onServiceDisconnected()方法中，将mService对象置为null。
    private ServiceConnection mConn = new ServiceConnection() {
        //绑定成功时
        //onServiceConnected()方法中第二个参数IBinder是由MusicService的onBind()方法返回的IBinder对象，
        // 由于我们在onBind()方法中返回的是MusicServiceBinder类实例，
        // 因此可以通过MusicServiceBinder类的getService方法获得MusicService实例，
        // 并将MusicService实例赋值给mService对象，从而确保在MainActivity中获得MusicService的对象引用，
        // 这样就可以在MainActivity中使用MusicService所暴露出来的几个接口(pause()、play()
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MusicServiceBinder binder =
                    (MusicService.MusicServiceBinder) iBinder;
            mService = binder.getService();
            mBound = true;
        }
        //解绑成功时
        @Override
        public void onServiceDisconnected(
                ComponentName componentName) {
            mService = null;
            mBound = false;
        }
    };
    //当活动可见并且用户可以与之交互时调用此方法。它绑定音乐播放后台服务，使得MainActivity可以与MusicService进行交互。
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(MainActivity.this,
                MusicService.class);
        //绑定服务
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "绑定!");
    }
    //当活动不再可见或用户无法与之交互时调用此方法。它解绑音乐播放后台服务，断开与MusicService的连接。
    @Override
    protected void onStop() {
        //解绑服务
        unbindService(mConn);
        mBound = false;
        super.onStop();
        Log.d(TAG, "解绑!");
    }

    //接收和处理消息，并在主线程中更新UI
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        //处理接收到的消息。在这里，它根据消息的类型（msg.what）进行判断，
        //如果是更新进度的消息（UPDATE_PROGRESS），则从消息中获取进度值（msg.arg1），
        //并将该进度值设置到进度条控件（pbProgress.setProgress(position)）中
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_PROGRESS:
                    int position = msg.arg1;
                    pbProgress.setProgress(position);
                    break;
                default:
                    break;
            }
        }
    };

    //在后台线程中获取音乐播放进度并发送更新消息的进程。
    //它循环获取当前音乐的播放进度，并通过mHandler发送更新消息给主线程
    private class MusicProgressRunnable implements Runnable {
        public MusicProgressRunnable() {
        }

        @Override
        public void run() {
            boolean mThreadWorking = true;
            while (mThreadWorking) {
                try {
                    //判断是否绑定服务
                    if (mService != null) {
                        //获取当前音乐的播放进度
                        int position =
                                mService.getCurrentPosition();
                        Message message = new Message();
                        //将消息类型设置为更新进度的消息（UPDATE_PROGRESS）
                        message.what = UPDATE_PROGRESS;
                        //将进度值设置为获取到的音乐播放进度（position）
                        message.arg1 = position;
                        //通过 mHandler 发送消息给主线程进行处理和更新
                        mHandler.sendMessage(message);
                    }
                    //判断音乐是否正在播放，如果正在播放则继续循环获取进度，否则退出循环。
                    mThreadWorking = mService.isPlaying();
                    //每次循环结束后，线程休眠100毫秒
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }
    //用于接收特定的广播事件的广播接收器。它主要用于接收音乐播放开始和停止的广播事件，并在收到广播后更新音乐进度条。
    public class MusicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mService != null) {
                pbProgress = findViewById(R.id.progress);
                pbProgress.setMax(mService.getDuration());
                new Thread(new MusicProgressRunnable()).start();
            }
        }
    }
    //设置专辑图片
    private void loadingCover(String mediaUri) {
        //获取音频和视频文件元数据信息的对象
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        //传入路径
        mediaMetadataRetriever.setDataSource(mediaUri);
        //获取嵌入在媒体文件中的封面图片。
        byte[] picture = mediaMetadataRetriever.getEmbeddedPicture();
        //没有获取到
        if (picture == null || picture.length == 0) {
            //只定义封面图片
            Glide.with(MainActivity.this).load(R.drawable.music01).into(ivAlbumThumbnail);
        } else {
            //将获取到的图片转换为Bitmap对象
            Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
            //将获取到的图片放进控件中
            Glide.with(MainActivity.this)
                    .load(bitmap)
                    .into(ivAlbumThumbnail);
        }
    }
}