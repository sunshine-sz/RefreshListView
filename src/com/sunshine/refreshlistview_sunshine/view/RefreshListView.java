package com.sunshine.refreshlistview_sunshine.view;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sunshine.refreshlistview_sunshine.R;

public class RefreshListView extends ListView implements OnScrollListener {

	private LinearLayout mHeaderRoot;// 定义外线性布局的ID
	private int downY = -1;
	private View mPullDownView;// 头布局

	private int mPullDownHeaderHeghit;// 下拉头布局的高度

	/**
	 * 刷新的进度圈
	 */
	private ProgressBar mPb;
	/**
	 * 下拉刷新的图片
	 */
	private ImageView mIvArrow;

	private int FristVisiblePosition = -1;// 当前ListView第一条目的索引

	private final int PULL_DOWN = 0;// 下拉刷新
	private final int RELEASE_REFRESH = 1;// 释放刷新
	private final int REFRESHING = 2;// 正在刷新

	private int currentState = PULL_DOWN;// 当前下拉刷新的状态
	/**
	 * 下拉的状态
	 */
	private TextView mRefreshState;
	/**
	 * 最后刷新的时间
	 */
	private TextView mRefreshTime;

	/**
	 * 向上旋转的动画
	 */
	private RotateAnimation upRt;
	/**
	 * 向下旋转的动画
	 */
	private RotateAnimation downRt;
	/**
	 * 用户添加进来的头部文件轮播图
	 */
	private View mCustomHeaderView;
	/**
	 * 当前ListView刷新数据的事件
	 */
	private OnRefreshListener mOnRefreshListener;
	private int mFooterHeader;// 脚布局的高度
	private View refreshFooterView;// 脚布局对象
	private boolean isLoadingMore = false;//是否正在加载更多 默认为false
	
	private boolean isEnableLoadingMore = false;//是否启用加载更多，默认为不起用。
	private boolean isEnablePullDown = false;//是否启用下拉刷新，默认为不起用。

	public RefreshListView(Context context) {
		super(context);
		initHeader();
		initFooter();
		setOnScrollListener(this);
	}

	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initHeader();
		initFooter();
		setOnScrollListener(this);
	}

	/**
	 * 添加脚布局（更多）
	 */
	private void initFooter() {
		refreshFooterView = View.inflate(getContext(),
				R.layout.refresh_listview_footer, null);

		refreshFooterView.measure(0, 0);// 测量
		mFooterHeader = refreshFooterView.getMeasuredHeight();
		refreshFooterView.setPadding(0, -mFooterHeader, 0, 0);// 隐藏脚布局
		this.addFooterView(refreshFooterView);
	}

	/**
	 * 添加头部布局
	 */
	private void initHeader() {
		View refreshView = View.inflate(getContext(),
				R.layout.refreshlistview_header, null);
		mHeaderRoot = (LinearLayout) refreshView
				.findViewById(R.id.ll_refresh_listview_root);

		mPullDownView = refreshView.findViewById(R.id.ll_pull_down_view);
		mIvArrow = (ImageView) refreshView
				.findViewById(R.id.iv_refresh_listview_arrow);
		mPb = (ProgressBar) refreshView.findViewById(R.id.pb_refresh_listview);
		mRefreshState = (TextView) refreshView
				.findViewById(R.id.tv_refresh_listview_state);
		mRefreshTime = (TextView) refreshView
				.findViewById(R.id.tv_refresh_listview_time);
		mRefreshTime.setText("最后刷新的时间：" + getCurrentTime());

		// 把下拉头布局隐藏
		mPullDownView.measure(0, 0);// 测量下拉头布局
		mPullDownHeaderHeghit = mPullDownView.getMeasuredHeight();
		mPullDownView.setPadding(0, -mPullDownHeaderHeghit, 0, 0);
		this.addHeaderView(refreshView);

		initAnimation();

	}

	/**
	 * 初始化头布局的动画
	 */
	private void initAnimation() {
		upRt = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		upRt.setDuration(500);
		upRt.setFillAfter(true);

		downRt = new RotateAnimation(-180, -360, Animation.RELATIVE_TO_SELF,
				0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		downRt.setDuration(500);
		downRt.setFillAfter(true);
	}

	/**
	 * 添加一个自定义的头部布局对象
	 * 
	 * @param view
	 */
	public void addCustomHeaderView(View view) {
		mCustomHeaderView = view;
		mHeaderRoot.addView(view);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:// 按下
			downY = (int) ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:// 滑动
			if(!isEnablePullDown){
				break;
			}
			// 如果当前状态为刷新状态，那么直接跳出，不进行任何操作
			if (currentState == REFRESHING) {
				break;
			}

			if (mCustomHeaderView != null) {

				// 如果用户自定义添加的头布局（轮播图）没有完全显示，那么不进行下拉刷新操作，直接跳出

				// 取出ListView在屏幕中的Y值
				int[] location = new int[2];
				this.getLocationOnScreen(location);// 取出ListView在屏幕的X,Y的值
				int listViewY = location[1];
				// 取出轮播图在屏幕中Y值
				mCustomHeaderView.getLocationOnScreen(location);
				int mCustomHeaderViewY = location[1];

				if (mCustomHeaderViewY < listViewY) {
					break;
				}
			}

			if (downY == -1) {
				downY = (int) ev.getY();
			}
			int moveY = (int) ev.getY();
			int paddingTop = -mPullDownHeaderHeghit + (moveY - downY);
			if (paddingTop > -mPullDownHeaderHeghit
					&& FristVisiblePosition == 0) {
				if (paddingTop > 0 && currentState == PULL_DOWN) {// 当前把头布局完全显示出来并且
																	// 当前的状态为下拉刷新
					System.out.println("松开刷新");
					currentState = RELEASE_REFRESH;// 把当前的状态改为释放刷新
					refreshPullDownHeaderView();
				} else if (paddingTop < 0 && currentState == RELEASE_REFRESH) {
					System.out.println("下拉刷新");
					currentState = PULL_DOWN;
					refreshPullDownHeaderView();
				}

				mPullDownView.setPadding(0, paddingTop, 0, 0);
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:// 提起
			downY = -1;
			if (currentState == PULL_DOWN) {
				// 当前把头布局完全显示出来并且 当前的状态为下拉刷新
				mPullDownView.setPadding(0, -mPullDownHeaderHeghit, 0, 0);
			} else if (currentState == RELEASE_REFRESH) {
				currentState = REFRESHING;
				refreshPullDownHeaderView();
				mPullDownView.setPadding(0, 0, 0, 0);

				if (mOnRefreshListener != null) {
					mOnRefreshListener.OnPullDownRefresh();// 回调刷新数据的事件
				}
			}
			break;
		}
		return super.onTouchEvent(ev);
	}

	private void refreshPullDownHeaderView() {
		switch (currentState) {
		case PULL_DOWN:// 下拉刷新
			mIvArrow.startAnimation(downRt);
			mRefreshState.setText("下拉刷新");
			break;
		case RELEASE_REFRESH:// 释放刷新
			mIvArrow.startAnimation(upRt);
			mRefreshState.setText("释放刷新");
			break;
		case REFRESHING:// 正在刷新
			mIvArrow.setVisibility(View.INVISIBLE);
			mIvArrow.clearAnimation();
			mPb.setVisibility(View.VISIBLE);
			mRefreshState.setText("正在刷新");
			break;

		default:
			break;
		}
	}

	/**
	 * 当滚动时触发此方法
	 * 
	 * @param firstVisibleItem
	 *            当前滚动时，显示在第一个的Item
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		FristVisiblePosition = firstVisibleItem;
	}

	/**
	 * 
	 * @param scrollState
	 *            SCROLL_STATE_IDLE:停止 SCROLL_STATE_TOUCH_SCROLL：触摸滑动
	 *            SCROLL_STATE_FLING：滑行
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		
		if(!isEnableLoadingMore){
			return;
		}
		// 如果滚动停止或者快速滑动到底部就要加载更多的操作
		if (scrollState == SCROLL_STATE_IDLE
				|| scrollState == SCROLL_STATE_FLING) {
			if (this.getLastVisiblePosition() == (getCount() - 1)
					&& !isLoadingMore) {
				refreshFooterView.setPadding(0, 0, 0, 0);
				//让ListView滚到底部
				this.setSelection(getCount());
				isLoadingMore = true;
				
				//调用用户的回调事件
				if(mOnRefreshListener!=null){
					mOnRefreshListener.OnLoadingMore();
				}
			}
		}

	}

	public void setOnRefreshListener(OnRefreshListener listener) {
		this.mOnRefreshListener = listener;
	}

	/**
	 * 当数据刷新后调用此方法，隐藏头布局
	 */
	public void OnRefreshFinish() {
		if (currentState == REFRESHING) {
			currentState = PULL_DOWN;
			mPb.setVisibility(View.INVISIBLE);
			mIvArrow.setVisibility(View.VISIBLE);
			mRefreshState.setText("下拉刷新");
			mRefreshTime.setText("最后刷新的时间：" + getCurrentTime());
			mPullDownView.setPadding(0, -mPullDownHeaderHeghit, 0, 0);
		}else if(isLoadingMore){
			isLoadingMore = false;
			refreshFooterView.setPadding(0, -mFooterHeader, 0, 0);
		}
	}

	/**
	 * 获取当前的时间 2014-11-11 11:11:11
	 * 
	 * @return
	 */
	public String getCurrentTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date());
	}
	
	
	/**
	 * 是否启用加载更多功能
	 * @param isEnableLoadingMore true为启用
	 */
	public void setEnableLoadingMore(boolean isEnableLoadingMore){
		this.isEnableLoadingMore = isEnableLoadingMore;
	}
	/**
	 * 是否启用下拉刷新功能
	 * @param isEnablePullDown true为启用
	 */
	public void setEnablePullDown(boolean isEnablePullDown){
		this.isEnablePullDown = isEnablePullDown;
	}
}
