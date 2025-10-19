package util;

/**
 * 输入验证工具
 */
public class InputValidator {
    
    /**
     * 验证用户名（4-20位字母数字）
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return username.matches("^[a-zA-Z0-9]{4,20}$");
    }
    
    /**
     * 验证密码（6-20位）
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return password.length() >= 6 && password.length() <= 20;
    }
    
    /**
     * 验证价格（大于0）
     */
    public static boolean isValidPrice(double price) {
        return price > 0;
    }
    
    /**
     * 验证评分（1-5星）
     */
    public static boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }
    
    /**
     * 验证字符串不为空
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}


