package entity;

import java.time.LocalDateTime;

/**
 * 评价实体类
 * 
 * 核心设计：
 * 1. 关联订单ID
 * 2. 支持评分（1-5星）和评价内容
 * 3. 记录评价者和被评价者
 */
public class Review {
    
    private String reviewId;            // 评价ID
    private String orderId;             // 订单ID
    private String productId;           // 商品ID
    private String reviewerId;          // 评价者ID（买家）
    private String revieweeId;          // 被评价者ID（卖家）
    private int rating;                 // 评分（1-5星）
    private String content;             // 评价内容
    private LocalDateTime createTime;   // 创建时间
    
    /**
     * 构造器
     */
    public Review(String reviewId, String orderId, String productId,
                  String reviewerId, String revieweeId, int rating, String content) {
        this.reviewId = reviewId;
        this.orderId = orderId;
        this.productId = productId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.rating = rating;
        this.content = content;
        this.createTime = LocalDateTime.now();
    }
    
    /**
     * 无参构造器（用于JSON反序列化）
     */
    public Review() {
    }
    
    /**
     * 是否是好评（4星及以上）
     */
    public boolean isPositive() {
        return rating >= 4;
    }
    
    /**
     * 是否是差评（2星及以下）
     */
    public boolean isNegative() {
        return rating <= 2;
    }
    
    // ========== Getters and Setters ==========
    
    public String getReviewId() {
        return reviewId;
    }
    
    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getReviewerId() {
        return reviewerId;
    }
    
    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }
    
    public String getRevieweeId() {
        return revieweeId;
    }
    
    public void setRevieweeId(String revieweeId) {
        this.revieweeId = revieweeId;
    }
    
    public int getRating() {
        return rating;
    }
    
    public void setRating(int rating) {
        this.rating = rating;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    @Override
    public String toString() {
        return String.format("Review[id=%s, orderId=%s, rating=%d星, content=%s]",
                reviewId, orderId, rating, content);
    }
}


