package util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 百度翻译API工具类
 * 
 * API文档：https://fanyi-api.baidu.com/doc/21
 * 免费版本：每月200万字符，QPS=1（每秒1次请求）
 * 
 * 配置说明：
 * - API密钥从 config.properties 文件读取
 * - 请参考 config.properties.example 创建配置文件
 */
public class BaiduTranslateAPI {
    
    private static String APP_ID;
    private static String SECURITY_KEY;
    private static final String TRANS_API_HOST = "http://api.fanyi.baidu.com/api/trans/vip/translate";
    
    // 缓存翻译结果，避免重复调用API
    private static final Map<String, String> CACHE = new HashMap<>();
    
    // 限流控制（百度免费版QPS=1）
    private static long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL = 1100; // 1.1秒间隔，留点余量
    
    // 静态初始化块：从配置文件加载API密钥
    static {
        loadConfig();
    }
    
    /**
     * 从配置文件加载API密钥
     */
    private static void loadConfig() {
        Properties props = new Properties();
        
        // 尝试从多个位置加载配置文件
        String[] configPaths = {
            "config.properties",           // 项目根目录（优先）
            "src/config.properties",       // src目录
            "data/config.properties"       // data目录
        };
        
        boolean loaded = false;
        for (String path : configPaths) {
            File configFile = new File(path);
            if (configFile.exists()) {
                try (InputStream input = new FileInputStream(configFile)) {
                    props.load(input);
                    APP_ID = props.getProperty("baidu.translate.appid");
                    SECURITY_KEY = props.getProperty("baidu.translate.key");
                    
                    if (APP_ID != null && !APP_ID.isEmpty() && 
                        SECURITY_KEY != null && !SECURITY_KEY.isEmpty()) {
                        loaded = true;
                        // 配置加载成功，不再输出提示信息
                        break;
                    }
                } catch (IOException e) {
                    System.err.println("⚠ 读取配置文件失败: " + path + " - " + e.getMessage());
                }
            }
        }
        
        if (!loaded) {
            System.err.println("========================================");
            System.err.println("⚠ 警告：未找到有效的百度翻译API配置文件");
            System.err.println("========================================");
            System.err.println("请按以下步骤配置：");
            System.err.println("1. 将 config.properties.example 复制为 config.properties");
            System.err.println("2. 在 config.properties 中填入您的百度翻译API信息");
            System.err.println("3. 访问 https://fanyi-api.baidu.com/ 获取API密钥");
            System.err.println("========================================");
            System.err.println("系统将以降级模式运行（表格显示可能为中文）");
            System.err.println("========================================");
            // 使用空值，translate方法会检测并返回原文
            APP_ID = "";
            SECURITY_KEY = "";
        }
    }
    
    /**
     * 翻译文本（中文到英文）
     * @param query 要翻译的文本
     * @return 翻译后的英文
     */
    public static String translate(String query) {
        if (query == null || query.trim().isEmpty()) {
            return query;
        }
        
        // 0. 检查API密钥是否已配置
        if (APP_ID == null || APP_ID.isEmpty() || SECURITY_KEY == null || SECURITY_KEY.isEmpty()) {
            // 未配置密钥，返回原文（降级模式）
            return query;
        }
        
        // 1. 检查缓存
        if (CACHE.containsKey(query)) {
            return CACHE.get(query);
        }
        
        try {
            // 2. 限流控制
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRequest = currentTime - lastRequestTime;
            if (timeSinceLastRequest < MIN_REQUEST_INTERVAL) {
                Thread.sleep(MIN_REQUEST_INTERVAL - timeSinceLastRequest);
            }
            
            // 3. 生成签名
            String salt = String.valueOf(System.currentTimeMillis());
            String sign = generateSign(query, salt);
            
            // 4. 构建请求URL
            String urlStr = TRANS_API_HOST + 
                "?q=" + URLEncoder.encode(query, "UTF-8") +
                "&from=zh" +
                "&to=en" +
                "&appid=" + APP_ID +
                "&salt=" + salt +
                "&sign=" + sign;
            
            // 5. 发送HTTP请求
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            // 6. 读取响应
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // 7. 更新最后请求时间
            lastRequestTime = System.currentTimeMillis();
            
            // 8. 解析JSON响应
            String result = parseResponse(response.toString());
            
            // 9. 缓存结果
            if (result != null && !result.isEmpty()) {
                CACHE.put(query, result);
                return result;
            }
            
            // 10. 翻译失败，返回原文
            return query;
            
        } catch (Exception e) {
            // API调用失败，返回原文
            System.err.println("百度翻译API调用失败: " + e.getMessage());
            return query;
        }
    }
    
    /**
     * 生成MD5签名
     * 签名规则：appid+query+salt+密钥 的MD5值
     */
    private static String generateSign(String query, String salt) {
        try {
            String src = APP_ID + query + salt + SECURITY_KEY;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(src.getBytes(StandardCharsets.UTF_8));
            
            // 转换为32位小写16进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5签名生成失败", e);
        }
    }
    
    /**
     * 解析百度翻译API响应
     * 响应格式：{"from":"zh","to":"en","trans_result":[{"src":"原文","dst":"译文"}]}
     */
    private static String parseResponse(String jsonResponse) {
        try {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(jsonResponse, JsonObject.class);
            
            // 检查是否有错误
            if (json.has("error_code")) {
                String errorCode = json.get("error_code").getAsString();
                String errorMsg = json.has("error_msg") ? json.get("error_msg").getAsString() : "Unknown error";
                System.err.println("百度翻译API错误: " + errorCode + " - " + errorMsg);
                return null;
            }
            
            // 提取翻译结果
            if (json.has("trans_result")) {
                JsonArray transResult = json.getAsJsonArray("trans_result");
                if (transResult.size() > 0) {
                    JsonObject firstResult = transResult.get(0).getAsJsonObject();
                    return firstResult.get("dst").getAsString();
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("解析翻译结果失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 清空缓存（用于测试或内存管理）
     */
    public static void clearCache() {
        CACHE.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public static int getCacheSize() {
        return CACHE.size();
    }
}

