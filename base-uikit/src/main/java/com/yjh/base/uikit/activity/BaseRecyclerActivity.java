package com.yjh.base.uikit.activity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import com.yjh.base.uikit.adapter.SimpleAdapter;
import com.yjh.base.uikit.controller.IRefreshListener;
import com.yjh.base.uikit.controller.LoadMoreController;
import com.yjh.base.uikit.controller.StateController;
import com.yjh.base.uikit.controller.SwipeRefreshController;
import com.yjh.base.uikit.decoration.SpaceItemDecoration;
import com.yjh.base.uikit.databinding.UikitLayoutBaseRecyclerBinding;
import java.util.List;

/**
 *
 * 默认使用标准单列表布局 LayoutBaseRecyclerBinding。
 * 如果业务需要大改，直接通过泛型传入自定义的 ViewBinding 即可
 * Created by jiahui on 2026/07/14
 */
public abstract class BaseRecyclerActivity<T,VB extends ViewBinding> extends BaseActivity<VB> {

    protected RecyclerView mRecyclerView;
    protected SimpleAdapter<T, ? extends ViewBinding> mAdapter;
    protected StateController mStateController;
    protected LoadMoreController mLoadMoreController;
    protected SwipeRefreshController mRefreshController;
    private int mDefaultSpace = 16;

    @Override
    protected void initView() {
        super.initView();

        // 1. 获取列表控件
        mRecyclerView = getRecyclerView();
        if (mRecyclerView != null) {
            mRecyclerView.setLayoutManager(getLayoutManager());
            if (shouldAddDefaultSpaceDecoration()) {
                mRecyclerView.addItemDecoration(new SpaceItemDecoration(mDefaultSpace));
            }

            mAdapter = createAdapter();
            mRecyclerView.setAdapter(mAdapter);

            // 2. 初始化多状态页
            mStateController = new StateController(this, mRecyclerView);
            initStatusViewStub(mStateController);
            registerController("state_controller", mStateController);

            // 3. 初始化分页
            mLoadMoreController = new LoadMoreController(mRecyclerView, mAdapter);
            if (isSupportLoadMore()) {
                mLoadMoreController.setOnLoadMoreListener(this::onLoadMore);
                registerController("loadMore_controller", mLoadMoreController);
            }
        }
    }

    @Override
    protected void onRegisterControllers() {
        super.onRegisterControllers();
        if (getSwipeRefreshLayoutId() != 0) {
            mRefreshController = new SwipeRefreshController(this, getSwipeRefreshLayoutId());
            // 如果子类本身实现了 IRefreshListener，直接绑定
            if (this instanceof IRefreshListener) {
                mRefreshController.setOnRefreshListener((IRefreshListener) this);
            }
            registerController("refresh_controller", mRefreshController);
        }
    }

    /**
     * 默认寻找默认单列表布局里的 contentView
     * 如果子类魔改了布局，重写此方法返回自定义的 RecyclerView 即可
     */
    protected RecyclerView getRecyclerView() {
        if (binding instanceof UikitLayoutBaseRecyclerBinding) {
            return ((UikitLayoutBaseRecyclerBinding) binding).contentView;
        }
        return null;
    }

    /**
     * 默认寻找默认单列表布局里的刷新控件 ID
     */
    protected int getSwipeRefreshLayoutId() {
        if (binding instanceof UikitLayoutBaseRecyclerBinding) {
            return ((UikitLayoutBaseRecyclerBinding) binding).swipeRefresh.getId();
        }
        return 0;
    }

    protected void initStatusViewStub(StateController stateController) {
        if (binding instanceof UikitLayoutBaseRecyclerBinding) {
            UikitLayoutBaseRecyclerBinding defaultBinding = (UikitLayoutBaseRecyclerBinding) binding;
            stateController.setEmptyViewStub(defaultBinding.emptyStub);
            stateController.setErrorViewStub(defaultBinding.errorStub);
        }
    }

    protected abstract SimpleAdapter<T, ? extends ViewBinding> createAdapter();

    public void onLoadMore() {}

    public void autoRefresh() {
        if (mRefreshController != null) mRefreshController.autoRefresh();
    }

    public void refreshComplete() {
        if (mRefreshController != null) mRefreshController.finishRefresh();
    }

    public void refreshListSuccess(List<T> list) {
        refreshComplete();
        if (mAdapter != null) mAdapter.setList(list);
        if (mStateController != null) mStateController.handleData(list);
        if (mLoadMoreController != null) {
            mLoadMoreController.reset(isSupportLoadMore(), getEndFooterText());
        }
    }

    public void refreshListFailed(String msg) {
        refreshComplete();
        if (mAdapter == null || mAdapter.getItemCount() == 0) {
            if (mStateController != null) mStateController.showError();
        } else {
            if (mLoadMoreController != null) {
                mLoadMoreController.loadMoreFail();
            } else {
                showError(msg);
            }
        }
    }

    public void loadMoreSuccess(List<T> list, boolean hasMore) {
        if (mAdapter != null) mAdapter.addList(list);
        if (mLoadMoreController != null) mLoadMoreController.loadMoreSuccess(hasMore);
    }

    public void loadMoreFailed() {
        if (mLoadMoreController != null) mLoadMoreController.loadMoreFail();
    }

    protected RecyclerView.LayoutManager getLayoutManager() { return new LinearLayoutManager(this); }

    protected String getEndFooterText() { return "已经到底啦"; }

    protected boolean shouldAddDefaultSpaceDecoration() { return true; }

    protected boolean isSupportLoadMore() { return true; }

    public void showContent() { if (mStateController != null) mStateController.showContent(); }

}
