package util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 翻译工具类
 * 用于在表格显示时将中文内容翻译为英文
 * 
 * 设计说明：
 * - 数据存储使用中文
 * - 表格显示时自动翻译为英文
 * - 使用精确映射 + 智能翻译双重机制
 */
public class TranslationUtil {
    
    // 精确映射表（优先级最高）- 针对完整字符串
    private static final Map<String, String> EXACT_MAPPING = new HashMap<>();
    
    // 词汇映射表
    private static final Map<String, String> DICTIONARY = new HashMap<>();
    
    static {
        // ========== 精确映射表（完整字符串） ==========
        
        // 商品标题
        EXACT_MAPPING.put("羽毛球拍（双拍）", "Badminton Racket (Pair)");
        EXACT_MAPPING.put("Java编程思想（第4版）", "Thinking in Java (4th Edition)");
        EXACT_MAPPING.put("Nike运动鞋", "Nike Sports Shoes");
        EXACT_MAPPING.put("耐克运动鞋", "Nike Sports Shoes");
        EXACT_MAPPING.put("iPhone 13 Pro", "iPhone 13 Pro");
        EXACT_MAPPING.put("苹果 iPhone 13 Pro", "iPhone 13 Pro");
        EXACT_MAPPING.put("移动应用实践开发基础教程", "Mobile Application Practical Development Basic Tutorial");
        
        // 商品描述
        EXACT_MAPPING.put("李宁N90，使用半年", "Li-Ning N90, used for half a year");
        EXACT_MAPPING.put("经典教材，几乎全新", "Classic textbook, almost new");
        EXACT_MAPPING.put("42码，穿过3次", "Size 42, worn 3 times");
        EXACT_MAPPING.put("9成新，无划痕，配件齐全", "Like new condition, no scratches, complete accessories");
        EXACT_MAPPING.put("帮助完成移动应用的开发，这本书目前只使用过2-3次", "Helps with mobile app development, used 2-3 times");
        
        // 成色描述
        EXACT_MAPPING.put("未开封或几乎全新", "Unopened or like new");
        EXACT_MAPPING.put("很少使用，无明显磨损", "Rarely used, no visible wear");
        EXACT_MAPPING.put("轻微使用痕迹", "Light signs of use");
        EXACT_MAPPING.put("有明显磨损但功能完好", "Visible wear but fully functional");
        
        // 评价内容
        EXACT_MAPPING.put("书很好用，我很喜欢", "Great book, I really like it");
        EXACT_MAPPING.put("这双鞋子有点脏，穿起来也不舒服呢", "The shoes are a bit dirty and uncomfortable to wear");
        
        // 申诉相关
        EXACT_MAPPING.put("申诉理由", "Appeal reason");
        EXACT_MAPPING.put("拒绝：拒绝理由", "Rejected: reason");
        EXACT_MAPPING.put("已拒绝：拒绝理由", "Rejected: reason");
        
        // 角色组合
        EXACT_MAPPING.put("Buyer and Seller", "Buyer and Seller");
        EXACT_MAPPING.put("买家 and 卖家", "Buyer and Seller");
        
        // 状态短语
        EXACT_MAPPING.put("等待卖家确认", "Waiting for seller confirmation");
        EXACT_MAPPING.put("已确认，等待收货", "Confirmed, waiting for delivery");
        EXACT_MAPPING.put("等待您确认", "Waiting for your confirmation");
        EXACT_MAPPING.put("已确认，等待买家收货", "Confirmed, waiting for buyer to receive");
        EXACT_MAPPING.put("交易完成", "Transaction completed");
        
        // ========== 词汇映射表 ==========
        
        // 枚举值
        DICTIONARY.put("全新", "Brand New");
        DICTIONARY.put("几乎全新", "Like New");
        DICTIONARY.put("良好", "Good");
        DICTIONARY.put("可接受", "Acceptable");
        
        DICTIONARY.put("电子产品", "Electronics");
        DICTIONARY.put("图书", "Books");
        DICTIONARY.put("服装", "Clothing");
        DICTIONARY.put("运动", "Sports");
        DICTIONARY.put("日用品", "Daily");
        DICTIONARY.put("其他", "Other");
        
        DICTIONARY.put("正常", "Active");
        DICTIONARY.put("已封禁", "Banned");
        DICTIONARY.put("已注销", "Deleted");
        DICTIONARY.put("在售", "Available");
        DICTIONARY.put("已预订", "Reserved");
        DICTIONARY.put("已售出", "Sold");
        DICTIONARY.put("已下架", "Removed");
        
        DICTIONARY.put("待确认", "Pending");
        DICTIONARY.put("已确认", "Confirmed");
        DICTIONARY.put("已完成", "Completed");
        DICTIONARY.put("已取消", "Cancelled");
        
        DICTIONARY.put("买家", "Buyer");
        DICTIONARY.put("卖家", "Seller");
        DICTIONARY.put("管理员", "Admin");
        
        // 通用短语
        DICTIONARY.put("暂无评价", "No reviews");
        DICTIONARY.put("等待审核", "Waiting for review");
        DICTIONARY.put("已通过", "Approved");
        DICTIONARY.put("已拒绝", "Rejected");
        DICTIONARY.put("Processed", "Processed");
        DICTIONARY.put("Pending", "Pending");
        DICTIONARY.put("Waiting for review", "Waiting for review");
    }
    
    /**
     * 翻译文本为英文（用于表格显示）
     * @param chinese 中文文本
     * @return 英文文本
     */
    public static String toEnglish(String chinese) {
        if (chinese == null || chinese.trim().isEmpty()) {
            return chinese;
        }
        
        // 0. 如果已经是英文，直接返回
        if (isEnglish(chinese)) {
            return chinese;
        }
        
        // 1. 优先使用精确映射（最高优先级，快速、离线）
        if (EXACT_MAPPING.containsKey(chinese)) {
            return EXACT_MAPPING.get(chinese);
        }
        
        // 2. 检查是否在词汇表中
        if (DICTIONARY.containsKey(chinese)) {
            return DICTIONARY.get(chinese);
        }
        
        // 3. 使用百度翻译API（支持任意中文内容）
        try {
            String translated = BaiduTranslateAPI.translate(chinese);
            // 如果翻译成功且与原文不同，返回翻译结果
            if (translated != null && !translated.equals(chinese)) {
                return translated;
            }
        } catch (Exception e) {
            // API调用失败，继续使用原文
            System.err.println("翻译API调用失败: " + e.getMessage());
        }
        
        // 4. 如果API也失败，返回原文
        return chinese;
    }
    
    /**
     * 判断字符串是否主要为英文
     */
    private static boolean isEnglish(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // 计算英文字符比例
        int englishChars = 0;
        int totalChars = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                totalChars++;
                if (c < 128) { // ASCII范围内的字符
                    englishChars++;
                }
            }
        }
        
        // 如果英文字符占80%以上，认为是英文
        return totalChars > 0 && (englishChars * 1.0 / totalChars) > 0.8;
    }
    
    /**
     * 批量翻译数组
     */
    public static String[] toEnglishArray(String... texts) {
        if (texts == null) {
            return null;
        }
        String[] result = new String[texts.length];
        for (int i = 0; i < texts.length; i++) {
            result[i] = toEnglish(texts[i]);
        }
        return result;
    }
}

