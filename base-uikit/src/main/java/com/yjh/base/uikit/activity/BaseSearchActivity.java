package com.yjh.base.uikit.activity;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.recyclerview.widget.RecyclerView;

import com.yjh.base.uikit.R;
import com.yjh.base.uikit.databinding.UikitAcBaseSearchBinding;
import com.yjh.base.uikit.databinding.UikitItemHistoryBinding;
import com.yjh.base.uikit.listener.IRefreshListener;
import com.yjh.base.uikit.widget.dialog.center.CommonDialog;
import com.yjh.base.utils.util.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 搜索专用基类
 * Created by jiahui on 2026/08/10
 */
public abstract class BaseSearchActivity<T> extends BaseRecyclerActivity<T, UikitAcBaseSearchBinding>
        implements IRefreshListener {

    protected UikitAcBaseSearchBinding uikitAcBaseSearchBinding;
    protected String mCurrentKeyword = "";

    @Override
    protected UikitAcBaseSearchBinding initBinding(LayoutInflater inflater) {
        uikitAcBaseSearchBinding = UikitAcBaseSearchBinding.inflate(inflater);
        return uikitAcBaseSearchBinding;
    }

    @Override
    protected RecyclerView attachRecyclerView() {
        return uikitAcBaseSearchBinding.contentView;
    }

    @Override
    protected View attachRefreshLayout() {
        return uikitAcBaseSearchBinding.swipeRefresh;
    }

    @Override
    public void initView() {
        super.initView();

        // 绑定搜索结果点击事件
        if (mAdapter != null) {
            mAdapter.setOnItemClickListener((view, viewId, position, data) -> {
                onSearchResultClick(data, position);
            });
        }
    }

    @Override
    public void initListener() {
        super.initListener();

        // 返回
        uikitAcBaseSearchBinding.ivBack.setOnClickListener(v -> finish());

        // 清空输入框
        uikitAcBaseSearchBinding.ivClear.setOnClickListener(v -> {
            uikitAcBaseSearchBinding.etSearch.setText("");
            showKeyboard(uikitAcBaseSearchBinding.etSearch);
            mCurrentKeyword = "";
            showHistoryView();
        });

        // 清空历史记录弹窗
        uikitAcBaseSearchBinding.ivDeleteHistory.setOnClickListener(v -> {
            CommonDialog dialog = new CommonDialog();
            dialog.setTitle("提示");
            dialog.setContent("确认删除历史记录吗？");
            dialog.setOnConfirmListener(result -> {
                clearSearchHistory();
                loadAndShowHistory();
                dialog.dismiss();
            });
            dialog.setOnCancelListener(result -> dialog.dismiss());
            dialog.show(getSupportFragmentManager(), "dialog_history");
        });

        // 文本输入监听
        uikitAcBaseSearchBinding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean hasContent = s.length() > 0;
                uikitAcBaseSearchBinding.ivClear.setVisibility(hasContent ? View.VISIBLE : View.GONE);

                // 输入框被删空时，自动切换回历史记录界面
                if (!hasContent) {
                    mCurrentKeyword = "";
                    showHistoryView();
                }
            }
        });

        // 软键盘搜索按键
        uikitAcBaseSearchBinding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // 点击搜索按钮
        uikitAcBaseSearchBinding.tvActionSearch.setOnClickListener(v -> performSearch());
    }

    @Override
    public void initData() {
        super.initData();
        uikitAcBaseSearchBinding.etSearch.postDelayed(() -> showKeyboard(uikitAcBaseSearchBinding.etSearch), 200);
        loadAndShowHistory();
    }

    /**
     * 触发搜索
     */
    protected void performSearch() {
        String keyword = uikitAcBaseSearchBinding.etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) return;

        hideKeyboard();
        hideHistoryView();

        mCurrentKeyword = keyword;
        saveSearchHistory(keyword);

        autoRefresh();
    }

    @Override
    public void onRefresh() {
        if (TextUtils.isEmpty(mCurrentKeyword)) {
            finishRefresh();
            return;
        }
        doSearch(mCurrentKeyword);
    }

    @Override
    public void onLoadMore() {
        if (TextUtils.isEmpty(mCurrentKeyword)) {
            loadMoreFailed();
            return;
        }
        doLoadMore(mCurrentKeyword);
    }

    protected abstract void doSearch(String keyword);

    protected void doLoadMore(String keyword) {

    }

    protected void loadAndShowHistory() {
        List<String> history = getSearchHistory();

        if (history.isEmpty()) {
            uikitAcBaseSearchBinding.nsvHistory.setVisibility(View.GONE);
            return;
        }

        if (TextUtils.isEmpty(uikitAcBaseSearchBinding.etSearch.getText())) {
            uikitAcBaseSearchBinding.nsvHistory.setVisibility(View.VISIBLE);
        }

        uikitAcBaseSearchBinding.flowHistory.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String keyword : history) {
            UikitItemHistoryBinding itemBinding = UikitItemHistoryBinding.inflate(
                    inflater,
                    uikitAcBaseSearchBinding.flowHistory,
                    false
            );

            itemBinding.tvName.setText(keyword);

            itemBinding.getRoot().setOnClickListener(v -> {
                uikitAcBaseSearchBinding.etSearch.setText(keyword);
                uikitAcBaseSearchBinding.etSearch.setSelection(keyword.length());
                hideKeyboard();
                hideHistoryView();
                mCurrentKeyword = keyword;
                autoRefresh();
            });

            uikitAcBaseSearchBinding.flowHistory.addView(itemBinding.getRoot());
        }
    }

    /**
     * 保存搜索历史（去重 + 最多保留10条 + 倒序排列）
     */
    private void saveSearchHistory(String keyword) {
        List<String> historyList = getSearchHistory();

        // 安全移除重复项
        historyList.remove(keyword);
        // 插到最前面
        historyList.add(0, keyword);

        // 控制最大条数
        if (historyList.size() > 10) {
            historyList = new ArrayList<>(historyList.subList(0, 10));
        }

        // 使用 String.join 高效拼接字符串
        String historyString = String.join(",", historyList);
        SharedPreferencesUtils.setParam(this, getHistoryKey(), historyString);
    }

    /**
     * 读取搜索历史（修复 Arrays.asList 不可变集合 Bug）
     */
    protected List<String> getSearchHistory() {
        String oldHistory = SharedPreferencesUtils.getParam(this, getHistoryKey(), "");
        if (TextUtils.isEmpty(oldHistory)) {
            return new ArrayList<>();
        }
        // 必须用 new ArrayList包裹，否则返回的 List 不支持 remove/add 操作！
        return new ArrayList<>(Arrays.asList(oldHistory.split(",")));
    }

    protected void clearSearchHistory() {
        SharedPreferencesUtils.removeParam(this, getHistoryKey());
    }

    protected void showHistoryView() {
        uikitAcBaseSearchBinding.nsvHistory.setVisibility(View.VISIBLE);
        uikitAcBaseSearchBinding.swipeRefresh.setVisibility(View.GONE);
        loadAndShowHistory();
    }

    protected void hideHistoryView() {
        uikitAcBaseSearchBinding.nsvHistory.setVisibility(View.GONE);
        uikitAcBaseSearchBinding.swipeRefresh.setVisibility(View.VISIBLE);
    }

    protected void showKeyboard(EditText view) {
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
    }

    protected void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    protected String getHistoryKey() {
        return "common_search_history";
    }

    protected void onSearchResultClick(T item, int position) {
    }

    @Override
    protected View getTopView() {
        return uikitAcBaseSearchBinding.titleBar;
    }

    @Override
    protected int getStatusBarColor() {
        return R.color.uikit_white;
    }
}