package com.example.shunxing;

/**
 * 项目数据模型类
 * 用于存储项目卡片的相关信息
 */
public class Project {
    private String name; // 项目名称
    private String lastModified; // 最后修改时间
    private int thumbnailResId; // 缩略图资源ID
    private boolean isHas3DModel; // 是否有3D模型

    /**
     * 构造方法
     * @param name 项目名称
     * @param lastModified 最后修改时间
     * @param thumbnailResId 缩略图资源ID
     */
    public Project(String name, String lastModified, int thumbnailResId) {
        this(name, lastModified, thumbnailResId, false);
    }

    /**
     * 构造方法
     * @param name 项目名称
     * @param lastModified 最后修改时间
     * @param thumbnailResId 缩略图资源ID
     * @param isHas3DModel 是否有3D模型
     */
    public Project(String name, String lastModified, int thumbnailResId, boolean isHas3DModel) {
        this.name = name;
        this.lastModified = lastModified;
        this.thumbnailResId = thumbnailResId;
        this.isHas3DModel = isHas3DModel;
    }

    /**
     * 获取项目名称
     * @return 项目名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置项目名称
     * @param name 项目名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取最后修改时间
     * @return 最后修改时间
     */
    public String getLastModified() {
        return lastModified;
    }

    /**
     * 设置最后修改时间
     * @param lastModified 最后修改时间
     */
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * 获取缩略图资源ID
     * @return 缩略图资源ID
     */
    public int getThumbnailResId() {
        return thumbnailResId;
    }

    /**
     * 设置缩略图资源ID
     * @param thumbnailResId 缩略图资源ID
     */
    public void setThumbnailResId(int thumbnailResId) {
        this.thumbnailResId = thumbnailResId;
    }

    /**
     * 获取是否有3D模型
     * @return 是否有3D模型
     */
    public boolean isHas3DModel() {
        return isHas3DModel;
    }

    /**
     * 设置是否有3D模型
     * @param has3DModel 是否有3D模型
     */
    public void setHas3DModel(boolean has3DModel) {
        isHas3DModel = has3DModel;
    }
}
