package entity;

import enums.OrderStatus;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * 
 * 核心设计：
 * 1. 记录买家、卖家、商品信息
 * 2. 支持完整的状态流转（待确认→已确认→已完成/已取消）
 * 3. 记录关键时间节点
 */
public class Order {
    
    private String orderId;             // 订单ID
    private String productId;           // 商品ID
    private String buyerId;             // 买家ID
    private String sellerId;            // 卖家ID
    private double price;               // 成交价格
    private OrderStatus status;         // 订单状态
    private LocalDateTime createTime;   // 创建时间
    private LocalDateTime confirmTime;  // 确认时间
    private LocalDateTime completeTime; // 完成时间
    private String cancelReason;        // 取消原因
    
    /**
     * 构造器
     */
    public Order(String orderId, String productId, String buyerId, 
                 String sellerId, double price) {
        this.orderId = orderId;
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.price = price;
        this.status = OrderStatus.PENDING;  // 初始状态为待确认
        this.createTime = LocalDateTime.now();
    }
    
    /**
     * 无参构造器（用于JSON反序列化）
     */
    public Order() {
    }
    
    // ========== 业务方法 ==========
    
    /**
     * 卖家确认订单
     */
    public void confirm() {
        if (status == OrderStatus.PENDING) {
            this.status = OrderStatus.CONFIRMED;
            this.confirmTime = LocalDateTime.now();
        } else {
            throw new IllegalStateException("只有待确认的订单可以确认");
        }
    }
    
    /**
     * 买家确认收货
     */
    public void complete() {
        if (status == OrderStatus.CONFIRMED) {
            this.status = OrderStatus.COMPLETED;
            this.completeTime = LocalDateTime.now();
        } else {
            throw new IllegalStateException("只有已确认的订单可以完成");
        }
    }
    
    /**
     * 取消订单
     */
    public void cancel(String reason) {
        if (status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED) {
            this.status = OrderStatus.CANCELLED;
            this.cancelReason = reason;
            this.completeTime = LocalDateTime.now();  // 使用完成时间记录取消时间
        } else {
            throw new IllegalStateException("当前状态不可取消");
        }
    }
    
    /**
     * 是否可以评价
     */
    public boolean canBeReviewed() {
        return status == OrderStatus.COMPLETED;
    }
    
    // ========== Getters and Setters ==========
    
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
    
    public String getBuyerId() {
        return buyerId;
    }
    
    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getConfirmTime() {
        return confirmTime;
    }
    
    public void setConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }
    
    public LocalDateTime getCompleteTime() {
        return completeTime;
    }
    
    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }
    
    public String getCancelReason() {
        return cancelReason;
    }
    
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
    
    @Override
    public String toString() {
        return String.format("Order[id=%s, productId=%s, buyerId=%s, status=%s, price=%.2f]",
                orderId, productId, buyerId, status.getDisplayName(), price);
    }
}


