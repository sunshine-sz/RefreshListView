package com.sunshine.refreshlistview_sunshine.view;

public interface OnRefreshListener {
	/**
	 * 当下拉刷新时回调此方法，刷新数据
	 */
	public void OnPullDownRefresh();

	/**
	 * 加载更多
	 */
	public void OnLoadingMore();
}
