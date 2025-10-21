package handler;

import entity.Appeal;
import entity.Order;
import entity.Product;
import entity.User;
import enums.OrderStatus;
import enums.ProductCategory;
import enums.ProductStatus;
import enums.UserRole;
import enums.UserStatus;
import exception.ResourceNotFoundException;
import repository.DataCenter;
import service.AdminService;
import service.UserService;
import util.ConsoleUtil;
import util.InputValidator;
import util.PerfectTableFormatter;
import util.TranslationUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * 管理员菜单处理器
 * 
 * 负责管理员功能的用户界面交互，包括用户管理、申诉处理和系统统计。
 */
public class AdminMenuHandler extends BaseMenuHandler {
    private final AdminService adminService;
    
    public AdminMenuHandler(UserService userService, AdminService adminService) {
        super(userService);
        this.adminService = adminService;
    }
    
    @Override
    public void displayAndHandle() {
        ConsoleUtil.printDivider();
        System.out.println("【管理员功能】");
        System.out.println("[1] 用户管理");
        System.out.println("[2] 系统统计");
        System.out.println("[3] 处理申诉");
        System.out.println("[0] 退出登录");
        ConsoleUtil.printDivider();
        
        System.out.print("请选择：");
        String choice = scanner.nextLine();
        
        try {
            switch (choice) {
                case "1" -> handleUserManagement();
                case "2" -> handleSystemStats();
                case "3" -> handleAppeals();
                case "0" -> handleLogout();
                default -> ConsoleUtil.printError("无效选项");
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
    }
    
    private void handleAppeals() {
        DataCenter dc = DataCenter.getInstance();
        List<Appeal> appeals = dc.findUnprocessedAppeals();
        
        if (appeals.isEmpty()) {
            ConsoleUtil.printInfo("暂无待处理申诉");
            return;
        }
        
        ConsoleUtil.printTitle("处理申诉");
        
        // 使用FlipTables专业库创建表格
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                .setHeaders("No.", "Appeal ID", "Username", "Appeal Time", "Reason");
        
        for (int i = 0; i < appeals.size(); i++) {
            Appeal appeal = appeals.get(i);
            User user = dc.findUserById(appeal.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
            
            String timeStr = appeal.getCreateTime().format(
                    DateTimeFormatter.ofPattern("MM-dd HH:mm"));
            
            table.addRow(
                String.valueOf(i + 1),
                appeal.getAppealId(),
                user.getUsername(),
                timeStr,
                TranslationUtil.toEnglish(appeal.getReason())
            );
        }
        
        table.print();
        
        Integer index = readIntSafely("\n选择要处理的申诉编号（0返回）：", "申诉编号无效，请输入有效数字");
        if (index != null && index > 0 && index <= appeals.size()) {
            Appeal selected = appeals.get(index - 1);
            User user = dc.findUserById(selected.getUserId()).orElseThrow();
            
            System.out.println("\n[1] 通过申诉（解封用户）");
            System.out.println("[2] 拒绝申诉");
            System.out.println("[0] 取消");
            System.out.print("请选择：");
            String decision = scanner.nextLine();
            
            switch (decision) {
                case "1" -> {
                    // 解封用户
                    user.setStatus(UserStatus.ACTIVE);
                    user.increaseReputation(20);
                    selected.process("已通过：账号已解封");
                    ConsoleUtil.printSuccess("申诉已通过，用户已解封");
                }
                case "2" -> {
                    System.out.print("拒绝理由：");
                    String reason = scanner.nextLine();
                    selected.process("已拒绝：" + reason);
                    ConsoleUtil.printSuccess("申诉已拒绝");
                }
                default -> ConsoleUtil.printInfo("已取消");
            }
        }
    }
    
    private void handleUserManagement() {
        User admin = userService.getCurrentUser();
        List<User> users = adminService.getAllUsers(admin);
        
        ConsoleUtil.printTitle("用户管理");
        
        // 使用FlipTables专业库创建表格
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                .setHeaders("No.", "Username", "Role", "Status", "Reputation");
        
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            // 构建角色信息（使用中文"和"代替符号）
            StringBuilder roleInfo = new StringBuilder();
            if (u.hasRole(UserRole.BUYER) && u.hasRole(UserRole.SELLER)) {
                roleInfo.append("Buyer and Seller");
            } else if (u.hasRole(UserRole.BUYER)) {
                roleInfo.append("Buyer");
            } else if (u.hasRole(UserRole.SELLER)) {
                roleInfo.append("Seller");
            }
            if (u.hasRole(UserRole.ADMIN)) {
                if (roleInfo.length() > 0) roleInfo.append(" and ");
                roleInfo.append("Admin");
            }
            
            table.addRow(
                String.valueOf(i + 1),
                u.getUsername(),
                TranslationUtil.toEnglish(roleInfo.toString()),
                TranslationUtil.toEnglish(u.getStatus().getDisplayName()),
                String.valueOf(u.getReputation())
            );
        }
        
        table.print();
        
        System.out.println("\n[1] 封禁用户  [2] 解封用户  [0] 返回");
        System.out.print("请选择：");
        String choice = scanner.nextLine();
        
        if (choice.equals("1") || choice.equals("2")) {
            Integer index = readIntSafely("输入用户编号：", "用户编号无效，请输入有效数字");
            if (index != null && index > 0 && index <= users.size()) {
                User selected = users.get(index - 1);
                if (choice.equals("1")) {
                    adminService.banUser(admin, selected.getUserId());
                    ConsoleUtil.printSuccess("用户已封禁");
                } else {
                    adminService.unbanUser(admin, selected.getUserId());
                    ConsoleUtil.printSuccess("用户已解封");
                }
            }
        }
    }
    
    private void handleSystemStats() {
        User admin = userService.getCurrentUser();
        
        while (true) {
            AdminService.SystemStats stats = adminService.getSystemStats(admin);
            
            ConsoleUtil.printTitle("系统统计");
            System.out.println("用户总数：" + stats.userCount());
            System.out.println("商品总数：" + stats.productCount());
            System.out.println("订单总数：" + stats.orderCount());
            System.out.println("评价总数：" + stats.reviewCount());
            
            System.out.println("\n查看详细信息：");
            System.out.println("[1] 用户统计");
            System.out.println("[2] 商品统计");
            System.out.println("[3] 订单统计");
            System.out.println("[0] 返回");
            System.out.print("请选择：");
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1" -> showUserStats(admin);
                case "2" -> showProductStats(admin);
                case "3" -> showOrderStats(admin);
                case "0" -> {
                    return;
                }
                default -> ConsoleUtil.printError("无效选项");
            }
        }
    }
    
    private void showUserStats(User admin) {
        List<User> users = adminService.getAllUsers(admin);
        
        ConsoleUtil.printTitle("用户统计");
        
        // 统计各角色数量
        long buyerCount = users.stream().filter(u -> u.hasRole(UserRole.BUYER)).count();
        long sellerCount = users.stream().filter(u -> u.hasRole(UserRole.SELLER)).count();
        long adminCount = users.stream().filter(u -> u.hasRole(UserRole.ADMIN)).count();
        
        System.out.println("买家数：" + buyerCount);
        System.out.println("卖家数：" + sellerCount);
        System.out.println("管理员数：" + adminCount);
        
        // 统计各状态数量
        long activeCount = users.stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
        long bannedCount = users.stream().filter(u -> u.getStatus() == UserStatus.BANNED).count();
        
        System.out.println("\n用户状态：");
        System.out.println("活跃：" + activeCount);
        System.out.println("封禁：" + bannedCount);
        
        // 信誉分统计
        double avgReputation = users.stream().mapToInt(User::getReputation).average().orElse(0);
        System.out.println("\n平均信誉：" + String.format("%.1f", avgReputation));
        
        // 显示信誉前5的用户
        System.out.println("\n信誉排行榜（前5名）：");
        users.stream()
            .sorted((u1, u2) -> Integer.compare(u2.getReputation(), u1.getReputation()))
            .limit(5)
            .forEach(u -> System.out.printf("  %s - %d 分%n", u.getUsername(), u.getReputation()));
        
        System.out.print("\n按0返回系统统计：");
        scanner.nextLine();
    }
    
    private void showProductStats(User admin) {
        DataCenter dc = DataCenter.getInstance();
        List<Product> products = dc.getAllProducts();
        
        ConsoleUtil.printTitle("商品统计");
        
        // 统计各状态数量
        long availableCount = products.stream()
            .filter(p -> p.getStatus() == ProductStatus.AVAILABLE).count();
        long reservedCount = products.stream()
            .filter(p -> p.getStatus() == ProductStatus.RESERVED).count();
        long soldCount = products.stream()
            .filter(p -> p.getStatus() == ProductStatus.SOLD).count();
        
        System.out.println("在售：" + availableCount);
        System.out.println("预定：" + reservedCount);
        System.out.println("已售：" + soldCount);
        
        // 统计各分类数量
        System.out.println("\n商品分类统计：");
        for (ProductCategory category : ProductCategory.values()) {
            long count = products.stream()
                .filter(p -> p.getCategory() == category).count();
            System.out.printf("  %s: %d%n", category.getDisplayName(), count);
        }
        
        // 价格统计
        double avgPrice = products.stream().mapToDouble(Product::getPrice).average().orElse(0);
        double maxPrice = products.stream().mapToDouble(Product::getPrice).max().orElse(0);
        double minPrice = products.stream().mapToDouble(Product::getPrice).min().orElse(0);
        
        System.out.println("\n价格统计：");
        System.out.println("平均价格：￥" + String.format("%.2f", avgPrice));
        System.out.println("最高价格：￥" + String.format("%.2f", maxPrice));
        System.out.println("最低价格：￥" + String.format("%.2f", minPrice));
        
        System.out.print("\n按0返回系统统计：");
        scanner.nextLine();
    }
    
    private void showOrderStats(User admin) {
        DataCenter dc = DataCenter.getInstance();
        List<Order> orders = dc.getAllOrders();
        
        ConsoleUtil.printTitle("订单统计");
        
        // 统计各状态数量
        long pendingCount = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long confirmedCount = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        long completedCount = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
        long cancelledCount = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
        
        System.out.println("待确认：" + pendingCount);
        System.out.println("已确认：" + confirmedCount);
        System.out.println("已完成：" + completedCount);
        System.out.println("已取消：" + cancelledCount);
        
        // 交易金额统计
        double totalAmount = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
            .mapToDouble(Order::getPrice).sum();
        double avgAmount = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
            .mapToDouble(Order::getPrice).average().orElse(0);
        
        System.out.println("\n交易金额统计（已完成订单）：");
        System.out.println("总金额：￥" + String.format("%.2f", totalAmount));
        System.out.println("平均订单金额：￥" + String.format("%.2f", avgAmount));
        
        // 订单完成率
        double completionRate = orders.isEmpty() ? 0 : 
            (double) completedCount / orders.size() * 100;
        System.out.println("\n订单完成率：" + String.format("%.1f%%", completionRate));
        
        System.out.print("\n按0返回系统统计：");
        scanner.nextLine();
    }
    
    private void handleLogout() {
        userService.logout();
        ConsoleUtil.printSuccess("退出登录成功");
    }
    
}

