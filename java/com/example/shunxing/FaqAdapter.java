package com.example.shunxing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

/**
 * FAQ 适配器
 */
public class FaqAdapter extends RecyclerView.Adapter<FaqAdapter.FaqViewHolder> {

    private final List<FaqItem> items;

    public FaqAdapter(List<FaqItem> items) {
        this.items = items;
    }

    @Override
    public FaqViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_faq, parent, false);
        return new FaqViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FaqViewHolder holder, int position) {
        FaqItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * FAQ 项数据类
     */
    public static class FaqItem {
        private final String question;
        private final String answer;
        private boolean isExpanded = false;

        public FaqItem(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }

        public boolean isExpanded() {
            return isExpanded;
        }

        public void setExpanded(boolean expanded) {
            isExpanded = expanded;
        }
    }

    /**
     * FAQ ViewHolder
     */
    static class FaqViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView questionTextView;
        TextView answerTextView;

        public FaqViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            questionTextView = itemView.findViewById(R.id.questionTextView);
            answerTextView = itemView.findViewById(R.id.answerTextView);
        }

        public void bind(FaqItem item) {
            questionTextView.setText(item.getQuestion());
            answerTextView.setText(item.getAnswer());
            answerTextView.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);

            // 设置点击事件
            cardView.setOnClickListener(v -> {
                item.setExpanded(!item.isExpanded());
                answerTextView.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
            });
        }
    }
}
