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

    // 控制是否显示 Item 图标下方的文字
    private boolean mShowItemName = true;

    private ItemBinder<T> mItemBinder;
    private OnItemClickListener<T> mListener;

    public interface ItemBinder<T> {
        void onBind(UikitItemGridPanelOptionBinding binding, T data, int position);
    }

    public interface OnItemClickListener<T> {
        void onItemClick(T data, int globalPosition);
    }

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

    /**
     * 设置是否显示每个图标下方的文字（tv_item_name）
     */
    public GridPanelBottomDialog<T> showTitle(boolean show) {
        this.mShowItemName = show;
        return this;
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

        // 1. 切割数据
        List<List<T>> pagesData = partitionList(mTotalList, pageSize);

        // 2. 指示器显示控制
        if (pagesData.size() <= 1) {
            mDotContainer.setVisibility(View.GONE);
        } else {
            mDotContainer.setVisibility(View.VISIBLE);
            setupDots(pagesData.size());
        }

        // 3. 渲染 ViewPager2
        SimpleAdapter<List<T>, UikitItemGridPageBinding> pageAdapter = new SimpleAdapter<>(
                requireContext(),
                UikitItemGridPageBinding::inflate,
                (pageBinding, pageList, pageIndex) -> {
                    RecyclerView rv = pageBinding.rvPageGrid;
                    rv.setLayoutManager(new GridLayoutManager(getContext(), mColumns));

                    // 内层渲染网格 Item
                    SimpleAdapter<T, UikitItemGridPanelOptionBinding> itemAdapter = new SimpleAdapter<>(
                            requireContext(),
                            UikitItemGridPanelOptionBinding::inflate,
                            (itemBinding, itemData, itemIndex) -> {

                                // 【核心修复点】：在这里统一控制 tv_item_name 的显隐！
                                itemBinding.tvItemName.setVisibility(mShowItemName ? View.VISIBLE : View.GONE);

                                // 回调给外部设置数据
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
                        dismiss();
                    });

                    itemAdapter.setList(pageList);
                    rv.setAdapter(itemAdapter);
                }
        );

        pageAdapter.setList(pagesData);
        mViewPager.setAdapter(pageAdapter);

        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });
    }

    private List<List<T>> partitionList(List<T> list, int pageSize) {
        List<List<T>> pages = new ArrayList<>();
        if (list == null || list.isEmpty()) return pages;

        int size = list.size();
        for (int i = 0; i < size; i += pageSize) {
            pages.add(new ArrayList<>(list.subList(i, Math.min(size, i + pageSize))));
        }
        return pages;
    }

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

    private void updateDots(int selectIndex) {
        int count = mDotContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View dot = mDotContainer.getChildAt(i);
            dot.setBackgroundResource(i == selectIndex ? R.drawable.uikit_bg_dot_selected : R.drawable.uikit_bg_dot_unselected);
        }
    }
}