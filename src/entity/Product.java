package entity;

import enums.ProductCategory;
import enums.ProductCondition;
import enums.ProductStatus;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 
 * 核心设计：
 * 1. 包含RESERVED状态，支持严谨的状态流转
 * 2. 记录卖家ID，用于权限校验
 * 3. 成色、分类使用枚举
 */
public class Product {
    
    private String productId;           // 商品ID
    private String title;               // 商品标题
    private String description;         // 商品描述
    private double price;               // 价格
    private ProductCategory category;   // 分类
    private ProductCondition condition; // 成色
    private ProductStatus status;       // 状态
    private String sellerId;            // 卖家ID
    private LocalDateTime publishTime;  // 发布时间
    private LocalDateTime updateTime;   // 更新时间
    
    /**
     * 构造器
     */
    public Product(String productId, String title, String description, 
                   double price, ProductCategory category, 
                   ProductCondition condition, String sellerId) {
        this.productId = productId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.condition = condition;
        this.status = ProductStatus.AVAILABLE;  // 初始状态为可售
        this.sellerId = sellerId;
        this.publishTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 无参构造器（用于JSON反序列化）
     */
    public Product() {
    }
    
    // ========== 业务方法 ==========
    
    /**
     * 检查是否可以购买
     */
    public boolean isAvailable() {
        return status == ProductStatus.AVAILABLE;
    }
    
    /**
     * 预订商品（下单时调用）
     */
    public void reserve() {
        if (status == ProductStatus.AVAILABLE) {
            this.status = ProductStatus.RESERVED;
            this.updateTime = LocalDateTime.now();
        } else {
            throw new IllegalStateException("当前状态不允许预订商品");
        }
    }
    
    /**
     * 取消预订（订单取消时调用）
     */
    public void cancelReservation() {
        if (status == ProductStatus.RESERVED) {
            this.status = ProductStatus.AVAILABLE;
            this.updateTime = LocalDateTime.now();
        }
    }
    
    /**
     * 标记为已售出（确认收货时调用）
     */
    public void markAsSold() {
        if (status == ProductStatus.RESERVED) {
            this.status = ProductStatus.SOLD;
            this.updateTime = LocalDateTime.now();
        } else {
            throw new IllegalStateException("当前状态不允许标记为已售");
        }
    }
    
    /**
     * 下架商品
     */
    public void remove() {
        if (status == ProductStatus.AVAILABLE) {
            this.status = ProductStatus.REMOVED;
            this.updateTime = LocalDateTime.now();
        } else if (status == ProductStatus.REMOVED) {
            throw new IllegalStateException("商品已下架，无法重复下架");
        } else {
            throw new IllegalStateException("Only available products can be removed");
        }
    }
    
    /**
     * 重新上架
     */
    public void reList() {
        if (status == ProductStatus.REMOVED) {
            this.status = ProductStatus.AVAILABLE;
            this.updateTime = LocalDateTime.now();
        }
    }
    
    // ========== Getters and Setters ==========
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public ProductCategory getCategory() {
        return category;
    }
    
    public void setCategory(ProductCategory category) {
        this.category = category;
    }
    
    public ProductCondition getCondition() {
        return condition;
    }
    
    public void setCondition(ProductCondition condition) {
        this.condition = condition;
    }
    
    public ProductStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProductStatus status) {
        this.status = status;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    
    public LocalDateTime getPublishTime() {
        return publishTime;
    }
    
    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    @Override
    public String toString() {
        return String.format("Product[id=%s, title=%s, price=%.2f, status=%s]",
                productId, title, price, status.getDisplayName());
    }
}


