package repository;

import entity.Message;
import entity.Order;
import entity.Product;
import entity.Review;
import entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据中心 - 单例模式
 * 
 * 设计模式：单例模式（Singleton Pattern）
 * 
 * 设计说明：
 * 1. 使用饿汉式单例，线程安全
 * 2. 使用ConcurrentHashMap存储数据，支持并发访问
 * 3. 提供CRUD操作的统一接口
 * 
 * 答辩要点：
 * Q: 为什么选择饿汉式而不是懒汉式？
 * A: 因为DataCenter是核心组件，必然会被使用，没有延迟加载的必要。
 *    饿汉式更简单，且天然线程安全。
 * 
 * Q: 为什么使用ConcurrentHashMap？
 * A: 虽然本项目是单线程应用，但使用ConcurrentHashMap是良好的工程实践，
 *    为未来可能的并发场景做准备，且性能开销可以忽略。
 */
public class DataCenter {
    
    // ========== 单例实现 ==========
    
    /**
     * 饿汉式单例：类加载时就创建实例
     */
    private static final DataCenter INSTANCE = new DataCenter();
    
    /**
     * 获取单例实例
     */
    public static DataCenter getInstance() {
        return INSTANCE;
    }
    
    /**
     * 私有构造器，防止外部创建实例
     */
    private DataCenter() {
        System.out.println("[系统] DataCenter 初始化完成");
    }
    
    // ========== 数据存储 ==========
    
    /**
     * 用户数据（key: userId, value: User）
     */
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    /**
     * 商品数据（key: productId, value: Product）
     */
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    
    /**
     * 订单数据（key: orderId, value: Order）
     */
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    
    /**
     * 评价数据（key: reviewId, value: Review）
     */
    private final Map<String, Review> reviews = new ConcurrentHashMap<>();
    
    /**
     * 消息数据（key: messageId, value: Message）
     */
    private final Map<String, Message> messages = new ConcurrentHashMap<>();
    
    // ========== 用户相关操作 ==========
    
    /**
     * 添加用户
     */
    public void addUser(User user) {
        users.put(user.getUserId(), user);
    }
    
    /**
     * 根据ID查找用户
     */
    public Optional<User> findUserById(String userId) {
        return Optional.ofNullable(users.get(userId));
    }
    
    /**
     * 根据用户名查找用户
     */
    public Optional<User> findUserByUsername(String username) {
        return users.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }
    
    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(String userId) {
        users.remove(userId);
    }
    
    /**
     * 检查用户名是否存在
     */
    public boolean existsUsername(String username) {
        return users.values().stream()
                .anyMatch(u -> u.getUsername().equals(username));
    }
    
    // ========== 商品相关操作 ==========
    
    /**
     * 添加商品
     */
    public void addProduct(Product product) {
        products.put(product.getProductId(), product);
    }
    
    /**
     * 根据ID查找商品
     */
    public Optional<Product> findProductById(String productId) {
        return Optional.ofNullable(products.get(productId));
    }
    
    /**
     * 获取所有商品
     */
    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }
    
    /**
     * 根据卖家ID获取商品列表
     */
    public List<Product> findProductsBySeller(String sellerId) {
        return products.values().stream()
                .filter(p -> p.getSellerId().equals(sellerId))
                .toList();
    }
    
    /**
     * 删除商品
     */
    public void deleteProduct(String productId) {
        products.remove(productId);
    }
    
    // ========== 订单相关操作 ==========
    
    /**
     * 添加订单
     */
    public void addOrder(Order order) {
        orders.put(order.getOrderId(), order);
    }
    
    /**
     * 根据ID查找订单
     */
    public Optional<Order> findOrderById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }
    
    /**
     * 获取所有订单
     */
    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }
    
    /**
     * 根据买家ID获取订单列表
     */
    public List<Order> findOrdersByBuyer(String buyerId) {
        return orders.values().stream()
                .filter(o -> o.getBuyerId().equals(buyerId))
                .toList();
    }
    
    /**
     * 根据卖家ID获取订单列表
     */
    public List<Order> findOrdersBySeller(String sellerId) {
        return orders.values().stream()
                .filter(o -> o.getSellerId().equals(sellerId))
                .toList();
    }
    
    /**
     * 根据商品ID获取订单
     */
    public Optional<Order> findOrderByProductId(String productId) {
        return orders.values().stream()
                .filter(o -> o.getProductId().equals(productId))
                .findFirst();
    }
    
    /**
     * 删除订单
     */
    public void deleteOrder(String orderId) {
        orders.remove(orderId);
    }
    
    // ========== 评价相关操作 ==========
    
    /**
     * 添加评价
     */
    public void addReview(Review review) {
        reviews.put(review.getReviewId(), review);
    }
    
    /**
     * 根据ID查找评价
     */
    public Optional<Review> findReviewById(String reviewId) {
        return Optional.ofNullable(reviews.get(reviewId));
    }
    
    /**
     * 获取所有评价
     */
    public List<Review> getAllReviews() {
        return new ArrayList<>(reviews.values());
    }
    
    /**
     * 根据订单ID查找评价
     */
    public Optional<Review> findReviewByOrderId(String orderId) {
        return reviews.values().stream()
                .filter(r -> r.getOrderId().equals(orderId))
                .findFirst();
    }
    
    /**
     * 根据被评价者ID获取评价列表
     */
    public List<Review> findReviewsByReviewee(String revieweeId) {
        return reviews.values().stream()
                .filter(r -> r.getRevieweeId().equals(revieweeId))
                .toList();
    }
    
    /**
     * 删除评价
     */
    public void deleteReview(String reviewId) {
        reviews.remove(reviewId);
    }
    
    // ========== 统计相关方法 ==========
    
    /**
     * 获取用户总数
     */
    public int getUserCount() {
        return users.size();
    }
    
    /**
     * 获取商品总数
     */
    public int getProductCount() {
        return products.size();
    }
    
    /**
     * 获取订单总数
     */
    public int getOrderCount() {
        return orders.size();
    }
    
    /**
     * 获取评价总数
     */
    public int getReviewCount() {
        return reviews.size();
    }
    
    // ========== 消息相关操作 ==========
    
    /**
     * 添加消息
     */
    public void addMessage(Message message) {
        messages.put(message.getMessageId(), message);
    }
    
    /**
     * 根据ID查找消息
     */
    public Optional<Message> findMessageById(String messageId) {
        return Optional.ofNullable(messages.get(messageId));
    }
    
    /**
     * 获取所有消息
     */
    public List<Message> getAllMessages() {
        return new ArrayList<>(messages.values());
    }
    
    /**
     * 根据用户ID获取消息列表
     */
    public List<Message> findMessagesByUserId(String userId) {
        return messages.values().stream()
                .filter(m -> m.getUserId().equals(userId))
                .sorted((m1, m2) -> m2.getCreateTime().compareTo(m1.getCreateTime())) // 降序排列
                .toList();
    }
    
    /**
     * 获取消息总数
     */
    public int getMessageCount() {
        return messages.size();
    }
    
    /**
     * 删除消息
     */
    public void deleteMessage(String messageId) {
        messages.remove(messageId);
    }
    
    /**
     * 清空所有数据（用于测试）
     */
    public void clearAll() {
        users.clear();
        products.clear();
        orders.clear();
        reviews.clear();
        messages.clear();
        System.out.println("[系统] 数据已清空");
    }
}


