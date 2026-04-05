package com.example.shunxing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class TutorialActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private Button btnNext;
    private Button btnSkip;
    
    private List<TutorialItem> tutorialItems;
    private TutorialAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        
        initViews();
        initTutorialItems();
        setupViewPager();
        setupDots();
        setupListeners();
    }
    
    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
    }
    
    private void initTutorialItems() {
        tutorialItems = new ArrayList<>();
        
        // 第一步：欢迎页面
        tutorialItems.add(new TutorialItem(
                R.drawable.ic_logo,
                getString(R.string.tutorial_1_title),
                getString(R.string.tutorial_1_desc)
        ));
        
        // 第二步：测量方法说明
        tutorialItems.add(new TutorialItem(
                R.drawable.ic_camera,
                getString(R.string.tutorial_2_title),
                getString(R.string.tutorial_2_desc)
        ));
        
        // 第三步：旋转扫描过程
        tutorialItems.add(new TutorialItem(
                R.drawable.ic_rotate,
                "360° 旋转扫描",
                "缓慢移动手机，环绕腰部一周完成扫描"
        ));
        
        // 第四步：结果查看
        tutorialItems.add(new TutorialItem(
                R.drawable.ic_health,
                getString(R.string.tutorial_3_title),
                getString(R.string.tutorial_3_desc)
        ));
    }
    
    private void setupViewPager() {
        adapter = new TutorialAdapter(tutorialItems);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                updateButtons(position);
            }
        });
    }
    
    private void setupDots() {
        dotsLayout.removeAllViews();
        for (int i = 0; i < tutorialItems.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    16, 16
            );
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.circle_background);
            dot.setAlpha(0.5f);
            dotsLayout.addView(dot);
        }
        updateDots(0);
    }
    
    private void updateDots(int position) {
        for (int i = 0; i < dotsLayout.getChildCount(); i++) {
            View dot = dotsLayout.getChildAt(i);
            if (i == position) {
                dot.setAlpha(1.0f);
                dot.setBackgroundResource(R.drawable.circle_background_focused);
            } else {
                dot.setAlpha(0.5f);
                dot.setBackgroundResource(R.drawable.circle_background);
            }
        }
    }
    
    private void updateButtons(int position) {
        if (position == tutorialItems.size() - 1) {
            btnNext.setText(R.string.finish);
            btnSkip.setVisibility(View.GONE);
        } else {
            btnNext.setText(R.string.next);
            btnSkip.setVisibility(View.VISIBLE);
        }
    }
    
    private void setupListeners() {
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPosition = viewPager.getCurrentItem();
                if (currentPosition < tutorialItems.size() - 1) {
                    viewPager.setCurrentItem(currentPosition + 1);
                } else {
                    // 完成引导，标记为已使用
                    markTutorialCompleted();
                    // 跳转到主界面
                    startActivity(new Intent(TutorialActivity.this, HomeActivity.class));
                    finish();
                }
            }
        });
        
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markTutorialCompleted();
                startActivity(new Intent(TutorialActivity.this, HomeActivity.class));
                finish();
            }
        });
    }
    
    private void markTutorialCompleted() {
        SharedPreferences prefs = getSharedPreferences("TutorialPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("tutorial_completed", true).apply();
    }
    
    public static boolean isTutorialCompleted(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences("TutorialPrefs", android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean("tutorial_completed", false);
    }
    
    private static class TutorialItem {
        int imageResId;
        String title;
        String description;
        
        TutorialItem(int imageResId, String title, String description) {
            this.imageResId = imageResId;
            this.title = title;
            this.description = description;
        }
    }
    
    private class TutorialAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder> {
        
        private List<TutorialItem> items;
        
        TutorialAdapter(List<TutorialItem> items) {
            this.items = items;
        }
        
        @Override
        public TutorialViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_tutorial, parent, false);
            return new TutorialViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(TutorialViewHolder holder, int position) {
            TutorialItem item = items.get(position);
            holder.imageView.setImageResource(item.imageResId);
            holder.titleTextView.setText(item.title);
            holder.descriptionTextView.setText(item.description);
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }
        
        class TutorialViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ImageView imageView;
            TextView titleTextView;
            TextView descriptionTextView;
            
            TutorialViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
                titleTextView = itemView.findViewById(R.id.titleTextView);
                descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            }
        }
    }
}