package com.sunshine.refreshlistview_sunshine.view;

public interface OnRefreshListener {
	/**
	 * ������ˢ��ʱ�ص��˷�����ˢ������
	 */
	public void OnPullDownRefresh();

	/**
	 * ���ظ���
	 */
	public void OnLoadingMore();
}
