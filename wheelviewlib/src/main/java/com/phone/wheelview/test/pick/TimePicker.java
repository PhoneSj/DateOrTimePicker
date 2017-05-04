package com.phone.wheelview.test.pick;

import android.text.TextUtils;
import android.text.format.DateFormat;

import com.huawei.ucd.lego.wheel.APWheelView;
import com.huawei.ucd.lego.wheel.APWheelView.OnAPWheelListener;
import com.huawei.ucd.lego.wheel.DateUtils;
import com.huawei.ucd.lego.wheel.PickerView;
import com.huawei.ucd.lego.wheel.WheelView;
import com.huawei.ucd.lego.wheel.WheelView.OnWheelListener;

import java.util.ArrayList;
import java.util.List;

import ucd.ui.framework.Settings.GLBaseSettings;
import ucd.ui.framework.core.GLBase;
import ucd.ui.util.resources.DOMLoader.Dom.XMLAttributeSet;
import ucd.ui.widget.cbb.util.LogUtils;

public class TimePicker extends PickerView {

    private final String TAG = "TimePickerView";
    private final boolean TAG_ENABLE = true;
    private static final String SET24OR12 = "set24or12";
    private static final String IS24HOUR = "is24Hour";
    private static final String TIME_SPAN = "timeSpan";
    //当前模式
    private Mode mode = Mode.HOUR;
    //上午、下午
    private List<String> aps;
    private int selectedAPPosition;
    //当前时
    private List<String> hoursFor12;
    private List<String> hoursFor24;
    private int selectedHourPosition;
    //当前分
    private List<String> minutesSpan1;
    private List<String> minutesSpan5;
    private int selectedMinutePosition;
    //子控件
    private APWheelView apView;
    private WheelView hourView;
    private WheelView minuteView;
    //分钟间隔
    private String timeSpan = "1";
    //是否允许外界设置小时制
    private boolean set24or12;
    //是否是12小时制
    private boolean is24Hours;

    /**
     * 两种显示模式：12小时制、24小时制
     **/
    public enum Mode {
        HOUR, HOUR_OF_DAY
    }

    public TimePicker(GLBase root, int w, int h) {
        super(root, w, h);
    }

    @Override
    protected void initAttrs(XMLAttributeSet attrs, String url) {
        super.initAttrs(attrs, url);
        //是否允许外界设置小时制
        String set24or12Str = attrs.removeString(SET24OR12);
        if (!TextUtils.isEmpty(set24or12Str)) {
            setData(SET24OR12, set24or12Str);
            set24or12 = Boolean.parseBoolean(set24or12Str);
        } else {
            set24or12 = false;
        }
        //是否允许外界设置小时制
        String is24HoursStr = attrs.removeString(IS24HOUR);
        if (!TextUtils.isEmpty(is24HoursStr)) {
            setData(IS24HOUR, is24HoursStr);
            is24Hours = Boolean.parseBoolean(is24HoursStr);
        } else {
            is24Hours = false;
        }
        //分钟间隔
        String timeSpanStr = attrs.removeString(TIME_SPAN);
        if (!TextUtils.isEmpty(timeSpanStr)) {
            setData(TIME_SPAN, timeSpanStr);
            timeSpan = timeSpanStr;
        } else {
            timeSpan = "1";
        }
    }

    @Override
    protected void initView() {
        //初始化当前小时进制
        if (set24or12) {
            mode = is24Hours ? Mode.HOUR_OF_DAY : Mode.HOUR;
        } else {
            boolean is24Time = DateFormat.is24HourFormat(root.getContext());
            LogUtils.showD(TAG, "is24Time:" + is24Time, TAG_ENABLE);
            mode = is24Time ? Mode.HOUR_OF_DAY : Mode.HOUR;
        }
        //默认子控件宽度
        if (mode == mode.HOUR_OF_DAY) {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 2;
        } else {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 3;
        }
        //上午、下午
        apView = new APWheelView(root, columnWidth, getMeasuredHeight() - paddingV * 2);
        add(apView);
        //时
        hourView = new WheelView(root, columnWidth, getMeasuredHeight() - paddingV * 2);
        add(hourView);
        //分
        minuteView = new WheelView(root, columnWidth, getMeasuredHeight() - paddingV * 2);
        add(minuteView);
    }

    @Override
    protected void initEvent() {
        apView.setOnAPWheelListener(new OnAPWheelListener() {

            @Override
            public void onAPWheel(int position, String content) {
                selectedAPPosition = position;
                selectedTime.setAm(position == 0 ? true : false);
                performPick();
            }
        });
        hourView.setOnWheelListener(new OnWheelListener() {

            @Override
            public void onWheel(int position, String content) {
                selectedHourPosition = position;
                selectedTime.setHour(position);
                performPick();
            }
        });
        minuteView.setOnWheelListener(new OnWheelListener() {

            @Override
            public void onWheel(int position, String content) {
                selectedMinutePosition = position;
                selectedTime.setMinute(timeSpan.equals("5") ? position * 5 : position);
                performPick();
            }
        });
    }

    protected void performPick() {
        if (onPickerListener != null) {
            onPickerListener.onPicker(selectedTime);
        }
    }

    @Override
    protected void initData() {
        //ap
        aps = new ArrayList<String>();
        aps.add("AM");
        aps.add("PM");
        //hour
        hoursFor12 = new ArrayList<String>();
        hoursFor24 = new ArrayList<String>();
        for (int i = 0; i < 12; i++) {
            hoursFor12.add(DateUtils.fillZero(i));
        }
        for (int i = 0; i < 24; i++) {
            hoursFor24.add(DateUtils.fillZero(i));
        }
        //minute
        minutesSpan1 = new ArrayList<String>();
        minutesSpan5 = new ArrayList<String>();
        for (int i = 0; i < 60; i++) {
            minutesSpan1.add(DateUtils.fillZero(i));
        }
        for (int i = 0; i < 12; i++) {
            minutesSpan5.add(DateUtils.fillZero(i * 5));
        }
    }

    @Override
    protected void onInit(GLBaseSettings info) {
        super.onInit(info);
        apView.setDatas(aps);
        if (mode == Mode.HOUR) {
            hourView.setDatas(hoursFor12);
        } else {
            hourView.setDatas(hoursFor24);
        }
        if (timeSpan.equals("5")) {
            minuteView.setDatas(minutesSpan5);
        } else {
            minuteView.setDatas(minutesSpan1);
        }
        //布局
        layoutChildren();
    }

    /**
     * 对子控件布局
     **/
    protected void layoutChildren() {
        resizeChildren();
        setWeelViewStyle(apView);
        setWeelViewStyle(hourView);
        setWeelViewStyle(minuteView);
        if (mode == Mode.HOUR_OF_DAY) {
            //隐藏上午、下午控件
            hourView.setX(paddingH);
            hourView.setY(paddingV);
            minuteView.setX(paddingH + columnWidth);
            minuteView.setY(paddingV);
            apView.setAlpha(0f);
        } else if (mode == Mode.HOUR) {
            hourView.setX(paddingH);
            hourView.setY(paddingV);
            minuteView.setX(paddingH + columnWidth);
            minuteView.setY(paddingV);
            apView.setAlpha(1f);
            apView.setX(paddingH + columnWidth * 2);
            apView.setY(paddingV);
        }
    }

    /**
     * 重置子控件的大小
     **/
    private void resizeChildren() {
        if (mode == mode.HOUR_OF_DAY) {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 2;
        } else {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 3;
        }
        apView.setLayoutParams(columnWidth, apView.getMeasuredHeight());
        hourView.setLayoutParams(columnWidth, hourView.getMeasuredHeight());
        minuteView.setLayoutParams(columnWidth, minuteView.getMeasuredHeight());
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * 设置模式
     *
     * @param mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
        if (isStartInflater) {
            layoutChildren();
            if (mode == Mode.HOUR) {
                hourView.setDatas(hoursFor12);
            } else {
                hourView.setDatas(hoursFor24);
            }
        } else {
            LogUtils.showE(TAG, "初始化没完成");
        }
    }

    /**
     * 选中的ap下标
     *
     * @return
     */
    public int getSelectedAPPosition() {
        return selectedAPPosition;
    }

    /**
     * 选中的hour下标
     *
     * @return
     */
    public int getSelectedHourPosition() {
        return selectedHourPosition;
    }

    /**
     * 选中的minute下标
     *
     * @return
     */
    public int getSelectedMinutePosition() {
        return selectedMinutePosition;
    }

    /**
     * 获取ap数据源
     *
     * @return
     */
    public List<String> getAps() {
        return aps;
    }

    /**
     * 获取hour数据源
     *
     * @return
     */
    public List<String> getHours() {
        if (mode == Mode.HOUR) {
            return hoursFor12;
        } else {
            return hoursFor24;
        }
    }

    /**
     * 获取minute数据源
     *
     * @return
     */
    public List<String> getMinutes() {
        if (timeSpan.equals("5")) {
            return minutesSpan5;
        } else {
            return minutesSpan1;
        }
    }

    public String getTimeSpan() {
        return timeSpan;
    }

    /**
     * 设置时间分钟的间隔
     *
     * @param timeSpan
     */
    public void setTimeSpan(final String timeSpan) {
        this.timeSpan = timeSpan;
        if (null == minuteView) {
            return;
        }
        if (timeSpan.equals("5")) {
            minuteView.setDatas(minutesSpan5);
        } else {
            minuteView.setDatas(minutesSpan1);
        }
    }

    public boolean isIs24Hours() {
        return is24Hours;
    }

    /**
     * 设置时间的小时进制
     *
     * @param is24Hours
     */
    public void setIs24Hours(boolean is24Hours) {
        if (set24or12) {
            this.is24Hours = is24Hours;
            mode = is24Hours ? Mode.HOUR_OF_DAY : Mode.HOUR;
            setMode(mode);
        }
    }

    public boolean isSet24or12() {
        return set24or12;
    }

    /**
     * 是否允许使用外接设置的小时进制
     *
     * @param set24or12:true表示允许使用，false表示不允许
     */
    public void setSet24or12(boolean set24or12) {
        this.set24or12 = set24or12;
    }
}
