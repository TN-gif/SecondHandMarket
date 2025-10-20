package util;

import com.jakewharton.fliptables.FlipTable;

import java.util.ArrayList;
import java.util.List;

/**
 * 专业表格格式化工具（使用FlipTables库）
 * 
 * 该类封装了FlipTables库的使用，提供简单的API用于创建和显示表格
 * FlipTables库原生支持Unicode字符，能够正确处理中文对齐
 */
public class ProfessionalTableFormatter {
    
    /**
     * 表格构建器
     */
    public static class Table {
        private String[] headers;
        private List<String[]> rows;
        
        public Table() {
            this.rows = new ArrayList<>();
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
            this.rows.add(row);
            return this;
        }
        
        /**
         * 渲染并打印表格
         */
        public void print() {
            if (headers == null || headers.length == 0) {
                System.out.println("表格未设置标题");
                return;
            }
            
            if (rows.isEmpty()) {
                System.out.println("暂无数据");
                return;
            }
            
            // 将List转换为二维数组
            String[][] data = new String[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                data[i] = rows.get(i);
            }
            
            // 使用FlipTable渲染
            String output = FlipTable.of(headers, data);
            System.out.println(output);
        }
        
        /**
         * 渲染并返回表格字符串
         */
        public String render() {
            if (headers == null || headers.length == 0) {
                return "表格没有设置表头";
            }
            
            if (rows.isEmpty()) {
                return "暂无数据";
            }
            
            // 将List转换为二维数组
            String[][] data = new String[rows.size()][];
            for (int i = 0; i < rows.size(); i++) {
                data[i] = rows.get(i);
            }
            
            // 使用FlipTable渲染
            return FlipTable.of(headers, data);
        }
    }
    
    /**
     * 创建一个新表格
     */
    public static Table createTable() {
        return new Table();
    }
}

