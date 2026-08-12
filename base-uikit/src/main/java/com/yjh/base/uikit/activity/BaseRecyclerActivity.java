package com.yjh.base.uikit.activity;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import com.yjh.base.uikit.R;
import com.yjh.base.uikit.adapter.SimpleAdapter;
import com.yjh.base.uikit.controller.PagingController;
import com.yjh.base.uikit.listener.IRefreshListener;
import com.yjh.base.uikit.controller.StateController;
import com.yjh.base.uikit.controller.SwipeRefreshController;
import com.yjh.base.uikit.decoration.SpaceItemDecoration;
import java.util.List;

/**
 * 在 BaseActivity 基础上增加了 Controller 和一些抽象方法
 * Created by jiahui on 2026/07/14
 */
public abstract class BaseRecyclerActivity<T,VB extends ViewBinding> extends BaseActivity<VB>
        implements PagingController.OnPagingListener{

    protected RecyclerView mRecyclerView;
    protected SimpleAdapter<T, ? extends ViewBinding> mAdapter;
    protected StateController mStateController;
    protected PagingController mPagingController;
    protected SwipeRefreshController mRefreshController;
    private int mDefaultSpace = 16;

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public SimpleAdapter<T, ? extends ViewBinding> getAdapter() {
        return mAdapter;
    }

    public PagingController getPagingController() {
        return mPagingController;
    }

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
                if (mRecyclerView == null || mRecyclerView.getParent() == null) return;

                ViewGroup oldParent = (ViewGroup) mRecyclerView.getParent();
                int childIndex = oldParent.indexOfChild(mRecyclerView);
                ViewGroup.LayoutParams oldParams = mRecyclerView.getLayoutParams();

                // 1. 创建一个通用的容器，充当保护罩
                FrameLayout wrapperContainer = new FrameLayout(this);
                // 设置保底高度 (260dp)，确保列表为空时缺省页有足够的展示空间，且能在卡片内居中
                int minHeightPx = (int) (260 * getResources().getDisplayMetrics().density);
                wrapperContainer.setMinimumHeight(minHeightPx);

                // 2. 将 RecyclerView 从原父容器中移除，移入 wrapperContainer
                oldParent.removeView(mRecyclerView);
                FrameLayout.LayoutParams rvParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                wrapperContainer.addView(mRecyclerView, rvParams);

                // 3. 动态创建缺省页 ViewStub 放入 wrapperContainer
                ViewStub emptyStub = new ViewStub(this);
                emptyStub.setLayoutResource(R.layout.uikit_view_state_empty);

                ViewStub errorStub = new ViewStub(this);
                errorStub.setLayoutResource(R.layout.uikit_view_state_error);

                FrameLayout.LayoutParams stubParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

                wrapperContainer.addView(emptyStub, stubParams);
                wrapperContainer.addView(errorStub, stubParams);

                // 4. 把包装好的 wrapperContainer 插回原父容器的对应位置
                oldParent.addView(wrapperContainer, childIndex, oldParams);

                // 5. 将 ViewStub 注册给 StateController
                mStateController.setEmptyViewStub(emptyStub);
                mStateController.setErrorViewStub(errorStub);
            }
            registerController("state_controller", mStateController);

            // 3. 初始化分页
            mPagingController = new PagingController(this, this);
            int footerBgRes = setFooterBackgroundColorRes();
            if (footerBgRes != 0) {
                mPagingController.setFooterBackgroundColorRes(footerBgRes);
                mPagingController.setFooterBackgroundColorRes(footerBgRes);
                registerController("loadMore_controller", mPagingController);
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

    public void refreshListSuccess(List<T> list) {
        refreshComplete();
        if (mPagingController != null) {
            mPagingController.refreshSuccess(list);
        } else if (mAdapter != null) {
            mAdapter.setList(list);
        }

        if (mStateController != null) {
            mStateController.handleData(list);
        }
    }

    public void refreshListFailed(String msg) {
        refreshComplete();
        if (mAdapter == null || mAdapter.getItemCount() == 0) {
            if (mStateController != null) mStateController.showError();
        } else {
            showError(msg);
        }
    }

    public void loadMoreSuccess(List<T> list, boolean hasMore) {
        if (mPagingController != null) {
            mPagingController.loadMoreSuccess(list);
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
    public int getPageSize() {
        return 15;
    }

    @Override
    public String getEndFooterText() {
        return PagingController.OnPagingListener.super.getEndFooterText();
    }

    @Override
    public void onLoadMore(int page, int pageSize) {

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

    protected RecyclerView.LayoutManager getLayoutManager() { return new LinearLayoutManager(this); }

    protected boolean shouldAddDefaultSpaceDecoration() { return true; }

    public void autoRefresh() {
        if (mRefreshController != null) mRefreshController.autoRefresh();
    }

    public void showContent() { if (mStateController != null) mStateController.showContent(); }

    public void refreshComplete() {
        if (mRefreshController != null) mRefreshController.finishRefresh();
    }

    protected int setFooterBackgroundColorRes() {
        return 0;
    }

    protected boolean isSupportPaging(){
        return false;
    }

}
