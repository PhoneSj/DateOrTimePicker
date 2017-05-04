package com.phone.wheelview.test.pick;

import android.text.TextUtils;

import com.huawei.ucd.lego.wheel.DateUtils;
import com.huawei.ucd.lego.wheel.LunarCalender;
import com.huawei.ucd.lego.wheel.PickerView;
import com.huawei.ucd.lego.wheel.WheelView;
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
import ucd.ui.widget.system.Switch;
import ucd.ui.widget.system.Switch.OnCheckedChangeListener;

public class DatePicker extends PickerView {

    private final String TAG = "DataPickerView";
    private final boolean TAG_ENABLE = true;
    private static final String SHOW_LUNAR = "showLunar";
    //年的范围为上下50年
    private static final int YEAR_DELTA = 30;
    //当前模式
    private Mode mode = Mode.YEAR_MONTH_DAY;
    //年
    private WheelView yearView;
    private List<String> yearsForSolar;
    private List<String> yearsForLunar;
    private int selectedYearIndex = 0;
    //月
    private WheelView monthView;
    private List<String> monthsForSolar;
    private List<String> monthsForLunar;
    private int selectedMonthIndex = 0;
    //日
    private WheelView dayView;
    private List<String> daysForSolar;
    private List<String> daysForLunar;
    private int selectedDayIndex = 0;
    //农历
    private final int marginWheel = DensityUtil.convertByRatio(10);
    private final int lunarHeight = DensityUtil.convertByRatio(54);
    private final int lunarWidth = DensityUtil.convertByRatio(108);
    private Switch lunarSwitch;
    private Text lunarText;
    //是否可以提供设置农历的开关
    private boolean showLunar = true;
    private boolean isLunar;

    public enum Mode {
        YEAR_MONTH_DAY, YEAR_MONTH, MONTH_DAY
    }

    public DatePicker(GLBase root, int w, int h) {
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
    }

    @Override
    protected void initView() {
        //默认子控件宽度
        if (mode == Mode.MONTH_DAY || mode == Mode.YEAR_MONTH) {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 2;
        } else if (mode == Mode.YEAR_MONTH_DAY) {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 3;
        }

        int wheelHeight;
        if (showLunar) {
            wheelHeight = getMeasuredHeight() - lunarHeight - marginWheel - paddingV * 2;
        } else {
            wheelHeight = getMeasuredHeight() - paddingV * 2;
        }
        //年
        yearView = new WheelView(root, columnWidth, wheelHeight);
        add(yearView);
        //月
        monthView = new WheelView(root, columnWidth, wheelHeight);
        add(monthView);
        //日
        dayView = new WheelView(root, columnWidth, wheelHeight);
        add(dayView);
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
        //选择监听器
        yearView.setOnWheelListener(new OnWheelListener() {

            @Override
            public void onWheel(int position, String content) {
                selectedYearIndex = position;
                //根据年份、月份动态计算当月天数
                if (isLunar) {
                    //农历
                    String yearName = yearsForLunar.get(selectedYearIndex);
                    String monthName = monthsForLunar.get(selectedMonthIndex);
                    String dayName = daysForLunar.get(selectedDayIndex);
                    int lunarYear = LunarCalender.yearNameToNumber(yearName);
                    int lunarMonth = LunarCalender.monthNameToNumber(monthName);
                    int lunarDay = LunarCalender.dayNameToNumber(dayName);
                    int leapMonth = LunarCalender.leapMonth(lunarYear);
                    //月
                    monthsForLunar.clear();
                    for (int i = 1; i <= 12; i++) {
                        monthsForLunar.add(LunarCalender.numberToMonthName(i, false));
                        if (i == leapMonth) {
                            monthsForLunar.add(LunarCalender.numberToMonthName(i, true));
                        }
                    }
                    if (selectedMonthIndex >= monthsForLunar.size()) {
                        selectedMonthIndex = monthsForLunar.size() - 1;
                    }
                    monthView.setDatas(monthsForLunar, selectedMonthIndex);
                    //日
                    int daySum = LunarCalender.daysInMonth(lunarYear, lunarMonth, lunarMonth == leapMonth);
                    daysForLunar.clear();
                    for (int i = 1; i <= daySum; i++) {
                        daysForLunar.add(LunarCalender.numberToDayName(i));
                    }
                    if (selectedDayIndex >= daySum) {
                        //月变动时，日保持选中的不变（当超过当前月的最大日时，默认选中当前月最大日）
                        selectedDayIndex = daySum - 1;
                    }
                    dayView.setDatas(daysForLunar, selectedDayIndex);
                } else {
                    //公历
                    daysForSolar.clear();
                    int maxDays = DateUtils.calculateDaysInMonth(stringToInteger(content),
                            stringToInteger(monthsForSolar.get(selectedMonthIndex)));
                    for (int i = 1; i <= maxDays; i++) {
                        daysForSolar.add(i + "");
                    }
                    if (selectedDayIndex >= maxDays) {
                        //年或月变动时，保持之前选择的日不动：如果之前选择的日是之前年月的最大日，则日自动为该年月的最大日
                        selectedDayIndex = daysForSolar.size() - 1;
                    }
                    dayView.setDatas(daysForSolar, selectedDayIndex);
                }
                performPick();
            }
        });

        monthView.setOnWheelListener(new OnWheelListener() {

            @Override
            public void onWheel(int position, String content) {
                selectedMonthIndex = position;
                if (isLunar) {
                    //农历
                    String yearName = yearsForLunar.get(selectedYearIndex);
                    String monthName = monthsForLunar.get(selectedMonthIndex);
                    String dayName = daysForLunar.get(selectedDayIndex);
                    int lunarYear = LunarCalender.yearNameToNumber(yearName);
                    int lunarMonth = LunarCalender.monthNameToNumber(monthName);
                    int lunarDay = LunarCalender.dayNameToNumber(dayName);
                    boolean isLeapMonth = lunarMonth == LunarCalender.leapMonth(lunarYear);
                    int daySum = LunarCalender.daysInMonth(lunarYear, lunarMonth, isLeapMonth);
                    //日
                    daysForLunar.clear();
                    for (int i = 1; i <= daySum; i++) {
                        daysForLunar.add(LunarCalender.numberToDayName(i));
                    }
                    if (selectedDayIndex >= daySum) {
                        //月变动时，日保持选中的不变（当超过当前月的最大日时，默认选中当前月最大日）
                        selectedDayIndex = daySum - 1;
                    }
                    dayView.setDatas(daysForLunar, selectedDayIndex);
                } else {
                    //公历
                    //年月日或年月模式下，需要根据年份及月份动态计算天数
                    daysForSolar.clear();
                    int maxDays = DateUtils.calculateDaysInMonth(stringToInteger(yearsForSolar.get(selectedYearIndex)),
                            stringToInteger(content));
                    for (int i = 1; i <= maxDays; i++) {
                        daysForSolar.add(i + "");
                    }
                    if (selectedDayIndex >= maxDays) {
                        //年或月变动时，保持之前选择的日不动：如果之前选择的日是之前年月的最大日，则日自动为该年月的最大日
                        selectedDayIndex = daysForSolar.size() - 1;
                    }
                    dayView.setDatas(daysForSolar, selectedDayIndex);
                }
                performPick();
            }
        });

        dayView.setOnWheelListener(new OnWheelListener() {

            @Override
            public void onWheel(int position, String content) {
                selectedDayIndex = position;
                performPick();
            }
        });

        lunarSwitch.setOnCheckChangedListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(boolean isChecked) {
                if (isChecked) {
                    toLunar();
                    isLunar = true;
                } else {
                    toSolar();
                    isLunar = false;
                }
            }
        });
    }

    protected void performPick() {
        if (isLunar) {
            String yearName = yearsForLunar.get(selectedYearIndex);
            String monthName = monthsForLunar.get(selectedMonthIndex);
            String dayName = daysForLunar.get(selectedDayIndex);
            //农历
            int lunarYear = LunarCalender.yearNameToNumber(yearName);
            int lunarMonth = LunarCalender.monthNameToNumber(monthName);
            int lunarDay = LunarCalender.dayNameToNumber(dayName);
            boolean isLeapMonth = monthName.startsWith("闰");
            //公历
            int solar[] = LunarCalender.lunarToSolar(lunarYear, lunarMonth, lunarDay, isLeapMonth);
            int solarYear = solar[0];
            int solarMonth = solar[1];
            int solarDay = solar[2];

            //当农历转换的公历超出范围时，显示公历范围的最后一天
            Calendar calendar = Calendar.getInstance();
            if (solarYear > calendar.get(Calendar.YEAR) + YEAR_DELTA) {
                solarYear = calendar.get(Calendar.YEAR) + YEAR_DELTA;
                solarMonth = 12;
                solarDay = 31;
            }
            selectedTime.setYear(solarYear);
            selectedTime.setMonth(solarMonth);
            selectedTime.setDay(solarDay);
        } else {
            String yearName = yearsForSolar.get(selectedYearIndex);
            String monthName = monthsForSolar.get(selectedMonthIndex);
            String dayName = daysForSolar.get(selectedDayIndex);
            selectedTime.setYear(Integer.parseInt(yearName));
            selectedTime.setMonth(Integer.parseInt(monthName));
            selectedTime.setDay(Integer.parseInt(dayName));
        }
        if (onPickerListener != null) {
            onPickerListener.onPicker(selectedTime);
        }
    }

    protected void toSolar() {
        String yearName = yearsForLunar.get(selectedYearIndex);
        String monthName = monthsForLunar.get(selectedMonthIndex);
        String dayName = daysForLunar.get(selectedDayIndex);
        //农历
        int lunarYear = LunarCalender.yearNameToNumber(yearName);
        int lunarMonth = LunarCalender.monthNameToNumber(monthName);
        int lunarDay = LunarCalender.dayNameToNumber(dayName);
        boolean isLeapMonth = monthName.startsWith("闰");
        //公历
        int solar[] = LunarCalender.lunarToSolar(lunarYear, lunarMonth, lunarDay, isLeapMonth);
        int solarYear = solar[0];
        int solarMonth = solar[1];
        int solarDay = solar[2];

        //当农历转换的公历超出范围时，显示公历范围的最后一天
        Calendar calendar = Calendar.getInstance();
        if (solarYear > calendar.get(Calendar.YEAR) + YEAR_DELTA) {
            solarYear = calendar.get(Calendar.YEAR) + YEAR_DELTA;
            solarMonth = 12;
            solarDay = 31;
        }
        //年
        yearsForSolar.clear();
        int year = calendar.get(Calendar.YEAR);
        for (int i = year - YEAR_DELTA; i <= year + YEAR_DELTA; i++) {
            yearsForSolar.add(i + "");
        }
        //月
        monthsForSolar.clear();
        for (int i = 1; i <= 12; i++) {
            monthsForSolar.add(i + "");
        }
        //日
        daysForSolar.clear();
        int daySum = LunarCalender.daysInSolarMonth(solarYear, solarMonth);
        for (int i = 1; i <= daySum; i++) {
            daysForSolar.add(i + "");
        }
        selectedYearIndex = solarYear - (year - YEAR_DELTA);
        selectedMonthIndex = solarMonth - 1;
        selectedDayIndex = solarDay - 1;
        yearView.setDatas(yearsForSolar, selectedYearIndex);
        monthView.setDatas(monthsForSolar, selectedMonthIndex);
        dayView.setDatas(daysForSolar, selectedDayIndex);
    }

    protected void toLunar() {
        //公历切换到农历
        int solarYear = stringToInteger(yearsForSolar.get(selectedYearIndex));
        int solarMonth = stringToInteger(monthsForSolar.get(selectedMonthIndex));
        int solarDay = stringToInteger(daysForSolar.get(selectedDayIndex));
        int lunar[] = LunarCalender.solarToLunar(solarYear, solarMonth, solarDay);
        int lunarYear = lunar[0];
        int lunarMonth = lunar[1];
        int lunarDay = lunar[2];
        boolean isLeapMonth = lunar[3] == 1 ? true : false;
        int leapMonth = LunarCalender.leapMonth(lunarYear);
        //是否是闰月之后或者闰月
        boolean isOrAfterLeapMonth = ((leapMonth != 0) && lunarMonth > leapMonth) || isLeapMonth;

        //当公历转换为农历超过农历范围时，显示农历的第一天
        Calendar calendar = Calendar.getInstance();
        if (lunarYear < calendar.get(Calendar.YEAR) - YEAR_DELTA) {
            lunarYear = calendar.get(Calendar.YEAR) - YEAR_DELTA;
            lunarMonth = 1;
            lunarDay = 1;
        }
        //年
        yearsForLunar.clear();
        int year = calendar.get(Calendar.YEAR);
        for (int i = year - YEAR_DELTA; i <= year + YEAR_DELTA; i++) {
            yearsForLunar.add(LunarCalender.numberToYearName(i));
        }
        //月
        monthsForLunar.clear();
        for (int i = 1; i <= 12; i++) {
            monthsForLunar.add(LunarCalender.numberToMonthName(i, false));
            if (i == leapMonth) {
                monthsForLunar.add(LunarCalender.numberToMonthName(i, true));
            }
        }
        //日
        daysForLunar.clear();
        int daySum = LunarCalender.daysInMonth(lunarYear, lunarMonth, lunarMonth == leapMonth);
        for (int i = 1; i <= daySum; i++) {
            daysForLunar.add(LunarCalender.numberToDayName(i));
        }
        //农历将要选中的下标
        selectedYearIndex = lunarYear - (year - YEAR_DELTA);
        selectedMonthIndex = lunarMonth - 1 + (isOrAfterLeapMonth ? 1 : 0);
        selectedDayIndex = lunarDay - 1;
        yearView.setDatas(yearsForLunar, selectedYearIndex);
        monthView.setDatas(monthsForLunar, selectedMonthIndex);
        dayView.setDatas(daysForLunar, selectedDayIndex);
    }

    @Override
    protected void initData() {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        //年
        yearsForSolar = new ArrayList<String>();
        yearsForLunar = new ArrayList<String>();
        for (int i = currentYear - YEAR_DELTA; i <= currentYear + YEAR_DELTA; i++) {
            yearsForSolar.add(DateUtils.fillZero(i));
        }
        //月
        monthsForSolar = new ArrayList<String>();
        monthsForLunar = new ArrayList<String>();
        for (int i = 1; i <= 12; i++) {
            monthsForSolar.add(DateUtils.fillZero(i));
        }
        //日
        daysForSolar = new ArrayList<String>();
        daysForLunar = new ArrayList<String>();
        for (int i = 1; i <= 31; i++) {
            daysForSolar.add(DateUtils.fillZero(i));
        }
    }

    @Override
    protected void onInit(GLBaseSettings info) {
        super.onInit(info);
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        selectedYearIndex = YEAR_DELTA;
        selectedMonthIndex = currentMonth;
        selectedDayIndex = currentDay - 1;
        if (mode == Mode.YEAR_MONTH || mode == Mode.YEAR_MONTH_DAY) {
            yearView.setDatas(yearsForSolar, selectedYearIndex);
        }
        monthView.setDatas(monthsForSolar, selectedMonthIndex);
        if (mode == Mode.MONTH_DAY || mode == Mode.YEAR_MONTH_DAY) {
            dayView.setDatas(daysForSolar, selectedDayIndex);
        }
        layoutChildren();
    }

    protected void layoutChildren() {
        resizeChildren();
        setWeelViewStyle(yearView);
        setWeelViewStyle(monthView);
        setWeelViewStyle(dayView);
        if (mode == Mode.YEAR_MONTH_DAY) {
            //隐藏上午、下午控件
            yearView.setAlpha(1f);
            yearView.setX(paddingH);
            yearView.setY(paddingV);
            monthView.setX(paddingH + columnWidth);
            monthView.setY(paddingV);
            dayView.setAlpha(1f);
            dayView.setX(paddingH + columnWidth * 2);
            dayView.setY(paddingV);
        } else if (mode == Mode.YEAR_MONTH) {
            yearView.setAlpha(1f);
            yearView.setX(paddingH);
            yearView.setY(paddingV);
            monthView.setX(paddingH + columnWidth);
            monthView.setY(paddingV);
            dayView.setAlpha(0f);
        } else if (mode == Mode.MONTH_DAY) {
            yearView.setAlpha(0f);
            monthView.setX(paddingH);
            monthView.setY(paddingV);
            dayView.setAlpha(1f);
            dayView.setX(paddingH + columnWidth);
            dayView.setY(paddingV);
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
        if (mode == mode.YEAR_MONTH_DAY) {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 3;
        } else {
            columnWidth = (getMeasuredWidth() - paddingH * 2) / 2;
        }
        yearView.setLayoutParams(columnWidth, wheelHeight);
        monthView.setLayoutParams(columnWidth, wheelHeight);
        dayView.setLayoutParams(columnWidth, wheelHeight);
    }

    private int stringToInteger(String text) {
        if (text.startsWith("0")) {
            //截取掉前缀0以便转换为整数
            text = text.substring(1);
        }
        return Integer.parseInt(text);
    }

    public int getSelectedYearIndex() {
        return selectedYearIndex;
    }

    public void setSelectedYearIndex(int selectedYearIndex) {
        this.selectedYearIndex = selectedYearIndex;
    }

    public int getSelectedMonthIndex() {
        return selectedMonthIndex;
    }

    public void setSelectedMonthIndex(int selectedMonthIndex) {
        this.selectedMonthIndex = selectedMonthIndex;
    }

    public int getSelectedDayIndex() {
        return selectedDayIndex;
    }

    public void setSelectedDayIndex(int selectedDayIndex) {
        this.selectedDayIndex = selectedDayIndex;
    }

    public List<String> getYears() {
        return isLunar ? yearsForLunar : yearsForSolar;
    }

    public List<String> getMonths() {
        return isLunar ? monthsForLunar : monthsForSolar;
    }

    public List<String> getDays() {
        return isLunar ? daysForLunar : daysForSolar;
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
        } else {
            LogUtils.showE(TAG, "初始化没完成");
        }
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
