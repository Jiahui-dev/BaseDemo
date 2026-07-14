package com.yjh.base.uikit.decoration;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by jiahui on 2026/02/09
 */
public class SpaceItemDecoration extends RecyclerView.ItemDecoration {

    private final int space;//间距大小（px）

    public SpaceItemDecoration(int spaceDp) {
        this.space = spaceDp;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

//        //不是第一个 Item 时，给顶部添加间距
//        if (parent.getChildAdapterPosition(view) > 0) {
//            outRect.top=space;
//        }
        outRect.top = space;
    }
}
