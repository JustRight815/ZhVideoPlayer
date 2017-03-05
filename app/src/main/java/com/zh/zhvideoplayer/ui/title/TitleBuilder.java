package com.zh.zhvideoplayer.ui.title;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zh.zhvideoplayer.R;

/**
 * 标题栏配置，采用builder模式
 * @author zh
 * 2015-11-25
 */
public class TitleBuilder {

    private View titleBar;
    private TextView left_textview;
    private ImageView left_imageview;
    private TextView middle_textview;
    private TextView right_textview;
    private ImageView right_imageview;
    private RelativeLayout left_back_layout;
    private Activity mActivity;

    /**
     * 第一种  初始化方式
     * 布局中直接引用进文件的初始化方式
     * @param context
     */
    public TitleBuilder(Activity context) {
    	mActivity = context;
    	titleBar = context.findViewById(R.id.title_bar);
    	left_back_layout = (RelativeLayout) context.findViewById(R.id.title_left_back_layout);
        left_textview = (TextView) context.findViewById(R.id.title_left_textview);
        left_imageview = (ImageView) context.findViewById(R.id.title_left_imageview);
        middle_textview = (TextView) context.findViewById(R.id.title_middle_textview);
        right_textview = (TextView) context.findViewById(R.id.title_right_textview);
        right_imageview = (ImageView) context.findViewById(R.id.title_right_imageview);
    }

    /**
     * 第二种初始化方式
     * 用代码创建布局或者在fragment中的时候使用
     * @param context
     */
    public TitleBuilder(Activity context, View view) {
    	mActivity = context;
    	titleBar = view.findViewById(R.id.title_bar);
    	left_back_layout = (RelativeLayout) view.findViewById(R.id.title_left_back_layout);
        left_textview = (TextView) view.findViewById(R.id.title_left_textview);
        left_imageview = (ImageView) view.findViewById(R.id.title_left_imageview);
        middle_textview = (TextView) view.findViewById(R.id.title_middle_textview);
        right_textview = (TextView) view.findViewById(R.id.title_right_textview);
        right_imageview = (ImageView) view.findViewById(R.id.title_right_imageview);
    }

    /**
     * 设置中间文字的背景色
     */
    public TitleBuilder setMiddleTitleBgRes(int resid) {
        middle_textview.setBackgroundResource(resid);
        return this;
    }
    /**
     * 设置中间文字的文本
     *
     * @param text
     * @return
     */
    public TitleBuilder setMiddleTitleText(String text) {
        middle_textview.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        middle_textview.setText(text);
        return this;
    }

   
    /**
     * 设置左边图片的资源
     * @param useDftLeftImage 是否使用默认的图片资源
     * @param resId
     * @return
     */
    public TitleBuilder setLeftImageRes(boolean useDftLeftImage, int resId) {
    	if(useDftLeftImage){
    		left_imageview.setVisibility(View.VISIBLE);
    		return this;
    	}
        left_imageview.setVisibility(resId > 0 ? View.VISIBLE : View.GONE);
        left_imageview.setImageResource(resId);
        return this;
    }

    /**
     * 设置左边的文字内容
     * @param text
     * @return
     */
    public TitleBuilder setLeftText(String text) {
        left_textview.setVisibility(TextUtils.isEmpty(text) ? View.GONE:View.VISIBLE);
        left_textview.setText(text);
        return this;
    }

    /**
     * 设置左边文字或图片的点击事件
     * @param useDftListener 是否使用默认的监听事件
     * @param listener
     * @return
     */
    public TitleBuilder setLeftTextOrImageListener(boolean useDftListener, OnClickListener listener) {
    	if(useDftListener == true){
    		left_imageview.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					mActivity.finish();
				}
			});
    		return this;
    	}
        if (left_imageview.getVisibility() == View.VISIBLE) {
            left_imageview.setOnClickListener(listener);
        } else if (left_textview.getVisibility() == View.VISIBLE) {
            left_textview.setOnClickListener(listener);
        }
        return this;
    }

    /**
     * 设置右边图片的资源
     *
     * @param resId
     * @return
     */
    public TitleBuilder setRightImageRes(int resId) {
        right_imageview.setVisibility(resId > 0 ? View.VISIBLE : View.GONE);
        right_imageview.setImageResource(resId);
        return this;
    }

    /**
     * 设置右边文字的内容
     *
     * @param text
     * @return
     */
    public TitleBuilder setRightText(String text) {
        right_textview.setVisibility(TextUtils.isEmpty(text) ? View.GONE:View.VISIBLE);
        right_textview.setText(text);
        return this;
    }

    /**
     * 设置右边文字或图片的点击事件
     */
    public TitleBuilder setRightTextOrImageListener(OnClickListener listener) {

        if (right_imageview.getVisibility() == View.VISIBLE) {
            right_imageview.setOnClickListener(listener);
        } else if (right_textview.getVisibility() == View.VISIBLE) {
            right_textview.setOnClickListener(listener);
        }
        return this;
    }

    public View build(){
        return titleBar;
    }

}
