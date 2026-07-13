package com.yjh.base.uikit.widget.linkage;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import com.yjh.base.uikit.R;
import java.util.List;

public class LinkageView extends LinearLayout {

    private RecyclerView mRvLeft;//左侧列表
    private RecyclerView mRvRight;//右侧列表
    private LinkageLeftAdapter mLeftAdapter;
    private RecyclerView.Adapter mRightAdapter;
    private LinearLayoutManager mRightLayoutManager;
    private boolean mIsClickTriggered = false;//防止点击左侧时，右侧滚动触发左侧的再次选中，导致死循环或抖动
    private List<Integer> mHeaderPositions;//记录右侧每个 Header 的位置

    public LinkageView(Context context) {
        this(context, null);
    }

    public LinkageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context){
        setOrientation(HORIZONTAL);
        //加载布局
        LayoutInflater.from(context).inflate(R.layout.uikit_view_linkage,this,true);
        mRvLeft=findViewById(R.id.rv_linkage_left);
        mRvRight=findViewById(R.id.rv_linkage_right);
        // 初始化 LayoutManager
        mRvLeft.setLayoutManager(new LinearLayoutManager(context));
        mRightLayoutManager = new LinearLayoutManager(context);
        mRvRight.setLayoutManager(mRightLayoutManager);
    }

    /**
     * 初始化数据和适配器
     * @param leftAdapter 左侧适配器 (必须继承 LinkageLeftAdapter)
     * @param rightAdapter 右侧适配器
     * @param headerPositions 右侧列表中，每个标题(Header)所在的索引位置列表
     */
    public void init(LinkageLeftAdapter leftAdapter,RecyclerView.Adapter rightAdapter, List<Integer> headerPositions){
        this.mLeftAdapter = leftAdapter;
        this.mRightAdapter = rightAdapter;
        this.mHeaderPositions = headerPositions;

        mRvLeft.setAdapter(mLeftAdapter);
        mRvRight.setAdapter(mRightAdapter);

        initListener();
    }

    private void initListener(){
        // 1. 左侧点击事件 -> 右侧滚动
        mLeftAdapter.setOnItemClickListener((position) -> {
            mIsClickTriggered = true;
            // 左侧选中状态更新
            mLeftAdapter.setSelectedIndex(position);

            // 右侧滚动到对应位置
            if (mHeaderPositions != null && position < mHeaderPositions.size()) {
                int targetPos = mHeaderPositions.get(position);
                // scrollToPositionWithOffset 能让目标项置顶，比 smoothScrollToPosition 准
                mRightLayoutManager.scrollToPositionWithOffset(targetPos, 0);
            }
        });

        // 2. 右侧滚动事件 -> 左侧联动
        mRvRight.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (mIsClickTriggered) {
                        mIsClickTriggered = false;
                        // 滚动停止后的校准逻辑，同样改为基于中间位置
                        // 这里通常建议保持原来的逻辑，或者也改成下面的中间逻辑
                        updateLeftSelectionBasedOnCenter();
                    }
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (mIsClickTriggered) {
                    return;
                }

                // 1. 计算右侧列表的中心 Y 坐标
                float centerY = mRvRight.getHeight() / 2f;
                // X 坐标只要在 View 范围内即可
                float centerX = mRvRight.getWidth() / 2f;

                // 2. 找到中心坐标下的那个 View
                android.view.View centerView = mRvRight.findChildViewUnder(centerX, centerY);

                if (centerView != null) {
                    // 3. 获取这个 View 在 Adapter 中的位置
                    int centerPos = mRvRight.getChildAdapterPosition(centerView);

                    // 4. 判断这个位置属于哪个分类
                    int categoryIndex = getCategoryIndexByRightPos(centerPos);

                    // 5. 更新左侧选中 (防止重复刷新)
                    if (categoryIndex != -1 && categoryIndex != mLeftAdapter.getCheckedPosition()) {
                        mLeftAdapter.setSelectedIndex(categoryIndex);
                        mRvLeft.smoothScrollToPosition(categoryIndex);
                    }
                }
            }
        });

    }

    /**
     * 根据右侧当前的位置，查找它属于第几个 Header
     * 算法：在 headerPositions 数组中查找当前 pos 处于哪个区间
     */
    private int getCategoryIndexByRightPos(int rightPos) {
        if (mHeaderPositions == null || mHeaderPositions.isEmpty()) return -1;

        for (int i = 0; i < mHeaderPositions.size(); i++) {
            int headerPos = mHeaderPositions.get(i);
            int nextHeaderPos = (i + 1 < mHeaderPositions.size()) ? mHeaderPositions.get(i + 1) : Integer.MAX_VALUE;

            if (rightPos >= headerPos && rightPos < nextHeaderPos) {
                return i;
            }
        }
        return 0;
    }


    /**
     * 外部调用：直接滚动右侧到指定位置
     * @param position 右侧列表的索引 (包括标题和商品)
     */
    public void scrollToRightPosition(int position) {
        if (mRightLayoutManager != null) {
            // 1. 让右侧滚动到指定位置，并且置顶 (offset=0)
            mRightLayoutManager.scrollToPositionWithOffset(position, 0);

            // 2. 只有滚动还不够，左侧的高亮也要跟过去
            int categoryIndex = getCategoryIndexByRightPos(position);

            if (categoryIndex != -1) {
                mLeftAdapter.setSelectedIndex(categoryIndex);
                mRvLeft.smoothScrollToPosition(categoryIndex);
            }
        }
    }


    /**
     * 平滑滚动到指定位置，并强制置顶
     */
    public void smoothScrollToPositionAtTop(int position) {
        if (mRvRight == null || mRightLayoutManager == null) return;

        int currentPos = mRightLayoutManager.findFirstVisibleItemPosition();
        if (Math.abs(position - currentPos) > 10) {
            // 先瞬移到目标的前/后 10 个位置
            int jumpTo = position > currentPos ? position - 10 : position + 10;
            mRightLayoutManager.scrollToPositionWithOffset(jumpTo, 0);
        }

        RecyclerView.SmoothScroller smoothScroller = createSmoothScroller();
        smoothScroller.setTargetPosition(position);

        // 在开始滚动前，锁住左侧联动，防止滚动过程中左侧乱跳
        mIsClickTriggered = true;

        mRightLayoutManager.startSmoothScroll(smoothScroller);
    }

    public RecyclerView.SmoothScroller createSmoothScroller() {
        return new LinearSmoothScroller(getContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }
             //如果觉得滚动太慢或太快，可以重写这个方法调整速度
             @Override
             public float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                 return 25f / displayMetrics.densityDpi; // 数值越小滚得越快
             }
        };
    }

    private void updateLeftSelectionBasedOnCenter() {
        float centerY = mRvRight.getHeight() / 2f;
        float centerX = mRvRight.getWidth() / 2f;
        android.view.View centerView = mRvRight.findChildViewUnder(centerX, centerY);
        if (centerView != null) {
            int centerPos = mRvRight.getChildAdapterPosition(centerView);
            int categoryIndex = getCategoryIndexByRightPos(centerPos);
            if (categoryIndex != -1) {
                mLeftAdapter.setSelectedIndex(categoryIndex);
                mRvLeft.smoothScrollToPosition(categoryIndex);
            }
        }
    }
}
