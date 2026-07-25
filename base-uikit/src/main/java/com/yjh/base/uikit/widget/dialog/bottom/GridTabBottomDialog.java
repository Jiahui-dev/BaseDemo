package com.yjh.base.uikit.widget.dialog.bottom;

import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.yjh.base.uikit.R;
import com.yjh.base.uikit.adapter.SimpleAdapter;
import com.yjh.base.uikit.databinding.UikitItemGridPanelOptionBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jiahui on 2026/07/25
 */
public class GridTabBottomDialog<T> extends BaseBottomDialog {

    private TabLayout mTabLayout;
    private RecyclerView mRecyclerView;

    private int mColumns = 4; // 每行列数
    private boolean mShowItemName = true;

    private final List<TabCategory<T>> mCategories = new ArrayList<>();
    private SimpleAdapter<T, UikitItemGridPanelOptionBinding> mAdapter;

    private ItemBinder<T> mItemBinder;
    private OnItemClickListener<T> mListener;

    public interface ItemBinder<T> {
        void onBind(UikitItemGridPanelOptionBinding binding, T data, int position);
    }

    public interface OnItemClickListener<T> {
        void onItemClick(T data, int globalPosition);
    }

    public static class TabCategory<T> {
        private final String title;
        private final List<T> dataList;

        public TabCategory(String title, List<T> dataList) {
            this.title = title;
            this.dataList = dataList != null ? dataList : new ArrayList<>();
        }

        public String getTitle() { return title; }
        public List<T> getDataList() { return dataList; }
    }

    public static <T> GridTabBottomDialog<T> newInstance(
            int columns,
            List<TabCategory<T>> categories,
            ItemBinder<T> binder,
            OnItemClickListener<T> listener) {

        GridTabBottomDialog<T> dialog = new GridTabBottomDialog<>();
        dialog.mColumns = Math.max(1, columns);
        if (categories != null) dialog.mCategories.addAll(categories);
        dialog.mItemBinder = binder;
        dialog.mListener = listener;
        return dialog;
    }

    public GridTabBottomDialog<T> showTitle(boolean show) {
        this.mShowItemName = show;
        return this;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uikit_dialog_grid_tab_panel;
    }

    @Override
    protected void initView(View root) {
        mTabLayout = root.findViewById(R.id.tab_layout);
        mRecyclerView = root.findViewById(R.id.rv_content);

        // 只有一个分类且没有分类名时，隐藏 Tab 栏
        boolean isMultiTab = mCategories.size() > 1 || (mCategories.size() == 1 && !mCategories.get(0).getTitle().isEmpty());
        mTabLayout.setVisibility(isMultiTab ? View.VISIBLE : View.GONE);

        // 初始化 网格布局
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), mColumns));

        // 通用 单层 SimpleAdapter 渲染图标项
        mAdapter = new SimpleAdapter<>(
                requireContext(),
                UikitItemGridPanelOptionBinding::inflate,
                (binding, data, position) -> {
                    binding.tvItemName.setVisibility(mShowItemName ? View.VISIBLE : View.GONE);
                    if (mItemBinder != null) {
                        mItemBinder.onBind(binding, data, position);
                    }
                }
        );

        mAdapter.setOnItemClickListener((v, viewId, position, data) -> {
            if (mListener != null) {
                mListener.onItemClick(data, position);
            }
            dismiss();
        });

        mRecyclerView.setAdapter(mAdapter);

        // 初始化 Tab 栏并绑定切换事件
        mTabLayout.removeAllTabs();
        for (TabCategory<T> category : mCategories) {
            mTabLayout.addTab(mTabLayout.newTab().setText(category.getTitle()));
        }

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 默认显示第 0 个分类数据
        if (!mCategories.isEmpty()) {
            switchCategory(0);
        }

        root.post(() -> {
            View parent = (View) root.getParent();
            if (parent != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(parent);

                // 1. 禁止下滑拖拽隐藏
                behavior.setHideable(false);

                // 2. 禁止手势拖拽 (AndroidX Material 库 1.3.0+ 支持)
                behavior.setDraggable(false);
            }
        });
    }

    /**
     * 切换 Tab 分类数据源
     */
    private void switchCategory(int categoryIndex) {
        if (categoryIndex < 0 || categoryIndex >= mCategories.size()) return;

        List<T> dataList = mCategories.get(categoryIndex).getDataList();
        mAdapter.setList(dataList);

        // 每次切换分类时自动回顶
        mRecyclerView.scrollToPosition(0);
    }
}