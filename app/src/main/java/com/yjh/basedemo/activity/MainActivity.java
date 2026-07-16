package com.yjh.basedemo.activity;

import android.view.LayoutInflater;

import androidx.recyclerview.widget.RecyclerView;
import com.yjh.base.uikit.activity.BaseRecyclerActivity;
import com.yjh.base.uikit.adapter.BaseRecyclerAdapter;
import com.yjh.base.uikit.controller.IRefreshListener;
import com.yjh.basedemo.adapter.MyCollectionAdapter;
import com.yjh.basedemo.databinding.AcMyCollectionBinding;
import com.yjh.basedemo.model.bean.CollectionBean;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseRecyclerActivity<CollectionBean, AcMyCollectionBinding> implements IRefreshListener {

    private int mCurrentPage = 1;

    @Override
    protected BaseRecyclerAdapter<CollectionBean> createAdapter() {
        return new MyCollectionAdapter(this);
    }

    @Override
    protected AcMyCollectionBinding onBindingInflate(LayoutInflater inflater) {
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

    @Override
    protected RecyclerView getRecyclerView() {
        return binding.rvCollectionList;
    }

    @Override
    protected int getSwipeRefreshLayoutId() {
        return binding.swipeRefreshLayout.getId();
    }

    private void requestCollectionData(int page) {
        // 假设这里去异步调用了网络请求...
        boolean isSuccess = true;
        List<CollectionBean> resultList = getMockData(page);

        if (page == 1) {
            // 🌟 下拉刷新成功：数据喂给基类，基类自动刷新列表、关闭刷新、计算缺省页状态
            refreshListSuccess(resultList);
            // 更新顶部魔改卡片的数据
            binding.tvCollectionCount.setText("当前共收藏了 " + resultList.size() + " 个宝贝");
        } else {
            // 🌟 上拉加载成功：数据喂给基类，第二个参数传入是否有下一页（比如总共5页）
            boolean hasMore = page < 5;
            binding.tvCollectionCount.setText("当前共收藏了 " + resultList.size()*page + " 个宝贝aaa");
            loadMoreSuccess(resultList, hasMore);
        }
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
