package com.example.ggmusic;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cursoradapter.widget.CursorAdapter;

public class MediaCursorAdapter extends CursorAdapter {
    //CursorAdapter这个类是继承于BaseAdapter的它是一个虚类它为 Cursor和ListView连接提供了桥梁
    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public MediaCursorAdapter(Context context){// MediaCursorAdapter类继承自CursorAdapter，它是一个用于将Cursor和ListView连接的桥梁。
        super(context,null,0);
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        //只在实例化的时候调用,数据增加的时候也会调用,但是在重绘(比如修改条目里的TextView的内容)的时候不会被调用
        //主要做项视图布局的加载操作
        View itemView = mLayoutInflater.inflate(R.layout.list_item,viewGroup,false);
        // 将list_item.xml布局文件转换为View对象。
        // 在这个例子中，itemView是一个View对象，它包含了列表视图中每个项的布局
        if(itemView != null){
            ViewHolder vh = new ViewHolder();
            vh.tvTitle = itemView.findViewById(R.id.tv_title);
            vh.tvArtist = itemView.findViewById(R.id.tv_artist);
            vh.tvOrder = itemView.findViewById(R.id.tv_order);
            vh.divider = itemView.findViewById(R.id.divider);
            itemView.setTag(vh);
            return itemView;
        }
        return null;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // 在绘制每个Item之前调用，也会在重绘时调用。
        // 用于绑定数据到视图
        ViewHolder vh = (ViewHolder) view.getTag();

        int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

        String title = cursor.getString(titleIndex);
        String artist = cursor.getString(artistIndex);

        int position = cursor.getPosition();

        if (vh != null) {
            vh.tvTitle.setText(title);
            vh.tvArtist.setText(artist);
            vh.tvOrder.setText(Integer.toString(position+1));
        }
    }

    public class ViewHolder {
        TextView tvTitle;
        TextView tvArtist;
        TextView tvOrder;
        View divider;
    }
}
