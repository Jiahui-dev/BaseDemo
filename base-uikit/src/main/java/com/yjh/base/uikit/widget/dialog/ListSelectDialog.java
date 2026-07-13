package com.yjh.base.uikit.widget.dialog;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yjh.base.uikit.R;
import com.yjh.base.uikit.adapter.BaseRecyclerAdapter;
import com.yjh.base.uikit.adapter.holder.BaseViewHolder;
import java.util.ArrayList;
import java.util.List;

public class ListSelectDialog<T> extends BaseDialog {

    private String mTitle;
    private List<T> mData = new ArrayList<>();
    private OnItemClickListener<T> mListener;
    private ItemTextConverter<T> mConverter;

    /**
     * 定义点击回调接口
     */
    public interface OnItemClickListener<T> {
        void onItemClick(T item, int position);
    }

    /**
     * 定义数据转换接口，让弹窗知道显示哪个字段
     */
    public interface ItemTextConverter<T> {
        String convert(T item);
    }

    public static <T> ListSelectDialog<T> newInstance() {
        return new ListSelectDialog<>();
    }

    public ListSelectDialog<T> setTitle(String title) {
        this.mTitle = title;
        return this;
    }

    public ListSelectDialog<T> setData(List<T> data, ItemTextConverter<T> converter) {
        this.mData = data;
        this.mConverter = converter;
        return this;
    }

    public ListSelectDialog<T> setOnItemClickListener(OnItemClickListener<T> listener) {
        this.mListener = listener;
        return this;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uikit_dialog_list;
    }

    @Override
    protected void initView(View root) {
        TextView tvTitle = root.findViewById(R.id.tv_dialog_title);
        RecyclerView rvList = root.findViewById(R.id.rv_dialog_list);

        // 1. 设置标题 (如果不设置则 GONE，符合靠左悬浮逻辑)
        if (!TextUtils.isEmpty(mTitle)) {
            tvTitle.setText(mTitle);
            tvTitle.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setVisibility(View.GONE);
        }

        // 2. 配置 RecyclerView
        rvList.setLayoutManager(new LinearLayoutManager(getContext()));
        BaseRecyclerAdapter<T> adapter = new BaseRecyclerAdapter<T>(getContext()) {
            @Override
            protected int getItemLayoutId(int viewType) {
                return R.layout.unkit_item_simple_text;
            }

            @Override
            protected void bindData(BaseViewHolder holder, T data, int position) {
                String showText = (mConverter != null) ? mConverter.convert(data) : data.toString();
                holder.setText(R.id.tv_item_text, showText);

                holder.itemView.setOnClickListener(v -> {
                    if (mListener != null) {
                        mListener.onItemClick(data, position);
                    }
                    dismiss();
                });
            }
        };
        rvList.setAdapter(adapter);
        adapter.setList(mData);

        // 3. 动态控制最大高度
        rvList.post(() -> {
            int maxHeight = (int) (getResources().getDisplayMetrics().heightPixels * 0.4); // 设置最大高度为屏幕的 40%
            if (rvList.getMeasuredHeight() > maxHeight) {
                ViewGroup.LayoutParams params = rvList.getLayoutParams();
                params.height = maxHeight;
                rvList.setLayoutParams(params);
            }
        });
    }
}