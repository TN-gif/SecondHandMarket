package handler;

import dto.SearchCriteria;
import entity.Message;
import entity.Order;
import entity.Product;
import entity.Review;
import entity.User;
import enums.*;
import observer.UserMessageReceiver;
import repository.DataCenter;
import service.*;
import strategy.ProductSortStrategies;
import util.ConsoleUtil;
import util.InputValidator;
import util.PerfectTableFormatter;
import util.TranslationUtil;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 用户菜单处理器
 * 
 * 负责买家和卖家功能的用户界面交互。
 * 根据用户角色动态显示相应的功能菜单。
 */
public class UserMenuHandler extends BaseMenuHandler {
    private final ProductService productService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    
    public UserMenuHandler(UserService userService, ProductService productService,
                          OrderService orderService, ReviewService reviewService) {
        super(userService);
        this.productService = productService;
        this.orderService = orderService;
        this.reviewService = reviewService;
    }
    
    @Override
    public void displayAndHandle() {
        User user = userService.getCurrentUser();
        
        // 显示用户信息
        ConsoleUtil.printInfo(String.format("欢迎，%s | 信誉：%d [%s]", 
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
            
            menuActions.put(String.valueOf(menuIndex), this::handleCancelOrder);
            System.out.println("[" + menuIndex++ + "] 取消订单");
            
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
            
            menuActions.put(String.valueOf(menuIndex), this::handleManageProducts);
            System.out.println("[" + menuIndex++ + "] 管理商品");
            
            menuActions.put(String.valueOf(menuIndex), this::handleMyOrdersAsSeller);
            System.out.println("[" + menuIndex++ + "] 我的订单（卖家）");
            
            menuActions.put(String.valueOf(menuIndex), this::handleConfirmOrder);
            System.out.println("[" + menuIndex++ + "] 确认订单");
            
            menuActions.put(String.valueOf(menuIndex), this::handleCancelOrderAsSeller);
            System.out.println("[" + menuIndex++ + "] 取消订单");
            
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
        
        // 使用FlipTables专业库创建表格
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                .setHeaders("No.", "Product Name", "Price", "Category", "Condition", "Seller Rating");
        
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            // 获取卖家评分
            double avgRating = reviewService.getAverageRating(p.getSellerId());
            String ratingStr = avgRating > 0 ? String.format("%.1f", avgRating) : TranslationUtil.toEnglish("暂无评价");
            
            table.addRow(
                String.valueOf(i + 1),
                TranslationUtil.toEnglish(p.getTitle()),
                String.format("%.2f", p.getPrice()),
                TranslationUtil.toEnglish(p.getCategory().getDisplayName()),
                TranslationUtil.toEnglish(p.getCondition().getDescription()),
                ratingStr
            );
        }
        
        table.print();
        
        // 购买商品或查看详情
        Integer index = readIntSafely("\n输入商品编号查看详情（0返回）：", "商品编号无效，请输入有效数字");
        if (index != null && index > 0 && index <= products.size()) {
                Product selected = products.get(index - 1);
            handleProductDetail(selected);
        }
    }
    
    private void handleProductDetail(Product product) {
        ConsoleUtil.printTitle("商品详情");
        System.out.println("商品：" + product.getTitle());
        System.out.println("价格：￥" + product.getPrice());
        System.out.println("描述：" + product.getDescription());
        System.out.println("分类：" + product.getCategory().getDisplayName());
        System.out.println("成色：" + product.getCondition().getDescription());
        
        // 显示卖家信息和评价
        User seller = userService.getUserById(product.getSellerId());
        double avgRating = reviewService.getAverageRating(product.getSellerId());
        List<Review> reviews = reviewService.getReviewsBySeller(product.getSellerId());
        
        System.out.println("\n卖家信息：");
        System.out.println("卖家：" + seller.getUsername());
        System.out.println("信誉：" + seller.getReputation() + " 分");
        if (avgRating > 0) {
            System.out.printf("评分：%.1f (%d条评价)%n", avgRating, reviews.size());
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
        while (true) {
            System.out.println("\n[1] 购买商品  [2] 查看所有评价  [0] 返回");
            System.out.print("请选择：");
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1" -> {
                    handlePurchaseProduct(product);
                    return;
                }
                case "2" -> handleViewSellerReviews(product.getSellerId());
                case "0" -> {
                    return;
                }
                default -> ConsoleUtil.printError("无效选项");
            }
        }
    }
    
    private void handlePurchaseProduct(Product product) {
        ConsoleUtil.printTitle("购买商品");
        System.out.println("商品：" + product.getTitle());
        System.out.println("价格：￥" + product.getPrice());
        System.out.println("描述：" + product.getDescription());
        System.out.print("\n确认购买？(y/n)：");
        
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            // 简化的付款流程
            ConsoleUtil.printTitle("确认支付");
            System.out.println("订单金额：￥" + product.getPrice());
            System.out.println("\n选择支付方式：");
            System.out.println("[1] 支付宝");
            System.out.println("[2] 微信支付");
            System.out.println("[3] 银行卡");
            System.out.print("请选择支付方式（1-3）：");
            String payMethod = scanner.nextLine();
            
            String payMethodName = switch (payMethod) {
                case "1" -> "支付宝";
                case "2" -> "微信支付";
                case "3" -> "银行卡";
                default -> "未知";
            };
            
            if (!payMethod.matches("[1-3]")) {
                ConsoleUtil.printError("支付方式无效，已取消购买");
                return;
            }
            
            System.out.print("\n确认支付 ￥" + product.getPrice() + "？(y/n)：");
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
            ConsoleUtil.printSuccess("订单已创建！订单ID：" + order.getOrderId());
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
        System.out.println("1.电子产品 2.图书 3.服装 4.运动 5.日用品 6.其他");
        System.out.print("请选择：");
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
        
            // 价格输入（带即时验证）
        Double maxPrice = null;
        while (true) {
        System.out.print("最高价格（回车跳过）：");
        String maxPriceStr = scanner.nextLine();
            
            if (maxPriceStr.trim().isEmpty()) {
                break; // 跳过价格筛选
            }
            
            if (!InputValidator.isValidDouble(maxPriceStr)) {
                ConsoleUtil.printError("价格格式无效，请输入有效数字");
                System.out.print("是否重试？(y/n)：");
                if (!scanner.nextLine().equalsIgnoreCase("y")) {
                    ConsoleUtil.printInfo("已取消价格筛选");
                    break;
                }
                continue;
            }
            
            Double price = InputValidator.parseDoubleSafe(maxPriceStr);
            if (price == null || price <= 0) {
                ConsoleUtil.printError("价格必须大于0");
                System.out.print("是否重试？(y/n)：");
                if (!scanner.nextLine().equalsIgnoreCase("y")) {
                    ConsoleUtil.printInfo("已取消价格筛选");
                    break;
                }
                continue;
            }
            
            maxPrice = price;
            break;
        }
        
        // 构建搜索条件
        SearchCriteria.Builder builder = new SearchCriteria.Builder();
        if (!keyword.isEmpty()) builder.keyword(keyword);
        if (category != null) builder.category(category);
        if (maxPrice != null) builder.maxPrice(maxPrice);
        
        List<Product> results = productService.searchProducts(builder.build());
        
        if (results.isEmpty()) {
            ConsoleUtil.printInfo("未找到符合条件的商品");
        } else {
            ConsoleUtil.printSuccess("找到 " + results.size() + " 件商品");
            
            // 使用FlipTables专业库创建表格
            PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                    .setHeaders("No.", "Product Name", "Price", "Category", "Condition", "Seller Rating");
            
            for (int i = 0; i < results.size(); i++) {
                Product p = results.get(i);
                // 获取卖家评分
                double avgRating = reviewService.getAverageRating(p.getSellerId());
                String ratingStr = avgRating > 0 ? String.format("%.1f", avgRating) : TranslationUtil.toEnglish("暂无评价");
                
                table.addRow(
                    String.valueOf(i + 1),
                    TranslationUtil.toEnglish(p.getTitle()),
                    String.format("%.2f", p.getPrice()),
                    TranslationUtil.toEnglish(p.getCategory().getDisplayName()),
                    TranslationUtil.toEnglish(p.getCondition().getDescription()),
                    ratingStr
                );
            }
            
            table.print();
            
            // 询问是否查看详情或购买
            Integer index = readIntSafely("\n输入商品编号查看详情（0返回）：", "商品编号无效，请输入有效数字");
            if (index != null && index > 0 && index <= results.size()) {
                Product selected = results.get(index - 1);
                handleProductDetail(selected);
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
        
        // 使用FlipTables专业库创建表格
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                .setHeaders("Order ID", "Product Name", "Price", "Status");
        
        for (Order order : orders) {
            Product product = productService.getProductById(order.getProductId());
            String statusDesc = getOrderStatusForBuyer(order.getStatus());
            
            table.addRow(
                order.getOrderId(),
                TranslationUtil.toEnglish(product.getTitle()),
                String.format("%.2f", order.getPrice()),
                TranslationUtil.toEnglish(statusDesc)
            );
        }
        
        table.print();
    }
    
    private void handleConfirmReceipt() {
        User buyer = userService.getCurrentUser();
        List<Order> orders = orderService.getOrdersByBuyer(buyer.getUserId());
        
        // 筛选出已确认的订单
        List<Order> confirmedOrders = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.CONFIRMED)
            .toList();
        
        if (confirmedOrders.isEmpty()) {
            ConsoleUtil.printInfo("暂无待确认收货的订单");
            return;
        }
        
        ConsoleUtil.printTitle("确认收货");
        
        // 使用表格展示待确认收货的订单
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
            .setHeaders("No.", "Order ID", "Product Name", "Price", "Seller");
        
        for (int i = 0; i < confirmedOrders.size(); i++) {
            Order order = confirmedOrders.get(i);
            Product product = productService.getProductById(order.getProductId());
            User seller = userService.getUserById(order.getSellerId());
            table.addRow(
                String.valueOf(i + 1),
                order.getOrderId(),
                TranslationUtil.toEnglish(product.getTitle()),
                String.format("%.2f", order.getPrice()),
                seller.getUsername()
            );
        }
        
        table.print();
        
        Integer index = readIntSafely("选择订单编号（0返回）：", "订单编号无效，请输入有效数字");
        if (index != null && index > 0 && index <= confirmedOrders.size()) {
                Order selected = confirmedOrders.get(index - 1);
                orderService.confirmReceipt(buyer, selected.getOrderId());
                ConsoleUtil.printSuccess("确认收货成功！");
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
            ConsoleUtil.printInfo("暂无可评价的订单");
            return;
        }
        
        ConsoleUtil.printTitle("评价订单");
        
        // 使用表格展示可评价的订单
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
            .setHeaders("No.", "Order ID", "Product Name", "Price", "Seller");
        
        for (int i = 0; i < reviewableOrders.size(); i++) {
            Order order = reviewableOrders.get(i);
            Product product = productService.getProductById(order.getProductId());
            User seller = userService.getUserById(order.getSellerId());
            table.addRow(
                String.valueOf(i + 1),
                order.getOrderId(),
                TranslationUtil.toEnglish(product.getTitle()),
                String.format("%.2f", order.getPrice()),
                seller.getUsername()
            );
        }
        
        table.print();
        
        Integer index = readIntSafely("选择订单编号（0返回）：", "订单编号无效，请输入有效数字");
        if (index != null && index > 0 && index <= reviewableOrders.size()) {
                Order selected = reviewableOrders.get(index - 1);
                
                // 评分输入（带即时验证）
                int rating = 0;
                while (rating == 0) {
                System.out.print("评分（1-5星）：");
                    String ratingInput = scanner.nextLine();
                    
                    if (ratingInput.equals("0")) {
                        ConsoleUtil.printInfo("已取消评价");
                        return;
                    }
                    
                    try {
                        int tempRating = Integer.parseInt(ratingInput);
                        if (tempRating < 1 || tempRating > 5) {
                            ConsoleUtil.printError("评分必须在1-5星之间，请重新输入");
                            System.out.print("继续评价？(y/n)：");
                            if (!scanner.nextLine().equalsIgnoreCase("y")) {
                                ConsoleUtil.printInfo("已取消评价");
                                return;
                            }
                            continue;
                        }
                        rating = tempRating;
                    } catch (NumberFormatException e) {
                        ConsoleUtil.printError("评分格式无效，请输入1-5之间的数字");
                        System.out.print("继续评价？(y/n)：");
                        if (!scanner.nextLine().equalsIgnoreCase("y")) {
                            ConsoleUtil.printInfo("已取消评价");
                            return;
                        }
                    }
                }
                
                // 评价内容输入（带即时验证）
                String content = null;
                while (content == null) {
                    System.out.print("评价内容（5-200字）：");
                    String input = scanner.nextLine();
                    
                    if (input.equals("0")) {
                        ConsoleUtil.printInfo("已取消评价");
                        return;
                    }
                    
                    if (input.trim().isEmpty()) {
                        ConsoleUtil.printError("评价内容不能为空");
                        System.out.print("继续评价？(y/n)：");
                        if (!scanner.nextLine().equalsIgnoreCase("y")) {
                            ConsoleUtil.printInfo("已取消评价");
                            return;
                        }
                        continue;
                    }
                    
                    if (input.length() < 5 || input.length() > 200) {
                        ConsoleUtil.printError("评价内容长度必须为5-200字");
                        System.out.print("继续评价？(y/n)：");
                        if (!scanner.nextLine().equalsIgnoreCase("y")) {
                            ConsoleUtil.printInfo("已取消评价");
                            return;
                        }
                        continue;
                    }
                    
                    content = input;
                }
                
                reviewService.createReview(buyer, selected.getOrderId(), rating, content);
                ConsoleUtil.printSuccess("评价提交成功！");
        }
    }
    
    // ========== 卖家功能处理方法 ==========
    
    private void handlePublishProduct() {
        User seller = userService.getCurrentUser();
        
        ConsoleUtil.printTitle("发布商品");
        ConsoleUtil.printInfo("提示：在任何步骤输入'0'返回菜单");
        
        // 1. 输入商品标题
        System.out.print("商品标题（2-50字）：");
        String title = scanner.nextLine();
        if (title.equals("0")) {
            ConsoleUtil.printInfo("已取消发布");
            return;
        }
        if (title.isEmpty() || title.length() < 2 || title.length() > 50) {
            ConsoleUtil.printError("标题长度必须为2-50字");
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
            ConsoleUtil.printError("描述长度必须为10-500字");
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
            ConsoleUtil.printError("价格格式无效");
            return;
        }
        
        // 4. 选择分类
        System.out.println("\n分类：");
        System.out.println("1.电子产品 2.图书 3.服装 4.运动 5.日用品 6.其他");
        System.out.print("请选择（0返回）：");
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
        System.out.print("请选择（0返回）：");
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
        System.out.println("价格：￥" + price);
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
            ConsoleUtil.printInfo("暂未发布商品");
            return;
        }
        
        ConsoleUtil.printTitle("我的商品（卖家）");
        
        // 使用FlipTables专业库创建表格
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                .setHeaders("Product Name", "Price", "Status");
        
        for (Product p : products) {
            table.addRow(
                TranslationUtil.toEnglish(p.getTitle()),
                String.format("%.2f", p.getPrice()),
                TranslationUtil.toEnglish(p.getStatus().getDisplayName())
            );
        }
        
        table.print();
    }
    
    private void handleMyOrdersAsSeller() {
        User seller = userService.getCurrentUser();
        List<Order> orders = orderService.getOrdersBySeller(seller.getUserId());
        
        if (orders.isEmpty()) {
            ConsoleUtil.printInfo("暂无订单");
            return;
        }
        
        ConsoleUtil.printTitle("我的订单（卖家）");
        
            // 使用FlipTables专业库创建表格
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                .setHeaders("Order ID", "Product Name", "Price", "Status");
        
        for (Order order : orders) {
            Product product = productService.getProductById(order.getProductId());
            String statusDesc = getOrderStatusForSeller(order.getStatus());
            
            table.addRow(
                order.getOrderId(),
                TranslationUtil.toEnglish(product.getTitle()),
                String.format("%.2f", order.getPrice()),
                TranslationUtil.toEnglish(statusDesc)
            );
        }
        
        table.print();
    }
    
    private void handleConfirmOrder() {
        User seller = userService.getCurrentUser();
        List<Order> orders = orderService.getOrdersBySeller(seller.getUserId());
        
        // 筛选出待确认的订单
        List<Order> pendingOrders = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING)
            .toList();
        
        if (pendingOrders.isEmpty()) {
            ConsoleUtil.printInfo("暂无待确认订单");
            return;
        }
        
        ConsoleUtil.printTitle("确认订单");
        
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
            .setHeaders("No.", "Order ID", "Product Name", "Price", "Buyer");
        
        for (int i = 0; i < pendingOrders.size(); i++) {
            Order order = pendingOrders.get(i);
            Product product = productService.getProductById(order.getProductId());
            User buyer = userService.getUserById(order.getBuyerId());
            table.addRow(
                String.valueOf(i + 1),
                order.getOrderId(),
                TranslationUtil.toEnglish(product.getTitle()),
                String.format("%.2f", order.getPrice()),
                buyer.getUsername()
            );
        }
        
        table.print();
        
        Integer index = readIntSafely("选择订单编号（0返回）：", "订单编号无效，请输入有效数字");
        if (index != null && index > 0 && index <= pendingOrders.size()) {
                Order selected = pendingOrders.get(index - 1);
                orderService.confirmOrder(seller, selected.getOrderId());
                ConsoleUtil.printSuccess("订单确认成功！");
            }
    }
    
    // ========== 评价相关处理方法 ==========
    
    private void handleViewReviews() {
        while (true) {
            ConsoleUtil.printTitle("查看评价");
            System.out.println("[1] 查看卖家评价");
            System.out.println("[2] 查看我的评价");
            System.out.println("[0] 返回");
            System.out.print("请选择：");
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1" -> {
                    // 显示所有卖家列表
                    List<User> sellers = userService.getAllSellers();
                    if (sellers.isEmpty()) {
                        ConsoleUtil.printInfo("暂无卖家");
                        continue;
                    }
                    
                    System.out.println("\n卖家列表：");
                    
                    // 使用FlipTables专业库创建表格
                    PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                            .setHeaders("No.", "Seller", "Reputation", "Avg Rating");
                    
                    for (int i = 0; i < sellers.size(); i++) {
                        User seller = sellers.get(i);
                        double avgRating = reviewService.getAverageRating(seller.getUserId());
                        String ratingStr = avgRating > 0 ? String.format("%.1f", avgRating) : TranslationUtil.toEnglish("暂无评价");
                        
                        table.addRow(
                            String.valueOf(i + 1),
                            seller.getUsername(),
                            String.valueOf(seller.getReputation()),
                            ratingStr
                        );
                    }
                    
                    table.print();
                    
                    Integer index = readIntSafely("\n选择卖家编号（0返回）：", "卖家编号无效，请输入有效数字");
                    if (index != null && index > 0 && index <= sellers.size()) {
                        User selected = sellers.get(index - 1);
                        handleViewSellerReviews(selected.getUserId());
                    }
                }
                case "2" -> handleMyReviewsAsBuyer();
                case "0" -> {
                    return;
                }
                default -> ConsoleUtil.printError("无效选项");
            }
        }
    }
    
    private void handleViewSellerReviews(String sellerId) {
        User seller = userService.getUserById(sellerId);
        List<Review> reviews = reviewService.getReviewsBySeller(sellerId);
        
        if (reviews.isEmpty()) {
            ConsoleUtil.printInfo("该卖家暂无评价");
            return;
        }
        
        ConsoleUtil.printTitle("卖家评价 - " + seller.getUsername());
        double avgRating = reviewService.getAverageRating(sellerId);
        System.out.printf("平均评分：%.1f（%d条评价）%n%n", avgRating, reviews.size());
        
        // 按时间降序显示所有评价
        reviews.stream()
            .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
            .forEach(r -> {
                String stars = "★".repeat(r.getRating()) + "☆".repeat(5 - r.getRating());
                String timeStr = r.getCreateTime().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                System.out.printf("%s [%s]%n  %s%n%n", stars, timeStr, r.getContent());
            });
    }
    
    private void handleMyReviewsAsBuyer() {
        User buyer = userService.getCurrentUser();
        DataCenter dc = DataCenter.getInstance();
        
        // 获取买家发表的所有评价
        List<Review> reviews = dc.getAllReviews().stream()
            .filter(r -> r.getReviewerId().equals(buyer.getUserId()))
            .toList();
        
        if (reviews.isEmpty()) {
            ConsoleUtil.printInfo("您还未发表任何评价");
            return;
        }
        
        ConsoleUtil.printTitle("我的评价");
        for (Review review : reviews) {
            Order order = orderService.getOrderById(review.getOrderId());
            Product product = productService.getProductById(order.getProductId());
            User seller = userService.getUserById(review.getRevieweeId());
            
            String stars = "★".repeat(review.getRating()) + "☆".repeat(5 - review.getRating());
            String timeStr = review.getCreateTime().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            System.out.printf("商品：%s | 卖家：%s%n", product.getTitle(), seller.getUsername());
            System.out.printf("%s [%s]%n", stars, timeStr);
            System.out.printf("  %s%n%n", review.getContent());
        }
    }
    
    private void handleMyReviews() {
        User seller = userService.getCurrentUser();
        List<Review> reviews = reviewService.getReviewsBySeller(seller.getUserId());
        
        if (reviews.isEmpty()) {
            ConsoleUtil.printInfo("暂未收到评价");
            return;
        }
        
        ConsoleUtil.printTitle("我收到的评价");
        double avgRating = reviewService.getAverageRating(seller.getUserId());
        System.out.printf("平均评分：%.1f（%d条评价）%n%n", avgRating, reviews.size());
        
        for (Review review : reviews) {
            Order order = orderService.getOrderById(review.getOrderId());
            Product product = productService.getProductById(order.getProductId());
            
            String stars = "★".repeat(review.getRating()) + "☆".repeat(5 - review.getRating());
            String timeStr = review.getCreateTime().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            System.out.printf("商品：%s%n", product.getTitle());
            System.out.printf("%s [%s]%n", stars, timeStr);
            System.out.printf("  %s%n%n", review.getContent());
        }
    }
    
    // ========== 订单取消功能 ==========
    
    /**
     * 买家取消订单
     */
    private void handleCancelOrder() {
        User buyer = userService.getCurrentUser();
        List<Order> orders = orderService.getOrdersByBuyer(buyer.getUserId());
        
        // 筛选出可以取消的订单（待确认或已确认的订单）
        List<Order> cancellableOrders = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.CONFIRMED)
            .toList();
        
        if (cancellableOrders.isEmpty()) {
            ConsoleUtil.printInfo("暂无可取消的订单");
            return;
        }
        
        ConsoleUtil.printTitle("取消订单");
        
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
            .setHeaders("No.", "Order ID", "Product Name", "Price", "Status");
        
        for (int i = 0; i < cancellableOrders.size(); i++) {
            Order order = cancellableOrders.get(i);
            Product product = productService.getProductById(order.getProductId());
            String statusDesc = getOrderStatusForBuyer(order.getStatus());
            table.addRow(
                String.valueOf(i + 1),
                order.getOrderId(),
                TranslationUtil.toEnglish(product.getTitle()),
                String.format("%.2f", order.getPrice()),
                TranslationUtil.toEnglish(statusDesc)
            );
        }
        
        table.print();
        
        Integer index = readIntSafely("\n选择要取消的订单编号（0返回）：", 
            "订单编号无效，请输入有效数字");
        if (index != null && index > 0 && index <= cancellableOrders.size()) {
            Order selected = cancellableOrders.get(index - 1);
            Product product = productService.getProductById(selected.getProductId());
            
            System.out.println("\n=== 订单信息 ===");
            System.out.println("订单ID：" + selected.getOrderId());
            System.out.println("商品：" + product.getTitle());
            System.out.println("价格：￥" + selected.getPrice());
            System.out.println("状态：" + getOrderStatusForBuyer(selected.getStatus()));
            
            // 输入取消理由，支持重试
            String reason = null;
            while (reason == null) {
                System.out.print("\n请输入取消理由（5-200字）：");
                String input = scanner.nextLine();
                
                if (input.trim().isEmpty()) {
                    ConsoleUtil.printError("取消理由不能为空");
                    System.out.print("是否继续？(y/n)：");
                    String retry = scanner.nextLine();
                    if (!retry.equalsIgnoreCase("y")) {
                        ConsoleUtil.printInfo("已取消操作");
                        return;
                    }
                } else if (input.trim().length() < 5) {
                    ConsoleUtil.printError("取消理由至少需要5个字");
                    System.out.print("是否继续？(y/n)：");
                    String retry = scanner.nextLine();
                    if (!retry.equalsIgnoreCase("y")) {
                        ConsoleUtil.printInfo("已取消操作");
                        return;
                    }
                } else if (input.trim().length() > 200) {
                    ConsoleUtil.printError("取消理由最多200个字");
                    System.out.print("是否继续？(y/n)：");
                    String retry = scanner.nextLine();
                    if (!retry.equalsIgnoreCase("y")) {
                        ConsoleUtil.printInfo("已取消操作");
                        return;
                    }
                } else {
                    reason = input;
                }
            }
            
            System.out.print("\n确认取消？(y/n)：");
            if (scanner.nextLine().equalsIgnoreCase("y")) {
                orderService.cancelOrder(buyer, selected.getOrderId(), reason);
                ConsoleUtil.printSuccess("订单已取消");
                ConsoleUtil.printInfo("信誉-5。商品已恢复为可售状态。");
            } else {
                ConsoleUtil.printInfo("已放弃取消");
            }
        }
    }
    
    /**
     * 卖家取消订单
     */
    private void handleCancelOrderAsSeller() {
        User seller = userService.getCurrentUser();
        List<Order> orders = orderService.getOrdersBySeller(seller.getUserId());
        
        // 筛选出可以取消的订单
        List<Order> cancellableOrders = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.CONFIRMED)
            .toList();
        
        if (cancellableOrders.isEmpty()) {
            ConsoleUtil.printInfo("暂无可取消的订单");
            return;
        }
        
        ConsoleUtil.printTitle("取消订单（卖家）");
        
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
            .setHeaders("No.", "Order ID", "Product Name", "Price", "Status");
        
        for (int i = 0; i < cancellableOrders.size(); i++) {
            Order order = cancellableOrders.get(i);
            Product product = productService.getProductById(order.getProductId());
            String statusDesc = getOrderStatusForSeller(order.getStatus());
            table.addRow(
                String.valueOf(i + 1),
                order.getOrderId(),
                TranslationUtil.toEnglish(product.getTitle()),
                String.format("%.2f", order.getPrice()),
                TranslationUtil.toEnglish(statusDesc)
            );
        }
        
        table.print();
        
        Integer index = readIntSafely("\n选择要取消的订单编号（0返回）：", 
            "订单编号无效，请输入有效数字");
        if (index != null && index > 0 && index <= cancellableOrders.size()) {
            Order selected = cancellableOrders.get(index - 1);
            Product product = productService.getProductById(selected.getProductId());
            
            System.out.println("\n=== 订单信息 ===");
            System.out.println("订单ID：" + selected.getOrderId());
            System.out.println("商品：" + product.getTitle());
            System.out.println("价格：￥" + selected.getPrice());
            System.out.println("状态：" + getOrderStatusForSeller(selected.getStatus()));
            
            // 输入取消理由，支持重试
            String reason = null;
            while (reason == null) {
                System.out.print("\n请输入取消理由（5-200字）：");
                String input = scanner.nextLine();
                
                if (input.trim().isEmpty()) {
                    ConsoleUtil.printError("取消理由不能为空");
                    System.out.print("是否继续？(y/n)：");
                    String retry = scanner.nextLine();
                    if (!retry.equalsIgnoreCase("y")) {
                        ConsoleUtil.printInfo("已取消操作");
                        return;
                    }
                } else if (input.trim().length() < 5) {
                    ConsoleUtil.printError("取消理由至少需要5个字");
                    System.out.print("是否继续？(y/n)：");
                    String retry = scanner.nextLine();
                    if (!retry.equalsIgnoreCase("y")) {
                        ConsoleUtil.printInfo("已取消操作");
                        return;
                    }
                } else if (input.trim().length() > 200) {
                    ConsoleUtil.printError("取消理由最多200个字");
                    System.out.print("是否继续？(y/n)：");
                    String retry = scanner.nextLine();
                    if (!retry.equalsIgnoreCase("y")) {
                        ConsoleUtil.printInfo("已取消操作");
                        return;
                    }
                } else {
                    reason = input;
                }
            }
            
            System.out.print("\n确认取消？(y/n)：");
            if (scanner.nextLine().equalsIgnoreCase("y")) {
                orderService.cancelOrder(seller, selected.getOrderId(), reason);
                ConsoleUtil.printSuccess("订单已取消");
                ConsoleUtil.printInfo("信誉-5。商品已恢复为可售状态。");
            } else {
                ConsoleUtil.printInfo("已放弃取消");
            }
        }
    }
    
    /**
     * 管理商品（编辑/下架）
     */
    private void handleManageProducts() {
        User seller = userService.getCurrentUser();
        List<Product> products = productService.getProductsBySeller(seller.getUserId());
        
        if (products.isEmpty()) {
            ConsoleUtil.printInfo("暂无商品可管理");
            return;
        }
        
        ConsoleUtil.printTitle("管理商品");
        
            PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                .setHeaders("No.", "Product Name", "Price", "Status");
        
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            table.addRow(
                String.valueOf(i + 1),
                TranslationUtil.toEnglish(p.getTitle()),
                String.format("%.2f", p.getPrice()),
                TranslationUtil.toEnglish(p.getStatus().getDisplayName())
            );
        }
        
        table.print();
        
        Integer index = readIntSafely("\n选择商品编号（0返回）：", 
            "商品编号无效，请输入有效数字");
        if (index != null && index > 0 && index <= products.size()) {
            Product selected = products.get(index - 1);
            handleProductManagementOptions(selected);
        }
    }
    
    /**
     * 商品管理选项
     */
    private void handleProductManagementOptions(Product product) {
        ConsoleUtil.printTitle("商品管理 - " + product.getTitle());
        System.out.println("当前价格：￥" + product.getPrice());
        System.out.println("状态：" + product.getStatus().getDisplayName());
        System.out.println("描述：" + product.getDescription());
        System.out.println();
        
        System.out.println("[1] 编辑商品信息");
        System.out.println("[2] 下架商品");
        System.out.println("[3] 重新上架商品");
        System.out.println("[0] 返回");
        System.out.print("请选择：");
        
        String choice = scanner.nextLine();
        User seller = userService.getCurrentUser();
        
        try {
            switch (choice) {
                case "1" -> handleEditProduct(seller, product);
                case "2" -> handleRemoveProduct(seller, product);
                case "3" -> handleReListProduct(seller, product);
                case "0" -> {}
                default -> ConsoleUtil.printError("无效选项");
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
    }
    
    /**
     * 编辑商品
     */
    private void handleEditProduct(User seller, Product product) {
        if (product.getStatus() != ProductStatus.AVAILABLE) {
            ConsoleUtil.printError("只能编辑在售商品");
            return;
        }
        
        ConsoleUtil.printTitle("编辑商品");
        ConsoleUtil.printInfo("提示：按回车保持当前值，输入'0'取消");
        
        System.out.print("\n新标题（当前：" + product.getTitle() + "）：");
        String newTitle = scanner.nextLine();
        if (newTitle.equals("0")) {
            ConsoleUtil.printInfo("已取消编辑");
            return;
        }
        if (newTitle.trim().isEmpty()) {
            newTitle = product.getTitle();
        }
        
        System.out.print("新描述（当前：" + product.getDescription() + "）：");
        String newDescription = scanner.nextLine();
        if (newDescription.equals("0")) {
            ConsoleUtil.printInfo("已取消编辑");
            return;
        }
        if (newDescription.trim().isEmpty()) {
            newDescription = product.getDescription();
        }
        
        System.out.print("新价格（当前：￥" + product.getPrice() + "）：");
        String priceInput = scanner.nextLine();
        if (priceInput.equals("0")) {
            ConsoleUtil.printInfo("已取消编辑");
            return;
        }
        double newPrice = product.getPrice();
        if (!priceInput.trim().isEmpty()) {
            try {
                newPrice = Double.parseDouble(priceInput);
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("价格格式无效");
                return;
            }
        }
        
        System.out.print("\n确认修改？(y/n)：");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            productService.editProduct(seller, product.getProductId(), newTitle, newDescription, newPrice);
            ConsoleUtil.printSuccess("商品更新成功！");
        } else {
            ConsoleUtil.printInfo("已取消编辑");
        }
    }
    
    /**
     * 下架商品
     */
    private void handleRemoveProduct(User seller, Product product) {
        if (product.getStatus() == ProductStatus.REMOVED) {
            ConsoleUtil.printError("商品已下架，无法重复下架");
            return;
        }
        
        System.out.print("\n确认下架商品'" + product.getTitle() + "'？(y/n)：");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            productService.removeProduct(seller, product.getProductId());
            ConsoleUtil.printSuccess("商品下架成功");
        } else {
            ConsoleUtil.printInfo("操作已取消");
        }
    }
    
    /**
     * 重新上架商品
     */
    private void handleReListProduct(User seller, Product product) {
        if (product.getStatus() != ProductStatus.REMOVED) {
            ConsoleUtil.printInfo("只能重新上架已下架的商品");
            return;
        }
        
        System.out.print("\n确认重新上架商品'" + product.getTitle() + "'？(y/n)：");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            productService.reListProduct(seller, product.getProductId());
            ConsoleUtil.printSuccess("商品重新上架成功");
        } else {
            ConsoleUtil.printInfo("操作已取消");
        }
    }
    
    // ========== 通用功能处理方法 ==========
    
    private void handleMyMessages() {
        User user = userService.getCurrentUser();
        
        // 获取历史消息（持久化的）
        NotificationService notificationService = new NotificationService();
        List<Message> historyMessages = notificationService.getMessageHistory(user.getUserId());
        
        if (historyMessages.isEmpty()) {
            ConsoleUtil.printInfo("暂无消息");
        } else {
            ConsoleUtil.printTitle("我的消息");
            System.out.println(String.format("共计：%d 条消息\n", historyMessages.size()));
            
            for (Message msg : historyMessages) {
                String timeStr = msg.getCreateTime().format(
                    DateTimeFormatter.ofPattern("MM-dd HH:mm"));
                System.out.printf("[%s] %s%n", timeStr, msg.getContent());
            }
        }
    }
    
    private void handleLogout() {
        userService.logout();
        ConsoleUtil.printSuccess("退出登录成功");
    }
}

