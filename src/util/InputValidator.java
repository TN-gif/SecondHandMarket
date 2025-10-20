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
    
    /**
     * 验证字符串是否为有效整数
     */
    public static boolean isValidInteger(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 验证字符串是否为有效小数
     */
    public static boolean isValidDouble(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 安全解析整数，失败返回null
     */
    public static Integer parseIntSafe(String str) {
        if (!isValidInteger(str)) {
            return null;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 安全解析小数，失败返回null
     */
    public static Double parseDoubleSafe(String str) {
        if (!isValidDouble(str)) {
            return null;
        }
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 验证评价内容长度
     */
    public static boolean isValidReviewContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        return content.length() >= 5 && content.length() <= 200;
    }
}


