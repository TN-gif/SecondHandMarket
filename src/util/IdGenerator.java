package util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器
 * 
 * 设计说明：
 * 使用AtomicLong生成唯一ID，格式为：前缀 + 时间戳 + 序号
 */
public class IdGenerator {
    
    private static final AtomicLong counter = new AtomicLong(1);
    
    /**
     * 生成用户ID
     */
    public static String generateUserId() {
        return "U" + System.currentTimeMillis() + counter.getAndIncrement();
    }
    
    /**
     * 生成商品ID
     */
    public static String generateProductId() {
        return "P" + System.currentTimeMillis() + counter.getAndIncrement();
    }
    
    /**
     * 生成订单ID
     */
    public static String generateOrderId() {
        return "O" + System.currentTimeMillis() + counter.getAndIncrement();
    }
    
    /**
     * 生成评价ID
     */
    public static String generateReviewId() {
        return "R" + System.currentTimeMillis() + counter.getAndIncrement();
    }
    
    /**
     * 生成消息ID
     */
    public static String generateMessageId() {
        return "M" + System.currentTimeMillis() + counter.getAndIncrement();
    }
    
    /**
     * 生成申诉ID
     */
    public static String generateAppealId() {
        return "A" + System.currentTimeMillis() + counter.getAndIncrement();
    }
}


