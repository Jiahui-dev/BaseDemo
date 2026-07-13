package com.yjh.base.core.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.yjh.base.core.annotation.IntentParam;
import com.yjh.base.utils.util.LogUtils;
import java.lang.reflect.Field;

/**
 * Created by jiahui on 2026/07/11
 */
public class BaseRouter {

    private static final String TAG="BaseRouter";

    private BaseRouter(){}

    private static class InstanceHolder{
        private static final BaseRouter INSTANCE=new BaseRouter();
    }

    /**
     * 获取单例实例
     */
    public static BaseRouter getInstance(){
        return InstanceHolder.INSTANCE;
    }

    /**
     * 通过反射把Intent的值赋给了@IntentParam的字段
     */
    public void inject(Object target) {
        if (target == null) return;

        Bundle extras = getBundle(target);

        // 如果没有任何参数传入，直接结束
        if (extras == null) return;

        // 2. 执行统一的反射注入逻辑
        Field[] fields = target.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(IntentParam.class)) {
                IntentParam intentParam = field.getAnnotation(IntentParam.class);
                String key = intentParam.name().isEmpty() ? field.getName() : intentParam.name();

                if (extras.containsKey(key)) {
                    try {
                        field.setAccessible(true);
                        Object value = extras.get(key);
                        field.set(target, value);
                    } catch (Exception e) {
                        LogUtils.error(TAG, "参数注入失败: " + field.getName() + " -> " + e.getMessage());
                    }
                }
            }
        }
    }

    @Nullable
    private static Bundle getBundle(Object target) {
        Bundle extras = null;

        // 1. 根据传入的对象类型，自动抓取对应的数据源 Bundle
        if (target instanceof Activity) {
            Activity activity = (Activity) target;
            if (activity.getIntent() != null) {
                extras = activity.getIntent().getExtras();
            }
        } else if (target instanceof Fragment) {
            Fragment fragment = (Fragment) target;
            extras = fragment.getArguments(); // 优先读取 Fragment 自己的 arguments

            // 兜底策略：如果 Fragment 自己没传，则尝试读取它依附的 Activity 的 Intent 数据
            if (extras == null && fragment.getActivity() != null && fragment.getActivity().getIntent() != null) {
                extras = fragment.getActivity().getIntent().getExtras();
            }
        }
        return extras;
    }

    /**
     * 获取跳转建造者（传入强类型契约）
     */
    public <T extends Activity> Postcard<T> build(IRoutePath<T> routePath){
        if (routePath == null || routePath.getTargetClass() == null) {
            throw new IllegalArgumentException("路由目标Class不能为空！");
        }
        return new Postcard<>(routePath.getTargetClass());
    }

    public <T extends Activity> void navigation(Context context,Postcard<T> postcard){
        Class<T> targetClass=postcard.getTargetClass();

        Intent intent=new Intent(context,targetClass);
        intent.putExtras(postcard.getBundle());
        if (postcard.getFlags() != -1) {
            intent.setFlags(postcard.getFlags());
        }
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static class Postcard<T extends Activity>{
        private final Class<T> targetClass;
        private final Bundle bundle=new Bundle();
        private int flags=-1;

        public Postcard(Class<T> targetClass){
            this.targetClass=targetClass;
        }

        public Class<T> getTargetClass(){
            return targetClass;
        }

        public Bundle getBundle(){
            return bundle;
        }

        public int getFlags(){
            return flags;
        }

        public Postcard<T> with(Bundle extras){
            if (extras != null) {
                this.bundle.putAll(extras);
            }
            return this;
        }

        public Postcard<T> withString(String key,String value){
            bundle.putString(key,value);
            return this;
        }

        public Postcard<T> withInt(String key, int value) {
            bundle.putInt(key, value);
            return this;
        }

        public Postcard<T> withLong(String key, long value) {
            bundle.putLong(key, value);
            return this;
        }

        public Postcard<T> withDouble(String key, double value) {
            bundle.putDouble(key, value);
            return this;
        }

        public Postcard<T> withBoolean(String key, boolean value) {
            bundle.putBoolean(key, value);
            return this;
        }

        public Postcard<T> withFlags(int flags) {
            this.flags = flags;
            return this;
        }

        /**
         * 结束链式调用，发起跳转
         */
        public void navigation(Context context) {
            BaseRouter.getInstance().navigation(context, this);
        }
    }
}
