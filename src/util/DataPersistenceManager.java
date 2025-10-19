package util;

import adapter.LocalDateTimeAdapter;
import adapter.UserRoleSetAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import entity.Message;
import entity.Order;
import entity.Product;
import entity.Review;
import entity.User;
import enums.UserRole;
import repository.DataCenter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 数据持久化管理器
 * 使用JSON格式保存数据
 * 
 * 技术要点：
 * 1. 为EnumSet注册自定义TypeAdapter
 * 2. 为LocalDateTime注册自定义TypeAdapter
 * 3. 优雅地处理文件不存在的情况
 * 
 * 答辩要点：
 * - EnumSet序列化问题：Gson默认序列化复杂，自定义适配器简化为数组
 * - LocalDateTime序列化：JDK 8时间API需要自定义适配器
 * - TypeToken解决泛型擦除：使用TypeToken保留泛型类型信息
 */
public class DataPersistenceManager {
    private static final String DATA_DIR = "data/";
    private static final String USERS_FILE = DATA_DIR + "users.json";
    private static final String PRODUCTS_FILE = DATA_DIR + "products.json";
    private static final String ORDERS_FILE = DATA_DIR + "orders.json";
    private static final String REVIEWS_FILE = DATA_DIR + "reviews.json";
    private static final String MESSAGES_FILE = DATA_DIR + "messages.json";
    
    private final Gson gson;
    
    public DataPersistenceManager() {
        // 配置Gson，注册自定义TypeAdapter
        gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            // 关键：注册EnumSet的TypeAdapter（解决序列化问题）
            .registerTypeAdapter(
                new TypeToken<Set<UserRole>>(){}.getType(), 
                new UserRoleSetAdapter()
            )
            .create();
        
        // 创建数据目录
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    /**
     * 保存所有数据
     */
    public void saveAll() {
        DataCenter dc = DataCenter.getInstance();
        
        try {
            saveToFile(USERS_FILE, dc.getAllUsers());
            saveToFile(PRODUCTS_FILE, dc.getAllProducts());
            saveToFile(ORDERS_FILE, dc.getAllOrders());
            saveToFile(REVIEWS_FILE, dc.getAllReviews());
            saveToFile(MESSAGES_FILE, dc.getAllMessages());
            
            ConsoleUtil.printInfo("数据已保存");
        } catch (IOException e) {
            ConsoleUtil.printError("数据保存失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载所有数据
     */
    public void loadAll() {
        DataCenter dc = DataCenter.getInstance();
        
        try {
            // 加载用户
            List<User> users = loadFromFile(USERS_FILE, 
                new TypeToken<List<User>>(){}.getType());
            if (users != null) {
                users.forEach(dc::addUser);
            }
            
            // 加载商品
            List<Product> products = loadFromFile(PRODUCTS_FILE,
                new TypeToken<List<Product>>(){}.getType());
            if (products != null) {
                products.forEach(dc::addProduct);
            }
            
            // 加载订单
            List<Order> orders = loadFromFile(ORDERS_FILE,
                new TypeToken<List<Order>>(){}.getType());
            if (orders != null) {
                orders.forEach(dc::addOrder);
            }
            
            // 加载评价
            List<Review> reviews = loadFromFile(REVIEWS_FILE,
                new TypeToken<List<Review>>(){}.getType());
            if (reviews != null) {
                reviews.forEach(dc::addReview);
            }
            
            // 加载消息
            List<Message> messages = loadFromFile(MESSAGES_FILE,
                new TypeToken<List<Message>>(){}.getType());
            if (messages != null) {
                messages.forEach(dc::addMessage);
            }
            
            ConsoleUtil.printInfo("数据已加载");
        } catch (IOException e) {
            ConsoleUtil.printInfo("首次运行，初始化数据");
        }
    }
    
    /**
     * 保存对象到文件
     */
    private void saveToFile(String filename, Object data) throws IOException {
        String json = gson.toJson(data);
        Files.writeString(Path.of(filename), json, StandardCharsets.UTF_8);
    }
    
    /**
     * 从文件加载对象
     */
    private <T> T loadFromFile(String filename, Type type) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }
        
        String json = Files.readString(Path.of(filename), StandardCharsets.UTF_8);
        return gson.fromJson(json, type);
    }
}


