package com.zh.zhvideoplayer.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.provider.MediaStore.Video.Thumbnails;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.zh.zhvideoplayer.R;
import com.zh.zhvideoplayer.bean.LocalVideoBean;
import com.zh.zhvideoplayer.util.StringUtils;

import java.util.List;
/**
 * 本地视频列表的适配器
 * 
 */
public class LocalVideoListAdapter extends BaseAdapter {

	private List<LocalVideoBean> mVideoList;
	private Context mContext;
	private LocalVideoImageLoader mVideoImageLoader;
	
	
	public LocalVideoListAdapter(List<LocalVideoBean> videoList, Context context) {
		super();
		this.mVideoList = videoList;
		this.mContext = context;
		mVideoImageLoader = new LocalVideoImageLoader(context);// 初始化缩略图载入方法
	}

	@Override
	public int getCount() {
		return mVideoList.size();
	}

	@Override
	public LocalVideoBean getItem(int position) {
		return mVideoList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if(convertView == null){
			convertView = LayoutInflater.from(mContext).inflate(R.layout.video_local_video_item, null);
			viewHolder = new ViewHolder(convertView);
			convertView.setTag(viewHolder);			
		} else{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.setData(mVideoList.get(position));
		viewHolder.image.setTag(mVideoImageLoader.hashKeyForDisk(mVideoList.get(position).path));//绑定imageview
		mVideoImageLoader.showThumbByAsynctack(mVideoList.get(position).path, viewHolder.image);
		return convertView;
	}
	
	private static class ViewHolder{
		private ImageView image;
		private TextView title;
		private TextView duration;
		Bitmap bitmap = Bitmap.createBitmap( 10,10, Config.ARGB_4444 );
		
		public ViewHolder(View view){
			image = (ImageView) view.findViewById(R.id.image);
			title = (TextView) view.findViewById(R.id.title);
			duration = (TextView) view.findViewById(R.id.duration);
		}
		
		public void setData(LocalVideoBean video){
			title.setText("名称：" + video.title);
			duration.setText("时长：" + StringUtils.generateTime(Long.parseLong(video.duration)));
//			sizeText.setText("大小：" + StringUtils.generateFileSize(video.size));
			image.setImageBitmap(bitmap);
		}
		
		/**
		 * 获取视频文件截图
		 * 
		 * @param path
		 *            视频文件的路径
		 * @return Bitmap 返回获取的Bitmap
		 */
		public static Bitmap getVideoThumb(String path) {
			MediaMetadataRetriever media = new MediaMetadataRetriever();
			media.setDataSource(path);
			return media.getFrameAtTime();
		}
		
		public static Bitmap getVideoThumb1(String path) {
			Bitmap  bitmap = ThumbnailUtils.createVideoThumbnail(path,
	                   Thumbnails.MICRO_KIND);//MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96,
//			bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
			return bitmap;
		}
	}
}
