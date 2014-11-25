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

	private LinearLayout mHeaderRoot;// ���������Բ��ֵ�ID
	private int downY = -1;
	private View mPullDownView;// ͷ����

	private int mPullDownHeaderHeghit;// ����ͷ���ֵĸ߶�

	/**
	 * ˢ�µĽ���Ȧ
	 */
	private ProgressBar mPb;
	/**
	 * ����ˢ�µ�ͼƬ
	 */
	private ImageView mIvArrow;

	private int FristVisiblePosition = -1;// ��ǰListView��һ��Ŀ������

	private final int PULL_DOWN = 0;// ����ˢ��
	private final int RELEASE_REFRESH = 1;// �ͷ�ˢ��
	private final int REFRESHING = 2;// ����ˢ��

	private int currentState = PULL_DOWN;// ��ǰ����ˢ�µ�״̬
	/**
	 * ������״̬
	 */
	private TextView mRefreshState;
	/**
	 * ���ˢ�µ�ʱ��
	 */
	private TextView mRefreshTime;

	/**
	 * ������ת�Ķ���
	 */
	private RotateAnimation upRt;
	/**
	 * ������ת�Ķ���
	 */
	private RotateAnimation downRt;
	/**
	 * �û���ӽ�����ͷ���ļ��ֲ�ͼ
	 */
	private View mCustomHeaderView;
	/**
	 * ��ǰListViewˢ�����ݵ��¼�
	 */
	private OnRefreshListener mOnRefreshListener;
	private int mFooterHeader;// �Ų��ֵĸ߶�
	private View refreshFooterView;// �Ų��ֶ���
	private boolean isLoadingMore = false;//�Ƿ����ڼ��ظ��� Ĭ��Ϊfalse
	
	private boolean isEnableLoadingMore = false;//�Ƿ����ü��ظ��࣬Ĭ��Ϊ�����á�
	private boolean isEnablePullDown = false;//�Ƿ���������ˢ�£�Ĭ��Ϊ�����á�

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
	 * ��ӽŲ��֣����ࣩ
	 */
	private void initFooter() {
		refreshFooterView = View.inflate(getContext(),
				R.layout.refresh_listview_footer, null);

		refreshFooterView.measure(0, 0);// ����
		mFooterHeader = refreshFooterView.getMeasuredHeight();
		refreshFooterView.setPadding(0, -mFooterHeader, 0, 0);// ���ؽŲ���
		this.addFooterView(refreshFooterView);
	}

	/**
	 * ���ͷ������
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
		mRefreshTime.setText("���ˢ�µ�ʱ�䣺" + getCurrentTime());

		// ������ͷ��������
		mPullDownView.measure(0, 0);// ��������ͷ����
		mPullDownHeaderHeghit = mPullDownView.getMeasuredHeight();
		mPullDownView.setPadding(0, -mPullDownHeaderHeghit, 0, 0);
		this.addHeaderView(refreshView);

		initAnimation();

	}

	/**
	 * ��ʼ��ͷ���ֵĶ���
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
	 * ���һ���Զ����ͷ�����ֶ���
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
		case MotionEvent.ACTION_DOWN:// ����
			downY = (int) ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:// ����
			if(!isEnablePullDown){
				break;
			}
			// �����ǰ״̬Ϊˢ��״̬����ôֱ���������������κβ���
			if (currentState == REFRESHING) {
				break;
			}

			if (mCustomHeaderView != null) {

				// ����û��Զ�����ӵ�ͷ���֣��ֲ�ͼ��û����ȫ��ʾ����ô����������ˢ�²�����ֱ������

				// ȡ��ListView����Ļ�е�Yֵ
				int[] location = new int[2];
				this.getLocationOnScreen(location);// ȡ��ListView����Ļ��X,Y��ֵ
				int listViewY = location[1];
				// ȡ���ֲ�ͼ����Ļ��Yֵ
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
				if (paddingTop > 0 && currentState == PULL_DOWN) {// ��ǰ��ͷ������ȫ��ʾ��������
																	// ��ǰ��״̬Ϊ����ˢ��
					System.out.println("�ɿ�ˢ��");
					currentState = RELEASE_REFRESH;// �ѵ�ǰ��״̬��Ϊ�ͷ�ˢ��
					refreshPullDownHeaderView();
				} else if (paddingTop < 0 && currentState == RELEASE_REFRESH) {
					System.out.println("����ˢ��");
					currentState = PULL_DOWN;
					refreshPullDownHeaderView();
				}

				mPullDownView.setPadding(0, paddingTop, 0, 0);
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:// ����
			downY = -1;
			if (currentState == PULL_DOWN) {
				// ��ǰ��ͷ������ȫ��ʾ�������� ��ǰ��״̬Ϊ����ˢ��
				mPullDownView.setPadding(0, -mPullDownHeaderHeghit, 0, 0);
			} else if (currentState == RELEASE_REFRESH) {
				currentState = REFRESHING;
				refreshPullDownHeaderView();
				mPullDownView.setPadding(0, 0, 0, 0);

				if (mOnRefreshListener != null) {
					mOnRefreshListener.OnPullDownRefresh();// �ص�ˢ�����ݵ��¼�
				}
			}
			break;
		}
		return super.onTouchEvent(ev);
	}

	private void refreshPullDownHeaderView() {
		switch (currentState) {
		case PULL_DOWN:// ����ˢ��
			mIvArrow.startAnimation(downRt);
			mRefreshState.setText("����ˢ��");
			break;
		case RELEASE_REFRESH:// �ͷ�ˢ��
			mIvArrow.startAnimation(upRt);
			mRefreshState.setText("�ͷ�ˢ��");
			break;
		case REFRESHING:// ����ˢ��
			mIvArrow.setVisibility(View.INVISIBLE);
			mIvArrow.clearAnimation();
			mPb.setVisibility(View.VISIBLE);
			mRefreshState.setText("����ˢ��");
			break;

		default:
			break;
		}
	}

	/**
	 * ������ʱ�����˷���
	 * 
	 * @param firstVisibleItem
	 *            ��ǰ����ʱ����ʾ�ڵ�һ����Item
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		FristVisiblePosition = firstVisibleItem;
	}

	/**
	 * 
	 * @param scrollState
	 *            SCROLL_STATE_IDLE:ֹͣ SCROLL_STATE_TOUCH_SCROLL����������
	 *            SCROLL_STATE_FLING������
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		
		if(!isEnableLoadingMore){
			return;
		}
		// �������ֹͣ���߿��ٻ������ײ���Ҫ���ظ���Ĳ���
		if (scrollState == SCROLL_STATE_IDLE
				|| scrollState == SCROLL_STATE_FLING) {
			if (this.getLastVisiblePosition() == (getCount() - 1)
					&& !isLoadingMore) {
				refreshFooterView.setPadding(0, 0, 0, 0);
				//��ListView�����ײ�
				this.setSelection(getCount());
				isLoadingMore = true;
				
				//�����û��Ļص��¼�
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
	 * ������ˢ�º���ô˷���������ͷ����
	 */
	public void OnRefreshFinish() {
		if (currentState == REFRESHING) {
			currentState = PULL_DOWN;
			mPb.setVisibility(View.INVISIBLE);
			mIvArrow.setVisibility(View.VISIBLE);
			mRefreshState.setText("����ˢ��");
			mRefreshTime.setText("���ˢ�µ�ʱ�䣺" + getCurrentTime());
			mPullDownView.setPadding(0, -mPullDownHeaderHeghit, 0, 0);
		}else if(isLoadingMore){
			isLoadingMore = false;
			refreshFooterView.setPadding(0, -mFooterHeader, 0, 0);
		}
	}

	/**
	 * ��ȡ��ǰ��ʱ�� 2014-11-11 11:11:11
	 * 
	 * @return
	 */
	public String getCurrentTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date());
	}
	
	
	/**
	 * �Ƿ����ü��ظ��๦��
	 * @param isEnableLoadingMore trueΪ����
	 */
	public void setEnableLoadingMore(boolean isEnableLoadingMore){
		this.isEnableLoadingMore = isEnableLoadingMore;
	}
	/**
	 * �Ƿ���������ˢ�¹���
	 * @param isEnablePullDown trueΪ����
	 */
	public void setEnablePullDown(boolean isEnablePullDown){
		this.isEnablePullDown = isEnablePullDown;
	}
}
