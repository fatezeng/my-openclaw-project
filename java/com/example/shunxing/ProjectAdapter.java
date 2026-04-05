package com.example.shunxing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * 项目卡片适配器
 * 用于在RecyclerView中显示项目卡片
 */
public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projects; // 项目列表数据
    private OnItemClickListener listener; // 项目点击监听器

    /**
     * 项目点击监听器接口
     */
    public interface OnItemClickListener {
        /**
         * 项目点击回调方法
         * @param project 被点击的项目
         */
        void onItemClick(Project project);
    }

    /**
     * 构造方法
     * @param projects 项目列表数据
     * @param listener 项目点击监听器
     */
    public ProjectAdapter(List<Project> projects, OnItemClickListener listener) {
        this.projects = projects;
        this.listener = listener;
    }

    /**
     * 创建项目卡片视图持有者
     * @param parent 父布局
     * @param viewType 视图类型
     * @return 项目卡片视图持有者
     */
    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project_card, parent, false);
        return new ProjectViewHolder(view);
    }

    /**
     * 绑定项目数据到视图
     * @param holder 视图持有者
     * @param position 位置
     */
    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        holder.tvProjectName.setText(project.getName());
        holder.tvLastModified.setText(project.getLastModified());
        // 加载项目缩略图
        holder.ivProjectThumbnail.setImageResource(project.getThumbnailResId());
        
        // 显示或隐藏3D模型图标
        if (project.isHas3DModel()) {
            holder.iv3DIcon.setVisibility(View.VISIBLE);
        } else {
            holder.iv3DIcon.setVisibility(View.GONE);
        }

        // 设置点击监听器
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(project);
            }
        });
    }

    /**
     * 获取项目数量
     * @return 项目数量
     */
    @Override
    public int getItemCount() {
        return projects.size();
    }

    /**
     * 设置项目列表数据
     * @param projects 新项目列表数据
     */
    public void setProjects(List<Project> projects) {
        this.projects = projects;
        notifyDataSetChanged();
    }

    /**
     * 项目卡片视图持有者
     */
    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProjectThumbnail; // 项目缩略图
        ImageView iv3DIcon; // 3D模型图标
        TextView tvProjectName; // 项目名称
        TextView tvLastModified; // 最后修改时间

        /**
         * 构造方法
         * @param itemView 项目卡片视图
         */
        ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProjectThumbnail = itemView.findViewById(R.id.ivProjectThumbnail);
            iv3DIcon = itemView.findViewById(R.id.iv3DIcon);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvLastModified = itemView.findViewById(R.id.tvLastModified);
        }
    }
}
