package util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 简单日志工具类
 * 
 * 功能：
 * 1. 支持不同级别的日志（DEBUG、INFO、WARN、ERROR）
 * 2. 同时输出到控制台和文件
 * 3. 自动创建日志目录
 * 4. 线程安全
 * 
 * 使用方法：
 * private static final SimpleLogger logger = SimpleLogger.getLogger(UserService.class);
 * logger.info("用户登录成功: userId={}, username={}", userId, username);
 */
public class SimpleLogger {
    
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_DIR = "logs/";
    private static final String LOG_FILE = LOG_DIR + "app.log";
    private static final String ERROR_LOG_FILE = LOG_DIR + "error.log";
    
    // 日志级别
    public enum Level {
        DEBUG(0, "调试"), INFO(1, "信息"), WARN(2, "警告"), ERROR(3, "错误");
        
        private final int value;
        private final String displayName;
        
        Level(int value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }
        
        public int getValue() {
            return value;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // 当前日志级别（可配置）
    private static Level currentLevel = Level.INFO;
    
    private final String className;
    
    /**
     * 获取 Logger 实例
     */
    public static SimpleLogger getLogger(Class<?> clazz) {
        return new SimpleLogger(clazz);
    }
    
    /**
     * 设置日志级别
     */
    public static void setLevel(Level level) {
        currentLevel = level;
    }
    
    /**
     * 私有构造器
     */
    private SimpleLogger(Class<?> clazz) {
        this.className = clazz.getSimpleName();
        ensureLogDirectoryExists();
    }
    
    /**
     * 确保日志目录存在
     */
    private void ensureLogDirectoryExists() {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * DEBUG 级别日志
     */
    public void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }
    
    /**
     * INFO 级别日志
     */
    public void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }
    
    /**
     * WARN 级别日志
     */
    public void warn(String message, Object... args) {
        log(Level.WARN, message, args);
    }
    
    /**
     * ERROR 级别日志
     */
    public void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }
    
    /**
     * ERROR 级别日志（带异常）
     */
    public void error(String message, Throwable throwable, Object... args) {
        log(Level.ERROR, message, args);
        logException(throwable);
    }
    
    /**
     * 记录日志
     */
    private synchronized void log(Level level, String message, Object... args) {
        // 级别过滤
        if (level.getValue() < currentLevel.getValue()) {
            return;
        }
        
        // 格式化消息
        String formattedMessage = formatMessage(message, args);
        
        // 构建日志行
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String logLine = String.format("%s [%s] %s - %s",
                timestamp, level.getDisplayName(), className, formattedMessage);
        
        // 不输出到控制台，保持界面整洁
        // System.out.println(logLine);
        
        // 写入文件
        writeToFile(LOG_FILE, logLine);
        
        // ERROR 级别额外写入错误日志文件
        if (level == Level.ERROR) {
            writeToFile(ERROR_LOG_FILE, logLine);
        }
    }
    
    /**
     * 记录异常堆栈
     */
    private synchronized void logException(Throwable throwable) {
        if (throwable == null) {
            return;
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();
        
        // 不输出到控制台，保持界面整洁
        // System.out.println(stackTrace);
        
        // 写入文件
        writeToFile(LOG_FILE, stackTrace);
        writeToFile(ERROR_LOG_FILE, stackTrace);
    }
    
    /**
     * 格式化消息（支持 {} 占位符）
     */
    private String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        
        String result = message;
        for (Object arg : args) {
            result = result.replaceFirst("\\{\\}", String.valueOf(arg));
        }
        return result;
    }
    
    /**
     * 写入文件
     */
    private void writeToFile(String filename, String content) {
        try {
            Files.write(
                Paths.get(filename),
                (content + System.lineSeparator()).getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            // 写入失败时输出到 System.err，避免递归调用
            System.err.println("写入日志失败：" + e.getMessage());
        }
    }
}


