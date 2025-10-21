package entity;

import enums.OrderStatus;
import java.time.LocalDateTime;

/**
 * 订单实体类
 * 
 * 管理订单的完整生命周期：PENDING → CONFIRMED → COMPLETED/CANCELLED。
 * 记录关键时间节点，用于跟踪订单进度和争议处理。
 */
public class Order {
    
    private String orderId;
    private String productId;
    private String buyerId;
    private String sellerId;
    private double price;
    private OrderStatus status;
    private LocalDateTime createTime;
    private LocalDateTime confirmTime;
    private LocalDateTime completeTime;
    private String cancelReason;
    
    /**
     * 构造订单对象
     * 
     * @param orderId 订单ID
     * @param productId 商品ID
     * @param buyerId 买家ID
     * @param sellerId 卖家ID
     * @param price 成交价格
     */
    public Order(String orderId, String productId, String buyerId, 
                 String sellerId, double price) {
        this.orderId = orderId;
        this.productId = productId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.price = price;
        this.status = OrderStatus.PENDING;
        this.createTime = LocalDateTime.now();
    }
    
    /**
     * 无参构造器
     * 
     * 用于JSON反序列化。
     */
    public Order() {
    }
    
    /**
     * 卖家确认订单
     * 
     * 将订单状态从PENDING变为CONFIRMED，表示卖家已接受订单准备发货。
     * 
     * @throws IllegalStateException 如果订单不是PENDING状态
     */
    public void confirm() {
        if (status == OrderStatus.PENDING) {
            this.status = OrderStatus.CONFIRMED;
            this.confirmTime = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Only pending orders can be confirmed");
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
            throw new IllegalStateException("Only confirmed orders can be completed");
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
            throw new IllegalStateException("当前状态不允许取消");
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


