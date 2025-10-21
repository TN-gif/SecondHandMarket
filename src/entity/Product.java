package entity;

import enums.ProductCategory;
import enums.ProductCondition;
import enums.ProductStatus;
import java.time.LocalDateTime;

/**
 * 商品实体类
 * 
 * 支持完整的商品生命周期状态管理：AVAILABLE → RESERVED → SOLD/AVAILABLE。
 * RESERVED状态用于订单创建时锁定商品，防止重复购买。
 */
public class Product {
    
    private String productId;
    private String title;
    private String description;
    private double price;
    private ProductCategory category;
    private ProductCondition condition;
    private ProductStatus status;
    private String sellerId;
    private LocalDateTime publishTime;
    private LocalDateTime updateTime;
    
    /**
     * 构造商品对象
     * 
     * @param productId 商品ID
     * @param title 标题
     * @param description 描述
     * @param price 价格
     * @param category 分类
     * @param condition 成色
     * @param sellerId 卖家ID
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
        this.status = ProductStatus.AVAILABLE;
        this.sellerId = sellerId;
        this.publishTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 无参构造器
     * 
     * 用于JSON反序列化。
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


