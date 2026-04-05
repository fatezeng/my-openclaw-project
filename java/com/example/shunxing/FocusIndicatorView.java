package com.example.shunxing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * 聚焦指示器视图
 * 功能：在相机预览界面中显示方形聚焦框，用于指示聚焦位置
 */
public class FocusIndicatorView extends View {
    private Paint focusPaint; // 画笔
    private int color; // 指示器颜色
    private int cornerLength; // 角线长度
    private int lineWidth; // 线宽
    private int size; // 指示器大小

    /**
     * 构造方法
     * @param context 上下文
     */
    public FocusIndicatorView(Context context) {
        super(context);
        init();
    }

    /**
     * 构造方法
     * @param context 上下文
     * @param attrs 属性集
     */
    public FocusIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * 构造方法
     * @param context 上下文
     * @param attrs 属性集
     * @param defStyleAttr 默认样式属性
     */
    public FocusIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化方法
     */
    private void init() {
        // 初始化画笔
        focusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        focusPaint.setStyle(Paint.Style.STROKE);
        focusPaint.setStrokeCap(Paint.Cap.SQUARE);

        // 初始化尺寸
        lineWidth = dpToPx(2);
        cornerLength = dpToPx(20);
        size = dpToPx(80);
        focusPaint.setStrokeWidth(lineWidth);

        // 设置默认颜色
        setColor(android.graphics.Color.parseColor("#9C27B0"));
        // 确保视图会被绘制
        setWillNotDraw(false);
    }

    /**
     * 设置指示器颜色
     * @param color 新颜色
     */
    public void setColor(int color) {
        this.color = color;
        focusPaint.setColor(color);
        // 触发重绘
        invalidate();
    }

    /**
     * 测量视图大小
     * @param widthMeasureSpec 宽度测量规格
     * @param heightMeasureSpec 高度测量规格
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 设置视图大小为固定值
        setMeasuredDimension(size, size);
    }

    /**
     * 绘制视图
     * @param canvas 画布
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int halfLine = lineWidth / 2;

        // 绘制左上角
        canvas.drawLine(halfLine, halfLine, halfLine + cornerLength, halfLine, focusPaint);
        canvas.drawLine(halfLine, halfLine, halfLine, halfLine + cornerLength, focusPaint);

        // 绘制右上角
        canvas.drawLine(width - halfLine - cornerLength, halfLine, width - halfLine, halfLine, focusPaint);
        canvas.drawLine(width - halfLine, halfLine, width - halfLine, halfLine + cornerLength, focusPaint);

        // 绘制左下角
        canvas.drawLine(halfLine, height - halfLine - cornerLength, halfLine, height - halfLine, focusPaint);
        canvas.drawLine(halfLine, height - halfLine, halfLine + cornerLength, height - halfLine, focusPaint);

        // 绘制右下角
        canvas.drawLine(width - halfLine - cornerLength, height - halfLine, width - halfLine, height - halfLine, focusPaint);
        canvas.drawLine(width - halfLine, height - halfLine - cornerLength, width - halfLine, height - halfLine, focusPaint);
    }

    /**
     * 将dp转换为px
     * @param dp dp值
     * @return px值
     */
    private int dpToPx(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}