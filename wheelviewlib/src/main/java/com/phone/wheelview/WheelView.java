package com.phone.wheelview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.Scroller;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class WheelView extends FrameLayout {

	private final String TAG = "WheelView";
	private final boolean TAG_ENABLE = true;
	private final static int INVALID_POSITION = -1;
	//文本默认样式
	private final static int DEFAULT_SELECTED_TEXT_SIZE_SP = 16;
	private final static int DEFAULT_NORMAL_TEXT_SIZE_SP = 14;
	private final static int DEFAULT_NORMAL_TEXT_SIZE_SP_LEVEL2 = 14;
	private final static int DEFAULT_SELECTED_TEXT_COLOR = 0xff007dff;
	private final static int DEFAULT_NORMAL_TEXT_COLOR = 0x7f000000;
	private final static int DEFAULT_NORMAL_TEXT_COLOR_LEVEL2 = 0x4c000000;
	//分割线默认样式
	private final static int DEFAULT_DIVIDER_COLOR = Color.RED;
	private final static int DEFAULT_DIVIDER_SIZE = 1;
	private final static int DEFAULT_DIVIDER_PADDING_LEFT = 5;
	private final static int DEFAULT_DIVIDER_PADDING_RIGHT = 5;

	private final int SCROLL_DURATION = 300;
	private int showCount = 2;
	private List<TextView> cache;
	private List<String> datas;
	private int rowHeight;
	private float lastMotionY;
	private int firstPosition;
	//文本样式
	private int selectedTextSize;
	private int normalTextSize;
	private int normalTextSize2 = DEFAULT_NORMAL_TEXT_SIZE_SP_LEVEL2;
	private int selectedTextColor;
	private int normalTextColor;
	private int normalTextColor2 = DEFAULT_NORMAL_TEXT_COLOR_LEVEL2;
	//分割线样式
	private int dividerColor;
	private int dividerPaddingLeft;
	private int dividerPaddingRight;
	private int dividerSize;
	private Paint dividerPaint;

	//速度监听
	private VelocityTracker velocityTracker;
	private float velocitY;
	private float velocitYThreshold = 200;

	/**
	 * 默认触发移动的手指移动距离
	 **/
	private int mTouchSlop;
	/**
	 * 正在拉动
	 **/
	private boolean mIsBeingDragged;
	//选中监听器
	private OnWheelListener onWheelListener;
	//进、借位监听器
	private OnWheelCarryListener onWheelCarryListener;
	//动画
	private FlingRunnable flingRunnable;
	//动画结束监听器
	private OnSmoothScrollFinishedListener onSmoothScrollFinishedListener = new OnSmoothScrollFinishedListener() {

		@Override
		public void onSmoothScrollFinished() {
			LogUtils.showV(TAG, "onSmoothScrollFinished", TAG_ENABLE);
			if (onWheelListener != null) {
				int position = getSelectedPosition();
				if (position >= 0 && position < datas.size()) {
					String conent = datas.get(position);
					onWheelListener.onWheel(position, conent);
					LogUtils.showV(TAG, "选中了第" + position + "项", TAG_ENABLE);
				} else {
					throw new RuntimeException("超出下标范围");
				}
			}

		}
	};

	public WheelView(Context context) {
		this(context, null);
	}

	public WheelView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public WheelView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		cache = new ArrayList<TextView>();
		velocityTracker = VelocityTracker.obtain();
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		ViewConfiguration.get(context);
		dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		//获取自定义属性
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WheelView);
		normalTextSize = sp2px(context,
				a.getDimension(R.styleable.WheelView_normalTextColor, DEFAULT_NORMAL_TEXT_SIZE_SP));
		selectedTextSize = sp2px(context,
				a.getDimension(R.styleable.WheelView_selectedTextColor, DEFAULT_SELECTED_TEXT_SIZE_SP));
		normalTextColor = a.getColor(R.styleable.WheelView_normalTextColor, DEFAULT_NORMAL_TEXT_COLOR);
		selectedTextColor = a.getColor(R.styleable.WheelView_selectedTextColor, DEFAULT_SELECTED_TEXT_COLOR);
		dividerColor = a.getColor(R.styleable.WheelView_dividerColor, DEFAULT_DIVIDER_COLOR);
		dividerSize = dp2px(context, a.getDimension(R.styleable.WheelView_dividerSize, DEFAULT_DIVIDER_SIZE));
		dividerPaddingLeft = dp2px(context,
				a.getDimension(R.styleable.WheelView_dividerPaddingLeft, DEFAULT_DIVIDER_PADDING_LEFT));
		dividerPaddingRight = dp2px(context,
				a.getDimension(R.styleable.WheelView_dividerPaddingRight, DEFAULT_DIVIDER_PADDING_RIGHT));

		dividerPaint.setColor(dividerColor);
		dividerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
	}

	public void setDatas(List<String> datas) {
		setDatas(datas, 0);
	}

	public void setDatas(List<String> datas, int selectedPosition) {
		if (datas == null) {
			return;
		}
		this.selectedPosition = selectedPosition;
		this.datas = datas;
		LogUtils.showW("phoneTest", "setDatas...======================================", true);
		rowHeight = getMeasuredHeight() / (showCount * 2 + 1);
		removeAllViews();
		cache.clear();
		initLayout();
		requestLayout();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		LogUtils.showW("phoneTest", "onMeasure...======================================", true);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		LogUtils.showW("phoneTest", "onSizeChanged...======================================", true);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		LogUtils.showW("phoneTest", "onLayout...======================================", true);
	}

	private void initLayout() {
		firstPosition = selectedPosition - showCount;
		for (int i = 0; i < showCount * 2 + 1; i++) {
			TextView child = getCacheView(i + firstPosition);
			child.setBackgroundColor(Color.BLUE);
			//			child.setY(i * rowHeight);
			child.layout(child.getLeft(), i * rowHeight, child.getRight(), (i + 1) * rowHeight);
			addView(child);
		}
		//更新样式
		updateStyle();
	}

	private void layoutChildren(float deltaY) {
		//		LogUtils.showV(TAG, "layoutChildren", TAG_ENABLE);
		int childCount = getChildCount();
		if (deltaY > 0) {
			//往下滑
			for (int i = 0; i < childCount; i++) {
				final View child = getChildAt(i);
				if (child.getTop() + deltaY > getMeasuredHeight()) {
					recycleViews(i, true);
					break;
				}
			}
			offsetTopAndBottom(deltaY);
			final View firstChild = getChildAt(0);
			fillUp(firstPosition - 1, firstChild.getTop());
		} else if (deltaY < 0) {
			//往上滑
			for (int i = childCount - 1; i >= 0; i--) {
				final View child = getChildAt(i);
				if (child.getTop() + deltaY + child.getMeasuredHeight() < 0) {
					recycleViews(i, false);
					break;
				}
			}
			offsetTopAndBottom(deltaY);
			final View lastChild = getChildAt(getChildCount() - 1);
			fillDown(firstPosition + getChildCount(), lastChild.getTop() + lastChild.getMeasuredHeight());
		}
		//更新样式
		updateStyle();
		//借位
		if (selectedPosition == 0 && getSelectedPosition() == datas.size() - 1) {
			if (onWheelCarryListener != null) {
				onWheelCarryListener.onWheelCarry(false);
			}
		}
		//进位
		if (selectedPosition == datas.size() - 1 && getSelectedPosition() == 0) {
			if (onWheelCarryListener != null) {
				onWheelCarryListener.onWheelCarry(true);
			}
		}
		selectedPosition = getSelectedPosition();
	}

	private void updateStyle() {
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final TextView child = (TextView) getChildAt(i);
			if (Math.abs(child.getTop() - showCount * rowHeight) < rowHeight * 0.5f) {
				child.setTextColor(selectedTextColor);
				child.setTextSize(selectedTextSize);
			} else if (Math.abs(child.getTop() - (showCount - 1) * rowHeight) < rowHeight * 0.5f
					|| Math.abs(child.getTop() - (showCount + 1) * rowHeight) < rowHeight * 0.5f) {
				child.setTextColor(normalTextColor);
				child.setTextSize(normalTextSize);
			} else {
				child.setTextColor(normalTextColor2);
				child.setTextSize(normalTextSize2);
			}
		}
	}

	private int selectedPosition = 0;

	private void fillDown(int pos, float nextTop) {
		int end = getMeasuredHeight();
		while (nextTop < end) {
			final TextView text = getCacheView(getCyclePosition(pos));
			//			text.setY(nextTop);
			text.layout(text.getLeft(), (int) nextTop, text.getRight(), (int) nextTop + text.getMeasuredHeight());
			addView(text);
			nextTop = (int) (text.getTop() + text.getMeasuredHeight());
			pos++;
		}
	}

	private void fillUp(int pos, float lastBottom) {
		int end = 0;
		while (lastBottom > end) {
			final TextView text = getCacheView(getCyclePosition(pos));
			//			text.setY(lastBottom - text.getMeasuredHeight());
			int top = (int) (lastBottom - text.getMeasuredHeight());
			text.layout(text.getLeft(), top, text.getRight(), top + text.getMeasuredHeight());
			addView(text, 0);
			lastBottom = (int) text.getTop();
			pos--;
			firstPosition--;
		}
	}

	private void offsetTopAndBottom(float deltaY) {
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View child = getChildAt(i);
			//			child.setY(child.getTop() + deltaY);
			int top = (int) (child.getTop() + deltaY);
			child.layout(child.getLeft(), top, child.getRight(), top + child.getMeasuredHeight());
		}
	}

	/**
	 * 回收子控件
	 **/
	private void recycleViews(int position, boolean isDown) {
		//		LogUtils.showV(TAG, "recycleViews", TAG_ENABLE);
		if (isDown) {
			int childCount = getChildCount();
			for (int i = childCount - 1; i >= position; i--) {
				View child = getChildAt(i);
				recycelView(child);
			}
		} else {
			for (int i = 0; i <= position; i++) {
				View child = getChildAt(0);
				recycelView(child);
				firstPosition++;
			}
		}
	}

	private void recycelView(View child) {
		TextView text = (TextView) child;
		removeView(text);
		cache.add(text);
	}

	/**
	 * 从缓存中获取子控件
	 **/
	private TextView getCacheView(int position) {
		//		LogUtils.showV(TAG, "getCacheView", TAG_ENABLE);
		TextView textView;
		if (cache.size() == 0) {
			//			textView = new TextView(root, getMeasuredWidth(), Math.round(rowHeight));
			textView = new TextView(getContext());
			//			FrameLayout.LayoutParams lp = new LayoutParams(getMeasuredWidth(), rowHeight);
			LogUtils.showW("phoneTest", "rowHeight:" + rowHeight, true);
			FrameLayout.LayoutParams lp = new LayoutParams(300, rowHeight);
			textView.setGravity(Gravity.CENTER);
			textView.setLayoutParams(lp);
		} else {
			textView = cache.remove(cache.size() - 1);
		}
		textView.setText(datas.get(getCyclePosition(position)));
		textView.setTextColor(normalTextColor2);
		textView.setTextSize(normalTextSize2);
		return textView;
	}

	private int getCyclePosition(int position) {
		while (position < 0) {
			position += datas.size();
		}
		return position % datas.size();
	}

	//	@Override
	//	protected void _drawChildren(GLBaseSettings arg0) {
	//		super._drawChildren(arg0);
	//		upLine.setY(rowHeight * showCount - upLine.getMeasuredHeight() / 2);
	//		upLine.draw();
	//		downLine.setY(rowHeight * (showCount + 1) - downLine.getMeasuredHeight() / 2);
	//		downLine.draw();
	//	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		int left = getPaddingLeft() + dividerPaddingLeft;
		int right = getMeasuredWidth() - getPaddingRight() - dividerPaddingRight;
		int upY = getMeasuredHeight() / 2 - rowHeight / 2 - dividerSize / 2;
		int downY = getMeasuredHeight() / 2 + rowHeight / 2 - dividerSize / 2;
		canvas.drawLine(left, upY, right, upY + dividerSize, dividerPaint);
		canvas.drawLine(left, downY, right, downY + dividerSize, dividerPaint);
	}

	/**
	 * 控件大小改变后，子控件大小位置、分割线都需要更新
	 */
	//	@Override
	//	public void setLayoutParams(int w, int h) {
	//		super.setLayoutParams(w, h);
	//		upLine.setLayoutParams(getMeasuredWidth(), lineSize);
	//		downLine.setLayoutParams(getMeasuredWidth(), lineSize);
	//		cache.clear();
	//		int childCount = getChildCount();
	//		rowHeight = getMeasuredHeight() / (showCount * 2 + 1);
	//		for (int i = 0; i < childCount; i++) {
	//			final View child = getChildAt(i);
	//			child.setLayoutParams(getMeasuredWidth(), rowHeight);
	//			child.setY(rowHeight * i);
	//		}
	//	}

	//	@Override
	//	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	//		super.onSizeChanged(w, h, oldw, oldh);
	//		cache.clear();
	//		int childCount = getChildCount();
	//		rowHeight = getMeasuredHeight() / (showCount * 2 + 1);
	//		LogUtils.showI("phoneTest", "rowHeight:" + rowHeight, true);
	//		for (int i = 0; i < childCount; i++) {
	//			final View child = getChildAt(i);
	//			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
	//			lp.width = LayoutParams.MATCH_PARENT;
	//			lp.height = rowHeight;
	//			child.layout(child.getLeft(), rowHeight * i, child.getRight(), rowHeight * (i + 1));
	//		}
	//
	//	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				lastMotionY = event.getY();
				velocitY = 0;
				mIsBeingDragged = false;
				break;
			case MotionEvent.ACTION_MOVE:
				float deltaY = event.getY() - lastMotionY;
				if (Math.abs(deltaY) > mTouchSlop || mIsBeingDragged) {
					mIsBeingDragged = true;
					lastMotionY = event.getY();
					layoutChildren(deltaY);
					velocityTracker.addMovement(event);
					velocityTracker.computeCurrentVelocity(100);
					velocitY = velocityTracker.getYVelocity();
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				if (mIsBeingDragged) {
					smoothScroll();
				}
				mIsBeingDragged = false;
				break;
		}
		return true;
	}

	private void smoothScroll() {
		LogUtils.showD(TAG, "smoothScroll", TAG_ENABLE);
		if (flingRunnable != null) {
			flingRunnable.stop();
			flingRunnable = null;
		}
		flingRunnable = new FlingRunnable(onSmoothScrollFinishedListener);
		flingRunnable.start(getDistance(), getDuration());
	}

	/**
	 * 计算动画时长
	 **/
	private int getDuration() {
		int duration = (int) (Math.abs(velocitY) / velocitYThreshold * 100 + SCROLL_DURATION);
		return duration;
	}

	/**
	 * 计算将要滑动的距离
	 **/
	private int getDistance() {
		int integerValue = (int) (velocitY / velocitYThreshold);
		//计算方法1
		//		GLObject firstChild = getChildAt(0);
		//		int firstDistance;
		//		if (Math.abs(firstChild.getY()) < rowHeight / 2) {
		//			firstDistance = (int) -firstChild.getY();
		//		} else {
		//			firstDistance = -(int) firstChild.getY() - rowHeight;
		//		}
		//		return firstDistance + integerValue * integerValue * rowHeight;
		//计算方法2
		View centerChild = getCenterChild();
		int firstDistance = (int) (rowHeight * showCount - centerChild.getTop());
		return firstDistance + integerValue * Math.abs(integerValue) / 2 * rowHeight;
	}

	/**
	 * 获得最中心的子控件
	 **/
	private TextView getCenterChild() {
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final TextView child = (TextView) getChildAt(i);
			//设置初始化样式
			//			child.setData(STYLE, normalTextSize);
			child.setTextColor(normalTextColor);
			child.setTextSize(normalTextSize);
			if (Math.abs(child.getTop() - showCount * rowHeight) <= rowHeight * 1.0f / 2) {
				return child;
			}
		}
		//		return getChildAt(childCount - 1);
		return null;
	}

	public void setSelectedPosition(int selectedPosition) {
		setSelectedPosition(selectedPosition, SCROLL_DURATION);
	}

	/**
	 * 设置选中的下标
	 **/
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
		//调整误差的距离
		View centerChild = getCenterChild();
		int firstDistance = (int) (rowHeight * showCount - centerChild.getTop());
		//整数倍距离
		int distance = rowHeight * deltaPosition + firstDistance;
		flingRunnable = new FlingRunnable(onSmoothScrollFinishedListener);
		flingRunnable.start(distance, duration);
	}

	/**
	 * 获得选中的下标
	 **/
	public int getSelectedPosition() {
		int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View child = getChildAt(i);
			if (Math.abs(child.getTop() - showCount * rowHeight) < rowHeight / 2) {
				return getCyclePosition(i + firstPosition);
			}
		}
		return INVALID_POSITION;
	}

	//	@Override
	//	public void onDestroy() {
	//		super.onDestroy();
	//		if (velocityTracker != null) {
	//			velocityTracker.clear();
	//			velocityTracker.recycle();
	//			velocityTracker = null;
	//		}
	//	}

	public void postOnAnimation(Runnable action) {
		post(action);
	}

	private class FlingRunnable implements Runnable {

		private final Scroller mScroller;
		private int mLastY;
		private OnSmoothScrollFinishedListener mListener;

		FlingRunnable(OnSmoothScrollFinishedListener listener) {
			mScroller = new Scroller(WheelView.this.getContext(), new DecelerateInterpolator());
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
			WheelView.this.removeCallbacks(this);
			mScroller.abortAnimation();
		}

		public void stop() {
			mScroller.forceFinished(true);
			WheelView.this.removeCallbacks(this);
		}

	}

	/**
	 * 动画执行完毕监听回调
	 **/
	private static interface OnSmoothScrollFinishedListener {
		void onSmoothScrollFinished();
	}

	public interface OnWheelListener {
		void onWheel(int position, String content);
	}

	public OnWheelListener getOnWheelListener() {
		return onWheelListener;
	}

	public void setOnWheelListener(OnWheelListener onWheelListener) {
		this.onWheelListener = onWheelListener;
	}

	public interface OnWheelCarryListener {
		void onWheelCarry(boolean isCarry);
	}

	public OnWheelCarryListener getOnWheelCarryListener() {
		return onWheelCarryListener;
	}

	public void setOnWheelCarryListener(OnWheelCarryListener onWheelCarryListener) {
		this.onWheelCarryListener = onWheelCarryListener;
	}

	private int sp2px(Context context, float spValue) {
		final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
		return (int) (spValue * fontScale + 0.5f);
	}

	private int dp2px(Context context, float dpValue) {
		final float densityScale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * densityScale + 0.5f);
	}

}
