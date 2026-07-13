package com.yjh.base.uikit.widget.spinner;

import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.yjh.base.uikit.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DateSpinner extends BaseSpinner {
    private WheelView wvYear, wvMonth, wvDay;
    private int mode; // 1:Y, 2:YM, 3:YMD
    private Calendar now = Calendar.getInstance();
    private boolean isAllowFuture = false;
    private TextView tvTitle;

    public DateSpinner(Context context, int mode) {
        super(context);
        this.mode = mode;
        updateTitleByMode();
        if (wvMonth != null) wvMonth.setVisibility(mode >= 2 ? View.VISIBLE : View.GONE);
        if (wvDay != null) wvDay.setVisibility(mode >= 3 ? View.VISIBLE : View.GONE);
        initData();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uikit_view_date_spinner;
    }

    @Override
    protected void initView(View view) {
        wvYear = view.findViewById(R.id.wv_year);
        wvMonth = view.findViewById(R.id.wv_month);
        wvDay = view.findViewById(R.id.wv_day);
        tvTitle = view.findViewById(R.id.tv_title);

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            if (listener != null) {
                int year = Integer.parseInt(wvYear.getSelectedItem());
                int month = mode >= 2 ? Integer.parseInt(wvMonth.getSelectedItem()) : 0;
                int day = mode >= 3 ? Integer.parseInt(wvDay.getSelectedItem()) : 0;
                listener.onSelected(year, month, day);
            }
            dismiss();
        });

        // 联动监听
        wvYear.setOnItemSelectedListener(i -> updateMonths());
        wvMonth.setOnItemSelectedListener(i -> updateDays());
    }

    private void updateTitleByMode() {
        if (tvTitle == null) return;
        switch (mode) {
            case 1:
                tvTitle.setText("选择年份");
                break;
            case 2:
                tvTitle.setText("选择月份");
                break;
            case 3:
                tvTitle.setText("选择日期");
                break;
            default:
                tvTitle.setText("选择时间");
                break;
        }
    }

    private void initData() {
        int todayYear = now.get(Calendar.YEAR);
        int endYear = isAllowFuture ? todayYear + 20 : todayYear;

        // 1. 初始化年份
        List<String> years = new ArrayList<>();
        for (int i = 2000; i <= endYear; i++) {
            years.add(String.valueOf(i));
        }
        wvYear.setData(years);

        // 定位到今年
        int yearPos = years.indexOf(String.valueOf(todayYear));
        wvYear.setSelected(Math.max(0, yearPos));

        // 2. 触发级联更新
        updateMonths();
    }

    private void updateMonths() {
        if (mode < 2) return;

        int selYear = Integer.parseInt(wvYear.getSelectedItem());
        int todayYear = now.get(Calendar.YEAR);
        int todayMonth = now.get(Calendar.MONTH) + 1;

        // 动态限制月份上限
        int maxM = (!isAllowFuture && selYear >= todayYear) ? todayMonth : 12;

        List<String> months = new ArrayList<>();
        for (int i = 1; i <= maxM; i++) {
            months.add(String.valueOf(i));
        }
        wvMonth.setData(months);

        // 越界检查与初始定位
        int lastIndex = wvMonth.getSelectedIndex();
        if (selYear == todayYear && lastIndex == 0) {
            // 初始化进入或滑回今年时，强制选当前月
            wvMonth.setSelected(todayMonth - 1);
        } else if (lastIndex >= months.size()) {
            // 解决从 2025 滑到 2026 产生的空白 Bug
            wvMonth.setSelected(months.size() - 1);
        } else {
            wvMonth.setSelected(lastIndex);
        }

        updateDays();
    }

    private void updateDays() {
        if (mode < 3) return;

        int selYear = Integer.parseInt(wvYear.getSelectedItem());
        int selMonth = Integer.parseInt(wvMonth.getSelectedItem());
        int todayYear = now.get(Calendar.YEAR);
        int todayMonth = now.get(Calendar.MONTH) + 1;
        int todayDay = now.get(Calendar.DAY_OF_MONTH);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, selYear);
        cal.set(Calendar.MONTH, selMonth - 1);
        int maxD = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 动态限制天数上限
        if (!isAllowFuture && selYear >= todayYear && selMonth >= todayMonth) {
            maxD = todayDay;
        }

        List<String> days = new ArrayList<>();
        for (int i = 1; i <= maxD; i++) {
            days.add(String.valueOf(i));
        }
        wvDay.setData(days);

        // 越界检查与初始定位
        int lastIndex = wvDay.getSelectedIndex();
        if (selYear == todayYear && selMonth == todayMonth && lastIndex == 0) {
            wvDay.setSelected(todayDay - 1);
        } else if (lastIndex >= days.size()) {
            wvDay.setSelected(days.size() - 1);
        } else {
            wvDay.setSelected(lastIndex);
        }
    }

    public void setAllowFuture(boolean allow) {
        this.isAllowFuture = allow;
        initData();
    }
}