package util;

/**
 * 控制台输出工具
 * 
 * 设计说明：
 * 统一管理控制台输出格式，提供美化的输出方法
 */
public class ConsoleUtil {
    
    private static final String DIVIDER = "=".repeat(60);
    private static final String LINE = "-".repeat(60);
    
    /**
     * 打印分隔线
     */
    public static void printDivider() {
        System.out.println(DIVIDER);
    }
    
    /**
     * 打印线条
     */
    public static void printLine() {
        System.out.println(LINE);
    }
    
    /**
     * 打印标题
     */
    public static void printTitle(String title) {
        printDivider();
        System.out.println("  " + title);
        printDivider();
    }
    
    /**
     * 打印成功消息
     */
    public static void printSuccess(String message) {
        System.out.println("[成功] " + message);
    }
    
    /**
     * 打印错误消息
     */
    public static void printError(String message) {
        System.out.println("[错误] " + message);
    }
    
    /**
     * 打印信息消息
     */
    public static void printInfo(String message) {
        System.out.println("[信息] " + message);
    }
    
    /**
     * 打印警告消息
     */
    public static void printWarning(String message) {
        System.out.println("[警告] " + message);
    }
    
    /**
     * 清空控制台（尝试）
     */
    public static void clear() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // 如果清屏失败，打印空行
            System.out.println("\n".repeat(50));
        }
    }
    
    /**
     * 等待用户按回车继续
     */
    public static void waitForEnter() {
        System.out.print("\n按回车键继续...");
        try {
            System.in.read();
        } catch (Exception e) {
            // 忽略异常
        }
    }
}


