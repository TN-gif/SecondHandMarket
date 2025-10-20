package enums;

/**
 * 商品状态枚举
 * 
 * 设计说明：
 * 包含RESERVED中间状态，解决订单取消后商品状态恢复的问题
 * 
 * 状态流转：
 * AVAILABLE → 下单 → RESERVED → 确认收货 → SOLD
 *            ← 取消 ←
 * 
 * 答辩要点：
 * 为什么需要RESERVED状态？
 * 如果没有RESERVED状态，下单后商品立即变为SOLD，当订单取消时
 * 无法判断商品应该恢复为什么状态。引入RESERVED状态后，状态
 * 流转逻辑更加严谨，符合真实交易场景。
 */
public enum ProductStatus {
    /**
     * 可售：商品已发布，可以被购买
     */
    AVAILABLE("在售"),
    
    /**
     * 已预订：买家已下单但卖家未确认，或已确认但买家未收货
     */
    RESERVED("已预订"),
    
    /**
     * 已售出：交易完成
     */
    SOLD("已售出"),
    
    /**
     * 已下架：卖家主动下架
     */
    REMOVED("已下架");
    
    private final String displayName;
    
    ProductStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}


