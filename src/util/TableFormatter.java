package util;

import java.util.ArrayList;
import java.util.List;

/**
 * 表格格式化工具类
 * 基于East Asian Width标准精确计算字符显示宽度
 * 完美支持中英文混合对齐
 */
public class TableFormatter {
    
    /**
     * 获取字符的显示宽度
     * 基于Unicode East Asian Width标准
     */
    private static int getCharWidth(char c) {
        // CJK统一汉字
        if (c >= 0x4E00 && c <= 0x9FFF) return 2;
        if (c >= 0x3400 && c <= 0x4DBF) return 2;
        if (c >= 0x20000 && c <= 0x2A6DF) return 2;
        if (c >= 0x2A700 && c <= 0x2B73F) return 2;
        if (c >= 0x2B740 && c <= 0x2B81F) return 2;
        if (c >= 0x2B820 && c <= 0x2CEAF) return 2;
        if (c >= 0xF900 && c <= 0xFAFF) return 2;
        if (c >= 0x2F800 && c <= 0x2FA1F) return 2;
        
        // 全角ASCII和全角标点
        if (c >= 0xFF01 && c <= 0xFF60) return 2;
        if (c >= 0xFFE0 && c <= 0xFFE6) return 2;
        
        // CJK标点符号
        if (c >= 0x3000 && c <= 0x303F) return 2;
        
        // 平假名和片假名
        if (c >= 0x3040 && c <= 0x309F) return 2;
        if (c >= 0x30A0 && c <= 0x30FF) return 2;
        
        // 韩文音节
        if (c >= 0xAC00 && c <= 0xD7AF) return 2;
        
        // 默认为半角
        return 1;
    }
    
    /**
     * 计算字符串的显示宽度
     */
    public static int getDisplayWidth(String str) {
        if (str == null) return 0;
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            width += getCharWidth(str.charAt(i));
        }
        return width;
    }
    
    /**
     * 将字符串填充到指定的显示宽度（左对齐）
     */
    public static String padRight(String str, int targetWidth) {
        if (str == null) str = "";
        int currentWidth = getDisplayWidth(str);
        
        // 如果超长，截断
        if (currentWidth > targetWidth) {
            int width = 0;
            int cutIndex = 0;
            for (int i = 0; i < str.length(); i++) {
                int charWidth = getCharWidth(str.charAt(i));
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
        return str + " ".repeat(targetWidth - currentWidth);
    }
    
    /**
     * 将字符串填充到指定的显示宽度（右对齐）
     */
    public static String padLeft(String str, int targetWidth) {
        if (str == null) str = "";
        int currentWidth = getDisplayWidth(str);
        
        // 如果超长，截断
        if (currentWidth > targetWidth) {
            int width = 0;
            int cutIndex = 0;
            for (int i = 0; i < str.length(); i++) {
                int charWidth = getCharWidth(str.charAt(i));
                if (width + charWidth > targetWidth) {
                    break;
                }
                width += charWidth;
                cutIndex = i + 1;
            }
            str = str.substring(0, cutIndex);
            currentWidth = width;
        }
        
        // 左边补齐空格
        return " ".repeat(targetWidth - currentWidth) + str;
    }
    
    /**
     * 表格构建器
     */
    public static class Table {
        private final List<String[]> rows = new ArrayList<>();
        private final int[] columnWidths;
        private final boolean[] rightAlign;
        private String[] headers;
        
        public Table(int... columnWidths) {
            this.columnWidths = columnWidths;
            this.rightAlign = new boolean[columnWidths.length];
        }
        
        /**
         * 设置列右对齐
         */
        public Table setRightAlign(int... columnIndices) {
            for (int index : columnIndices) {
                if (index >= 0 && index < rightAlign.length) {
                    rightAlign[index] = true;
                }
            }
            return this;
        }
        
        /**
         * 设置表头
         */
        public Table setHeaders(String... headers) {
            this.headers = headers;
            return this;
        }
        
        /**
         * 添加一行数据
         */
        public Table addRow(String... row) {
            rows.add(row);
            return this;
        }
        
        /**
         * 渲染表格
         */
        public String render() {
            StringBuilder sb = new StringBuilder();
            
            // 计算总宽度（使用显示宽度）
            int totalDisplayWidth = 0;
            for (int width : columnWidths) {
                totalDisplayWidth += width + 4; // 每列之间4个空格间隔
            }
            
            String separator = "=".repeat(Math.min(totalDisplayWidth, 100));
            
            // 打印上边框
            sb.append(separator).append("\n");
            
            // 打印表头
            if (headers != null) {
                sb.append(formatRow(headers));
                sb.append(separator).append("\n");
            }
            
            // 打印数据行
            for (String[] row : rows) {
                sb.append(formatRow(row));
            }
            
            // 打印下边框
            sb.append(separator).append("\n");
            
            return sb.toString();
        }
        
        /**
         * 格式化一行（无边框格式）
         */
        private String formatRow(String[] row) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < columnWidths.length && i < row.length; i++) {
                if (i > 0) {
                    sb.append("    "); // 列间用4个空格分隔
                }
                if (rightAlign[i]) {
                    sb.append(padLeft(row[i], columnWidths[i]));
                } else {
                    sb.append(padRight(row[i], columnWidths[i]));
                }
            }
            sb.append("\n");
            return sb.toString();
        }
        
        /**
         * 打印表格
         */
        public void print() {
            System.out.print(render());
        }
    }
}

