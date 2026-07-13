package com.yjh.base.utils.util;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import java.io.File;

/**
 * Created by jiahui on 2025/12/30
 */
public class ImageUtils {

    /**
     * 加载图片核心方法
     *
     * @param context       上下文
     * @param imgView       图片控件
     */
    public static void loadImage(Context context, ImageView imgView, String pathOrResName) {
        if (pathOrResName == null || pathOrResName.isEmpty()) {
            // 设置一个默认图片，防止空白
            // imgView.setImageResource(R.drawable.default_image);
            return;
        }

        // 1. 先判断是不是本地文件路径（以 / 开头，且文件存在）
        if (pathOrResName.startsWith("/")) {
            File file = new File(pathOrResName);
            if (file.exists()) {
                // 是本地文件，直接加载
                imgView.setImageURI(Uri.fromFile(file));
                return;
            }
        }

        // 2. 如果不是路径，或者文件不存在，则认为是 Drawable 资源名称
        // 获取资源ID
        int imgResourceId = context.getResources().getIdentifier(pathOrResName, "drawable", context.getPackageName());
        if (imgResourceId != 0) {
            imgView.setImageResource(imgResourceId);
        } else {
            // imgView.setImageResource(R.drawable.ic_error);
        }
    }
}
