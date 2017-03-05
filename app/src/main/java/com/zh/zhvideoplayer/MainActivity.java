package com.zh.zhvideoplayer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.zh.zhvideoplayer.bean.LocalVideoBean;
import com.zh.zhvideoplayer.player.GetLocalVideosTask;
import com.zh.zhvideoplayer.player.LocalVideoListAdapter;
import com.zh.zhvideoplayer.player.PlayerActivity;
import com.zh.zhvideoplayer.ui.title.TitleBuilder;
import com.zh.zhvideoplayer.util.PermissionUtils;
import com.zh.zhvideoplayer.util.ScreenUtils;
import com.zh.zhvideoplayer.view.stateview.StateView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, GetLocalVideosTask.OnSuccessListener {
    private ListView mListView;
    private List<LocalVideoBean> mVideoList;
    private LocalVideoListAdapter mListAdapter;
    private GetLocalVideosTask mGetVideoTask;
    private StateView mStateView;// 加载状态控件，加载中、失败、成功
    private LinearLayout emptyView = null;
    private PullToRefreshListView mPullToRefreshListview;
    private Button mToTopBtn;// 返回顶部的按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View mView = View.inflate(getBaseContext(),
                R.layout.activity_main, null);
        setContentView(mView);
        initView(mView);
        initTitle();
        initListener();
        initDatas();
    }

    private void initTitle() {
        // 1.设置左边的图片按钮显示，以及事件 2.设置中间TextView显示的文字 3.设置右边的图片按钮显示，并设置事件
        new TitleBuilder(this).setLeftImageRes(false, 0)
                .setLeftTextOrImageListener(true, null)
                .setMiddleTitleText("本地视频");
    }

    /**
     * 初始化views
     *
     * @param mView
     */
    private void initView(View mView) {
        mStateView = (StateView) mView.findViewById(R.id.mStateView);
        mToTopBtn = (Button) mView.findViewById(R.id.btn_top);
        mPullToRefreshListview = (PullToRefreshListView) mView
                .findViewById(R.id.PullToRefreshListView);
        mPullToRefreshListview.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
        mPullToRefreshListview.setPullToRefreshOverScrollEnabled(false);
        // 得到真正的listview,我们在给listview设置adapter时或者设置onItemClick事件必须通过它，而不能用ptrlv_test
        mListView = mPullToRefreshListview.getRefreshableView();
        // 给ListView添加EmptyView(用于提示上拉加载数据，加载中，没有更多数据了)
        AbsListView.LayoutParams emptyViewlayoutParams = new AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT,
                AbsListView.LayoutParams.MATCH_PARENT);
        emptyView = (LinearLayout) LayoutInflater.from(this).inflate(
                R.layout.stateview_empty_view, mPullToRefreshListview, false);
        emptyView.setLayoutParams(emptyViewlayoutParams);
        emptyView.setGravity(Gravity.CENTER);
//				mListView.setEmptyView(emptyView);
        mPullToRefreshListview.setEmptyView(emptyView);
    }

    private void initListener() {
        mToTopBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                setListViewPos(0);
            }
        });

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int position, long arg3) {
                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                // 添加了setEmptyView(footerView)后，position-1要减1，防止数组越界
                intent.putExtra("video", mVideoList.get(position - 1));
                intent.putExtra("videoType", 1);
                startActivity(intent);
            }
        });

        mPullToRefreshListview.setOnScrollListener(new AbsListView.OnScrollListener() {
            int itemHeight = 0;
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,int visibleItemCount, int totalItemCount) {
//				// 当开始滑动且ListView底部的Y轴点超出屏幕最大范围时，显示或隐藏顶部按钮
                View c = view.getChildAt(0);
                if (c == null) {
                    return ;
                }
                if(itemHeight < c.getHeight()){
                    itemHeight = c.getHeight();
                }
                int height =  (firstVisibleItem + visibleItemCount -1) * itemHeight;
                if (height >= 1.5 * ScreenUtils.getScreenHeight(MainActivity.this)) {
                    mToTopBtn.setVisibility(View.VISIBLE);
                }else {
                    mToTopBtn.setVisibility(View.GONE);
                }
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }
        });

        // 设置刷新监听器
        mPullToRefreshListview
                .setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
                    @Override
                    public void onRefresh(
                            PullToRefreshBase<ListView> refreshView) {
                        if (mPullToRefreshListview.isRefreshing()) {
                            getDatas();
                        }
                    }
                });
    }

    private void initDatas() {
        mStateView.setCurrentState(StateView.STATE_LOADING);
        mVideoList = new ArrayList<LocalVideoBean>();
        mListAdapter = new LocalVideoListAdapter(mVideoList, this);
        mListView.setAdapter(mListAdapter);
        //6.0动态权限申请
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
            new PermissionUtils(this).needPermission(200);
        }else{
            getDatas();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 200: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    //同意给与权限  可以再此处调用拍照
                    Log.i("用户同意权限", "user granted the permission!");
                    getDatas();
                } else {

                    // permission denied, boo! Disable the
                    // f用户不同意 可以给一些友好的提示
                    Log.i("用户不同意权限", "user denied the permission!");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }



    private void getDatas() {
        mGetVideoTask = new GetLocalVideosTask();
        mGetVideoTask.setOnSuccessListener(this);
        mGetVideoTask.execute(this.getContentResolver());
    }

    @Override
    public void onSuccess(List<LocalVideoBean> videos) {
        if (videos.size() > 0) {
            mVideoList.clear();
            mVideoList.addAll(videos);
            mListAdapter.notifyDataSetChanged();
        } else {
        }
        ShowContent();
        // 我们可以 延迟 1秒左右，在调用onRefreshComplete 方法，可以解决该问题
        mListView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mPullToRefreshListview.setPullLabel("刷新完成");
                mPullToRefreshListview.onRefreshComplete();
                mPullToRefreshListview.setPullLabel("下拉刷新");
            }
        }, 1000);
    }

    @Override
    public void onClick(View arg0) {

    }

    private void ShowContent() {
        mStateView.setCurrentState(StateView.STATE_CONTENT);
        if (mVideoList.size() == 0 && emptyView != null) {
            emptyView.setVisibility(View.GONE);
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    emptyView.setVisibility(View.VISIBLE);
                }
            }, 20);
        }
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

    /**
     * 获取视频文件缩略图 API>=8(2.2)
     *
     * @param path
     *            视频文件的路径
     * @param kind
     *            缩略图的分辨率：MINI_KIND、MICRO_KIND、FULL_SCREEN_KIND
     * @return Bitmap 返回获取的Bitmap
     */
    public static Bitmap getVideoThumb2(String path, int kind) {
        return ThumbnailUtils.createVideoThumbnail(path, kind);
    }

    public static Bitmap getVideoThumb2(String path) {
        return getVideoThumb2(path,
                MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
    }

    /**
     * 滚动ListView到指定位置
     *
     * @param pos
     */
    private void setListViewPos(int pos) {
        if (android.os.Build.VERSION.SDK_INT >= 8) {
            mListView.smoothScrollToPosition(pos);
        } else {
            mListView.setSelection(pos);
        }
    }

}
