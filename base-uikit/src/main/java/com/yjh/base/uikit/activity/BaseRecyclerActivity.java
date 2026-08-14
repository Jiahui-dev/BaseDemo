package com.yjh.base.uikit.activity;

import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.yjh.base.uikit.R;
import com.yjh.base.uikit.adapter.SimpleAdapter;
import com.yjh.base.uikit.controller.PagingController;
import com.yjh.base.uikit.controller.SwipeRefreshController;
import com.yjh.base.uikit.decoration.SpaceItemDecoration;
import com.yjh.base.uikit.listener.IRefreshListener;

import java.util.List;

/**
 * 多状态与分页列表基类
 * Created by jiahui on 2026/07/14
 */
public abstract class BaseRecyclerActivity<T, VB extends ViewBinding> extends BaseActivity<VB>
        implements PagingController.OnPagingListener {

    protected RecyclerView mRecyclerView;
    protected SimpleAdapter<T, ? extends ViewBinding> mAdapter;
    protected PagingController mPagingController;
    protected SwipeRefreshController mRefreshController;

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public SimpleAdapter<T, ? extends ViewBinding> getAdapter() {
        return mAdapter;
    }

    public PagingController getPagingController() {
        return mPagingController;
    }

    protected RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(this);
    }

    @Override
    protected void onRegisterControllers() {
        super.onRegisterControllers();
        View refreshView = attachRefreshLayout();
        if (refreshView != null) {
            mRefreshController = new SwipeRefreshController(this, refreshView);
            if (this instanceof IRefreshListener) {
                mRefreshController.setOnRefreshListener((IRefreshListener) this);
            }
            registerController("refresh_controller", mRefreshController);
        }
    }

    @Override
    protected void initView() {
        super.initView();

        mRecyclerView = attachRecyclerView();
        if (mRecyclerView != null) {
            mRecyclerView.setLayoutManager(getLayoutManager());
            mRecyclerView.addItemDecoration(new SpaceItemDecoration(setGap()));
            mAdapter = createAdapter();
            mRecyclerView.setAdapter(mAdapter);

            if (mAdapter != null) {
                // 1. 设置默认空布局（如果全局都用一套，直接在基类加载）
                View defaultEmptyView = LayoutInflater.from(this)
                        .inflate(getEmptyLayoutResId(), mRecyclerView, false);
                mAdapter.setEmptyView(defaultEmptyView);

                // 2. 设置默认错误布局
                View defaultErrorView = LayoutInflater.from(this)
                        .inflate(getErrorLayoutResId(), mRecyclerView, false);
                mAdapter.setErrorView(defaultErrorView);

                // 3. 默认点击错误页重试触发 autoRefresh()
                mAdapter.setOnRetryListener(this::autoRefresh);
            }

            mPagingController = new PagingController(this, this);
            int footerBgRes = setFooterBackgroundColorRes();
            if (footerBgRes != 0) {
                mPagingController.setFooterBackgroundColorRes(footerBgRes);
            }
            registerController("loadMore_controller", mPagingController);
        }
    }

    @Override
    public String getEndFooterText() {
        return PagingController.OnPagingListener.super.getEndFooterText();
    }

    public void refreshListSuccess(List<T> list, boolean hasMore) {
        if (mRefreshController != null) {
            mRefreshController.finishRefresh();
        }

        if (mPagingController != null) {
            mPagingController.refreshSuccess(list, hasMore);
        }

        if (mAdapter != null) {
            if (list == null || list.isEmpty()) {
                // 列表为空：更新数据源并通知 Adapter 渲染空页面 TYPE_EMPTY
                mAdapter.setList(null);
                mAdapter.showEmpty();
            } else {
                // 有数据：正常渲染列表 TYPE_CONTENT
                mAdapter.setList(list);
            }
        }

    }

    public void refreshListFailed(String msg) {
        if (mRefreshController != null) {
            mRefreshController.finishRefresh();
        }

        if (mAdapter != null) {
            // 如果当前列表完全没数据，展示错误缺省页 TYPE_ERROR
            if (mAdapter.getList() == null || mAdapter.getList().isEmpty()) {
                mAdapter.showError(msg);
            } else {
                // 如果本地已有列表数据，刷新失败仅弹 Toast 提示
                showError(msg);
            }
        }
    }

    public void loadMoreSuccess(List<T> list, boolean hasMore) {
        if (mPagingController != null) {
            mPagingController.loadMoreSuccess(list, hasMore);
        } else if (mAdapter != null) {
            mAdapter.addList(list);
        }
    }

    public void loadMoreFailed() {
        if (mPagingController != null) {
            mPagingController.loadMoreFailed();
        }
    }

    @Override
    public void onLoadMore() {

    }

    protected View attachRefreshLayout() {
        return null;
    }

    protected abstract RecyclerView attachRecyclerView();

    protected abstract SimpleAdapter<T, ? extends ViewBinding> createAdapter();

    protected int setGap() {
        return 16;
    }

    public void autoRefresh() {

        if (mPagingController != null) {
            mPagingController.hideFooter();
        }

        if (mAdapter != null) {
            mAdapter.resetState();
        }

        if (mRefreshController != null) {
            mRefreshController.autoRefresh();
        }

    }

    public void finishRefresh() {
        if (mRefreshController != null) mRefreshController.finishRefresh();
    }

    protected int setFooterBackgroundColorRes() {
        return 0;
    }

    protected boolean isSupportPaging() {
        return false;
    }

    /**
     * 默认空布局
     */
    protected int getEmptyLayoutResId() {
        return R.layout.uikit_view_state_empty;
    }

    /**
     * 默认错误布局
     */
    protected int getErrorLayoutResId() {
        return R.layout.uikit_view_state_error;
    }

}