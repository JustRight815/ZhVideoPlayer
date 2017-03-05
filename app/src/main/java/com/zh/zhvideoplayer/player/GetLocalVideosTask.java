package com.zh.zhvideoplayer.player;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.zh.zhvideoplayer.bean.LocalVideoBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 异步获取本地视频列表
 */
public class GetLocalVideosTask extends AsyncTask<Object,Integer,List<LocalVideoBean>> {

    private OnSuccessListener onSuccessListener;
    
    @Override
    protected List<LocalVideoBean> doInBackground(Object... params) {
        ContentResolver contentResolver = (ContentResolver) params[0];
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projections = new String[]{
        		MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA
        };
        Cursor cursor = contentResolver.query(uri,projections,null,null,null);
        List<LocalVideoBean> videos = new ArrayList<LocalVideoBean>();
        while (cursor.moveToNext()){
            LocalVideoBean v = new LocalVideoBean(cursor.getString(0),cursor.getString(1),cursor.getLong(2),cursor.getString(3));
            videos.add(v);
        }
        cursor.close();
        return videos;
    }

    @Override
    protected void onPostExecute(List<LocalVideoBean> videos) {
        super.onPostExecute(videos);
        this.onSuccessListener.onSuccess(videos);
    }

    public interface OnSuccessListener{
        void onSuccess(List<LocalVideoBean> videos);
    }
    
    public void setOnSuccessListener(OnSuccessListener onSuccessListener){
        this.onSuccessListener = onSuccessListener;
    }
}
