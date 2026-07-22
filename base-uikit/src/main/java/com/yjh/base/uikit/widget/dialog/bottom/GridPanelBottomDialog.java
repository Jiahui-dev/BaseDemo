package com.yjh.base.uikit.widget.dialog.bottom;

import android.view.View;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.yjh.base.uikit.R;
import com.yjh.base.uikit.adapter.SimpleAdapter;
import com.yjh.base.uikit.databinding.UikitItemGridPageBinding;
import com.yjh.base.uikit.databinding.UikitItemGridPanelOptionBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动分页的通用网格底部弹窗
 * Created by jiahui on 2026/07/22
 */
public class GridPanelBottomDialog<T> extends BaseBottomDialog {

    private ViewPager2 mViewPager;
    private LinearLayout mDotContainer;

    private int mRows = 2;       // 行数
    private int mColumns = 3;    // 列数
    private List<T> mTotalList = new ArrayList<>();

    private ItemBinder<T> mItemBinder;
    private OnItemClickListener<T> mListener;

    // 绑定接口：外部决定怎么把 T 绑定到标准的图标+文本 ViewBinding 上
    public interface ItemBinder<T> {
        void onBind(UikitItemGridPanelOptionBinding binding, T data, int position);
    }

    // 点击事件接口
    public interface OnItemClickListener<T> {
        void onItemClick(T data, int globalPosition);
    }

    /**
     * 构建方法
     * @param rows 行数 (如 2)
     * @param columns 列数 (如 3)
     * @param list 数据列表
     * @param binder 数据绑定器
     * @param listener 点击监听
     */
    public static <T> GridPanelBottomDialog<T> newInstance(
            int rows,
            int columns,
            List<T> list,
            ItemBinder<T> binder,
            OnItemClickListener<T> listener) {

        GridPanelBottomDialog<T> dialog = new GridPanelBottomDialog<>();
        dialog.mRows = Math.max(1, rows);
        dialog.mColumns = Math.max(1, columns);
        if (list != null) dialog.mTotalList.addAll(list);
        dialog.mItemBinder = binder;
        dialog.mListener = listener;
        return dialog;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uikit_dialog_grid_panel;
    }

    @Override
    protected void initView(View root) {
        mViewPager = root.findViewById(R.id.view_pager);
        mDotContainer = root.findViewById(R.id.ll_dot_container);

        int pageSize = mRows * mColumns;
        int totalSize = mTotalList.size();

        // 1. 将总数据按每页容量切片
        List<List<T>> pagesData = partitionList(mTotalList, pageSize);

        // 2. 如果只有 1 页，隐藏底部指示器点；多于 1 页则展示并初始化点
        if (pagesData.size() <= 1) {
            mDotContainer.setVisibility(View.GONE);
        } else {
            mDotContainer.setVisibility(View.VISIBLE);
            setupDots(pagesData.size());
        }

        // 3. 使用你的 SimpleAdapter 渲染 ViewPager2 的每一页 (Page)
        SimpleAdapter<List<T>, UikitItemGridPageBinding> pageAdapter = new SimpleAdapter<>(
                requireContext(),
                UikitItemGridPageBinding::inflate,
                (pageBinding, pageList, pageIndex) -> {
                    // 初始化当前页的 RecyclerView (Grid)
                    RecyclerView rv = pageBinding.rvPageGrid;
                    rv.setLayoutManager(new GridLayoutManager(getContext(), mColumns));

                    // 内层使用 SimpleAdapter 渲染每一个网格 Item
                    SimpleAdapter<T, UikitItemGridPanelOptionBinding> itemAdapter = new SimpleAdapter<>(
                            requireContext(),
                            UikitItemGridPanelOptionBinding::inflate,
                            (itemBinding, itemData, itemIndex) -> {
                                if (mItemBinder != null) {
                                    mItemBinder.onBind(itemBinding, itemData, itemIndex);
                                }
                            }
                    );

                    itemAdapter.setOnItemClickListener((v, viewId, inPagePosition, data) -> {
                        if (mListener != null) {
                            int globalPosition = pageIndex * pageSize + inPagePosition;
                            mListener.onItemClick(data, globalPosition);
                        }
                        dismiss(); // 点击选中后关闭弹窗
                    });

                    itemAdapter.setList(pageList);
                    rv.setAdapter(itemAdapter);
                }
        );

        pageAdapter.setList(pagesData);
        mViewPager.setAdapter(pageAdapter);

        // 4. ViewPager2 滑动联动指示器点
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });
    }

    // 按每页大小将 List 切割为多个子 List
    private List<List<T>> partitionList(List<T> list, int pageSize) {
        List<List<T>> pages = new ArrayList<>();
        if (list == null || list.isEmpty()) return pages;

        int size = list.size();
        for (int i = 0; i < size; i += pageSize) {
            pages.add(new ArrayList<>(list.subList(i, Math.min(size, i + pageSize))));
        }
        return pages;
    }

    // 初始化指示器小圆点
    private void setupDots(int count) {
        mDotContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.uikit_bg_dot_unselected);
            mDotContainer.addView(dot);
        }
        updateDots(0);
    }

    // 切换选中的指示器点状态
    private void updateDots(int selectIndex) {
        int count = mDotContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View dot = mDotContainer.getChildAt(i);
            dot.setBackgroundResource(i == selectIndex ? R.drawable.uikit_bg_dot_selected : R.drawable.uikit_bg_dot_unselected);
        }
    }
}