package com.yjh.base.uikit.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.List;

/**
 * ViewPager2 强制使用 FragmentStateAdapter
 * Created by jiahui on 2026/01/30
 */

public class CommonFragmentAdapter extends FragmentStateAdapter {

    private final List<Fragment> mFragmentList;

    public CommonFragmentAdapter(@NonNull FragmentActivity fragmentActivity, List<Fragment> fragmentList) {
        super(fragmentActivity);
        this.mFragmentList = fragmentList;
    }

    // 构造函数（用于 Fragment 嵌套）
    // 这里的 fragment 参数就是 BaseTabFragment
    public CommonFragmentAdapter(@NonNull Fragment fragment, List<Fragment> fragmentList) {
        super(fragment); // 内部会自动使用 getChildFragmentManager()
        this.mFragmentList = fragmentList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return mFragmentList == null ? 0 : mFragmentList.size();
    }
}
