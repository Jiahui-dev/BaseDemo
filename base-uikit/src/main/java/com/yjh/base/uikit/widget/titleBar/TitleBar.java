package com.yjh.base.uikit.widget.titleBar;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.yjh.base.uikit.R;
import com.yjh.base.utils.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiahui on 2025/12/22
 */
public class TitleBar extends ConstraintLayout {

    private static final String TAG="TitleBar";
    private ImageView mIvBack;
    private LinearLayout mRightContainer;
    private TextSwitcher mTextSwitcher;
    private TextView mTvSearchBtn;
    private ConstraintLayout mSearchGroup;
    private TextView mTvCenterTitle;
    private List<String> mHintList = new ArrayList<>();
    private int mCurrentIndex = 0;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mRunnable;
    private boolean isRolling = false;

    public TitleBar(Context context) {
        this(context, null);
    }

    public TitleBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TitleBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        LayoutInflater.from(context).inflate(R.layout.uikit_view_title_bar, this, true);

        mIvBack = findViewById(R.id.iv_back);
        mRightContainer = findViewById(R.id.ll_right_custom_container);
        mTextSwitcher = findViewById(R.id.ts_hint_text);
        mSearchGroup = findViewById(R.id.cl_search_group);
        mTvCenterTitle = findViewById(R.id.tv_center_title);

        mTextSwitcher.setFactory(() -> {
            TextView textView = new TextView(getContext());
            textView.setTextSize(13);
            textView.setTextColor(Color.parseColor("#999999"));
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            textView.setSingleLine(true);
            // 设置 TextView 填满 TextSwitcher 的高度
            // 因为 TextSwitcher 本质是 FrameLayout，默认子 View 是 wrap_content 且靠顶部的
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            params.gravity = Gravity.CENTER_VERTICAL;

            textView.setLayoutParams(params);
            return textView;
        });

        mTextSwitcher.setInAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in));
        mTextSwitcher.setOutAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_out));

        mIvBack.setOnClickListener(v -> {
            if (context instanceof Activity) ((Activity) context).finish();
        });
    }

    /**
     * 由子类或外部调用，注入搜索关键词数据
     */
    public void setSearchHints(List<String> hints) {

        LogUtils.verbose(TAG, "setSearchHints: hints=" + hints);

        if (hints == null || hints.isEmpty()) return;

        this.mHintList = hints;
        this.mCurrentIndex = 0;

        // 立即显示第一个，避免空白
        mTextSwitcher.setCurrentText(mHintList.get(0));
    }

    /**
     * 设置右侧 View,优先使用 View 自身的 LayoutParams
     */
    public void addRightView(View view) {
        if (mRightContainer != null) {
            // 尝试获取 View 现有的 LayoutParams
            ViewGroup.LayoutParams existingParams = view.getLayoutParams();
            LinearLayout.LayoutParams targetParams;

            if (existingParams instanceof LinearLayout.LayoutParams) {
                // 如果外部已经设置了 LinearLayout.LayoutParams，就直接使用它
                targetParams = (LinearLayout.LayoutParams) existingParams;
            } else {
                // 如果外部没有设置，或者设置的类型不对，则使用默认的 WRAP_CONTENT
                targetParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
            }

            // 确保设置了左边距
            if (targetParams.leftMargin == 0) {
                targetParams.leftMargin = dp2px(10);
            }
            // 确保垂直居中
            //targetParams.gravity = Gravity.CENTER_VERTICAL;

            mRightContainer.addView(view, targetParams);
            LogUtils.verbose(TAG, "addRightView: view added");
        }
    }

    /**
     * 设置右侧 View 并指定大小 (单位: dp)
     */
    public void addRightView(View view, int widthDp, int heightDp) {
        if (view == null) return;

        int widthPx = dp2px(widthDp);
        int heightPx = dp2px(heightDp);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(widthPx, heightPx);
        // 保持你原有的逻辑：间距和居中
        params.leftMargin = dp2px(10);
        params.gravity = Gravity.CENTER_VERTICAL;

        view.setLayoutParams(params);

        if (view instanceof ImageView) {
            ((ImageView) view).setScaleType(ImageView.ScaleType.FIT_CENTER);
        }

        addRightView(view); // 调用原有的添加方法
    }

    /**
     * 设置返回键显隐
     */
    public void setBackVisible(boolean visible) {
        mIvBack.setVisibility(visible ? VISIBLE : GONE);
    }

    public void startRoll() {
        // 如果没有数据，就不轮播
        if (isRolling || mHintList.isEmpty()) return;

        isRolling = true;
        mRunnable = new Runnable() {
            @Override
            public void run() {

                if (!mHintList.isEmpty()) {
                    mCurrentIndex = (mCurrentIndex + 1) % mHintList.size();
                    mTextSwitcher.setText(mHintList.get(mCurrentIndex));
                }
                mHandler.postDelayed(this, 3000);
            }
        };
        mHandler.postDelayed(mRunnable, 3000); // 延迟3秒开始第一次切换
    }

    public void stopRoll() {
        isRolling = false;
        mHandler.removeCallbacks(mRunnable);
    }

    private int dp2px(float dp) {
        return (int) (getContext().getResources().getDisplayMetrics().density * dp + 0.5f);
    }

    /**
     * 需要切回搜索模式，可以调这个
     */
    public void showSearchMode() {
        if (mTvCenterTitle != null) {
            mTvCenterTitle.setVisibility(GONE);
        }
        if (mSearchGroup != null) {
            mSearchGroup.setVisibility(VISIBLE);
        }
    }

    public void setOnBackListener(OnClickListener listener) {
        if (mIvBack != null) {
            // 直接覆盖默认的监听器
            mIvBack.setOnClickListener(listener);
        }
    }

    /**
     * 设置中间标题（默认居中）
     */
    public void setTitle(String title) {
        setTitle(title, TitleGravity.CENTER);
    }

    /**
     * 重载方法：设置标题内容及位置
     *
     * @param title   标题文字
     * @param gravity 标题位置（TitleGravity.LEFT / CENTER / RIGHT）
     */
    public void setTitle(String title, TitleGravity gravity) {
        if (mTvCenterTitle == null) return;

        mTvCenterTitle.setText(title);
        mTvCenterTitle.setVisibility(VISIBLE);

        if (mSearchGroup != null) {
            mSearchGroup.setVisibility(GONE);
        }

        // 获取已有的 LayoutParams
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mTvCenterTitle.getLayoutParams();

        // 重置横向约束
        lp.startToStart = ConstraintLayout.LayoutParams.UNSET;
        lp.startToEnd = ConstraintLayout.LayoutParams.UNSET;
        lp.endToEnd = ConstraintLayout.LayoutParams.UNSET;
        lp.endToStart = ConstraintLayout.LayoutParams.UNSET;

        // 根据 gravity 重新绑定约束
        switch (gravity) {
            case LEFT:
                lp.startToEnd = R.id.iv_back;
                lp.leftMargin = dp2px(10);
                mTvCenterTitle.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                break;

            case RIGHT:
                lp.endToStart = R.id.ll_right_custom_container;
                lp.rightMargin = dp2px(10);
                mTvCenterTitle.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                break;

            case CENTER:
            default:
                lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
                mTvCenterTitle.setGravity(Gravity.CENTER);
                break;
        }

        mTvCenterTitle.setLayoutParams(lp);
    }

    /**
     * 修改中间标题的颜色
     */
    public void setTitleColor(int color) {
        if (mTvCenterTitle != null) {
            mTvCenterTitle.setTextColor(color);
        }
    }

    public enum TitleGravity {
        LEFT,   // 靠左
        CENTER, // 居中（默认）
        RIGHT   // 靠右（紧贴右侧自定义 View 容器左侧）
    }
}