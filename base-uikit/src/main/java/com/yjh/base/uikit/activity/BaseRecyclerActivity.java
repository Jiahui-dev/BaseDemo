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
import com.yjh.base.uikit.controller.StateController;
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
    protected StateController mStateController;
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

            mStateController = new StateController(this, mRecyclerView);

            mountViewStub(mStateController);

            registerController("state_controller", mStateController);

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

        if (mAdapter != null) {
            mAdapter.setList(list);
        }

        if (mPagingController != null) {
            mPagingController.refreshSuccess(list, hasMore);
        }

        if (mStateController != null) {
            mStateController.handleData(list);
        }
    }

    public void refreshListFailed(String msg) {

        mRefreshController.finishRefresh();

        if (mAdapter == null || mAdapter.getItemCount() == 0) {
            if (mStateController != null) mStateController.showError();
        } else {
            showError(msg);
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
     * 注入缺省页遮罩
     */
    private void mountViewStub(StateController stateController) {
        View targetView = attachRefreshLayout();
        if (targetView == null) {
            targetView = mRecyclerView;
        }

        ViewGroup parent = (ViewGroup) targetView.getParent();
        if (parent == null) return;

        FrameLayout maskContainer = new FrameLayout(this);

        ViewStub emptyStub = new ViewStub(this);
        emptyStub.setLayoutResource(R.layout.uikit_view_state_empty);

        ViewStub errorStub = new ViewStub(this);
        errorStub.setLayoutResource(R.layout.uikit_view_state_error);

        FrameLayout.LayoutParams stubParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        maskContainer.addView(emptyStub, stubParams);
        maskContainer.addView(errorStub, stubParams);

        // 2. 将 ViewStub 正确注册给 StateController
        stateController.setEmptyViewStub(emptyStub);
        stateController.setErrorViewStub(errorStub);

        // 3. 组装 View 树
        ViewGroup.LayoutParams maskParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        if (parent instanceof FrameLayout || parent instanceof androidx.constraintlayout.widget.ConstraintLayout) {
            parent.addView(maskContainer, maskParams);
        } else {
            int targetIndex = parent.indexOfChild(targetView);
            ViewGroup.LayoutParams targetParams = targetView.getLayoutParams();

            parent.removeView(targetView);

            FrameLayout wrapper = new FrameLayout(this);
            // 确保 targetView 居下，maskContainer 居上
            wrapper.addView(targetView, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            wrapper.addView(maskContainer, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            parent.addView(wrapper, targetIndex, targetParams);
        }
    }

}