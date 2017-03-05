package com.handmark.pulltorefresh.library.extras;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.R;

/**
 * 瀑布流下拉刷新
 *
 * @author zh
 *
 */
public class PullToRefreshStaggeredGridLayout1 extends
		PullToRefreshBase<RecyclerView> {
	public PullToRefreshStaggeredGridLayout1(Context context) {
		super(context);
	}

	public PullToRefreshStaggeredGridLayout1(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public final Orientation getPullToRefreshScrollDirection() {
		return Orientation.VERTICAL;
	}

	private RecyclerView recyclerView;
	private boolean isScrollOnHeader = true;
	private boolean isScrollOnFooter = false;

	@Override
	protected RecyclerView createRefreshableView(Context context,
												 AttributeSet attrs) {
		recyclerView = new RecyclerView(context, attrs);
		recyclerView.setId(R.id.straggereddGridLayout);
		final StaggeredGridLayoutManager mLayoutManager = new StaggeredGridLayoutManager(
				2, StaggeredGridLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(mLayoutManager);

		recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
			int[] lastVisibleItem;
			int[] fistVisibleItem;

			@Override
			public void onScrollStateChanged(RecyclerView recyclerView,
											 int newState) {

				if (null != fistVisibleItem) {
					isScrollOnHeader = 0 == fistVisibleItem[0];
				} else {
					isScrollOnHeader = true;
				}

				if (null != lastVisibleItem) {
					boolean isLast = mLayoutManager.getItemCount() - 1 == lastVisibleItem[0]
							|| mLayoutManager.getItemCount() == lastVisibleItem[1]
							|| mLayoutManager.getItemCount() - 1 == lastVisibleItem[1]
							|| mLayoutManager.getItemCount() == lastVisibleItem[0];
					isScrollOnFooter = newState == RecyclerView.SCROLL_STATE_IDLE
							&& isLast;
				} else {
					isScrollOnFooter = true;
				}
			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				fistVisibleItem = mLayoutManager
						.findFirstCompletelyVisibleItemPositions(new int[2]);
				lastVisibleItem = mLayoutManager
						.findLastCompletelyVisibleItemPositions(new int[2]);
			}

		});
		return recyclerView;
	}

	@Override
	protected boolean isReadyForPullStart() {
		return isScrollOnHeader;
	}

	@Override
	protected boolean isReadyForPullEnd() {

		return isScrollOnFooter;
	}

}