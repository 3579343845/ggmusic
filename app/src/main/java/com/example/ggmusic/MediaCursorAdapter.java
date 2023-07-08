package com.example.ggmusic;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cursoradapter.widget.CursorAdapter;

public class MediaCursorAdapter extends CursorAdapter {//继承CursorAdapter  用Cursor和ListView连接显示数据
    private Context mContext;//保存上下文对象
    private LayoutInflater mLayoutInflater;//布局解析器

    public MediaCursorAdapter(Context context){
        super(context,null,0);
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
    }

    //视图布局加载
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {//Cursor:ListView当前项的游标对象,读取相音乐属性字段;
        View itemView = mLayoutInflater.inflate(R.layout.list_item,viewGroup,false);//viewGroup视图容器

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
    public void bindView(View view, Context context, Cursor cursor) {// 用于绑定数据到视图
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

    public class ViewHolder {//暂存加载项视图布局后的各控件对象,避免通过findViewById()的方法重复进行查找绑定控件对象
        TextView tvTitle;
        TextView tvArtist;
        TextView tvOrder;
        View divider;
    }
}
