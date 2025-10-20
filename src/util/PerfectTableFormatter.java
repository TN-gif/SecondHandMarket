package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 完美表格格式化工具
 * 
 * 基于wcwidth算法精确计算字符显示宽度，确保中英文混合表格完美对齐
 */
public class PerfectTableFormatter {
    
    /**
     * 计算单个字符的显示宽度
     * 基于Unicode East Asian Width标准
     */
    private static int getCharWidth(int codePoint) {
        // 控制字符
        if (codePoint == 0 || codePoint == 0x034F || 
            (0x200B <= codePoint && codePoint <= 0x200F) ||
            codePoint == 0x2028 || codePoint == 0x2029 ||
            (0x202A <= codePoint && codePoint <= 0x202E) ||
            (0x2060 <= codePoint && codePoint <= 0x2063)) {
            return 0;
        }
        
        // CJK统一汉字 (U+4E00-U+9FFF)
        if (codePoint >= 0x4E00 && codePoint <= 0x9FFF) return 2;
        
        // CJK扩展A (U+3400-U+4DBF)
        if (codePoint >= 0x3400 && codePoint <= 0x4DBF) return 2;
        
        // CJK兼容汉字 (U+F900-U+FAFF)
        if (codePoint >= 0xF900 && codePoint <= 0xFAFF) return 2;
        
        // CJK兼容形式 (U+FE30-U+FE4F)
        if (codePoint >= 0xFE30 && codePoint <= 0xFE4F) return 2;
        
        // 全角ASCII、全角标点 (U+FF00-U+FFEF)
        if (codePoint >= 0xFF00 && codePoint <= 0xFF60) return 2;
        if (codePoint >= 0xFFE0 && codePoint <= 0xFFE6) return 2;
        
        // 中文标点符号 (U+3000-U+303F)
        if (codePoint >= 0x3000 && codePoint <= 0x303F) return 2;
        
        // 平假名 (U+3040-U+309F)
        if (codePoint >= 0x3040 && codePoint <= 0x309F) return 2;
        
        // 片假名 (U+30A0-U+30FF)
        if (codePoint >= 0x30A0 && codePoint <= 0x30FF) return 2;
        
        // 谚文音节 (U+AC00-U+D7AF)
        if (codePoint >= 0xAC00 && codePoint <= 0xD7AF) return 2;
        
        // CJK扩展B及以后 (U+20000-U+2FFFF)
        if (codePoint >= 0x20000 && codePoint <= 0x2FFFF) return 2;
        
        // 其他全角字符
        if (codePoint >= 0x1100 && codePoint <= 0x115F) return 2; // 谚文
        if (codePoint >= 0x2E80 && codePoint <= 0x2EFF) return 2; // CJK部首补充
        if (codePoint >= 0x2F00 && codePoint <= 0x2FDF) return 2; // 康熙部首
        if (codePoint >= 0x3200 && codePoint <= 0x32FF) return 2; // 带圈CJK字母
        if (codePoint >= 0x3300 && codePoint <= 0x33FF) return 2; // CJK兼容
        
        // 默认为1（半角）
        return 1;
    }
    
    /**
     * 计算字符串的显示宽度
     */
    private static int getDisplayWidth(String str) {
        if (str == null) return 0;
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            int codePoint = str.codePointAt(i);
            width += getCharWidth(codePoint);
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++; // 跳过低代理项
            }
        }
        return width;
    }
    
    /**
     * 截断字符串到指定显示宽度（如果超长）
     */
    private static String truncateToWidth(String str, int maxWidth) {
        if (str == null) return "";
        int width = 0;
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < str.length(); i++) {
            int codePoint = str.codePointAt(i);
            int charWidth = getCharWidth(codePoint);
            
            if (width + charWidth > maxWidth) {
                break;
            }
            
            result.appendCodePoint(codePoint);
            width += charWidth;
            
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++; // 跳过低代理项
            }
        }
        
        return result.toString();
    }
    
    /**
     * 将字符串填充到指定显示宽度（左对齐）
     */
    private static String padRight(String str, int targetWidth) {
        if (str == null) str = "";
        int currentWidth = getDisplayWidth(str);
        int spacesToAdd = targetWidth - currentWidth;
        if (spacesToAdd <= 0) {
            return truncateToWidth(str, targetWidth);
        }
        return str + " ".repeat(spacesToAdd);
    }
    
    /**
     * 将字符串填充到指定显示宽度（右对齐）
     */
    private static String padLeft(String str, int targetWidth) {
        if (str == null) str = "";
        int currentWidth = getDisplayWidth(str);
        int spacesToAdd = targetWidth - currentWidth;
        if (spacesToAdd <= 0) {
            return truncateToWidth(str, targetWidth);
        }
        return " ".repeat(spacesToAdd) + str;
    }
    
    /**
     * 表格构建器
     */
    public static class Table {
        private String[] headers;
        private List<String[]> rows;
        private int[] columnWidths;
        
        public Table() {
            this.rows = new ArrayList<>();
        }
        
        /**
         * 设置表头
         */
        public Table setHeaders(String... headers) {
            this.headers = headers;
            this.columnWidths = new int[headers.length];
            
            // 初始化列宽为表头宽度
            for (int i = 0; i < headers.length; i++) {
                columnWidths[i] = getDisplayWidth(headers[i]);
            }
            
            return this;
        }
        
        /**
         * 添加一行数据
         */
        public Table addRow(String... row) {
            if (headers == null) {
                throw new IllegalStateException("必须先设置表头");
            }
            if (row.length != headers.length) {
                throw new IllegalArgumentException("行数据列数必须与表头一致");
            }
            
            this.rows.add(row);
            
            // 更新列宽
            for (int i = 0; i < row.length; i++) {
                int width = getDisplayWidth(row[i]);
                if (width > columnWidths[i]) {
                    columnWidths[i] = width;
                }
            }
            
            return this;
        }
        
        /**
         * 打印表格
         */
        public void print() {
            if (headers == null || headers.length == 0) {
                System.out.println("暂无数据");
                return;
            }
            
            if (rows.isEmpty()) {
                System.out.println("暂无数据");
                return;
            }
            
            // 每列增加2个空格的padding
            for (int i = 0; i < columnWidths.length; i++) {
                columnWidths[i] += 2;
            }
            
            // 打印顶部边框
            printBorder("╔", "═", "╦", "╗");
            
            // 打印表头
            System.out.print("║");
            for (int i = 0; i < headers.length; i++) {
                System.out.print(" " + padRight(headers[i], columnWidths[i] - 1));
                System.out.print("║");
            }
            System.out.println();
            
            // 打印表头分隔线
            printBorder("╠", "═", "╬", "╣");
            
            // 打印数据行
            for (String[] row : rows) {
                System.out.print("║");
                for (int i = 0; i < row.length; i++) {
                    System.out.print(" " + padRight(row[i], columnWidths[i] - 1));
                    System.out.print("║");
                }
                System.out.println();
            }
            
            // 打印底部边框
            printBorder("╚", "═", "╩", "╝");
        }
        
        /**
         * 打印边框线
         */
        private void printBorder(String left, String middle, String cross, String right) {
            System.out.print(left);
            for (int i = 0; i < columnWidths.length; i++) {
                System.out.print(middle.repeat(columnWidths[i]));
                if (i < columnWidths.length - 1) {
                    System.out.print(cross);
                }
            }
            System.out.println(right);
        }
    }
    
    /**
     * 创建一个新表格
     */
    public static Table createTable() {
        return new Table();
    }
}

