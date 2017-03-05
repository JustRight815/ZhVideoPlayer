
package com.zh.zhvideoplayer.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.zh.zhvideoplayer.R;
import com.zh.zhvideoplayer.util.StringUtils;
import com.zh.zhvideoplayer.util.Utils;

import io.vov.vitamio.widget.MediaController;

/**
 * 自定义FrameLayout，覆盖在视频播放器控件VideoView上，用来显示开关、进度等各种控件，并负责处理手势事件
 * @author zh
 */
public class MyMediaController extends FrameLayout implements View.OnClickListener{
	private MediaController.MediaPlayerControl mPlayer;//Vitmio的videoView 默认实现了MediaPlayerControl接口，所以要传递一个videoView对象过来
	private Activity mContext;
	private View mContentView;//所在界面的布局
	private ProgressBar mProgress;//视频播放器的播放进度条
	private ImageView mPlayerbackBtn;//播放器左上角的返回键
	private TextView mEndTime, mCurrentTime;// 视频总时长，当前播放时间
	private String mTitle;//视频名称
	private long mDuration;//视频总时长
	private boolean mShowing = true; // 顶部或底部的工具栏布局是否正在显示
	private boolean mDragging;
	private boolean mInstantSeeking = true;
	
	public static final int DEFAULT_HIDDEN_TOOLS_TIMEOUT = 3000;// 默认顶部或底部的工具栏布局隐藏计时时间
	public static final int DEFAULT_HIDDEN_VOLUE_BRIGHT_TIMEOUT = 800;// 声音、亮度、快进、快退的父布局隐藏计时时间
	
	private static final int HIDDEN_VOLUME_BRIGHT_LAYOUT = 0;// handler标示  隐藏快进、快退、声音、亮度图标
	private static final int FADE_OUT = 1;// handler标示   隐藏底部和顶部的控制视图
	private static final int SHOW_PROGRESS = 2;// handler标示   更新视频播放进度
	
	private ImageView mPlayerPlaySwitch;//播放器左下角播放或暂停开关
	private ImageView mOrientationSwitch;//播放器右下角全屏或退出全屏开关
	private ImageView mPlayerCenterPlaySwitch;//播放器中间的播放或暂停开关
	private TextView mPlayerVideoName;//播放的视频名称
	private ImageView mLockScreenSwitch;//锁定或解锁屏幕控件开关
	public static  boolean isLockedTools = false;// 是否锁定了屏幕控件
	private RelativeLayout mTopTools;// 播放器顶部工具栏
	private RelativeLayout mBottomTools;// 播放器底部工具栏
	
	private GestureDetector mGestureDetector;//手势处理
	
	private int mMaxVolume;//最大声音
	private int mVolume = -1;//当前声音
	private float mBrightness = -1f;//当前亮度
	private float mFast_forward;
	private boolean isFast_Forword;
	private boolean isUp_downScroll;
	private AudioManager mAudioManager;
	private LinearLayout mVolumeBrightnessLayout;//声音或亮度的父布局
	private ImageView mVolumeOrBrightnessBg;//声音或亮度的图片
	private TextView mVolumeOrBrightnessProgress;//声音或亮度的当前进度
	private PlayerActivity  mPlayerActivity;
	private SetVideoURLListener setVideoURLListener;
	public interface SetVideoURLListener{
		void setVideoURI();
	}
	
	public void setSetVideoURLListener(SetVideoURLListener SetVideoURLListener){
		this.setVideoURLListener = SetVideoURLListener;
	}
	
	
	@SuppressLint("HandlerLeak")
	public  Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FADE_OUT:
				// 隐藏底部和顶部的控制视图
//				if( !PreferencesUtils.getBoolean(mContext, AppConst.isNotWifiPalyMediaKey, false) && !NetUtils.isWifiConnected(mContext)){
//					return;
//				}
				hiddenControllerTools(true);
				break;
			case SHOW_PROGRESS:
				// 更新视频播放进度
				setProgress();
				if (!mDragging) {
					msg = obtainMessage(SHOW_PROGRESS);
//					System.out.println("zh:::+++ SHOW_PROGRESS:SHOW_PROGRESS:SHOW_PROGRESS:");
					sendMessageDelayed(msg, 1000);
					updatePausePlay();
				}
				break;
			case HIDDEN_VOLUME_BRIGHT_LAYOUT:
				// 隐藏快进、快退、声音、亮度图标
				isFast_Forword = false;
				isUp_downScroll = false;
				mVolumeBrightnessLayout.setVisibility(View.GONE);
				break;
			default:
				break;
			}
		}
	};


	public MyMediaController(Context context) {
	    super(context);
	}
	
	public MyMediaController(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public MyMediaController(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
	
	public void initControllerTools(Activity activity, View contentView, io.vov.vitamio.widget.MediaController.MediaPlayerControl player, PlayerActivity playerActivity) {
		this.mContext = activity;
		this.mPlayer = player;
		this.mPlayerActivity =  playerActivity;
		this.mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		this.mContentView = contentView;
		this.mGestureDetector = new GestureDetector(mContext,new MyGestureListener());
		initControllerViews(mContentView);
	}

	private void initControllerViews(View v) {
		mPlayerPlaySwitch = (ImageView) v.findViewById(R.id.player_play_switch);
		mPlayerCenterPlaySwitch = (ImageView)v.findViewById(R.id.player_center_switch);
		mPlayerCenterPlaySwitch.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				doPauseResume();
			}
		});
		mPlayerPlaySwitch.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				doPauseResume();
			}
		});
		
		mProgress = (SeekBar) v.findViewById(R.id.player_seekbar);
		if (mProgress != null) {
			if (mProgress instanceof SeekBar) {
				SeekBar seeker = (SeekBar) mProgress;
				seeker.setOnSeekBarChangeListener(mSeekListener);
				seeker.setThumbOffset(1);
			}
			mProgress.setMax(1000);
		}
		mPlayerbackBtn  =(ImageView)v.findViewById(R.id.player_back_btn);
		mPlayerbackBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				int mCurrentOrientation = mContext.getResources().getConfiguration().orientation;
				if ( mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT ) {
					mContext.finish();
			    } else if ( mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE ) {
			    	mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			    }
			}
		});
		mEndTime = (TextView) v.findViewById(R.id.player_total_time);
		mCurrentTime = (TextView) v.findViewById(R.id.player_current_time);
		mPlayerVideoName = (TextView) v.findViewById(R.id.player_video_name);
		mPlayerVideoName.setText(mTitle);
		mOrientationSwitch  =(ImageView)v.findViewById(R.id.orientation_switch);
		mOrientationSwitch.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//播放器右下角的全屏或退出全屏按钮
				if (mContext.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
					mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				}else{
					mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//					mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //主动设置为横屏
				}
			}
		});
		mTopTools = (RelativeLayout) v.findViewById(R.id.player_top_bar);
		mBottomTools = (RelativeLayout) v.findViewById(R.id.player_bottom_layout);
		mLockScreenSwitch = (ImageView) v.findViewById(R.id.player_iv_lock_screen);
		mLockScreenSwitch.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				//播放器左边的锁定屏幕或解锁屏幕按钮
				if(isLockedTools){
					mLockScreenSwitch.setImageResource(R.mipmap.player_video_player_unlock);
					isLockedTools = false;
					showControllerTools();
				} else {
					mLockScreenSwitch.setImageResource(R.mipmap.player_video_player_lock);
					isLockedTools = true;
					mHandler.removeMessages(FADE_OUT);
					hiddenControllerTools(false);
					mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), DEFAULT_HIDDEN_TOOLS_TIMEOUT);
				}
			}
		});
		mVolumeBrightnessLayout = (LinearLayout) v.findViewById(R.id.volume_brightness_layout);
		mVolumeOrBrightnessBg = (ImageView) v.findViewById(R.id.operation_bg);
		mVolumeOrBrightnessProgress = (TextView) v.findViewById(R.id.operation_progress);
	}
	
	
	public ImageView getPlayerCenterPlaySwitch(){
		return mPlayerCenterPlaySwitch;
	}

	public void setMediaPlayer(MediaController.MediaPlayerControl player) {
		this.mPlayer = player;
		updatePausePlay();
	}

	/**
	 * Control the action when the seekbar dragged by user
	 * 
	 * @param seekWhenDragging True the media will seek periodically
	 */
	public void setInstantSeeking(boolean seekWhenDragging) {
		mInstantSeeking = seekWhenDragging;
	}

	/**
	 * 设置播放的视频的名称
	 * @param name
	 */
	public void setFileName(String name) {
		mTitle = name;
		if (mPlayerVideoName != null){
			mPlayerVideoName.setText(mTitle);
		}
	}

	public boolean isShowing() {
		return mShowing;
	}
	
	/**
	 * 处理各种控件的点击事件
	 */
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
//		case R.id.player_play_switch:
//			//播放器左下角的播放或暂停按钮
//			doPauseResume();
//			break;
//		case R.id.player_center_switch:
//			//播放器中间的播放或暂停按钮
//			doPauseResume();
//			break;
//		case R.id.player_back_btn:
//			//播放器左上角的返回按钮
//			int mCurrentOrientation = mContext.getResources().getConfiguration().orientation;
//			if ( mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT ) {
//				mContext.finish();
//		    } else if ( mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE ) {
//		    	mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//		    }
//			break;
//		case R.id.orientation_switch:
//			//播放器右下角的全屏或退出全屏按钮
//			if (mContext.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//				mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//			}else{
//				mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
////				mContext.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //主动设置为横屏
//			}
//			break;
//		case R.id.player_iv_lock_screen:
//			//播放器左边的锁定屏幕或解锁屏幕按钮
//			if(isLockedTools){
//				mLockScreenSwitch.setImageResource(R.drawable.video_player_unlock);
//				isLockedTools = false;
//				showControllerTools();
//			} else {
//				mLockScreenSwitch.setImageResource(R.drawable.video_player_lock);
//				isLockedTools = true;
//				mHandler.removeMessages(FADE_OUT);
//				hiddenControllerTools(false);
//				mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), DEFAULT_HIDDEN_TOOLS_TIMEOUT);
//			}
//			break;
		default:
			break;
		}
	}
	
	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		@Override
		public void onStartTrackingTouch(SeekBar bar) {
			System.out.println("zh:**********");
			mDragging = true;
			updatePausePlay();
			mHandler.removeMessages(SHOW_PROGRESS);
//			if (mInstantSeeking)
//				mAM.setStreamMute(AudioManager.STREAM_MUSIC, true);
		}


		@Override
		public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
			if (!fromuser){
				return;
			}
			long newposition = (mDuration * progress) / 1000;
			String time = StringUtils.generateTime(newposition);
			if (mCurrentTime != null){
				mCurrentTime.setText(time);
			}
		}

		@Override
		public void onStopTrackingTouch(SeekBar bar) {
			long newposition = (mDuration * bar.getProgress()) / 1000;
			if (mInstantSeeking){
				mPlayer.seekTo(newposition);
				System.out.println("zh:**********++++++++");
			}
			updatePausePlay();
//			mHandler.removeMessages(SHOW_PROGRESS);
//			mAM.setStreamMute(AudioManager.STREAM_MUSIC, false);
			mDragging = false;
//			mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS, 1000);
		}
	};

	/**
	 * 更新播放进度
	 * @return
	 */
	private long setProgress() {
		if (mPlayer == null || mDragging){
			return 0;
		}
		long position = mPlayer.getCurrentPosition();
		long duration = mPlayer.getDuration();
		if (mProgress != null) {
			if (duration > 0) {
				long pos = 1000L * position / duration;
				mProgress.setProgress((int) pos);
			}
			int percent = mPlayer.getBufferPercentage();
			mProgress.setSecondaryProgress(percent * 10);
		}

		mDuration = duration;

		if (mEndTime != null){
			mEndTime.setText(StringUtils.generateTime(mDuration));
		}
		if (mCurrentTime != null){
			mCurrentTime.setText(StringUtils.generateTime(position));
		}
		return position;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// 触摸结束后处理手势
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_UP:
				endGesture();
				break;
			default:
				break;
		}
		return mGestureDetector.onTouchEvent(event);
	}
	
	@Override
	public boolean onTrackballEvent(MotionEvent ev) {
//		show(sDefaultTimeout);
		return false;
	}
	
	private class MyGestureListener extends SimpleOnGestureListener {
		
		// 双击
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// 视频画面变成缩放或正常模式
			System.out.println("zh:::++++双击双击双击双击");
//			if (mLayout == VideoView.VIDEO_LAYOUT_ZOOM){
//				mLayout = VideoView.VIDEO_LAYOUT_ORIGIN;
//			} else{
//				mLayout++;
//			}				
//			if (mVideoView != null)
//				mVideoView.setVideoLayout(mLayout, 0);
			return true;
		}
		
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			//点击屏幕显示或不显示控制控件
			if(isLockedTools){
			} else {
				if (mShowing) {
					hiddenControllerTools(true);
				}else{
					showControllerTools();
				}
			}
			return super.onSingleTapConfirmed(e);
		}
		
		@Override
		public boolean onDown(MotionEvent e) {
			//点击屏幕显示或不显示锁定屏幕控件开关
			if(isLockedTools){
				if (mLockScreenSwitch.getVisibility() == View.VISIBLE) {
					mLockScreenSwitch.setVisibility(View.GONE);
				} else {
					mLockScreenSwitch.setVisibility(View.VISIBLE);
				}
			} 
			return true;
		}		
		
		//滑动
				@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			hiddenControllerTools(true);
			float mOldX = e1.getX();
			float mOldY = e1.getY();
			float mOldRawX = e1.getRawX();
			float mOldRawY = e1.getRawY();
			int y = (int) e2.getRawY();
			int x = (int) e2.getRawX();
			Display disp = mContext.getWindowManager().getDefaultDisplay();
			int windowWidth = disp.getWidth();
			int windowHeight = disp.getHeight();
//			System.out.println("zzzzzh: windowWidth " + windowWidth);
//			System.out.println("zzzzzh: windowHeight " + windowHeight);
//			System.out.println("zzzzzh: Math.abs(x- mOldX) " + Math.abs(x- mOldX));
//			System.out.println("zzzzzh: y " + y);
//			System.out.println("zzzzzh: mOldY " + mOldY);
//			System.out.println("zzzzzh: Math.abs(y- mOldY) " + Math.abs(y- mOldY));
//			System.out.println("zzzzzh: RawY " + e2.getY());
//			System.out.println("zzzzzh: mOldRawY " + mOldRawY);
//			System.out.println("zzzzzh: Math.abs(y- mOldRawY) " + Math.abs(y- mOldRawY));
//			System.out.println("zzzzzh: isUp_downScroll " + isUp_downScroll);
			
			int MaxScroll = windowHeight / 2;
			float mYDistance = mOldY - y;
			int mCurrentOrientation = getResources().getConfiguration().orientation;
			if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
				MaxScroll = dp2px(mContext, 180);
				mYDistance = mOldRawY - y;
			}else{
				
			}
			if (Math.abs(x- mOldX) >10 && Math.abs(x- mOldX) > Math.abs(mYDistance) && !isUp_downScroll) {
				//执行快进快退
				isFast_Forword = true;
				mFast_forward = (x - mOldX) / windowWidth ;
//				System.out.println("zh: mFast_forward " + mFast_forward + "   x - mOldX" + (x - mOldX) );
				fast_ForWord(mFast_forward);
			} else if (mOldX > windowWidth * 1.0 / 2 && Math.abs(mYDistance) > 3 && !isFast_Forword){
				// 右边滑动
				if(x > (windowWidth * 1.0 / 2.0) ){
					onVolumeSlide(mYDistance / MaxScroll );
				}
			} else if (mOldX < (windowWidth * 1.0 / 2.0) && Math.abs(mYDistance) > 3 && !isFast_Forword){
//				System.out.println("zh: 左边滑动 mOldX" + mOldX + "  windowWidth" + (windowWidth * 1.0 / 2.0));
				// 从左边开始滑动
				if(x < (windowWidth * 1.0 / 2.0) ){
					//只在左边滑动控制亮度，超出左边不更新亮度
					onBrightnessSlide( mYDistance / MaxScroll );
				}
			}
			return super.onScroll(e1, e2, distanceX, distanceY);
		}
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
	 * 根据播放状态更新 播放开关的图片
	 */
	public void updatePausePlay() {
		if (mContentView == null || mPlayerPlaySwitch == null){
			return;
		}
		if (mPlayer.isPlaying()){
			mPlayerPlaySwitch.setImageResource(R.mipmap.player_mediacontroller_pause);
//			mPlayerCenterPlaySwitch.setImageResource(R.drawable.player_center_switch_play);
			mPlayerCenterPlaySwitch.setVisibility(View.GONE);
		} else {
			mPlayerPlaySwitch.setImageResource(R.mipmap.player_mediacontroller_play);
			mPlayerCenterPlaySwitch.setImageResource(R.mipmap.player_center_switch_pause);
//			mPlayerCenterPlaySwitch.setVisibility(VISIBLE);
		}
	}

	public void doPauseResume() {
//		if( mPlayerActivity.isPlayLocalVideo == false && !PreferencesUtils.getBoolean(mContext, AppConst.isNotWifiPalyMediaKey, false) && !NetUtils.isWifiConnected(mContext)){
//			mPlayerActivity.ShowSetNetDialog();
//			return;
//		}
//
//		if( !PreferencesUtils.getBoolean(mContext, AppConst.isAutoPalyMediaKey, false) && mPlayerActivity.isFirstPlay){
//			setVideoURLListener.setVideoURI();
//			mPlayerActivity.isPlayPause = false;
//			return;
//		}
		if(mPlayerActivity.isFirstPlay){
			setVideoURLListener.setVideoURI();
			mPlayerActivity.isPlayPause = false;
			return;
		}
		
		 if (mPlayerActivity.isPlayComplete) {
			 mPlayer.seekTo(0);
			 mPlayerActivity.isPlayComplete = false;
	     } else {
	    	 if (mPlayer.isPlaying()){
	 			mPlayer.pause();
	 			mPlayerActivity.isPlayPause = true;
	 			//zh 
	 			mHandler.removeMessages(SHOW_PROGRESS);
	 			mPlayerCenterPlaySwitch.setImageResource(R.mipmap.player_center_switch_pause);
	 			mPlayerCenterPlaySwitch.setVisibility(View.VISIBLE);
	 		} else{
	 			mPlayer.start();
	 			mPlayerActivity.isPlayPause = false;
	 			//zh 
	 			mHandler.sendEmptyMessage(SHOW_PROGRESS);
//	 			mPlayerCenterPlaySwitch.setImageResource(R.drawable.player_center_switch_play);
	 			mPlayerCenterPlaySwitch.setVisibility(View.GONE);
	 		}
	     }
		 updatePausePlay();
	}
	
	 /**
     * 隐藏底部和顶部的控制视图
     */
	public void hiddenControllerTools(boolean  hideAllTools){
		int mCurrentOrientation = getResources().getConfiguration().orientation;
		if (mShowing) {
			mShowing = false;
			Utils.translateAnimation(mBottomTools, 0f, 0f, 0f, 1.0f, 400);
	        mBottomTools.setVisibility(View.GONE);
	        if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
	        	 Utils.translateAnimation(mTopTools, 0f, 0f, 0f, -1.0f, 400);
	 	        mTopTools.setVisibility(View.GONE);
	        }
		}
        if(hideAllTools){
        	mLockScreenSwitch.setVisibility(View.GONE);
        } else {
        	mLockScreenSwitch.setVisibility(View.VISIBLE);
        }
	}
	
	 /**
     * 显示底部和顶部的控制视图
     */
	public void showControllerTools() {
		int mCurrentOrientation = getResources().getConfiguration().orientation;
		mHandler.removeMessages(FADE_OUT);
		mHandler.removeMessages(FADE_OUT);
    	if (!isLockedTools  &&  !mShowing ) {
//    		mHandler.removeMessages(FADE_OUT);
//    		mHandler.removeMessages(FADE_OUT);
    		mShowing = true;
            mBottomTools.setVisibility(View.VISIBLE);
            Utils.translateAnimation(mBottomTools, 0f, 0f, 1.0f, 0f, 300);
    		if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
    			mTopTools.setVisibility(View.VISIBLE);
    			Utils.translateAnimation(mTopTools, 0f, 0f, -1.0f, 0f, 300);
    		} else {
//    			mTopTools.setVisibility(View.GONE);
    		}
    	}
    	if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
    		mLockScreenSwitch.setVisibility(View.VISIBLE);
    	}else{
    		mLockScreenSwitch.setVisibility(View.GONE);
    	}
    	mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), DEFAULT_HIDDEN_TOOLS_TIMEOUT);
    }
	
	/** 手势结束 */
	private void endGesture() {
		// 定时隐藏快进，快退，声音、音量
		mHandler.removeMessages(HIDDEN_VOLUME_BRIGHT_LAYOUT);
		mHandler.sendEmptyMessageDelayed(HIDDEN_VOLUME_BRIGHT_LAYOUT, DEFAULT_HIDDEN_VOLUE_BRIGHT_TIMEOUT);
		
		mVolume = -1;
		mBrightness = -1f;
		if (isFast_Forword && ! isLockedTools ) {
			mPlayer.seekTo(AfterFastBackProgress);
		}
		isFast_Forword = false;
		isUp_downScroll = false;
	}
	
	private long AfterFastBackProgress = 0 ;
	
	/**
	 * 滑动屏幕快进或快退
	 */
	private void fast_ForWord(float percent){
		if(isLockedTools){
			//已锁定工具栏
			return;
		}
		int scale = 0;//1500000 相当于滑动整个一次屏幕宽度，快进或快退26分 .  60000 一分钟。 比例可以根据视频的长短调整
		if(mPlayer.getDuration() >= 1500000){  
			scale = 1500000;
		} else if (mPlayer.getDuration() >= 150000){ // 2分半
			scale = 150000;
		}else{
			scale = 60000;
		}
				
		if (mPlayer.getCurrentPosition() + scale * percent < 0){ 
			AfterFastBackProgress = 0;			
		} else if(mPlayer.getCurrentPosition() + scale * percent > mPlayer.getDuration()) {
			AfterFastBackProgress = mPlayer.getDuration();			
		} else {			
			AfterFastBackProgress = mPlayer.getCurrentPosition() + (long)(scale * percent);
		}
		mVolumeOrBrightnessProgress.setText(StringUtils.generateTime(AfterFastBackProgress) + "/" + StringUtils.generateTime(mPlayer.getDuration()));
		if (percent > 0) {
			mVolumeOrBrightnessBg.setImageResource(R.mipmap.player_fullscress_duration_advance);
		} else {
			mVolumeOrBrightnessBg.setImageResource(R.mipmap.player_fullscress_duration_back);
		}
		mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
	}
	
	/**
	 * 滑动改变声音大小
	 * @param percent
	 */
	private void onVolumeSlide(float percent) {
		if(isLockedTools){
			//已锁定工具栏
			return;
		}
		// 取消之前的隐藏快进，快退，声音、音量计时器
		mHandler.removeMessages(HIDDEN_VOLUME_BRIGHT_LAYOUT);
		isUp_downScroll = true;
		mVolumeOrBrightnessBg.setImageResource(R.mipmap.player_video_volumn_bg);
		if (mVolume == -1) {
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (mVolume < 0){
				mVolume = 0;
			}
		}
		
		int index = (int) (percent * mMaxVolume) + mVolume;
		if (index > mMaxVolume){
			index = mMaxVolume;
		} else if (index < 0){
			index = 0;
		}
		// 变更声音
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
		int currProgress = (index * 100) / mMaxVolume ;
//		System.out.println("zh: mMaxVolume" + mMaxVolume + "  index " + index + "   currProgress" + currProgress);
		// 显示并变更进度条
		mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		mVolumeOrBrightnessProgress.setText("" + currProgress +"%");
	}

	/**
	 * 滑动改变亮度
	 * @param percent
	 */
	private void onBrightnessSlide(float percent) {
		if(isLockedTools){
			//已锁定工具栏
			return;
		}
		// 取消之前的隐藏快进，快退，声音、音量计时器
		mHandler.removeMessages(HIDDEN_VOLUME_BRIGHT_LAYOUT);
		isUp_downScroll = true;
		mVolumeOrBrightnessBg.setImageResource(R.mipmap.player_video_brightness_bg);
		if (mBrightness < 0) {
			mBrightness = mContext.getWindow().getAttributes().screenBrightness;
			if (mBrightness <= 0.0f ){
				mBrightness = 0.01f;
			}else if(mBrightness > 1.0f){
				mBrightness = 1.0f;
			}
		}
		WindowManager.LayoutParams lpa = mContext.getWindow().getAttributes();
		lpa.screenBrightness = mBrightness + percent;
		int currProgress = 0;
		if (lpa.screenBrightness > 1.0f){
			lpa.screenBrightness = 1.0f;
		} else if (lpa.screenBrightness < 0.01f){
			lpa.screenBrightness = 0.01f;
		}
		currProgress = (int) ((lpa.screenBrightness) * 100) ;
		mContext.getWindow().setAttributes(lpa);
		// 显示并变更进度条
		mVolumeBrightnessLayout.setVisibility(View.VISIBLE);
		mVolumeOrBrightnessProgress.setText("" + currProgress +"%");
	}
}
