package com.phone.wheelview.test.pick;

import android.text.TextUtils;
import android.text.format.DateFormat;

import com.huawei.ucd.lego.wheel.APWheelView;
import com.huawei.ucd.lego.wheel.APWheelView.OnAPWheelListener;
import com.huawei.ucd.lego.wheel.DateUtils;
import com.huawei.ucd.lego.wheel.Lunar;
import com.huawei.ucd.lego.wheel.PickerView;
import com.huawei.ucd.lego.wheel.WheelView;
import com.huawei.ucd.lego.wheel.WheelView.OnWheelCarryListener;
import com.huawei.ucd.lego.wheel.WheelView.OnWheelListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ucd.ui.framework.Settings.GLBaseSettings;
import ucd.ui.framework.core.GLBase;
import ucd.ui.framework.coreEx.Text;
import ucd.ui.util.DensityUtil;
import ucd.ui.util.resources.DOMLoader.Dom.XMLAttributeSet;
import ucd.ui.widget.cbb.util.LogUtils;
import ucd.ui.widget.cbb.util.ToastUtils;
import ucd.ui.widget.system.Switch;
import ucd.ui.widget.system.Switch.OnCheckedChangeListener;

import static com.phone.wheelview.R.styleable.WheelView;

/**
 * @author fengshaojun
 *         <p>
 *         不支持1970年1月1日之前的时间
 *         </P>
 */
public class DateAndTimePicker extends PickerView {

    private final String TAG = "DateAndTimePicker";
    private final boolean TAG_ENABLE = true;
    private static final String SHOW_LUNAR = "showLunar";
    private static final String SET24OR12 = "set24or12";
    private static final String IS24HOUR = "is24Hour";
    private static final String TIME_SPAN = "timeSpan";
    public static final int lastDaySum = 30;
    public static final int allDaySum = 90;
    //当前模式
    private Mode mode = Mode.HOUR;
    //天
    private WheelView dayView;
    private List<String> daysForSolar;//公历
    private List<String> dyasForLunar;//农历
    private int selectedDayPosition = lastDaySum;
    private boolean isLunar;//农历、公历
    //上午、下午
    private APWheelView apView;
    private List<String> aps;
    private int selectedAPPosition;
    //时
    private WheelView hourView;
    private List<String> hoursFor12;
    private List<String> hoursFor24;
    private int selectedHourPosition;
    //分
    private WheelView minuteView;
    private List<String> minutesSpan1;
    private List<String> minutesSpan5;
    private int selectedMinutePosition;
    private String timeSpan = "1";//分钟间隔
    //农历
    private final int marginWheel = DensityUtil.convertByRatio(10);
    private final int lunarHeight = DensityUtil.convertByRatio(54);
    private final int lunarWidth = DensityUtil.convertByRatio(108);
    private Switch lunarSwitch;
    private Text lunarText;
    //是否允许外界设置小时制
    private boolean set24or12;
    //是否是12小时制
    private boolean is24Hours;
    //是否显示切换农历的界面
    private boolean showLunar = true;

    /**
     * 两种显示模式：12小时制、24小时制
     **/
    public enum Mode {
        HOUR, HOUR_OF_DAY
    }

    public DateAndTimePicker(GLBase root, int w, int h) {
        super(root, w, h);
    }

    @Override
    protected void initAttrs(XMLAttributeSet attrs, String url) {
        super.initAttrs(attrs, url);
        //是否显示农历切换
        String showLunarStr = attrs.removeString(SHOW_LUNAR);
        if (!TextUtils.isEmpty(showLunarStr)) {
            setData(SHOW_LUNAR, showLunarStr);
            showLunar = Boolean.parseBoolean(showLunarStr);
        } else {
            showLunar = false;
        }
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
            //获取系统设置的事件进制（12小时制、24小时制）
            boolean is24Time = DateFormat.is24HourFormat(root.getContext());
            LogUtils.showD(TAG, "is24Time:" + is24Time, TAG_ENABLE);
            mode = is24Time ? Mode.HOUR_OF_DAY : Mode.HOUR;
        }
        //默认子控件宽度
        if (mode == mode.HOUR_OF_DAY) {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 4;
        } else {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 5;
        }
        int wheelHeight;
        if (showLunar) {
            wheelHeight = getMeasuredHeight() - lunarHeight - marginWheel - paddingV * 2;
        } else {
            wheelHeight = getMeasuredHeight() - paddingV * 2;
        }

        //天
        dayView = new WheelView(root, columnWidth * 2, wheelHeight);
        add(dayView);
        //上午下午
        apView = new APWheelView(root, columnWidth, wheelHeight);
        add(apView);
        //时
        hourView = new WheelView(root, columnWidth, wheelHeight);
        add(hourView);
        //分
        minuteView = new WheelView(root, columnWidth, wheelHeight);
        add(minuteView);
        //农历
        lunarText = new Text(root, lunarWidth, lunarHeight);
        lunarText.setStyle("wheel_picker_lunar_text");
        lunarText.setText("农历");
        add(lunarText);
        lunarSwitch = new Switch(root, lunarWidth, lunarHeight);
        lunarSwitch.setChecked(false, false);
        add(lunarSwitch);
    }

    @Override
    protected void initEvent() {
        //设置监听器
        dayView.setOnWheelListener(new OnWheelListener() {

            @Override
            public void onWheel(int position, String content) {
                selectedDayPosition = position;
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - lastDaySum + position);
                selectedTime.setYear(calendar.get(Calendar.YEAR));
                selectedTime.setMonth(calendar.get(Calendar.MONTH) + 1);
                selectedTime.setDay(calendar.get(Calendar.DAY_OF_MONTH));
                if (onPickerListener != null) {
                    onPickerListener.onPicker(selectedTime);
                }
            }
        });
        if (apView != null) {
            apView.setOnAPWheelListener(new OnAPWheelListener() {

                @Override
                public void onAPWheel(int position, String content) {
                    selectedAPPosition = position;
                    selectedTime.setAm(position == 0 ? true : false);
                    if (onPickerListener != null) {
                        onPickerListener.onPicker(selectedTime);
                    }
                }
            });
        }
        hourView.setOnWheelListener(new OnWheelListener() {

            @Override
            public void onWheel(int position, String content) {
                selectedHourPosition = position;
                selectedTime.setHour(position);
                if (onPickerListener != null) {
                    onPickerListener.onPicker(selectedTime);
                }
            }
        });
        hourView.setOnWheelCarryListener(new OnWheelCarryListener() {

            @Override
            public void onWheelCarry(boolean isCarry) {
                if (apView != null) {
                    ToastUtils.show(root, isCarry ? "进位" : "借位");
                    apView.setSelectedPosition(isCarry ? (apView.getSelectedPosition() + 1) % apView.getDatas().size()
                            : (apView.getSelectedPosition() + apView.getDatas().size() - 1) % apView.getDatas().size());
                }
            }
        });

        minuteView.setOnWheelListener(new OnWheelListener() {

            @Override
            public void onWheel(int position, String content) {
                selectedMinutePosition = position;
                selectedTime.setMinute(timeSpan.equals("5") ? position * 5 : position);
                if (onPickerListener != null) {
                    onPickerListener.onPicker(selectedTime);
                }
            }
        });
        lunarSwitch.setOnCheckChangedListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(boolean isChecked) {
                setLunar(isChecked);
            }
        });
    }

    @Override
    protected void initData() {
        //天
        daysForSolar = new ArrayList<String>();
        dyasForLunar = new ArrayList<String>();
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        LogUtils.showD(TAG, "dayOfYear:" + dayOfYear, TAG_ENABLE);
        //数据源为当天的前30天到后面的59天
        for (int i = dayOfYear - lastDaySum; i < dayOfYear + allDaySum - lastDaySum; i++) {
            final Calendar current = Calendar.getInstance();
            current.set(Calendar.DAY_OF_YEAR, i);
            //公历
            int month = current.get(Calendar.MONTH);
            int day = current.get(Calendar.DAY_OF_MONTH);
            int week = current.get(Calendar.DAY_OF_WEEK);
            String monthStr = (month + 1) + "月";
            String dayStr = day + "日";
            String weekStr = "";
            switch (week) {
                case Calendar.SUNDAY:
                    weekStr = "周日";
                    break;
                case Calendar.MONDAY:
                    weekStr = "周一";
                    break;
                case Calendar.TUESDAY:
                    weekStr = "周二";
                    break;
                case Calendar.WEDNESDAY:
                    weekStr = "周三";
                    break;
                case Calendar.THURSDAY:
                    weekStr = "周四";
                    break;
                case Calendar.FRIDAY:
                    weekStr = "周五";
                    break;
                case Calendar.SATURDAY:
                    weekStr = "周六";
                    break;
            }
            if (i == dayOfYear) {
                daysForSolar.add("今天" + weekStr);
            } else {
                daysForSolar.add(monthStr + dayStr + weekStr);
            }
            //农历
            final Lunar lunar = new Lunar(current);
            String lunarDay = lunar.toMonthDayString() + weekStr;
            dyasForLunar.add(lunarDay);
        }
        //上午、下午
        aps = new ArrayList<String>();
        aps.add("上午");
        aps.add("下午");
        //时
        hoursFor12 = new ArrayList<String>();
        hoursFor24 = new ArrayList<String>();
        for (int i = 0; i < 12; i++) {
            hoursFor12.add(DateUtils.fillZero(i));
        }
        for (int i = 0; i < 24; i++) {
            hoursFor24.add(DateUtils.fillZero(i));
        }
        //分
        minutesSpan1 = new ArrayList<String>();
        minutesSpan5 = new ArrayList<String>();
        for (int i = 0; i < 60; i++) {
            minutesSpan1.add(DateUtils.fillZero(i));
        }
        for (int i = 0; i < 12; i++) {
            minutesSpan5.add(DateUtils.fillZero(i * 5));
        }
        layoutChildren();
    }

    @Override
    protected void onInit(GLBaseSettings info) {
        super.onInit(info);
        if (isLunar) {
            dayView.setDatas(dyasForLunar, lastDaySum);
        } else {
            dayView.setDatas(daysForSolar, lastDaySum);
        }
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
        setWeelViewStyle(dayView);
        setWeelViewStyle(apView);
        setWeelViewStyle(hourView);
        setWeelViewStyle(minuteView);
        if (mode == Mode.HOUR_OF_DAY) {
            dayView.setX(paddingH);
            dayView.setY(paddingV);
            apView.setAlpha(0f);
            hourView.setX(paddingH + columnWidth * 2);
            hourView.setY(paddingV);
            minuteView.setX(paddingH + columnWidth * 3);
            minuteView.setY(paddingV);
        } else if (mode == Mode.HOUR) {
            dayView.setX(paddingH);
            dayView.setY(paddingV);
            apView.setAlpha(1f);
            apView.setX(paddingH + columnWidth * 2);
            apView.setY(paddingV);
            hourView.setX(paddingH + columnWidth * 3);
            hourView.setY(paddingV);
            minuteView.setX(paddingH + columnWidth * 4);
            minuteView.setY(paddingV);
        }
        if (showLunar) {
            lunarText.setAlpha(1f);
            lunarText.setX(paddingH);
            lunarText.setY(getMeasuredHeight() - paddingV - lunarHeight);
            lunarSwitch.setAlpha(1f);
            lunarSwitch.setX(getMeasuredWidth() - paddingH - lunarSwitch.getMeasuredWidth());
            lunarSwitch.setY(getMeasuredHeight() - paddingV - lunarHeight);
        } else {
            lunarText.setAlpha(0f);
            lunarSwitch.setAlpha(0f);
        }
    }

    /**
     * 重置子控件的大小
     **/
    private void resizeChildren() {
        int wheelHeight;
        if (showLunar) {
            wheelHeight = getMeasuredHeight() - lunarHeight - marginWheel - paddingV * 2;
        } else {
            wheelHeight = getMeasuredHeight() - paddingV * 2;
        }

        if (mode == mode.HOUR_OF_DAY) {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 4;
        } else {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 5;
        }
        dayView.setLayoutParams(columnWidth * 2, wheelHeight);
        apView.setLayoutParams(columnWidth, wheelHeight);
        hourView.setLayoutParams(columnWidth, wheelHeight);
        minuteView.setLayoutParams(columnWidth, wheelHeight);
    }

    public Mode getMode() {
        return mode;
    }

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

    public int getSelectedDayPosition() {
        return selectedDayPosition;
    }

    public int getSelectedAPPosition() {
        return selectedAPPosition;
    }

    public int getSelectedHourPosition() {
        return selectedHourPosition;
    }

    public int getSelectedMinutePosition() {
        return selectedMinutePosition;
    }

    public List<String> getDays() {
        return isLunar ? daysForSolar : dyasForLunar;
    }

    public List<String> getAps() {
        return aps;
    }

    public List<String> getHours() {
        if (mode == Mode.HOUR) {
            return hoursFor12;
        } else {
            return hoursFor24;
        }
    }

    public List<String> getMinutes() {
        if (timeSpan.equals("5")) {
            return minutesSpan5;
        } else {
            return minutesSpan1;
        }
    }

    public boolean isLunar() {
        return isLunar;
    }

    public void setLunar(boolean isLunar) {
        this.isLunar = isLunar;
        if (isLunar) {
            dayView.setDatas(dyasForLunar, selectedDayPosition);
        } else {
            dayView.setDatas(daysForSolar, selectedDayPosition);
        }
    }

    /**
     * 当选中的天/日发生改变时回调
     **/
    public interface OnPickerDayListener {
        public void onPickerDay(int position, String content);
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

    public boolean isShowLunar() {
        return showLunar;
    }

    /**
     * 设置是否显示农历/公历切换
     *
     * @param showLunar:true表示显示，false表示不显示
     */
    public void setShowLunar(boolean showLunar) {
        this.showLunar = showLunar;
        if (isStartInflater) {
            layoutChildren();
        } else {
            LogUtils.showE(TAG, "初始化没完成");
        }
    }
}
