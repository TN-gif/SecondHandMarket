package util;

import exception.BusinessException;

/**
 * 数据验证工具类
 * 
 * 功能：
 * 1. 提供各种数据验证方法
 * 2. 验证失败时抛出 BusinessException
 * 3. 统一验证规则，避免重复代码
 * 
 * 使用方法：
 * ValidationUtils.validateProductTitle(title);
 * ValidationUtils.validatePrice(price);
 */
public class ValidationUtils {
    
    // ========== 通用验证 ==========
    
    /**
     * 验证非空字符串
     */
    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(fieldName + " 不能为空");
        }
    }
    
    /**
     * 验证字符串长度范围
     */
    public static void validateLength(String value, String fieldName, int minLength, int maxLength) {
        validateNotEmpty(value, fieldName);
        
        int length = value.trim().length();
        if (length < minLength) {
            throw new BusinessException(
                String.format("%s 长度至少为 %d 字（当前：%d）", 
                    fieldName, minLength, length));
        }
        if (length > maxLength) {
            throw new BusinessException(
                String.format("%s 长度不能超过 %d 字（当前：%d）", 
                    fieldName, maxLength, length));
        }
    }
    
    /**
     * 验证数值范围
     */
    public static void validateRange(double value, String fieldName, double min, double max) {
        if (value < min || value > max) {
            throw new BusinessException(
                String.format("%s 必须在 %.2f 到 %.2f 之间（当前：%.2f）", 
                    fieldName, min, max, value));
        }
    }
    
    /**
     * 验证正数
     */
    public static void validatePositive(double value, String fieldName) {
        if (value <= 0) {
            throw new BusinessException(
                String.format("%s 必须为正数（当前：%.2f）", fieldName, value));
        }
    }
    
    /**
     * 验证非负数
     */
    public static void validateNonNegative(double value, String fieldName) {
        if (value < 0) {
            throw new BusinessException(
                String.format("%s 不能为负数（当前：%.2f）", fieldName, value));
        }
    }
    
    // ========== 用户相关验证 ==========
    
    /**
     * 验证用户名
     * 规则：4-20位字母或数字
     */
    public static void validateUsername(String username) {
        validateNotEmpty(username, "用户名");
        
        if (!username.matches("^[a-zA-Z0-9]{4,20}$")) {
            throw new BusinessException(
                "用户名必须为4-20位字母或数字（当前：" + username + "）");
        }
    }
    
    /**
     * 验证密码
     * 规则：6-20位任意字符
     */
    public static void validatePassword(String password) {
        validateNotEmpty(password, "密码");
        validateLength(password, "密码", 6, 20);
    }
    
    // ========== 商品相关验证 ==========
    
    /**
     * 验证商品标题
     * 规则：2-100字符
     */
    public static void validateProductTitle(String title) {
        validateNotEmpty(title, "商品标题");
        validateLength(title, "商品标题", 2, 100);
    }
    
    /**
     * 验证商品描述
     * 规则：最多1000字符，可以为空
     */
    public static void validateProductDescription(String description) {
        if (description != null && description.length() > 1000) {
            throw new BusinessException(
                "商品描述不能超过1000字（当前：" + 
                description.length() + "）");
        }
    }
    
    /**
     * 验证商品价格
     * 规则：0.01 - 1,000,000，最多2位小数
     */
    public static void validatePrice(double price) {
        validatePositive(price, "价格");
        validateRange(price, "价格", 0.01, 1000000.00);
        
        // 检查小数位数
        String priceStr = String.format("%.2f", price);
        double roundedPrice = Double.parseDouble(priceStr);
        if (Math.abs(price - roundedPrice) > 0.001) {
            throw new BusinessException(
                "价格最多保留2位小数（当前：" + price + "）");
        }
    }
    
    // ========== 订单相关验证 ==========
    
    /**
     * 验证订单取消原因
     * 规则：5-200字符
     */
    public static void validateCancelReason(String reason) {
        validateNotEmpty(reason, "取消理由");
        validateLength(reason, "取消理由", 5, 200);
    }
    
    // ========== 评价相关验证 ==========
    
    /**
     * 验证评价内容
     * 规则：10-500字符
     */
    public static void validateReviewContent(String content) {
        validateNotEmpty(content, "评价内容");
        validateLength(content, "评价内容", 10, 500);
    }
    
    /**
     * 验证评分
     * 规则：1-5的整数
     */
    public static void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException(
                "评分必须在1到5之间（当前：" + rating + "）");
        }
    }
    
    // ========== 申诉相关验证 ==========
    
    /**
     * 验证申诉原因
     * 规则：10-500字符
     */
    public static void validateAppealReason(String reason) {
        validateNotEmpty(reason, "申诉理由");
        validateLength(reason, "申诉理由", 10, 500);
    }
    
    // ========== ID 验证 ==========
    
    /**
     * 验证 ID 格式
     */
    public static void validateId(String id, String idType) {
        validateNotEmpty(id, idType);
        
        if (id.length() < 5) {
            throw new BusinessException(idType + " 格式无效");
        }
    }
}


