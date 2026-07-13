package com.yjh.base.utils.util;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by jiahui on 2025/12/30
 */
public class FileUtil {

    // 将Uri选中的图片复制到APP私有目录，并返回绝对路径
    public static String copyImageToAppDir(Context context, Uri uri) {
        try {
            // 1. 创建输入流（读取相册图片）
            InputStream inputStream = context.getContentResolver().openInputStream(uri);

            // 2. 创建文件名 (使用时间戳防止重名)
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "IMG_" + timeStamp + ".jpg";

            // 3. 创建目标文件 (存放在 app的 files/images 目录下)
            File dir = new File(context.getExternalFilesDir(null), "images");
            if (!dir.exists()) dir.mkdirs();
            File destFile = new File(dir, fileName);

            // 4. 开始复制
            OutputStream outputStream = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // 5. 关闭流
            outputStream.close();
            inputStream.close();

            // 6. 返回新文件的绝对路径 (存数据库就存这个!)
            return destFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}