package com.phone.wheelview.test;

import android.graphics.Color;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

import ucd.ui.framework.Settings.GLBaseSettings;
import ucd.ui.framework.core.GLBase;
import ucd.ui.framework.core.GLObject;
import ucd.ui.framework.core.Group;
import ucd.ui.framework.coreEx.Text;
import ucd.ui.util.resources.DOMLoader;
import ucd.ui.widget.cbb.util.LogUtils;

public class WheelView extends Group {

    private final String TAG = "WheelView";
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
    private GLObject upLine;
    private GLObject downLine;
    private int lineColor = Color.RED;
    private int lineSize = 2;
    private int firstPosition;
    //文本字体
    private String selectedTextStyle = DEFAULT_SELECTED_TEXTSTYLE;
    private String normalTextStyle = DEFAULT_NORMAL_TEXTSTYLE;
    private String normalTextStyle2 = DEFAULT_NORMAL_TEXTSTYLE_LEVEL2;
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

    public WheelView(GLBase arg0, int arg1, int arg2) {
        super(arg0, arg1, arg2);
        cache = new ArrayList<Text>();
        upLine = new GLObject(root, getMeasuredWidth(), lineSize);
        upLine.setPaintColor(lineColor);
        upLine.setParent(this);
        downLine = new GLObject(root, getMeasuredWidth(), lineSize);
        downLine.setPaintColor(lineColor);
        downLine.setParent(this);
        velocityTracker = VelocityTracker.obtain();
        mTouchSlop = ViewConfiguration.get(getRoot().getContext()).getScaledTouchSlop();
    }

    @Override
    protected void initAttrs(DOMLoader.Dom.XMLAttributeSet attrs, String url) {
        super.initAttrs(attrs, url);
    }

    public List<String> getDatas() {
        return datas;
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
        rowHeight = getMeasuredHeight() / (showCount * 2 + 1);
        delAll();
        cache.clear();
        initLayout();
    }

    private void initLayout() {
        firstPosition = selectedPosition - showCount;
        for (int i = 0; i < showCount * 2 + 1; i++) {
            Text child = getCacheView(i + firstPosition);
            child.setY(i * rowHeight);
            add(child);
        }
        //更新样式
        updateStyle();
    }

    private void layoutChildren(float deltaY) {
        //		LogUtils.showV(TAG, "layoutChildren", TAG_ENABLE);
        int childCount = getChildrenCount();
        if (deltaY > 0) {
            //往下滑
            for (int i = 0; i < childCount; i++) {
                final GLObject child = getChildAt(i);
                if (child.getY() + deltaY > getMeasuredHeight()) {
                    recycleViews(i, true);
                    break;
                }
            }
            offsetTopAndBottom(deltaY);
            final GLObject firstChild = getChildAt(0);
            fillUp(firstPosition - 1, firstChild.getY());
        } else if (deltaY < 0) {
            //往上滑
            for (int i = childCount - 1; i >= 0; i--) {
                final GLObject child = getChildAt(i);
                if (child.getY() + deltaY + child.getMeasuredHeight() < 0) {
                    recycleViews(i, false);
                    break;
                }
            }
            offsetTopAndBottom(deltaY);
            final GLObject lastChild = getChildAt(getChildrenCount() - 1);
            fillDown(firstPosition + getChildrenCount(), lastChild.getY() + lastChild.getMeasuredHeight());
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

    private int selectedPosition = 0;

    private void fillDown(int pos, float nextTop) {
        int end = getMeasuredHeight();
        while (nextTop < end) {
            final Text text = getCacheView(getCyclePosition(pos));
            text.setY(nextTop);
            add(text);
            nextTop = (int) (text.getY() + text.getMeasuredHeight());
            pos++;
        }
    }

    private void fillUp(int pos, float lastBottom) {
        int end = 0;
        while (lastBottom > end) {
            final Text text = getCacheView(getCyclePosition(pos));
            text.setY(lastBottom - text.getMeasuredHeight());
            add(text, 0);
            lastBottom = (int) text.getY();
            pos--;
            firstPosition--;
        }
    }

    private void offsetTopAndBottom(float deltaY) {
        int childCount = getChildrenCount();
        for (int i = 0; i < childCount; i++) {
            final GLObject child = getChildAt(i);
            child.setY(child.getY() + deltaY);
        }
    }

    /**
     * 回收子控件
     **/
    private void recycleViews(int position, boolean isDown) {
        //		LogUtils.showV(TAG, "recycleViews", TAG_ENABLE);
        if (isDown) {
            int childCount = getChildrenCount();
            for (int i = childCount - 1; i >= position; i--) {
                GLObject child = getChildAt(i);
                recycelView(child);
            }
        } else {
            for (int i = 0; i <= position; i++) {
                GLObject child = getChildAt(0);
                recycelView(child);
                firstPosition++;
            }
        }
    }

    private void recycelView(GLObject child) {
        Text text = (Text) child;
        remove(text);
        cache.add(text);
    }

    /**
     * 从缓存中获取子控件
     **/
    private Text getCacheView(int position) {
        //		LogUtils.showV(TAG, "getCacheView", TAG_ENABLE);
        Text text;
        if (cache.size() == 0) {
            text = new Text(root, getMeasuredWidth(), Math.round(rowHeight));
        } else {
            text = cache.remove(cache.size() - 1);
        }
        text.setText(datas.get(getCyclePosition(position)));
        text.setStyle(normalTextStyle2);
        return text;
    }

    private int getCyclePosition(int position) {
        while (position < 0) {
            position += datas.size();
        }
        return position % datas.size();
    }

    @Override
    protected void _drawChildren(GLBaseSettings arg0) {
        super._drawChildren(arg0);
        upLine.setY(rowHeight * showCount - upLine.getMeasuredHeight() / 2);
        upLine.draw();
        downLine.setY(rowHeight * (showCount + 1) - downLine.getMeasuredHeight() / 2);
        downLine.draw();
    }

    /**
     * 控件大小改变后，子控件大小位置、分割线都需要更新
     */
    @Override
    public void setLayoutParams(int w, int h) {
        super.setLayoutParams(w, h);
        upLine.setLayoutParams(getMeasuredWidth(), lineSize);
        downLine.setLayoutParams(getMeasuredWidth(), lineSize);
        cache.clear();
        int childCount = getChildrenCount();
        rowHeight = getMeasuredHeight() / (showCount * 2 + 1);
        for (int i = 0; i < childCount; i++) {
            final GLObject child = getChildAt(i);
            child.setLayoutParams(getMeasuredWidth(), rowHeight);
            child.setY(rowHeight * i);
        }
    }

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
        GLObject centerChild = getCenterChild();
        int firstDistance = (int) (rowHeight * showCount - centerChild.getY());
        return firstDistance + integerValue * Math.abs(integerValue) / 2 * rowHeight;
    }

    /**
     * 获得最中心的子控件
     **/
    private GLObject getCenterChild() {
        int childCount = getChildrenCount();
        for (int i = 0; i < childCount; i++) {
            final GLObject child = getChildAt(i);
            //设置初始化样式
            child.setData(STYLE, normalTextStyle);
            if (Math.abs(child.getY() - showCount * rowHeight) <= rowHeight * 1.0f / 2) {
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
        GLObject centerChild = getCenterChild();
        int firstDistance = (int) (rowHeight * showCount - centerChild.getY());
        //整数倍距离
        int distance = rowHeight * deltaPosition + firstDistance;
        flingRunnable = new FlingRunnable(onSmoothScrollFinishedListener);
        flingRunnable.start(distance, duration);
    }

    /**
     * 获得选中的下标
     **/
    public int getSelectedPosition() {
        int childCount = getChildrenCount();
        for (int i = 0; i < childCount; i++) {
            final GLObject child = getChildAt(i);
            if (Math.abs(child.getY() - showCount * rowHeight) < rowHeight / 2) {
                return getCyclePosition(i + firstPosition);
            }
        }
        return INVALID_POSITION;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (velocityTracker != null) {
            velocityTracker.clear();
            velocityTracker.recycle();
            velocityTracker = null;
        }
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
