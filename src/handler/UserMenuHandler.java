package handler;

import dto.SearchCriteria;
import entity.Order;
import entity.Product;
import entity.User;
import enums.*;
import observer.UserMessageReceiver;
import service.*;
import strategy.ProductSortStrategies;
import util.ConsoleUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 用户菜单处理器
 * 职责：处理买家/卖家登录后的交互
 */
public class UserMenuHandler implements MenuHandler {
    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final Scanner scanner;
    
    public UserMenuHandler(UserService userService, ProductService productService,
                          OrderService orderService, ReviewService reviewService) {
        this.userService = userService;
        this.productService = productService;
        this.orderService = orderService;
        this.reviewService = reviewService;
        this.scanner = new Scanner(System.in);
    }
    
    @Override
    public void displayAndHandle() {
        User user = userService.getCurrentUser();
        
        // 显示用户信息
        ConsoleUtil.printInfo(String.format("欢迎，%s | 信誉：%d分 [%s]", 
            user.getUsername(), user.getReputation(), user.getReputationLevel()));
        
        ConsoleUtil.printDivider();
        
        // 根据角色显示菜单
        int menuIndex = 1;
        Map<String, Runnable> menuActions = new LinkedHashMap<>();
        
        if (user.hasRole(UserRole.BUYER)) {
            System.out.println("【买家功能】");
            menuActions.put(String.valueOf(menuIndex), this::handleBrowseProducts);
            System.out.println("[" + menuIndex++ + "] 浏览商品");
            
            menuActions.put(String.valueOf(menuIndex), this::handleSearchProducts);
            System.out.println("[" + menuIndex++ + "] 搜索商品");
            
            menuActions.put(String.valueOf(menuIndex), this::handleMyOrders);
            System.out.println("[" + menuIndex++ + "] 我的订单");
            
            menuActions.put(String.valueOf(menuIndex), this::handleConfirmReceipt);
            System.out.println("[" + menuIndex++ + "] 确认收货");
            
            menuActions.put(String.valueOf(menuIndex), this::handleReviewOrder);
            System.out.println("[" + menuIndex++ + "] 评价订单");
            
            menuActions.put(String.valueOf(menuIndex), this::handleViewReviews);
            System.out.println("[" + menuIndex++ + "] 查看评价");
        }
        
        if (user.hasRole(UserRole.SELLER)) {
            System.out.println("【卖家功能】");
            menuActions.put(String.valueOf(menuIndex), this::handlePublishProduct);
            System.out.println("[" + menuIndex++ + "] 发布商品");
            
            menuActions.put(String.valueOf(menuIndex), this::handleMyProducts);
            System.out.println("[" + menuIndex++ + "] 我的商品");
            
            menuActions.put(String.valueOf(menuIndex), this::handleMyOrdersAsSeller);
            System.out.println("[" + menuIndex++ + "] 我的订单（卖家）");
            
            menuActions.put(String.valueOf(menuIndex), this::handleConfirmOrder);
            System.out.println("[" + menuIndex++ + "] 确认订单");
            
            menuActions.put(String.valueOf(menuIndex), this::handleMyReviews);
            System.out.println("[" + menuIndex++ + "] 我的评价");
        }
        
        System.out.println("【通用功能】");
        menuActions.put(String.valueOf(menuIndex), this::handleMyMessages);
        System.out.println("[" + menuIndex++ + "] 我的消息");
        
        menuActions.put("0", this::handleLogout);
        System.out.println("[0] 退出登录");
        
        ConsoleUtil.printDivider();
        
        System.out.print("请选择：");
        String choice = scanner.nextLine();
        
        try {
            Runnable action = menuActions.get(choice);
            if (action != null) {
                action.run();
            } else {
                ConsoleUtil.printError("无效选项");
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
    }
    
    // ========== 买家功能处理方法 ==========
    
    private void handleBrowseProducts() {
        List<Product> products = productService.searchProducts(
            new SearchCriteria.Builder().build());
        
        if (products.isEmpty()) {
            ConsoleUtil.printInfo("暂无商品");
            return;
        }
        
        // 按时间降序排序
        productService.sortProducts(products, ProductSortStrategies.BY_TIME_DESC);
        
        ConsoleUtil.printTitle("商品列表");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            // 获取卖家评分
            double avgRating = reviewService.getAverageRating(p.getSellerId());
            String ratingStr = avgRating > 0 ? String.format("★%.1f", avgRating) : "暂无评价";
            
            System.out.printf("[%d] %s - ¥%.2f [%s] [%s] 卖家评分:%s%n",
                i + 1, p.getTitle(), p.getPrice(),
                p.getCategory().getDisplayName(),
                p.getCondition().getDescription(),
                ratingStr);
        }
        
        // 购买商品或查看详情
        System.out.print("\n输入商品编号查看详情（0返回）：");
        try {
            int index = Integer.parseInt(scanner.nextLine());
            if (index > 0 && index <= products.size()) {
                Product selected = products.get(index - 1);
                handleProductDetail(selected);
            }
        } catch (NumberFormatException e) {
            // 忽略
        }
    }
    
    private void handleProductDetail(Product product) {
        ConsoleUtil.printTitle("商品详情");
        System.out.println("商品：" + product.getTitle());
        System.out.println("价格：¥" + product.getPrice());
        System.out.println("描述：" + product.getDescription());
        System.out.println("分类：" + product.getCategory().getDisplayName());
        System.out.println("成色：" + product.getCondition().getDescription());
        
        // 显示卖家信息和评价
        User seller = userService.getUserById(product.getSellerId());
        double avgRating = reviewService.getAverageRating(product.getSellerId());
        List<entity.Review> reviews = reviewService.getReviewsBySeller(product.getSellerId());
        
        System.out.println("\n卖家信息：");
        System.out.println("卖家：" + seller.getUsername());
        System.out.println("信誉：" + seller.getReputation() + "分");
        if (avgRating > 0) {
            System.out.printf("评分：★%.1f (%d条评价)%n", avgRating, reviews.size());
        } else {
            System.out.println("评分：暂无评价");
        }
        
        // 显示最近3条评价
        if (!reviews.isEmpty()) {
            System.out.println("\n最近评价：");
            reviews.stream()
                .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
                .limit(3)
                .forEach(r -> {
                    String stars = "★".repeat(r.getRating()) + "☆".repeat(5 - r.getRating());
                    System.out.printf("  %s %s%n", stars, r.getContent());
                });
        }
        
        // 询问操作
        System.out.println("\n[1] 购买  [2] 查看所有评价  [0] 返回");
        System.out.print("选择：");
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1" -> handlePurchaseProduct(product);
            case "2" -> handleViewSellerReviews(product.getSellerId());
        }
    }
    
    private void handlePurchaseProduct(Product product) {
        ConsoleUtil.printTitle("购买商品");
        System.out.println("商品：" + product.getTitle());
        System.out.println("价格：¥" + product.getPrice());
        System.out.println("描述：" + product.getDescription());
        System.out.print("\n确认购买？(y/n)：");
        
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            // 简化的付款流程
            ConsoleUtil.printTitle("付款确认");
            System.out.println("订单金额：¥" + product.getPrice());
            System.out.println("\n请选择支付方式：");
            System.out.println("[1] 支付宝");
            System.out.println("[2] 微信支付");
            System.out.println("[3] 银行卡");
            System.out.print("选择支付方式（1-3）：");
            String payMethod = scanner.nextLine();
            
            String payMethodName = switch (payMethod) {
                case "1" -> "支付宝";
                case "2" -> "微信支付";
                case "3" -> "银行卡";
                default -> "未知方式";
            };
            
            if (!payMethod.matches("[1-3]")) {
                ConsoleUtil.printError("支付方式无效，取消购买");
                return;
            }
            
            System.out.print("\n确认支付 ¥" + product.getPrice() + "？(y/n)：");
            if (scanner.nextLine().equalsIgnoreCase("y")) {
                // 模拟支付处理
                ConsoleUtil.printInfo("正在处理支付...");
                try {
                    Thread.sleep(1000); // 模拟支付延迟
                } catch (InterruptedException e) {
                    // 忽略
                }
                
                // 创建订单
                User buyer = userService.getCurrentUser();
                Order order = orderService.createOrder(buyer, product.getProductId());
                
                ConsoleUtil.printSuccess("支付成功！");
                ConsoleUtil.printSuccess("支付方式：" + payMethodName);
                ConsoleUtil.printSuccess("下单成功！订单号：" + order.getOrderId());
            } else {
                ConsoleUtil.printInfo("已取消支付");
            }
        }
    }
    
    private void handleSearchProducts() {
        ConsoleUtil.printTitle("搜索商品");
        System.out.print("关键词（回车跳过）：");
        String keyword = scanner.nextLine();
        
        System.out.println("分类（回车跳过）：");
        System.out.println("1.电子产品 2.图书教材 3.服装鞋帽 4.运动器材 5.生活用品 6.其他");
        System.out.print("选择：");
        String catChoice = scanner.nextLine();
        ProductCategory category = switch (catChoice) {
            case "1" -> ProductCategory.ELECTRONICS;
            case "2" -> ProductCategory.BOOKS;
            case "3" -> ProductCategory.CLOTHING;
            case "4" -> ProductCategory.SPORTS;
            case "5" -> ProductCategory.DAILY;
            case "6" -> ProductCategory.OTHER;
            default -> null;
        };
        
        System.out.print("最高价格（回车跳过）：");
        String maxPriceStr = scanner.nextLine();
        Double maxPrice = maxPriceStr.isEmpty() ? null : Double.parseDouble(maxPriceStr);
        
        // 构建搜索条件
        SearchCriteria.Builder builder = new SearchCriteria.Builder();
        if (!keyword.isEmpty()) builder.keyword(keyword);
        if (category != null) builder.category(category);
        if (maxPrice != null) builder.maxPrice(maxPrice);
        
        List<Product> results = productService.searchProducts(builder.build());
        
        if (results.isEmpty()) {
            ConsoleUtil.printInfo("没有找到符合条件的商品");
        } else {
            ConsoleUtil.printSuccess("找到 " + results.size() + " 件商品");
            for (int i = 0; i < results.size(); i++) {
                Product p = results.get(i);
                System.out.printf("[%d] %s - ¥%.2f [%s] [%s]%n",
                    i + 1, p.getTitle(), p.getPrice(), 
                    p.getCategory().getDisplayName(),
                    p.getCondition().getDescription());
            }
            
            // 询问是否购买
            System.out.print("\n输入商品编号购买（0返回）：");
            try {
                int index = Integer.parseInt(scanner.nextLine());
                if (index > 0 && index <= results.size()) {
                    Product selected = results.get(index - 1);
                    handlePurchaseProduct(selected);
                }
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
    }
    
    private void handleMyOrders() {
        User buyer = userService.getCurrentUser();
        List<Order> orders = orderService.getOrdersByBuyer(buyer.getUserId());
        
        if (orders.isEmpty()) {
            ConsoleUtil.printInfo("暂无订单");
            return;
        }
        
        ConsoleUtil.printTitle("我的订单（买家）");
        for (Order order : orders) {
            Product product = productService.getProductById(order.getProductId());
            String statusDesc = getOrderStatusForBuyer(order.getStatus());
            System.out.printf("订单号：%s | 商品：%s | 价格：¥%.2f | 状态：%s%n",
                order.getOrderId(), product.getTitle(), order.getPrice(), statusDesc);
        }
    }
    
    private void handleConfirmReceipt() {
        User buyer = userService.getCurrentUser();
        List<Order> orders = orderService.getOrdersByBuyer(buyer.getUserId());
        
        // 筛选出已确认的订单
        List<Order> confirmedOrders = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
            .toList();
        
        if (confirmedOrders.isEmpty()) {
            ConsoleUtil.printInfo("没有待收货的订单");
            return;
        }
        
        ConsoleUtil.printTitle("确认收货");
        for (int i = 0; i < confirmedOrders.size(); i++) {
            Order order = confirmedOrders.get(i);
            Product product = productService.getProductById(order.getProductId());
            System.out.printf("[%d] 订单号：%s | 商品：%s%n",
                i + 1, order.getOrderId(), product.getTitle());
        }
        
        System.out.print("选择订单编号（0返回）：");
        try {
            int index = Integer.parseInt(scanner.nextLine());
            if (index > 0 && index <= confirmedOrders.size()) {
                Order selected = confirmedOrders.get(index - 1);
                orderService.confirmReceipt(buyer, selected.getOrderId());
                ConsoleUtil.printSuccess("收货确认成功！");
            }
        } catch (NumberFormatException e) {
            // 忽略
        }
    }
    
    private void handleReviewOrder() {
        User buyer = userService.getCurrentUser();
        List<Order> orders = orderService.getOrdersByBuyer(buyer.getUserId());
        
        // 筛选出已完成且未评价的订单
        List<Order> reviewableOrders = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
            .filter(o -> {
                try {
                    reviewService.getReviewByOrderId(o.getOrderId());
                    return false;  // 已评价
                } catch (Exception e) {
                    return true;  // 未评价
                }
            })
            .toList();
        
        if (reviewableOrders.isEmpty()) {
            ConsoleUtil.printInfo("没有可评价的订单");
            return;
        }
        
        ConsoleUtil.printTitle("评价订单");
        for (int i = 0; i < reviewableOrders.size(); i++) {
            Order order = reviewableOrders.get(i);
            Product product = productService.getProductById(order.getProductId());
            System.out.printf("[%d] 订单号：%s | 商品：%s%n",
                i + 1, order.getOrderId(), product.getTitle());
        }
        
        System.out.print("选择订单编号（0返回）：");
        try {
            int index = Integer.parseInt(scanner.nextLine());
            if (index > 0 && index <= reviewableOrders.size()) {
                Order selected = reviewableOrders.get(index - 1);
                
                System.out.print("评分（1-5星）：");
                int rating = Integer.parseInt(scanner.nextLine());
                System.out.print("评价内容：");
                String content = scanner.nextLine();
                
                reviewService.createReview(buyer, selected.getOrderId(), rating, content);
                ConsoleUtil.printSuccess("评价成功！");
            }
        } catch (NumberFormatException e) {
            ConsoleUtil.printError("输入格式错误");
        }
    }
    
    // ========== 卖家功能处理方法 ==========
    
    private void handlePublishProduct() {
        User seller = userService.getCurrentUser();
        
        ConsoleUtil.printTitle("发布商品");
        ConsoleUtil.printInfo("提示：任何步骤输入'0'可返回菜单");
        
        // 1. 输入商品标题
        System.out.print("商品标题（2-50字）：");
        String title = scanner.nextLine();
        if (title.equals("0")) {
            ConsoleUtil.printInfo("已取消发布");
            return;
        }
        if (title.isEmpty() || title.length() < 2 || title.length() > 50) {
            ConsoleUtil.printError("标题长度必须在2-50字之间");
            return;
        }
        
        // 2. 输入商品描述
        System.out.print("商品描述（10-500字）：");
        String description = scanner.nextLine();
        if (description.equals("0")) {
            ConsoleUtil.printInfo("已取消发布");
            return;
        }
        if (description.isEmpty() || description.length() < 10 || description.length() > 500) {
            ConsoleUtil.printError("描述长度必须在10-500字之间");
            return;
        }
        
        // 3. 输入价格
        System.out.print("价格（0.01-99999.99）：");
        String priceStr = scanner.nextLine();
        if (priceStr.equals("0")) {
            ConsoleUtil.printInfo("已取消发布");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(priceStr);
            if (price < 0.01 || price > 99999.99) {
                ConsoleUtil.printError("价格必须在0.01-99999.99之间");
                return;
            }
        } catch (NumberFormatException e) {
            ConsoleUtil.printError("价格格式错误");
            return;
        }
        
        // 4. 选择分类
        System.out.println("\n分类：");
        System.out.println("1.电子产品 2.图书教材 3.服装鞋帽 4.运动器材 5.生活用品 6.其他");
        System.out.print("选择（输入0返回）：");
        String catChoice = scanner.nextLine();
        if (catChoice.equals("0")) {
            ConsoleUtil.printInfo("已取消发布");
            return;
        }
        ProductCategory category = switch (catChoice) {
            case "1" -> ProductCategory.ELECTRONICS;
            case "2" -> ProductCategory.BOOKS;
            case "3" -> ProductCategory.CLOTHING;
            case "4" -> ProductCategory.SPORTS;
            case "5" -> ProductCategory.DAILY;
            case "6" -> ProductCategory.OTHER;
            default -> null;
        };
        if (category == null) {
            ConsoleUtil.printError("分类选择无效");
            return;
        }
        
        // 5. 选择成色
        System.out.println("\n成色：");
        System.out.println("1.全新 2.几乎全新 3.良好 4.可接受");
        System.out.print("选择（输入0返回）：");
        String condChoice = scanner.nextLine();
        if (condChoice.equals("0")) {
            ConsoleUtil.printInfo("已取消发布");
            return;
        }
        ProductCondition condition = switch (condChoice) {
            case "1" -> ProductCondition.BRAND_NEW;
            case "2" -> ProductCondition.LIKE_NEW;
            case "3" -> ProductCondition.GOOD;
            case "4" -> ProductCondition.ACCEPTABLE;
            default -> null;
        };
        if (condition == null) {
            ConsoleUtil.printError("成色选择无效");
            return;
        }
        
        // 6. 确认发布
        System.out.println("\n=== 商品信息确认 ===");
        System.out.println("标题：" + title);
        System.out.println("描述：" + description);
        System.out.println("价格：¥" + price);
        System.out.println("分类：" + category.getDisplayName());
        System.out.println("成色：" + condition.getDescription());
        System.out.print("\n确认发布？(y/n)：");
        String confirm = scanner.nextLine();
        
        if (!confirm.equalsIgnoreCase("y")) {
            ConsoleUtil.printInfo("已取消发布");
            return;
        }
        
        Product product = productService.publishProduct(seller, title, description, 
                                                       price, category, condition);
        ConsoleUtil.printSuccess("商品发布成功！商品ID：" + product.getProductId());
    }
    
    private void handleMyProducts() {
        User seller = userService.getCurrentUser();
        List<Product> products = productService.getProductsBySeller(seller.getUserId());
        
        if (products.isEmpty()) {
            ConsoleUtil.printInfo("还没有发布商品");
            return;
        }
        
        ConsoleUtil.printTitle("我的商品（卖家）");
        for (Product p : products) {
            System.out.printf("• %s - ¥%.2f [%s]%n",
                p.getTitle(), p.getPrice(), p.getStatus().getDisplayName());
        }
    }
    
    private void handleMyOrdersAsSeller() {
        User seller = userService.getCurrentUser();
        List<Order> orders = orderService.getOrdersBySeller(seller.getUserId());
        
        if (orders.isEmpty()) {
            ConsoleUtil.printInfo("暂无订单");
            return;
        }
        
        ConsoleUtil.printTitle("我的订单（卖家）");
        for (Order order : orders) {
            Product product = productService.getProductById(order.getProductId());
            String statusDesc = getOrderStatusForSeller(order.getStatus());
            System.out.printf("订单号：%s | 商品：%s | 价格：¥%.2f | 状态：%s%n",
                order.getOrderId(), product.getTitle(), order.getPrice(), statusDesc);
        }
    }
    
    private void handleConfirmOrder() {
        User seller = userService.getCurrentUser();
        List<Order> orders = orderService.getOrdersBySeller(seller.getUserId());
        
        // 筛选出待确认的订单
        List<Order> pendingOrders = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING)
            .toList();
        
        if (pendingOrders.isEmpty()) {
            ConsoleUtil.printInfo("没有待确认的订单");
            return;
        }
        
        ConsoleUtil.printTitle("确认订单");
        for (int i = 0; i < pendingOrders.size(); i++) {
            Order order = pendingOrders.get(i);
            Product product = productService.getProductById(order.getProductId());
            System.out.printf("[%d] 订单号：%s | 商品：%s%n",
                i + 1, order.getOrderId(), product.getTitle());
        }
        
        System.out.print("选择订单编号（0返回）：");
        try {
            int index = Integer.parseInt(scanner.nextLine());
            if (index > 0 && index <= pendingOrders.size()) {
                Order selected = pendingOrders.get(index - 1);
                orderService.confirmOrder(seller, selected.getOrderId());
                ConsoleUtil.printSuccess("订单确认成功！");
            }
        } catch (NumberFormatException e) {
            // 忽略
        }
    }
    
    // ========== 评价相关处理方法 ==========
    
    private void handleViewReviews() {
        ConsoleUtil.printTitle("查看评价");
        System.out.println("[1] 查看指定卖家的评价");
        System.out.println("[2] 查看我发表的评价");
        System.out.println("[0] 返回");
        System.out.print("选择：");
        String choice = scanner.nextLine();
        
        if (choice.equals("1")) {
            // 显示所有卖家列表
            List<User> sellers = userService.getAllSellers();
            if (sellers.isEmpty()) {
                ConsoleUtil.printInfo("暂无卖家");
                return;
            }
            
            System.out.println("\n卖家列表：");
            for (int i = 0; i < sellers.size(); i++) {
                User seller = sellers.get(i);
                double avgRating = reviewService.getAverageRating(seller.getUserId());
                String ratingStr = avgRating > 0 ? String.format("★%.1f", avgRating) : "暂无评价";
                System.out.printf("[%d] %s - 信誉:%d分 - %s%n", 
                    i + 1, seller.getUsername(), seller.getReputation(), ratingStr);
            }
            
            System.out.print("\n选择卖家编号（0返回）：");
            try {
                int index = Integer.parseInt(scanner.nextLine());
                if (index > 0 && index <= sellers.size()) {
                    User selected = sellers.get(index - 1);
                    handleViewSellerReviews(selected.getUserId());
                }
            } catch (NumberFormatException e) {
                // 忽略
            }
        } else if (choice.equals("2")) {
            handleMyReviewsAsBuyer();
        }
    }
    
    private void handleViewSellerReviews(String sellerId) {
        User seller = userService.getUserById(sellerId);
        List<entity.Review> reviews = reviewService.getReviewsBySeller(sellerId);
        
        if (reviews.isEmpty()) {
            ConsoleUtil.printInfo("该卖家暂无评价");
            return;
        }
        
        ConsoleUtil.printTitle("卖家评价 - " + seller.getUsername());
        double avgRating = reviewService.getAverageRating(sellerId);
        System.out.printf("平均评分：★%.1f (%d条评价)%n%n", avgRating, reviews.size());
        
        // 按时间降序显示所有评价
        reviews.stream()
            .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
            .forEach(r -> {
                String stars = "★".repeat(r.getRating()) + "☆".repeat(5 - r.getRating());
                String timeStr = r.getCreateTime().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                System.out.printf("%s [%s]%n  %s%n%n", stars, timeStr, r.getContent());
            });
    }
    
    private void handleMyReviewsAsBuyer() {
        User buyer = userService.getCurrentUser();
        repository.DataCenter dc = repository.DataCenter.getInstance();
        
        // 获取买家发表的所有评价
        List<entity.Review> reviews = dc.getAllReviews().stream()
            .filter(r -> r.getReviewerId().equals(buyer.getUserId()))
            .toList();
        
        if (reviews.isEmpty()) {
            ConsoleUtil.printInfo("您还没有发表过评价");
            return;
        }
        
        ConsoleUtil.printTitle("我发表的评价");
        for (entity.Review review : reviews) {
            Order order = orderService.getOrderById(review.getOrderId());
            Product product = productService.getProductById(order.getProductId());
            User seller = userService.getUserById(review.getRevieweeId());
            
            String stars = "★".repeat(review.getRating()) + "☆".repeat(5 - review.getRating());
            String timeStr = review.getCreateTime().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            System.out.printf("商品：%s | 卖家：%s%n", product.getTitle(), seller.getUsername());
            System.out.printf("%s [%s]%n", stars, timeStr);
            System.out.printf("  %s%n%n", review.getContent());
        }
    }
    
    private void handleMyReviews() {
        User seller = userService.getCurrentUser();
        List<entity.Review> reviews = reviewService.getReviewsBySeller(seller.getUserId());
        
        if (reviews.isEmpty()) {
            ConsoleUtil.printInfo("还没有收到评价");
            return;
        }
        
        ConsoleUtil.printTitle("我收到的评价");
        double avgRating = reviewService.getAverageRating(seller.getUserId());
        System.out.printf("平均评分：★%.1f (%d条评价)%n%n", avgRating, reviews.size());
        
        for (entity.Review review : reviews) {
            Order order = orderService.getOrderById(review.getOrderId());
            Product product = productService.getProductById(order.getProductId());
            
            String stars = "★".repeat(review.getRating()) + "☆".repeat(5 - review.getRating());
            String timeStr = review.getCreateTime().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            System.out.printf("商品：%s%n", product.getTitle());
            System.out.printf("%s [%s]%n", stars, timeStr);
            System.out.printf("  %s%n%n", review.getContent());
        }
    }
    
    // ========== 通用功能处理方法 ==========
    
    private void handleMyMessages() {
        User user = userService.getCurrentUser();
        
        // 获取历史消息（持久化的）
        NotificationService notificationService = new NotificationService();
        List<entity.Message> historyMessages = notificationService.getMessageHistory(user.getUserId());
        
        if (historyMessages.isEmpty()) {
            ConsoleUtil.printInfo("暂无消息");
        } else {
            ConsoleUtil.printTitle("我的消息");
            System.out.println(String.format("共 %d 条消息\n", historyMessages.size()));
            
            for (entity.Message msg : historyMessages) {
                String timeStr = msg.getCreateTime().format(
                    java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));
                System.out.printf("[%s] %s%n", timeStr, msg.getContent());
            }
        }
    }
    
    private void handleLogout() {
        userService.logout();
        ConsoleUtil.printSuccess("已退出登录");
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 获取订单状态的买家视角描述
     */
    private String getOrderStatusForBuyer(OrderStatus status) {
        return switch (status) {
            case PENDING -> "待卖家确认";
            case CONFIRMED -> "卖家已确认，待收货";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
        };
    }
    
    /**
     * 获取订单状态的卖家视角描述
     */
    private String getOrderStatusForSeller(OrderStatus status) {
        return switch (status) {
            case PENDING -> "待您确认";
            case CONFIRMED -> "已确认，等待买家收货";
            case COMPLETED -> "交易完成";
            case CANCELLED -> "已取消";
        };
    }
}

