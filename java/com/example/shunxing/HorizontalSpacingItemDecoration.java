package com.example.shunxing;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView 水平间距装饰器
 * 功能：为水平滚动的 RecyclerView 项目添加统一的间距
 */
public class HorizontalSpacingItemDecoration extends RecyclerView.ItemDecoration {
    private final int spacing; // 间距大小

    /**
     * 构造方法
     * @param spacing 间距大小，单位为像素
     */
    public HorizontalSpacingItemDecoration(int spacing) {
        this.spacing = spacing;
    }

    /**
     * 设置项目的偏移量
     * @param outRect 用于存储偏移量的矩形
     * @param view 当前项目的视图
     * @param parent RecyclerView 父容器
     * @param state RecyclerView 当前状态
     */
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        
        // 为所有项目添加右侧间距
        outRect.right = spacing;
        
        // 为第一个项目添加左侧间距，确保整体布局对称
        if (position == 0) {
            outRect.left = spacing;
        }
    }
}