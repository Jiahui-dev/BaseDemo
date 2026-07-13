package com.yjh.base.uikit.adapter.holder;

import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yjh.base.uikit.R;

/**
 * 避免每次写 Adapter 都要写一个内部类 ViewHolder，并提供链式调用
 * Created by jiahui on 2026/01/28
 */

public class BaseViewHolder extends RecyclerView.ViewHolder{

    // 缓存 View，防止每次 findViewById
    private final SparseArray<View> mViews;

    public BaseViewHolder(@NonNull View itemView) {
        super(itemView);
        this.mViews=new SparseArray<>();
    }

    public <T extends View> T getView(@IdRes int viewId){
        View view=mViews.get(viewId);
        if (view == null) {
            view=itemView.findViewById(viewId);
            mViews.put(viewId,view);
        }
        return (T) view;
    }

    /**
     * 设置文本
     */
    public BaseViewHolder setText(@IdRes int viewId,CharSequence text){
        TextView tv=getView(viewId);
        if(tv!=null){
            tv.setText(text);
        }
        return this;//返回自己，支持链式调用
    }

    /**
     * 设置图片资源
     */
    public BaseViewHolder setImageResource(@IdRes int viewId, int resId) {
        ImageView iv = getView(viewId);
        if (iv != null) {
            iv.setImageResource(resId);
        }
        return this;
    }

    public BaseViewHolder setImageUrl(@IdRes int viewId, String url) {
        return setImageUrl(viewId, url, R.drawable.ic_launcher_background);
    }

    /**
     * 支持自定义加载失败的占位图
     * @param errorResId 加载失败时显示的图片资源ID (R.drawable.xxx)
     */
    public BaseViewHolder setImageUrl(@IdRes int viewId, String url, @DrawableRes int errorResId) {
        ImageView iv = getView(viewId);
        if (iv != null) {
            Glide.with(iv.getContext())
                    .load(url)
                    .override(300, 300) // 保持性能优化
                    .placeholder(errorResId) // 加载过程中显示的图，通常和失败图一样，防止闪烁
                    .error(errorResId)       // 加载失败时显示的图
                    .fallback(errorResId)    // url为null时显示的图
                    .into(iv);
        }
        return this;
    }

    /**
     * 设置点击事件
     */
    public BaseViewHolder setOnClickListener(@IdRes int viewId, View.OnClickListener listener) {
        View view = getView(viewId);
        if (view != null) {
            view.setOnClickListener(listener);
        }
        return this;
    }

    /**
     * 设置 RatingBar 的评分
     * 支持 float 或 int
     */
    public BaseViewHolder setRating(@IdRes int viewId, float rating) {
        // getView 返回的是 View，这里强转成 RatingBar
        // 因为泛型 T extends View，所以只要接收方声明为 RatingBar 就会自动强转
        RatingBar ratingBar = getView(viewId);
        if (ratingBar != null) {
            ratingBar.setRating(rating);
        }
        return this; // 返回自己，支持链式调用
    }

    /**
     * 设置 View 的可见性 (VISIBLE 或 GONE)
     * @param viewId 控件ID
     * @param visible true 为 VISIBLE, false 为 GONE
     */
    public BaseViewHolder setVisible(@IdRes int viewId, boolean visible) {
        View view = getView(viewId);
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        return this;
    }

    /**
     * 设置 View 的可见性 (VISIBLE 或 INVISIBLE)
     * 有时候需要保留占位空间，可以用这个
     */
    public BaseViewHolder setInVisible(@IdRes int viewId, boolean invisible) {
        View view = getView(viewId);
        if (view != null) {
            view.setVisibility(invisible ? View.INVISIBLE : View.VISIBLE);
        }
        return this;
    }

    /**
     * 专门的隐藏方法 (强制设置成 GONE)
     */
    public BaseViewHolder setGone(@IdRes int viewId, boolean isGone) {
        View view = getView(viewId);
        if (view != null) {
            view.setVisibility(isGone ? View.GONE : View.VISIBLE);
        }
        return this;
    }


}
