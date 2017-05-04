package com.phone.wheelview.test;

import java.util.Calendar;

import ucd.ui.framework.Settings.GLBaseSettings;
import ucd.ui.framework.core.GLBase;
import ucd.ui.framework.core.GLObject;
import ucd.ui.framework.core.Group;
import ucd.ui.util.DensityUtil;

public abstract class PickerView extends Group {

    private final String TAG = "WheelView";
    private final boolean TAG_ENABLE = true;
    //内距
    protected int paddingH = DensityUtil.convertByRatio(75);
    protected int paddingV=DensityUtil.convertByRatio(10);
    //分割线颜色
    protected int lineColor = 0x1A000000;
    //分割线大小
    protected int lineSize = 2;
    //选中的文本字体
    protected String selectedTextFont = "wheel_selected";
    //为选中的文本字体
    protected String normalTextFont = "wheel_normal";
    //
    protected int showCount = 2;
    //列宽
    protected int columnWidth;
    //监听器
    protected OnPickerListener onPickerListener;
    //标志开始绘制
    protected boolean isStartInflater;
    //当前选中的时间
    protected SelectedTime selectedTime;

    public PickerView(GLBase root, int w, int h) {
        super(root, w, h);
        selectedTime = new SelectedTime();
        Calendar calendar = Calendar.getInstance();
        selectedTime.setYear(calendar.get(Calendar.YEAR));
        selectedTime.setMonth(calendar.get(Calendar.MONTH) + 1);
        selectedTime.setDay(calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    protected void onInit(GLBaseSettings info) {
        super.onInit(info);
        initView();
        initEvent();
        initData();
        isStartInflater = true;
    }

    protected abstract void initView();

    protected abstract void initEvent();

    protected abstract void initData();

    public int getLineColor() {
        return lineColor;
    }

    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
    }

    public int getLineSize() {
        return lineSize;
    }

    public void setLineSize(int lineSize) {
        this.lineSize = lineSize;
    }

    public String getSelectedTextFont() {
        return selectedTextFont;
    }

    public void setSelectedTextFont(String selectedTextFont) {
        this.selectedTextFont = selectedTextFont;
    }

    public String getNormalTextFont() {
        return normalTextFont;
    }

    public void setNormalTextFont(String normalTextFont) {
        this.normalTextFont = normalTextFont;
    }

    public int getShowCount() {
        return showCount;
    }

    public void setShowCount(int showCount) {
        this.showCount = showCount;
    }

    protected void setWeelViewStyle(GLObject glObject) {
        if (glObject instanceof APWheelView) {
            APWheelView apWheelView = (APWheelView) glObject;
            apWheelView.setSelectedTextStyle(selectedTextFont);
            apWheelView.setNormalTextStyle(normalTextFont);
            apWheelView.setShowCount(showCount);
            apWheelView.setLineSize(lineSize);
            apWheelView.setLineColor(lineColor);
        } else {
            WheelView wheelView = (WheelView) glObject;
            wheelView.setSelectedTextStyle(selectedTextFont);
            wheelView.setNormalTextStyle(normalTextFont);
            wheelView.setShowCount(showCount);
            wheelView.setLineSize(lineSize);
            wheelView.setLineColor(lineColor);
        }

    }

    public interface OnPickerListener {
        public void onPicker(SelectedTime selectedTime);
    }

    public OnPickerListener getOnPickerListener() {
        return onPickerListener;
    }

    public void setOnPickerListener(OnPickerListener onPickerListener) {
        this.onPickerListener = onPickerListener;
    }

}
