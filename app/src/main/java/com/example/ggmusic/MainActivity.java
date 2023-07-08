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

    private final static String TAG = "Sucessfully";
    private final int REQUEST_EXTERNAL_STORAGE = 1;//返回码 可以判断是否有权限
    private static String[] PERMISSIONS_STORAGE = {//申请读写的权限
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private ContentResolver mContentResolver;//crud等  内容提供器

    private ListView mPlaylist;// 列表视图
    private MediaCursorAdapter mCursorAdapter;//适配器

    private final String SELECTION =
            MediaStore.Audio.Media.IS_MUSIC + " = ? " + " AND " +
                    MediaStore.Audio.Media.MIME_TYPE + " LIKE ? ";
    private final String[] SELECTION_ARGS = {//查询语句 "1" 表示选择音乐文件
            Integer.toString(1),
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
        mPlaylist = findViewById(R.id.lv_playlist);//
        mContentResolver = getContentResolver();
        mCursorAdapter = new MediaCursorAdapter(MainActivity.this);
        mPlaylist.setAdapter(mCursorAdapter);

        navigation = findViewById(R.id.navigation);//加载布局
        LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.bottom_media_toolbar,
                        navigation,
                        true);
        ivPlay = navigation.findViewById(R.id.iv_play);
        tvBottomTitle = navigation.findViewById(R.id.tv_bottom_title);
        tvBottomArtist = navigation.findViewById(R.id.tv_bottom_artist);
        ivAlbumThumbnail = navigation.findViewById(R.id.iv_thumbnail);
        navigation.setVisibility(View.GONE);

        //权限检测及申请
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {//是否已经获得指定的权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Log.d(TAG, "onCreate: 拒绝了权限请求");
            }
            else {
                requestPermissions(PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
                Log.d(TAG, "onCreate: 请求权限");
            }
        } else {
            initPlaylist();
        }


        mPlaylist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView,
                                    View view, int i, long l) {//i：被点击的列表项在适配器中的位置（索引）。l：被点击的列表项的行ID。
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
                    int dataIndex = cursor.getColumnIndex(//路径
                            MediaStore.Audio.Media.DATA);
                    String title = cursor.getString(titleIndex);
                    String artist = cursor.getString(artistIndex);
                    Long albumId = cursor.getLong(albumIdIndex);
                    String data = cursor.getString(dataIndex);
                    Uri dataUri = Uri.parse(data);
                    Intent serviceIntent = new Intent(MainActivity.this,
                            MusicService.class);
                    serviceIntent.putExtra(MainActivity.DATA_URI, data);
                    serviceIntent.putExtra(MainActivity.TITLE, title);
                    serviceIntent.putExtra(MainActivity.ARTIST, artist);
                    startService(serviceIntent);
                    Log.d("test:", "onItemClick: 歌曲"+title);


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
                mPlayStatus = !mPlayStatus;
                if (mPlayStatus == true) {
                    Log.d(TAG, "正在播放....");
                    mService.play();
                    ivPlay.setImageResource(R.drawable.baseline_pause_circle_outline_24);
                } else {
                    Log.d(TAG, "暂停播放....");
                    mService.pause();
                    ivPlay.setImageResource(R.drawable.baseline_play_circle_outline_24);
                }
            });
        }else {
            Log.d(TAG, "ivPlay null");
        }
        musicReceiver = new MusicReceiver();//广播接收器
        IntentFilter intentFilter = new IntentFilter();//指定要过滤的广播事件
        intentFilter.addAction(ACTION_MUSIC_START);//开始播放（广播）
        intentFilter.addAction(ACTION_MUSIC_STOP);//暂停播放（广播）
        registerReceiver(musicReceiver, intentFilter); //将 musicReceiver 注册为接收指定广播事件的接收器，使其能够接收到对应的广播。
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: 销毁");
        unregisterReceiver(musicReceiver);
        super.onDestroy();
    }

    //权限请求的结果返回。根据请求码和授权结果进行处理，用于处理权限请求的回调逻辑
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "开始获取数据");
                    initPlaylist();
                }
                break;
            default:
                break;
        }
    }

    private void initPlaylist(){ //初始化播放列表
        Cursor mCursor = mContentResolver.query(//查询结果
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                SELECTION,
                SELECTION_ARGS,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        );
        mCursorAdapter.swapCursor(mCursor);// 将新的Cursor对象与适配器相关联，以便在列表视图中显示新的数据
        mCursorAdapter.notifyDataSetChanged();// 通知适配器数据已更改，以便它可以更新列表视图以显示新的数据
    }

    private ServiceConnection mConn = new ServiceConnection() {//与音乐播放后台服务进行绑定和解绑操作的接口

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {//绑定成功时
            MusicService.MusicServiceBinder binder =
                    (MusicService.MusicServiceBinder) iBinder;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(//解绑成功时
                ComponentName componentName) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onStart() {//当活动可见并且用户可以与之交互时调用此方法。它绑定音乐播放后台服务，使得MainActivity可以与MusicService进行交互。
        super.onStart();
        Intent intent = new Intent(MainActivity.this,
                MusicService.class);
        bindService(intent, mConn, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "绑定!");
    }

    @Override
    protected void onStop() {//当活动不再可见或用户无法与之交互时调用此方法。它解绑音乐播放后台服务，断开与MusicService的连接。
        unbindService(mConn);
        mBound = false;
        super.onStop();
        Log.d(TAG, "解绑!");
    }


    private Handler mHandler = new Handler(Looper.getMainLooper()) {//接收和处理消息，并在主线程中更新UI
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

    private class MusicProgressRunnable implements Runnable {//在后台线程中获取音乐播放进度并发送更新消息的进程。
        public MusicProgressRunnable() {
        }

        @Override
        public void run() {
            boolean mThreadWorking = true;
            while (mThreadWorking) {
                try {
                    if (mService != null) {//判断是否绑定服务
                        int position = mService.getCurrentPosition();//获取当前音乐的播放进度
                        Message message = new Message();
                        message.what = UPDATE_PROGRESS;//将消息类型设置为更新进度的消息（UPDATE_PROGRESS）
                        message.arg1 = position;//将进度值设置为获取到的音乐播放进度（position）
                        mHandler.sendMessage(message);//通过 mHandler 发送消息给主线程进行处理和更新
                    }
                    mThreadWorking = mService.isPlaying();//判断音乐是否正在播放，如果正在播放则继续循环获取进度，否则退出循环。
                    Thread.sleep(100);//每次循环结束后，线程休眠100毫秒
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    public class MusicReceiver extends BroadcastReceiver {//用于接收特定的广播事件的广播接收器。它主要用于接收音乐播放开始和停止的广播事件，并在收到广播后更新音乐进度条。
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mService != null) {
                pbProgress = findViewById(R.id.progress);
                pbProgress.setMax(mService.getDuration());
                new Thread(new MusicProgressRunnable()).start();
            }
        }
    }

    private void loadingCover(String mediaUri) {//设置专辑图片
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();//获取音频和视频文件元数据信息的对象
        mediaMetadataRetriever.setDataSource(mediaUri);//传入路径
        byte[] picture = mediaMetadataRetriever.getEmbeddedPicture();//获取嵌入在媒体文件中的封面图片。
        if (picture == null || picture.length == 0) {
            Glide.with(MainActivity.this).load(R.drawable.music01).into(ivAlbumThumbnail);
        } else {
            Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
            Glide.with(MainActivity.this)
                    .load(bitmap)
                    .into(ivAlbumThumbnail);
        }
    }
}