package com.yjh.base.uikit.widget.dialog;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yjh.base.uikit.R;
import com.yjh.base.uikit.adapter.SimpleAdapter;
import com.yjh.base.uikit.databinding.UnkitItemSimpleTextBinding;
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
    private interface OnItemClickListener<T> {
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

    protected void initView(View root) {
        TextView tvTitle = root.findViewById(R.id.tv_dialog_title);
        RecyclerView rvList = root.findViewById(R.id.rv_dialog_list);

        if (!TextUtils.isEmpty(mTitle)) {
            tvTitle.setText(mTitle);
            tvTitle.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setVisibility(View.GONE);
        }

        rvList.setLayoutManager(new LinearLayoutManager(getContext()));

        // 直接实例化 SimpleAdapter，传入 Creator 和 Binder
        SimpleAdapter<T, UnkitItemSimpleTextBinding> adapter = new SimpleAdapter<>(
                getContext(),
                UnkitItemSimpleTextBinding::inflate, // 传入条目 ViewBinding 的渲染器
                (binding, data, position) -> {
                    String showText = (mConverter != null) ? mConverter.convert(data) : data.toString();

                    binding.tvItemText.setText(showText);
                }
        );

        adapter.setOnItemClickListener((view, viewId, position, data) -> {
            if (mListener != null) {
                mListener.onItemClick(data, position);
            }
            dismiss();
        });

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