package com.example.shunxing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * 常见问题页 Activity
 * 功能：展示常见问题列表，支持折叠展开查看答案
 * 详细功能：
 * 1. 展示常见问题列表：以卡片形式展示常见问题
 * 2. 支持折叠展开：点击问题卡片可以展开或折叠答案
 * 3. 自定义工具栏：包含返回按钮，点击可返回上一页
 */
public class FaqActivity extends AppCompatActivity {

    /**
     * UI 组件
     */
    private RecyclerView recyclerView; // 常见问题列表
    private Toolbar toolbar; // 工具栏

    /**
     * 数据
     */
    private List<FaqItem> faqItems = new ArrayList<>(); // 常见问题数据列表

    /**
     * 生命周期方法 - 创建Activity
     * @param savedInstanceState 保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);

        // 初始化 UI 组件
        initViews();
        // 设置工具栏
        setupToolbar();
        // 初始化常见问题数据
        initFaqData();
        // 设置 RecyclerView
        setupRecyclerView();
    }

    /**
     * 初始化 UI 组件
     */
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        toolbar = findViewById(R.id.toolbar);
        TextView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    /**
     * 设置工具栏
     */
    private void setupToolbar() {
        // 不再需要设置ActionBar，因为我们使用了自定义的工具栏布局
    }

    /**
     * 初始化常见问题数据
     */
    private void initFaqData() {
        faqItems.add(new FaqItem(
                "如何正确测量腰围？",
                "正确的测量方法是：自然站立，双脚分开与肩同宽，放松腹部，将软尺放在肚脐上方2cm处，水平环绕腰部一周，读取数值。确保软尺贴合皮肤但不压迫。"
        ));
        
        faqItems.add(new FaqItem(
                "应用支持哪些单位？",
                "应用支持厘米(cm)和英寸(inch)两种单位。您可以在设置页面切换单位，系统会自动转换所有历史数据。"
        ));
        
        faqItems.add(new FaqItem(
                "为什么需要输入身高？",
                "身高数据用于计算身体比例和健康指标，帮助提供更准确的健康建议。身高信息会保存在本地，不会上传到服务器。"
        ));
        
        faqItems.add(new FaqItem(
                "测量结果准确吗？",
                "应用使用先进的AR技术和机器学习算法，测量精度可达±0.5cm。为获得最准确的结果，请确保在光线充足的环境中，缓慢平稳地移动手机环绕腰部。"
        ));
        
        faqItems.add(new FaqItem(
                "数据会上传到云端吗？",
                "默认情况下，测量数据仅保存在本地设备。您可以在设置中开启云同步功能，将数据备份到云端，以便在更换设备时恢复数据。"
        ));
        
        faqItems.add(new FaqItem(
                "如何删除测量记录？",
                "在历史记录页面，长按某个测量记录，会弹出删除选项。您也可以进入设置页面，选择'清除所有数据'来删除所有测量记录。"
        ));
        
        faqItems.add(new FaqItem(
                "应用需要哪些权限？",
                "应用需要相机权限用于AR测量，存储权限用于保存测量结果和图片，通知权限用于发送测量提醒。所有权限都是可选的，您可以在系统设置中管理。"
        ));
        
        faqItems.add(new FaqItem(
                "为什么测量失败？",
                "测量失败可能的原因包括：光线不足、背景复杂、移动速度过快、手机摄像头遮挡等。请确保在光线充足的环境中，缓慢平稳地移动手机，保持摄像头清晰可见。"
        ));
        
        faqItems.add(new FaqItem(
                "如何分享测量结果？",
                "在测量结果页面，点击分享按钮，可以选择分享图片、PDF报告、Excel数据或3D模型数据。您可以通过邮件、消息等方式分享给他人。"
        ));
        
        faqItems.add(new FaqItem(
                "应用支持哪些设备？",
                "应用支持Android 8.0及以上版本，且具有ARCore功能的设备。您可以在Google Play商店查看设备兼容性列表。"
        ));
    }

    /**
     * 设置 RecyclerView
     */
    private void setupRecyclerView() {
        FaqAdapter adapter = new FaqAdapter(faqItems);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }



    /**
     * 常见问题项数据类
     * 用于存储单个常见问题的问题和答案
     */
    private static class FaqItem {
        private final String question; // 问题
        private final String answer; // 答案
        private boolean isExpanded = false; // 是否展开

        /**
         * 构造方法
         * @param question 问题
         * @param answer 答案
         */
        public FaqItem(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        /**
         * 获取问题
         * @return 问题
         */
        public String getQuestion() {
            return question;
        }

        /**
         * 获取答案
         * @return 答案
         */
        public String getAnswer() {
            return answer;
        }

        /**
         * 检查是否展开
         * @return 是否展开
         */
        public boolean isExpanded() {
            return isExpanded;
        }

        /**
         * 设置展开状态
         * @param expanded 是否展开
         */
        public void setExpanded(boolean expanded) {
            isExpanded = expanded;
        }
    }

    /**
     * 常见问题适配器
     * 用于将常见问题数据绑定到 RecyclerView
     */
    private static class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FaqViewHolder> {

        private final List<FaqItem> items; // 常见问题数据列表

        /**
         * 构造方法
         * @param items 常见问题数据列表
         */
        public FaqAdapter(List<FaqItem> items) {
            this.items = items;
        }

        /**
         * 创建 ViewHolder
         * @param parent 父容器
         * @param viewType 视图类型
         * @return ViewHolder
         */
        @Override
        public FaqViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_faq, parent, false);
            return new FaqViewHolder(view);
        }

        /**
         * 绑定数据到 ViewHolder
         * @param holder ViewHolder
         * @param position 位置
         */
        @Override
        public void onBindViewHolder(FaqViewHolder holder, int position) {
            FaqItem item = items.get(position);
            holder.questionTextView.setText(item.getQuestion());
            holder.answerTextView.setText(item.getAnswer());
            holder.answerTextView.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);

            // 设置点击事件
            holder.cardView.setOnClickListener(v -> {
                item.setExpanded(!item.isExpanded());
                notifyItemChanged(position);
            });
        }

        /**
         * 获取数据项数量
         * @return 数据项数量
         */
        @Override
        public int getItemCount() {
            return items.size();
        }

        /**
         * 常见问题ViewHolder
         * 用于持有常见问题项的视图
         */
        static class FaqViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView; // 卡片视图
            TextView questionTextView; // 问题文本视图
            TextView answerTextView; // 答案文本视图

            /**
             * 构造方法
             * @param itemView 视图
             */
            public FaqViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.cardView);
                questionTextView = itemView.findViewById(R.id.questionTextView);
                answerTextView = itemView.findViewById(R.id.answerTextView);
            }
        }
    }
}
