package com.example.shunxing;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

/**
 * 身高输入对话框
 * 使用 NumberPicker 实现滚轮选择器，用户体验更好
 */
public class HeightInputDialog extends Dialog {

    private static final int MIN_HEIGHT = 100;  // 最小身高 100cm
    private static final int MAX_HEIGHT = 250;  // 最大身高 250cm

    private NumberPicker numberPicker; // 身高选择器
    private Button btnConfirm; // 确认按钮
    private Button btnCancel; // 取消按钮
    private TextView tvUnit; // 单位显示文本

    private boolean isCm = true; // 默认使用厘米
    private OnHeightConfirmedListener listener; // 身高确认监听器

    /**
     * 身高确认监听器接口
     * 当用户确认身高输入时回调
     */
    public interface OnHeightConfirmedListener {
        /**
         * 身高确认回调方法
         * @param height 输入的身高值
         * @param isCm 是否使用厘米单位
         */
        void onHeightConfirmed(float height, boolean isCm);
    }

    /**
     * 构造方法
     * @param context 上下文
     */
    public HeightInputDialog(Context context) {
        super(context);
    }

    /**
     * 设置身高确认监听器
     * @param listener 身高确认监听器
     */
    public void setOnHeightConfirmedListener(OnHeightConfirmedListener listener) {
        this.listener = listener;
    }

    /**
     * 创建对话框时调用
     * 初始化视图、设置选择器和监听器
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_height_input);

        initViews();
        setupNumberPicker();
        setupListeners();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        numberPicker = findViewById(R.id.numberPicker);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);
        tvUnit = findViewById(R.id.tvUnit);
    }

    /**
     * 设置数字选择器
     * 配置范围、默认值和显示单位
     */
    private void setupNumberPicker() {
        // 设置范围
        numberPicker.setMinValue(MIN_HEIGHT);
        numberPicker.setMaxValue(MAX_HEIGHT);
        numberPicker.setValue(170); // 默认值 170cm

        // 设置单位显示
        updateUnitDisplay();

        // 设置滚动监听器
        numberPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            // 可以在这里添加额外的逻辑
        });
    }

    /**
     * 更新单位显示
     * 根据当前单位模式显示相应的单位文本
     */
    private void updateUnitDisplay() {
        tvUnit.setText(isCm ? "cm" : "inch");
    }

    /**
     * 设置监听器
     * 为确认按钮、取消按钮和单位切换文本设置点击监听器
     */
    private void setupListeners() {
        // 确认按钮
        btnConfirm.setOnClickListener(v -> {
            int value = numberPicker.getValue();
            float height;
            if (isCm) {
                height = value;
            } else {
                // 转换为英寸
                height = value * 2.54f;
            }

            if (listener != null) {
                listener.onHeightConfirmed(height, isCm);
            }
            dismiss();
        });

        // 取消按钮
        btnCancel.setOnClickListener(v -> {
            dismiss();
        });

        // 单位切换
        tvUnit.setOnClickListener(v -> {
            isCm = !isCm;
            updateUnitDisplay();
            // 切换单位时调整数值范围
            if (isCm) {
                numberPicker.setMinValue(MIN_HEIGHT);
                numberPicker.setMaxValue(MAX_HEIGHT);
                numberPicker.setValue(170);
            } else {
                // 英寸范围
                numberPicker.setMinValue(39); // 100cm ≈ 39英寸
                numberPicker.setMaxValue(98); // 250cm ≈ 98英寸
                numberPicker.setValue(67); // 170cm ≈ 67英寸
            }
        });
    }

    /**
     * 简化版本的监听器接口（只返回厘米值）
     */
    public interface SimpleOnHeightConfirmedListener {
        /**
         * 身高确认回调方法
         * @param height 输入的身高值（厘米）
         */
        void onHeightConfirmed(float height);
    }

    /**
     * 设置简化版监听器
     * @param listener 简化版身高确认监听器
     */
    public void setOnHeightConfirmedListener(SimpleOnHeightConfirmedListener listener) {
        this.listener = (height, isCm) -> {
            if (listener != null) {
                listener.onHeightConfirmed(height);
            }
        };
    }
}
