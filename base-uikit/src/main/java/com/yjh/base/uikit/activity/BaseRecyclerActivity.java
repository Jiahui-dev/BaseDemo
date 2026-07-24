package com.yjh.base.uikit.activity;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import com.yjh.base.uikit.R;
import com.yjh.base.uikit.adapter.SimpleAdapter;
import com.yjh.base.uikit.controller.IRefreshListener;
import com.yjh.base.uikit.controller.LoadMoreController;
import com.yjh.base.uikit.controller.StateController;
import com.yjh.base.uikit.controller.SwipeRefreshController;
import com.yjh.base.uikit.decoration.SpaceItemDecoration;
import java.util.List;

/**
 * 在 BaseActivity 基础上增加了 Controller 和一些抽象方法
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

        mRecyclerView = attachRecyclerView();
        if (mRecyclerView != null) {
            mRecyclerView.setLayoutManager(getLayoutManager());
            if (shouldAddDefaultSpaceDecoration()) {
                mRecyclerView.addItemDecoration(new SpaceItemDecoration(mDefaultSpace));
            }

            mAdapter = createAdapter();
            mRecyclerView.setAdapter(mAdapter);

            // 2. 初始化多状态页
            mStateController = new StateController(this, mRecyclerView);
            // 优先尝试子类的定制 ViewStub，如果没有，基类动态注入全局默认兜底
            if (!initStatusViewStub(mStateController)) {
                initStatusViews(mStateController);
            }
            registerController("state_controller", mStateController);

            // 3. 初始化分页
            mLoadMoreController = new LoadMoreController(mRecyclerView, mAdapter);
            int footerBgRes = setFooterBackgroundColorRes();
            if (footerBgRes != 0) {
                mLoadMoreController.setFooterBackgroundColorRes(footerBgRes);
            }
            if (isSupportLoadMore()) {
                mLoadMoreController.setOnLoadMoreListener(this::onLoadMore);
                registerController("loadMore_controller", mLoadMoreController);
            }
        }
    }

    @Override
    protected void onRegisterControllers() {
        super.onRegisterControllers();
        View refreshView = attachRefreshLayout();
        if (refreshView != null) {
            mRefreshController = new SwipeRefreshController(this, refreshView);
            // 如果子类本身实现了 IRefreshListener，直接绑定
            if (this instanceof IRefreshListener) {
                mRefreshController.setOnRefreshListener((IRefreshListener) this);
            }
            registerController("refresh_controller", mRefreshController);
        }
    }

    /**
     * 挂载子类布局中的 RecyclerView 实例
     */
    protected abstract RecyclerView attachRecyclerView();

    /**
     * 挂载子类布局中的刷新布局实例（如 SwipeRefreshLayout / SmartRefreshLayout）
     */
    protected View attachRefreshLayout() {
        return null;
    }

    protected abstract SimpleAdapter<T, ? extends ViewBinding> createAdapter();

    protected boolean initStatusViewStub(StateController stateController) {
        return false;
    }

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
            // 健壮性修复：如果第一页数据为空，或者数量特别少（比如少于10条），直接判定为没有更多，避免滑不动还显示加载
            boolean hasMore = isSupportLoadMore() && list != null && list.size() >= getPageSize();
            mLoadMoreController.updateLoadingState(hasMore, getEndFooterText());
        }
    }

    protected int getPageSize() {
        return 15;
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
        if (mLoadMoreController != null) {
            mLoadMoreController.loadMoreSuccess(hasMore);
        }
        if (mAdapter != null) {
            mAdapter.addList(list);
        }
    }

    public void loadMoreFailed() {
        if (mLoadMoreController != null) mLoadMoreController.loadMoreFail();
    }

    protected RecyclerView.LayoutManager getLayoutManager() { return new LinearLayoutManager(this); }

    protected String getEndFooterText() { return "已经到底啦"; }

    protected boolean shouldAddDefaultSpaceDecoration() { return true; }

    protected boolean isSupportLoadMore() { return true; }

    public void showContent() { if (mStateController != null) mStateController.showContent(); }

    /**
     * 动态注入全局默认的缺省页兜底
     */
    private void initStatusViews(StateController stateController) {
        if (mRecyclerView == null || mRecyclerView.getParent() == null) return;

        ViewGroup parent = (ViewGroup) mRecyclerView.getParent();

        // 动态创建空状态 ViewStub 并添加到父布局中
        ViewStub emptyStub = new ViewStub(this);
        // R.layout.uikit_view_state_empty 为你底层 common/uikit 模块里的通用标准空布局
        emptyStub.setLayoutResource(R.layout.uikit_view_state_empty);
        parent.addView(emptyStub, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        stateController.setEmptyViewStub(emptyStub);

        // 动态创建错误状态 ViewStub 并添加到父布局中
        ViewStub errorStub = new ViewStub(this);
        // R.layout.uikit_view_state_error 为你底层的通用标准错误布局
        errorStub.setLayoutResource(R.layout.uikit_view_state_error);
        parent.addView(errorStub, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        stateController.setErrorViewStub(errorStub);
    }

    protected int setFooterBackgroundColorRes() {
        return 0;
    }

}
