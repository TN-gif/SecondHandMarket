package util;

import enums.OrderStatus;

import java.util.Scanner;

/**
 * UI交互辅助工具类
 * 
 * 为控制台界面提供通用的输入处理和显示格式化功能。
 * 主要解决中英文混合显示时的字符宽度计算问题。
 */
public class UIHelper {
    
    private static final Scanner scanner = new Scanner(System.in);
    
    /**
     * 安全读取整数输入
     * 
     * 自动验证输入格式，如果格式错误则显示错误信息并返回null。
     * 
     * @param prompt 提示信息
     * @param errorMsg 格式错误时的提示信息
     * @return 解析后的整数，格式错误返回null
     */
    public static Integer readIntSafely(String prompt, String errorMsg) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        
        if (!InputValidator.isValidInteger(input)) {
            ConsoleUtil.printError(errorMsg != null ? errorMsg : "输入格式无效，请输入有效数字");
            return null;
        }
        
        return InputValidator.parseIntSafe(input);
    }
    
    /**
     * 读取字符串输入
     * 
     * @param prompt 提示信息
     * @return 用户输入的字符串
     */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
    
    /**
     * 读取确认输入（y/n）
     * 
     * @param prompt 提示信息
     * @return 如果输入y或Y返回true，否则返回false
     */
    public static boolean readConfirmation(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().equalsIgnoreCase("y");
    }
    
    /**
     * 带重试机制的输入读取
     * 
     * 当输入验证失败时，询问用户是否重试。
     * 
     * @param prompt 输入提示
     * @param validator 验证函数
     * @param errorMsg 验证失败的错误信息
     * @param <T> 返回值类型
     * @return 验证通过的输入值，用户放弃则返回null
     */
    public static <T> T readWithRetry(String prompt, java.util.function.Function<String, T> validator, String errorMsg) {
        while (true) {
            String input = readLine(prompt);
            
            if (input.equals("0")) {
                return null;
            }
            
            try {
                T result = validator.apply(input);
                if (result != null) {
                    return result;
                }
            } catch (Exception e) {
                ConsoleUtil.printError(errorMsg + ": " + e.getMessage());
            }
            
            if (!readConfirmation("是否重试？(y/n)：")) {
                return null;
            }
        }
    }
    
    /**
     * 获取订单状态的买家视角描述
     * 
     * @param status 订单状态
     * @return 买家视角的状态描述
     */
    public static String getOrderStatusForBuyer(OrderStatus status) {
        return switch (status) {
            case PENDING -> "等待卖家确认";
            case CONFIRMED -> "已确认，等待收货";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
        };
    }
    
    /**
     * 获取订单状态的卖家视角描述
     * 
     * @param status 订单状态
     * @return 卖家视角的状态描述
     */
    public static String getOrderStatusForSeller(OrderStatus status) {
        return switch (status) {
            case PENDING -> "等待您确认";
            case CONFIRMED -> "已确认，等待买家收货";
            case COMPLETED -> "交易完成";
            case CANCELLED -> "已取消";
        };
    }
    
    /**
     * 计算字符串的实际显示宽度
     * 
     * 为了在控制台中正确对齐中英文混合文本，需要精确计算显示宽度。
     * 中文、日文等全角字符占2个显示位置，英文等半角字符占1个位置。
     * 
     * @param str 要计算的字符串
     * @return 显示宽度（字符数）
     */
    public static int getDisplayWidth(String str) {
        if (str == null) return 0;
        
        int width = 0;
        for (char c : str.toCharArray()) {
            width += isFullWidth(c) ? 2 : 1;
        }
        return width;
    }
    
    /**
     * 判断字符是否为全角字符
     * 
     * 基于Unicode East Asian Width标准实现。
     * 全角字符在控制台中占用2个显示位置，包括：
     * - CJK统一汉字（中日韩文字）
     * - 平假名、片假名
     * - 韩文音节
     * - 全角标点和符号
     * 
     * @param c 要判断的字符
     * @return 如果是全角字符返回true
     */
    public static boolean isFullWidth(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        
        if (block == null) return false;
        
        // CJK相关字符块
        if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
            || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || block == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT
            || block == Character.UnicodeBlock.KANGXI_RADICALS) {
            return true;
        }
        
        // 日韩文字
        if (block == Character.UnicodeBlock.HIRAGANA
            || block == Character.UnicodeBlock.KATAKANA
            || block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HANGUL_JAMO
            || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
            return true;
        }
        
        // 全角ASCII和全角标点
        if (block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return (c >= 0xFF01 && c <= 0xFF60) || (c >= 0xFFE0 && c <= 0xFFE6);
        }
        
        // 某些全角标点符号
        if (block == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            return c >= 0x2000 && c <= 0x206F;
        }
        
        // 常用全角符号（在Windows终端中显示为双宽）
        if (c == '★' || c == '☆' || c == '●' || c == '○' || 
            c == '■' || c == '□' || c == '▲' || c == '△' ||
            c == '◆' || c == '◇' || c == '※' || c == '√' ||
            c == '✓' || c == '✗' || c == '✕') {
            return true;
        }
        
        return false;
    }
    
    /**
     * 填充字符串到指定显示宽度
     * 
     * 用于表格对齐。如果字符串超过目标宽度则截断，不足则用空格填充。
     * 
     * @param str 原始字符串
     * @param targetWidth 目标显示宽度
     * @return 填充后的字符串
     */
    public static String padToWidth(String str, int targetWidth) {
        if (str == null) str = "";
        
        int currentWidth = getDisplayWidth(str);
        
        // 超长则截断
        if (currentWidth > targetWidth) {
            int width = 0;
            int cutIndex = 0;
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                int charWidth = isFullWidth(c) ? 2 : 1;
                if (width + charWidth > targetWidth) {
                    break;
                }
                width += charWidth;
                cutIndex = i + 1;
            }
            str = str.substring(0, cutIndex);
            currentWidth = width;
        }
        
        // 补齐空格
        StringBuilder sb = new StringBuilder(str);
        for (int i = currentWidth; i < targetWidth; i++) {
            sb.append(" ");
        }
        
        return sb.toString();
    }
}



