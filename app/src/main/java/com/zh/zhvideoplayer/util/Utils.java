package com.zh.zhvideoplayer.util;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
/**
 * 播放器工具栏出现和隐藏动画
 */
public class Utils {
	/**
     * 位移动画
     *
     * @param view
     * @param xFrom
     * @param xTo
     * @param yFrom
     * @param yTo
     * @param duration
     */
    public static void translateAnimation(View view, float xFrom, float xTo,
                                          float yFrom, float yTo, long duration) {

        TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, xFrom, Animation.RELATIVE_TO_SELF, xTo,
                Animation.RELATIVE_TO_SELF, yFrom, Animation.RELATIVE_TO_SELF, yTo);
        translateAnimation.setFillAfter(false);
        translateAnimation.setDuration(duration);
        view.startAnimation(translateAnimation);
        translateAnimation.startNow();
    }

}
