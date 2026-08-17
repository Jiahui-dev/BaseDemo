package com.yjh.base.utils.util;

import android.app.Activity;
import android.content.Context;
import java.util.Stack;

public class AppUtils {

    private static Stack<Activity> activityStack = new Stack<>();

    public static void addActivity(Activity activity) {
        if (activityStack == null) {
            activityStack = new Stack<>();
        }
        activityStack.add(activity);
    }

    public static void removeActivity(Activity activity) {
        if (activity != null && activityStack != null) {
            activityStack.remove(activity);
        }
    }

    /**
     * 正确优雅地关闭 App 并移除最近任务卡片
     */
    public static void closeApp(Context context) {
        // 1. 获取当前传入的 Activity，或者从栈顶拿到一个活着的 Activity
        Activity currentActivity = null;
        if (context instanceof Activity) {
            currentActivity = (Activity) context;
        } else if (activityStack != null && !activityStack.isEmpty()) {
            currentActivity = activityStack.peek();
        }

        // 2. 先清理栈中其他的 Activity（排除 currentActivity，防止它提前被 finish）
        if (activityStack != null) {
            for (Activity activity : activityStack) {
                if (activity != null && activity != currentActivity && !activity.isFinishing()) {
                    activity.finish();
                }
            }
        }

        // 3. 最后由存活的 Activity 触发 finishAndRemoveTask()，一举销毁自身并移除 Task 卡片
        if (currentActivity != null && !currentActivity.isFinishing()) {
            currentActivity.finishAndRemoveTask();
        }

        if (activityStack != null) {
            activityStack.clear();
        }
    }
}