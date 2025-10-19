package enums;

/**
 * 订单状态枚举
 * 
 * 状态流转：
 * PENDING → CONFIRMED → COMPLETED
 *         ↘ CANCELLED
 */
public enum OrderStatus {
    /**
     * 待确认：买家已下单，等待卖家确认
     */
    PENDING("待确认"),
    
    /**
     * 已确认：卖家已确认，等待买家收货
     */
    CONFIRMED("已确认"),
    
    /**
     * 已完成：买家已确认收货，交易完成
     */
    COMPLETED("已完成"),
    
    /**
     * 已取消：订单被取消
     */
    CANCELLED("已取消");
    
    private final String displayName;
    
    OrderStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}


