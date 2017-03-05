package com.zh.zhvideoplayer.player;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.zh.zhvideoplayer.bean.LocalVideoBean;
import com.zh.zhvideoplayer.R;
import com.zh.zhvideoplayer.bean.Resources;
import com.zh.zhvideoplayer.ui.title.TitleBuilder;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnSeekCompleteListener;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.VideoView;
/**
 * 视频播放界面
 */
public class PlayerActivity extends AppCompatActivity implements OnClickListener, OnCompletionListener, OnInfoListener,
		OnPreparedListener, OnErrorListener, OnBufferingUpdateListener, OnSeekCompleteListener,MyMediaController.SetVideoURLListener {
	private MyMediaController mMediaController;
	private int mLayout = VideoView.VIDEO_LAYOUT_STRETCH;
	private View mContentView;
	
	private View mTitleLayout;//顶部标题栏布局
	private View mRl_PlayView;//播放器所在布局
	private View mLoadingView;// 加载中的进度条
	private VideoView mVideoView;// vitmio视频播放控件
	private RelativeLayout mTopTools;// 播放器顶部工具栏
	private ImageView mLockScreenSwitch;// 锁定或解锁屏幕控件
	private ImageView mOrientationSwitch;// 全屏或非全屏开关
//	private ImageView mVideoShotSwitch;// 截屏开关
	private SeekBar mPlayerSeekbar;// 视频播放进度控件
	public  boolean isPlayComplete = false;// 是否播放完成
	
	private Resources mVideoRes;
	private TextView mVideoName;// 视频名称
	private TextView mVideoDec;// 视频描述
	public  boolean isPlayPause = false;// 是否手动播放暂停
	public  boolean isFirstPlay = true;// 是否第一次播放
	public  boolean isPlayLocalVideo = false;// 是否是在播放本地视频
	private boolean mIsPrepared = false;// 是否已经准备好播放
	
	public PlayerActivity getPlayerActivity(){
		return this;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Vitamio.isInitialized(getApplicationContext());
		Vitamio.isInitialized(PlayerActivity.this);
		mContentView = View.inflate(this, R.layout.player_activity_play, null);
		setContentView(mContentView);
		initTitle();
		initViews();
		initDatas();
//		if(PreferencesUtils.getBoolean(this, AppConst.isAutoPalyMediaKey, false)){
//			//是否允许自动播放
////			mMediaController.getPlayerCenterPlaySwitch().setVisibility(View.GONE);
//			setVideoURI();
//		}else{
			mMediaController.getPlayerCenterPlaySwitch().setVisibility(View.VISIBLE);
			mLoadingView.setVisibility(View.GONE);
//		}
	}
	
	private void initTitle() {
		// 1.设置左边的图片按钮显示，以及事件 2.设置中间TextView显示的文字 3.设置右边的图片按钮显示，并设置事件
		new TitleBuilder(this).setLeftImageRes(true, 0)
				.setLeftTextOrImageListener(true, null)
				.setMiddleTitleText("视频详情");
	}
	
	private void initViews() {
		mVideoName = (TextView)findViewById(R.id.video_name);
		mVideoDec = (TextView)findViewById(R.id.video_dec);
		
		mMediaController = (MyMediaController) findViewById(R.id.MyMediaController);
		mTitleLayout =  findViewById(R.id.titlebar);
		mVideoView = (VideoView) findViewById(R.id.vitamio_videoview);
		mLoadingView = findViewById(R.id.video_loading);
		mRl_PlayView = findViewById(R.id.id_ViewLayout);
		
		mTopTools = (RelativeLayout)findViewById(R.id.player_top_bar);
		mLockScreenSwitch = (ImageView)findViewById(R.id.player_iv_lock_screen);
		mOrientationSwitch = (ImageView)findViewById(R.id.orientation_switch);
		mPlayerSeekbar = (SeekBar)findViewById(R.id.player_seekbar);
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnInfoListener(this);
		mVideoView.setOnPreparedListener(this);
		mVideoView.setOnErrorListener(this);
		mVideoView.setOnBufferingUpdateListener(this);
		mVideoView.setOnSeekCompleteListener(this);
	}

	private void initDatas() {
		int videoType = getIntent().getIntExtra("videoType", -1);
		if(videoType == 0){
			//在线视频
			mVideoRes = (Resources) getIntent()
	                .getParcelableExtra("video");
		}else if(videoType == 1){
			//本地视频
			isPlayLocalVideo = true;
			LocalVideoBean video = (LocalVideoBean) getIntent()
	                .getParcelableExtra("video");
			mVideoRes = new Resources();
			mVideoRes.setTitle(video.title);
			mVideoRes.setDescription("");
			mVideoRes.setLink(video.path);
			Log.e("zh","本地视频:");
			Log.e("zh","video.title:" + video.title);
			Log.e("zh","video.title:" + video.path);
		}
		
		ResetVideoPlayer();
		mVideoName.setText(mVideoRes.getVideoTitle());
		mVideoDec.setText(mVideoRes.getVideoDes());
		mMediaController.setSetVideoURLListener(this);
	}
	
	@Override
	public void setVideoURI() {	
		if(isPlayLocalVideo == true){
			//播放本地视频
			mVideoView.setVideoURI(Uri.parse(mVideoRes.getLink()));
			mMediaController.getPlayerCenterPlaySwitch().setVisibility(View.GONE);
			mLoadingView.setVisibility(View.VISIBLE);
			return;
		}
	}




	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		ResetVideoPlayer();
		super.onConfigurationChanged(newConfig);
	}
	
	private void ResetVideoPlayer(){
		// 设置显示名称
		mMediaController.initControllerTools(this, mContentView, mVideoView,this);
		mMediaController.setMediaPlayer(mVideoView);
		mMediaController.setFileName(mVideoRes.getVideoTitle());
		
		int mCurrentOrientation = getResources().getConfiguration().orientation;
		if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
			SetfullScreen(PlayerActivity.this, false);
			mMediaController.isLockedTools = false;
			
			mTitleLayout.setVisibility(View.VISIBLE);
			mTopTools.setVisibility(View.GONE);
			mLockScreenSwitch.setVisibility(View.GONE);
//			mVideoCenterSwitch.setVisibility(View.GONE);
			mOrientationSwitch.setImageResource(R.mipmap.player_fill);
			mRl_PlayView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp2px(PlayerActivity.this, 200)));
			if (mVideoView != null && mIsPrepared){
//				mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_ORIGIN, 0);
//				mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_STRETCH, 0);
//				mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, 0);
				mVideoView.setVideoLayout(-1, 0);
			}
		} else if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			SetfullScreen(PlayerActivity.this,true);
			mTitleLayout.setVisibility(View.GONE);
			mTopTools.setVisibility(View.VISIBLE);
			mLockScreenSwitch.setVisibility(View.VISIBLE);
//			mVideoCenterSwitch.setVisibility(View.GONE);
			mOrientationSwitch.setImageResource(R.mipmap.player_btn_scale);
			mRl_PlayView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			if (mVideoView != null && mIsPrepared){
//				mVideoView.setVideoLayout(mLayout, 0);
//				mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, 0);
				mVideoView.setVideoLayout(-1, 0);
			}
		}
		mVideoView.requestFocus();
	}
	
	
	/**
	 * 设置是否进入全屏
	 * @param activity
	 * @param enable
	 */
	public void SetfullScreen(Activity activity,boolean enable) {
	    if (enable) {
	        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
	        lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
	        activity.getWindow().setAttributes(lp);
	        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
	        hideVirtualButtons();
	    } else {
	        WindowManager.LayoutParams attr = activity.getWindow().getAttributes();
	        attr.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
	        activity.getWindow().setAttributes(attr);
	        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
	    }
	}
	
	/**
	 * 隐藏虚拟导航键，使用于api 19+
	 */
	@TargetApi(11)
	private void hideVirtualButtons() {
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        }
    }

	/**
	 * 播放器状态变化
	 */
	@Override
	public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
		switch (arg1) {
		case MediaPlayer.MEDIA_INFO_BUFFERING_START:
			// 开始缓存，暂停播放
			if (isPlaying()) {
				//System.out.println("zh::::MEDIA_INFO_BUFFERING_START");
				stopPlayer();
			}
			mLoadingView.setVisibility(View.VISIBLE);
			break;
		case MediaPlayer.MEDIA_INFO_BUFFERING_END:
			// 缓存完成，继续播放
			// System.out.println("zh::::MEDIA_INFO_BUFFERING_ENDMEDIA_INFO_BUFFERING_END");
			if(!isPlayPause){
				startPlayer();
				isPlayPause = false;
			}
			mLoadingView.setVisibility(View.GONE);
			break;
		case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
			// 显示 下载速度
			// System.out.println("zh::::901");
			if(!isPlayPause){
				startPlayer();
				isPlayPause = false;
			}
			mLoadingView.setVisibility(View.GONE);
			break;
		}
		return true;
	}

	/**
	 * //在视频预处理完成后调用。在视频预处理完成后被调用。此时视频的宽度、高度、宽高比信息已经获取到，此时可调用seekTo让视频从指定位置开始播放。
	 */
	@Override
	public void onPrepared(MediaPlayer mediaPlayer) {
		mIsPrepared = true;
	}
	
	/**
	 * 播放完成
	 */
	@Override
	public void onCompletion(MediaPlayer arg0) {
		mMediaController.updatePausePlay();
		mPlayerSeekbar.setProgress(mPlayerSeekbar.getMax());
		isPlayComplete = true;
		mMediaController.mHandler.removeMessages(2);
		mMediaController.getPlayerCenterPlaySwitch().setVisibility(View.VISIBLE);
	}

	/**
	 * 在异步操作调用过程中发生错误时调用。例如视频打开失败。
	 */
	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
		mLoadingView.setVisibility(View.GONE);
		//		mTv_NoPlay.setVisibility(View.VISIBLE);
		return false;
	}

	/**
	 * 在网络视频流缓冲变化时调用。
	 * @param arg0
	 * @param arg1
	 */
	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
//		mTv_NoPlay.setVisibility(View.GONE);
		mLoadingView.setVisibility(View.VISIBLE);
	}

	/**
	 * 在进度条拖动操作完成后调用。
	 */
	@Override
	public void onSeekComplete(MediaPlayer arg0) {
		mMediaController.mHandler.sendEmptyMessage(2);
	}
	
	/**
	 * 处理点击事件 
	 */
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected void onDestroy() {
		if(mMediaController != null && mMediaController.mHandler != null){
			//播放器所在界面销毁后，停止更新进度条
			mMediaController.mHandler.removeMessages(2);
		}
		super.onDestroy();
	}
	
	/**
	 * 停止播放
	 */
	private void stopPlayer() {
		if (mVideoView != null && mVideoView.isPlaying()){
			mVideoView.pause();
		}
	}

	private boolean isFirstIn = true;
	/**
	 * 开始播放
	 */
	private void startPlayer() {
		if (mVideoView != null && ! mVideoView.isPlaying()){
			mVideoView.requestFocus();
			mVideoView.start();
			mMediaController.updatePausePlay();
			if(isFirstIn){
				mMediaController.mHandler.sendEmptyMessage(2);
				mMediaController.mHandler.sendMessageDelayed(mMediaController.mHandler.obtainMessage(1), 3000);
				isFirstIn = false;
				isFirstPlay = false;
			}
		}
	}

	/**
	 * 播放器是否正在播放
	 * @return
	 */
	private boolean isPlaying() {
		return mVideoView != null && mVideoView.isPlaying();
	}
	
	/**
	 * dp转px
	 * @param context
	 * @param dpVal
	 * @return
	 */
	public static int dp2px(Context context, float dpVal) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				dpVal, context.getResources().getDisplayMetrics());
	}
	
	/**
	 * 显示非WIFI网络播放视频提示框
	 */
	public void ShowSetNetDialog() {
//		Toast.makeText(getActivity(), "当前已是最新版.", Toast.LENGTH_SHORT).show();
		//要用 android.support.v7.app.AlertDialog 并且设置主题
		AlertDialog dialog = new  AlertDialog.Builder(this)
			.setTitle("温馨提示")
			.setMessage("您正在使用非WIFI网络播放视频，会消耗手机流量，如执意使用请在设置内打开开关！")
			.setPositiveButton("确定", null)
			.create();
		dialog.show();
		WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
		params.width = getWindowManager().getDefaultDisplay().getWidth() * 5 / 6 ;
		//	params.height = 200 ;
		dialog.getWindow().setAttributes(params);
	}
	
	/**
	 * 显示设置网络对话框
	 */
	public void ShowCheckNetDialog() {
//		Toast.makeText(getActivity(), "当前已是最新版.", Toast.LENGTH_SHORT).show();
		//要用 android.support.v7.app.AlertDialog 并且设置主题
		AlertDialog dialog = new  AlertDialog.Builder(this)
			.setTitle("温馨提示")
			.setMessage("网络不给力，请检查您的网络！")
			.setPositiveButton("确定", null)
			.create();
		dialog.show();
		WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
		params.width = getWindowManager().getDefaultDisplay().getWidth() * 5 / 6 ;
		//	params.height = 200 ;
		dialog.getWindow().setAttributes(params);
	}
}
