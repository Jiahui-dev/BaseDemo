package com.yjh.basedemo.activity;

import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import com.yjh.base.uikit.activity.BaseRecyclerActivity;
import com.yjh.base.uikit.adapter.SimpleAdapter;
import com.yjh.base.uikit.controller.IRefreshListener;
import com.yjh.basedemo.databinding.AcMyCollectionBinding;
import com.yjh.basedemo.databinding.ItemCollectionBinding;
import com.yjh.basedemo.model.bean.CollectionBean;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseRecyclerActivity<CollectionBean, AcMyCollectionBinding> implements IRefreshListener {

    private int mCurrentPage = 1;

    @Override
    protected SimpleAdapter<CollectionBean, ItemCollectionBinding> createAdapter() {
        return new SimpleAdapter<>(
                this,
                ItemCollectionBinding::inflate, // 传入条目 ViewBinding 渲染器
                (binding, data, position) -> {   // 传入强类型数据绑定逻辑
                    // 100% 强类型 ViewBinding 赋值，杜绝任何 findViewById 或额外 Holder 文件
                    binding.tvCollectionName.setText(data.getName());

                    // 如果需要，这里可以直接方便地为子 View 加点击事件
                    // binding.btnDelete.setOnClickListener(v -> { ... });
                }
        );
    }

    @Override
    protected RecyclerView attachRecyclerView() {
        return binding.rvCollectionList;
    }

    @Override
    protected View attachRefreshLayout() {
        return binding.swipeRefreshLayout;
    }

    @Override
    protected AcMyCollectionBinding initBinding(LayoutInflater inflater) {
        return AcMyCollectionBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        super.initView();
        binding.tvCollectionCount.setText("数据加载中...");
    }

    @Override
    protected void initData() {
        autoRefresh();
    }

    @Override
    public void onRefresh() {
        mCurrentPage = 1;
        // 模拟请求网络接口
        requestCollectionData(mCurrentPage);
    }

    @Override
    public void onLoadMore() {
        mCurrentPage++;
        // 模拟请求分页网络接口
        requestCollectionData(mCurrentPage);
    }



    private void requestCollectionData(int page) {
        binding.tvCollectionCount.setText("数据加载中...");

        binding.getRoot().postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;

            List<CollectionBean> resultList = getMockData(page);

            if (page == 1) {
                refreshListSuccess(resultList);
                binding.tvCollectionCount.setText("当前共收藏了 " + resultList.size() + " 个宝贝");
            } else {
                // 如果最多只有5页，当 page == 5 时，代表这已经是最后一页了，后面没有了 (hasMore = false)
                boolean hasMore = page < 5;

                loadMoreSuccess(resultList, hasMore);
                binding.tvCollectionCount.setText("当前共收藏了 " + (mAdapter.getItemCount()) + " 个宝贝");
            }
        }, 500);
    }

    // 模拟数据源
    private List<CollectionBean> getMockData(int page) {
        List<CollectionBean> list = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            list.add(new CollectionBean("宝贝Item " + ((page - 1) * 15 + i)));
        }
        return list;
    }

}
