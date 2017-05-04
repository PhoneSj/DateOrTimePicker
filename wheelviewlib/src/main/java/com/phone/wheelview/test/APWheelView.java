package com.phone.wheelview.test;

import android.graphics.Color;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

import ucd.ui.framework.Settings.GLBaseSettings;
import ucd.ui.framework.core.GLBase;
import ucd.ui.framework.core.GLObject;
import ucd.ui.framework.core.Group;
import ucd.ui.framework.core.ImageEx;
import ucd.ui.framework.coreEx.Text;
import ucd.ui.widget.cbb.util.LogUtils;

public class APWheelView extends Group {

	private final String TAG = "APWheelView";
	private final boolean TAG_ENABLE = true;
	private final static int INVALID_POSITION = -1;
	private final static String DEFAULT_SELECTED_TEXTSTYLE = "wheel_selected";
	private final static String DEFAULT_NORMAL_TEXTSTYLE = "wheel_normal";
	private final static String DEFAULT_NORMAL_TEXTSTYLE_LEVEL2 = "wheel_normal_level2";
	private static final String STYLE = "style";
	private final int SCROLL_DURATION = 300;
	private int showCount = 2;
	private List<Text> cache;
	private List<String> datas;
	private int rowHeight;
	private float lastMotionY;
	//分割线
	private ImageEx upLine;
	private ImageEx downLine;
	private int lineColor = Color.RED;
	private int lineSize = 2;
	//文本字体
	private String selectedTextStyle = DEFAULT_SELECTED_TEXTSTYLE;
	private String normalTextStyle = DEFAULT_NORMAL_TEXTSTYLE;
	private String normalTextStyle2 = DEFAULT_NORMAL_TEXTSTYLE_LEVEL2;
	/** 默认触发移动的手指移动距离 **/
	private int mTouchSlop;
	/** 正在拉动 **/
	private boolean mIsBeingDragged;
	//选中监听器
	private OnAPWheelListener onAPWheelListener;
	//动画
	private FlingRunnable flingRunnable;
	//动画结束监听器
	private OnSmoothScrollFinishedListener onSmoothScrollFinishedListener = new OnSmoothScrollFinishedListener() {

		@Override
		public void onSmoothScrollFinished() {
			LogUtils.showV(TAG, "onSmoothScrollFinished", TAG_ENABLE);
			if (onAPWheelListener != null) {
				int position = getSelectedPosition();
				if (position >= 0 && position < datas.size()) {
					String conent = datas.get(position);
					onAPWheelListener.onAPWheel(position, conent);
					LogUtils.showV(TAG, "选中了第" + position + "项", TAG_ENABLE);
				} else {
					throw new RuntimeException("超出下标范围");
				}
			}

		}
	};

	public APWheelView(GLBase arg0, int arg1, int arg2) {
		super(arg0, arg1, arg2);
		cache = new ArrayList<Text>();
		upLine = new ImageEx(root, getMeasuredWidth(), lineSize);
		upLine.setPaintColor(lineColor);
		upLine.setParent(this);
		downLine = new ImageEx(root, getMeasuredWidth(), lineSize);
		downLine.setPaintColor(lineColor);
		downLine.setParent(this);
		mTouchSlop = ViewConfiguration.get(getRoot().getContext()).getScaledTouchSlop();
	}

	public List<String> getDatas() {
		return datas;
	}

	public void setDatas(List<String> datas) {
		if (datas == null) {
			return;
		}
		this.datas = datas;
		rowHeight = getMeasuredHeight() / (showCount * 2 + 1);
		delAll();
		cache.clear();
		initLayout();
	}

	public void setDatas(List<String> datas, int selectedPosition) {
		if (datas == null) {
			return;
		}
		setDatas(datas);
		setSelectedPosition(selectedPosition, SCROLL_DURATION);
	}

	/** 初始化布局 **/
	private void initLayout() {
		for (int i = 0; i < datas.size(); i++) {
			Text child = new Text(root, getMeasuredWidth(), rowHeight);
			child.setText(datas.get(i));
			child.setY((showCount + i) * rowHeight);
			child.setStyle(normalTextStyle2);
			add(child);
		}
		updateStyle();
	}

	/** 重新布局 **/
	private void layoutChildren(float deltaY) {
		//		LogUtils.showV(TAG, "layoutChildren", TAG_ENABLE);
		int childCount = getChildrenCount();
		final GLObject firstChild = getChildAt(0);
		final GLObject lastChild = getChildAt(childCount - 1);
		if (firstChild.getY() + deltaY > showCount * rowHeight) {
			//下移位置过大
			deltaY = showCount * rowHeight - firstChild.getY();
		} else if (lastChild.getY() + lastChild.getMeasuredHeight() + deltaY < (showCount + 1) * rowHeight) {
			deltaY = (showCount + 1) * rowHeight - lastChild.getY() - lastChild.getMeasuredHeight();
		}
		offsetTopAndBottom(deltaY);
		updateStyle();
	}

	private void updateStyle() {
		int childCount = getChildrenCount();
		for (int i = 0; i < childCount; i++) {
			final Text child = (Text) getChildAt(i);
			//获取之前的样式
			String style = (String) child.getData(STYLE);
			if (Math.abs(child.getY() - showCount * rowHeight) < rowHeight * 0.5f) {
				if (!selectedTextStyle.equals(style)) {
					child.setStyle(selectedTextStyle);
					child.setData(STYLE, selectedTextStyle);
				}
			} else if (Math.abs(child.getY() - (showCount - 1) * rowHeight) < rowHeight * 0.5f
					|| Math.abs(child.getY() - (showCount + 1) * rowHeight) < rowHeight * 0.5f) {
				if (!normalTextStyle.equals(style)) {
					child.setStyle(normalTextStyle);
					child.setData(STYLE, normalTextStyle);
				}
			} else {
				if (!normalTextStyle2.equals(style)) {
					child.setStyle(normalTextStyle2);
					child.setData(STYLE, normalTextStyle2);
				}
			}
		}
	}

	/** 上下移动子控件 **/
	private void offsetTopAndBottom(float deltaY) {
		int childCount = getChildrenCount();
		for (int i = 0; i < childCount; i++) {
			final GLObject child = getChildAt(i);
			child.setY(child.getY() + deltaY);
		}
	}

	@Override
	protected void _drawChildren(GLBaseSettings arg0) {
		super._drawChildren(arg0);
		upLine.setY(rowHeight * showCount - upLine.getMeasuredHeight() / 2);
		upLine.draw();
		downLine.setY(rowHeight * (showCount + 1) - downLine.getMeasuredHeight() / 2);
		downLine.draw();
	}

	@Override
	public void setLayoutParams(int w, int h) {
		super.setLayoutParams(w, h);
		upLine.setLayoutParams(getMeasuredWidth(), lineSize);
		downLine.setLayoutParams(getMeasuredWidth(), lineSize);
		int childCount = getChildrenCount();
		rowHeight = getMeasuredHeight() / (showCount * 2 + 1);
		for (int i = 0; i < childCount; i++) {
			final GLObject child = getChildAt(i);
			child.setLayoutParams(getMeasuredWidth(), rowHeight);
		}
		offsetTopAndBottom(0);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				lastMotionY = event.getY();
				mIsBeingDragged = false;
				break;
			case MotionEvent.ACTION_MOVE:
				float deltaY = event.getY() - lastMotionY;
				if (Math.abs(deltaY) > mTouchSlop || mIsBeingDragged) {
					lastMotionY = event.getY();
					layoutChildren(deltaY);
					mIsBeingDragged = true;
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (mIsBeingDragged) {
					smoothScroll();
				}
				mIsBeingDragged = false;
				break;
			default:
				break;
		}
		return true;
	}

	private void smoothScroll() {
		LogUtils.showD(TAG, "smoothScroll", TAG_ENABLE);
		if (flingRunnable != null) {
			flingRunnable.stop();
		}
		flingRunnable = new FlingRunnable(onSmoothScrollFinishedListener);
		flingRunnable.start(getDistance(), SCROLL_DURATION);
	}

	private int getDistance() {
		GLObject centerChild = getCenterChild();
		int distance = (int) (rowHeight * showCount - centerChild.getY());
		return distance;
	}

	/** 获得最中心的子控件 **/
	private GLObject getCenterChild() {
		int childCount = getChildrenCount();
		for (int i = 0; i < childCount; i++) {
			final GLObject child = getChildAt(i);
			if (Math.abs(child.getY() - showCount * rowHeight) <= rowHeight / 2) {
				return child;
			}
		}
		return null;
	}

	public void setSelectedPosition(int selectedPosition) {
		setSelectedPosition(selectedPosition, SCROLL_DURATION);
	}

	/** 设置选中的下标 **/
	public void setSelectedPosition(int selectedPosition, int duration) {
		LogUtils.showD(TAG, "setSelectedPosition", TAG_ENABLE);
		if (selectedPosition < 0 && selectedPosition >= datas.size()) {
			return;
		}
		if (flingRunnable != null) {
			flingRunnable.stop();
		}
		//注意方向：当新position比之前大时，移动距离的负数
		int deltaPosition = getSelectedPosition() - selectedPosition;
		LogUtils.showD(TAG, "selectedPosition:" + deltaPosition, TAG_ENABLE);
		//调整误差的距离
		GLObject centerChild = getCenterChild();
		int firstDistance = (int) (rowHeight * showCount - centerChild.getY());
		//整数倍距离
		int distance = rowHeight * deltaPosition + firstDistance;
		LogUtils.showD(TAG, "distance:" + distance, TAG_ENABLE);
		flingRunnable = new FlingRunnable(onSmoothScrollFinishedListener);
		flingRunnable.start(distance, duration);
	}

	/** 获得选中的下标 **/
	public int getSelectedPosition() {
		int childCount = getChildrenCount();
		for (int i = 0; i < childCount; i++) {
			final GLObject child = getChildAt(i);
			if (Math.abs(child.getY() - showCount * rowHeight) < rowHeight / 2) {
				return i;
			}
		}
		return INVALID_POSITION;
	}

	private void postOnAnimation(Runnable action) {
		root.post(action);
	}

	private class FlingRunnable implements Runnable {

		private final Scroller mScroller;
		private int mLastY;
		private OnSmoothScrollFinishedListener mListener;

		FlingRunnable(OnSmoothScrollFinishedListener listener) {
			mScroller = new Scroller(root.getContext(), new DecelerateInterpolator());
			mListener = listener;
		}

		public void start(int dy, int duration) {
			mScroller.startScroll(0, 0, 0, dy, duration);
			postOnAnimation(this);
		}

		@Override
		public void run() {
			final Scroller scroller = mScroller;
			boolean more = scroller.computeScrollOffset();
			int nowY = scroller.getCurrY();
			int deltaY = nowY - mLastY;
			mLastY = nowY;
			layoutChildren(deltaY);
			if (more) {
				postOnAnimation(this);
			} else {
				end();
				if (null != mListener) {
					mListener.onSmoothScrollFinished();
				}
			}
		}

		void end() {
			root.removeCallbacks(this);
			mScroller.abortAnimation();
		}

		public void stop() {
			mScroller.forceFinished(true);
			root.removeCallbacks(this);
		}

	}

	/** 动画执行完毕监听回调 **/
	private static interface OnSmoothScrollFinishedListener {
		void onSmoothScrollFinished();
	}

	public interface OnAPWheelListener {
		void onAPWheel(int position, String content);
	}

	public OnAPWheelListener getOnAPWheelListener() {
		return onAPWheelListener;
	}

	public void setOnAPWheelListener(OnAPWheelListener onAPWheelListener) {
		this.onAPWheelListener = onAPWheelListener;
	}

	public int getShowCount() {
		return showCount;
	}

	public void setShowCount(int showCount) {
		this.showCount = showCount;
	}

	public String getSelectedTextStyle() {
		return selectedTextStyle;
	}

	public void setSelectedTextStyle(String selectedTextStyle) {
		this.selectedTextStyle = selectedTextStyle;
	}

	public String getNormalTextStyle() {
		return normalTextStyle;
	}

	public void setNormalTextStyle(String normalTextStyle) {
		this.normalTextStyle = normalTextStyle;
	}

	public int getLineColor() {
		return lineColor;
	}

	public void setLineColor(int lineColor) {
		this.lineColor = lineColor;
		upLine.setPaintColor(lineColor);
		downLine.setPaintColor(lineColor);
	}

	public int getLineSize() {
		return lineSize;
	}

	public void setLineSize(int lineSize) {
		this.lineSize = lineSize;
		upLine.setLayoutParams(upLine.getMeasuredWidth(), lineSize);
		downLine.setLayoutParams(downLine.getMeasuredWidth(), lineSize);
	}

}
