package com.example.shunxing;

import android.content.Context;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * 聚焦指示器管理器
 * 功能：管理相机预览界面中的聚焦指示器，支持显示、移动、更新颜色等操作
 */
public class FocusIndicatorManager {
    private static FocusIndicatorManager instance; // 单例实例
    private FocusIndicatorView currentIndicator; // 当前聚焦指示器视图
    private FrameLayout container; // 容器布局

    /**
     * 获取单例实例
     * @return FocusIndicatorManager 实例
     */
    public static synchronized FocusIndicatorManager getInstance() {
        if (instance == null) {
            instance = new FocusIndicatorManager();
        }
        return instance;
    }

    /**
     * 初始化管理器
     * @param container 容器布局，用于添加聚焦指示器
     */
    public void init(FrameLayout container) {
        this.container = container;
    }

    /**
     * 在指定位置显示聚焦指示器
     * @param x X坐标
     * @param y Y坐标
     * @param color 指示器颜色
     */
    public void showAt(float x, float y, int color) {
        if (container == null) {
            return;
        }

        removeCurrent(); // 移除当前指示器
        currentIndicator = new FocusIndicatorView(container.getContext()); // 创建新指示器
        currentIndicator.setColor(color); // 设置颜色

        // 设置布局参数
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        // 计算边距，使指示器中心位于点击位置
        params.leftMargin = (int) (x - dpToPx(container.getContext(), 40));
        params.topMargin = (int) (y - dpToPx(container.getContext(), 40));

        container.addView(currentIndicator, params); // 添加到容器
    }

    /**
     * 移动聚焦指示器到新位置
     * @param x X坐标
     * @param y Y坐标
     */
    public void moveTo(float x, float y) {
        if (currentIndicator == null || container == null) {
            return;
        }

        // 更新布局参数
        FrameLayout.LayoutParams params = 
                (FrameLayout.LayoutParams) currentIndicator.getLayoutParams();
        if (params != null) {
            params.leftMargin = (int) (x - dpToPx(container.getContext(), 40));
            params.topMargin = (int) (y - dpToPx(container.getContext(), 40));
            currentIndicator.setLayoutParams(params);
        }
    }

    /**
     * 更新聚焦指示器颜色
     * @param color 新颜色
     */
    public void updateColor(int color) {
        if (currentIndicator != null) {
            currentIndicator.setColor(color);
        }
    }

    /**
     * 检查是否存在聚焦指示器
     * @return 是否存在指示器
     */
    public boolean hasIndicator() {
        return currentIndicator != null;
    }

    /**
     * 移除当前聚焦指示器
     */
    public void removeCurrent() {
        if (currentIndicator != null && container != null) {
            container.removeView(currentIndicator);
            currentIndicator = null;
        }
    }

    /**
     * 清除聚焦指示器
     */
    public void clear() {
        removeCurrent();
    }

    /**
     * 将dp转换为px
     * @param context 上下文
     * @param dp dp值
     * @return px值
     */
    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}