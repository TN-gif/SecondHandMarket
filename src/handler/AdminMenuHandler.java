package handler;

import entity.User;
import service.AdminService;
import service.UserService;
import util.ConsoleUtil;

import java.util.List;
import java.util.Scanner;

/**
 * 管理员菜单处理器
 * 职责：处理管理员的交互
 */
public class AdminMenuHandler implements MenuHandler {
    private final UserService userService;
    private final AdminService adminService;
    private final Scanner scanner;
    
    public AdminMenuHandler(UserService userService, AdminService adminService) {
        this.userService = userService;
        this.adminService = adminService;
        this.scanner = new Scanner(System.in);
    }
    
    @Override
    public void displayAndHandle() {
        ConsoleUtil.printDivider();
        System.out.println("【管理员功能】");
        System.out.println("[1] 用户管理");
        System.out.println("[2] 系统统计");
        System.out.println("[0] 退出登录");
        ConsoleUtil.printDivider();
        
        System.out.print("请选择：");
        String choice = scanner.nextLine();
        
        try {
            switch (choice) {
                case "1" -> handleUserManagement();
                case "2" -> handleSystemStats();
                case "0" -> handleLogout();
                default -> ConsoleUtil.printError("无效选项");
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
    }
    
    private void handleUserManagement() {
        User admin = userService.getCurrentUser();
        List<User> users = adminService.getAllUsers(admin);
        
        ConsoleUtil.printTitle("用户管理");
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            System.out.printf("[%d] %s | 状态：%s | 信誉：%d分%n",
                i + 1, u.getUsername(), u.getStatus().getDisplayName(), u.getReputation());
        }
        
        System.out.println("\n[1] 封禁用户  [2] 解封用户  [0] 返回");
        System.out.print("选择：");
        String choice = scanner.nextLine();
        
        if (choice.equals("1") || choice.equals("2")) {
            System.out.print("输入用户编号：");
            try {
                int index = Integer.parseInt(scanner.nextLine());
                if (index > 0 && index <= users.size()) {
                    User selected = users.get(index - 1);
                    if (choice.equals("1")) {
                        adminService.banUser(admin, selected.getUserId());
                        ConsoleUtil.printSuccess("用户已封禁");
                    } else {
                        adminService.unbanUser(admin, selected.getUserId());
                        ConsoleUtil.printSuccess("用户已解封");
                    }
                }
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("输入格式错误");
            }
        }
    }
    
    private void handleSystemStats() {
        User admin = userService.getCurrentUser();
        AdminService.SystemStats stats = adminService.getSystemStats(admin);
        
        ConsoleUtil.printTitle("系统统计");
        System.out.println("用户总数：" + stats.userCount());
        System.out.println("商品总数：" + stats.productCount());
        System.out.println("订单总数：" + stats.orderCount());
        System.out.println("评价总数：" + stats.reviewCount());
        
        System.out.println("\n查看详细信息：");
        System.out.println("[1] 用户详细统计");
        System.out.println("[2] 商品详细统计");
        System.out.println("[3] 订单详细统计");
        System.out.println("[0] 返回");
        System.out.print("选择：");
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1" -> showUserStats(admin);
            case "2" -> showProductStats(admin);
            case "3" -> showOrderStats(admin);
        }
    }
    
    private void showUserStats(User admin) {
        List<User> users = adminService.getAllUsers(admin);
        
        ConsoleUtil.printTitle("用户详细统计");
        
        // 统计各角色数量
        long buyerCount = users.stream().filter(u -> u.hasRole(enums.UserRole.BUYER)).count();
        long sellerCount = users.stream().filter(u -> u.hasRole(enums.UserRole.SELLER)).count();
        long adminCount = users.stream().filter(u -> u.hasRole(enums.UserRole.ADMIN)).count();
        
        System.out.println("买家数量：" + buyerCount);
        System.out.println("卖家数量：" + sellerCount);
        System.out.println("管理员数量：" + adminCount);
        
        // 统计各状态数量
        long activeCount = users.stream().filter(u -> u.getStatus() == enums.UserStatus.ACTIVE).count();
        long bannedCount = users.stream().filter(u -> u.getStatus() == enums.UserStatus.BANNED).count();
        
        System.out.println("\n用户状态：");
        System.out.println("正常：" + activeCount);
        System.out.println("封禁：" + bannedCount);
        
        // 信誉分统计
        double avgReputation = users.stream().mapToInt(User::getReputation).average().orElse(0);
        System.out.println("\n平均信誉分：" + String.format("%.1f", avgReputation));
        
        // 显示信誉前5的用户
        System.out.println("\n信誉排行榜（前5名）：");
        users.stream()
            .sorted((u1, u2) -> Integer.compare(u2.getReputation(), u1.getReputation()))
            .limit(5)
            .forEach(u -> System.out.printf("  %s - %d分%n", u.getUsername(), u.getReputation()));
    }
    
    private void showProductStats(User admin) {
        repository.DataCenter dc = repository.DataCenter.getInstance();
        List<entity.Product> products = dc.getAllProducts();
        
        ConsoleUtil.printTitle("商品详细统计");
        
        // 统计各状态数量
        long availableCount = products.stream()
            .filter(p -> p.getStatus() == enums.ProductStatus.AVAILABLE).count();
        long reservedCount = products.stream()
            .filter(p -> p.getStatus() == enums.ProductStatus.RESERVED).count();
        long soldCount = products.stream()
            .filter(p -> p.getStatus() == enums.ProductStatus.SOLD).count();
        
        System.out.println("在售：" + availableCount);
        System.out.println("预定中：" + reservedCount);
        System.out.println("已售出：" + soldCount);
        
        // 统计各分类数量
        System.out.println("\n商品分类统计：");
        for (enums.ProductCategory category : enums.ProductCategory.values()) {
            long count = products.stream()
                .filter(p -> p.getCategory() == category).count();
            System.out.printf("  %s：%d%n", category.getDisplayName(), count);
        }
        
        // 价格统计
        double avgPrice = products.stream().mapToDouble(entity.Product::getPrice).average().orElse(0);
        double maxPrice = products.stream().mapToDouble(entity.Product::getPrice).max().orElse(0);
        double minPrice = products.stream().mapToDouble(entity.Product::getPrice).min().orElse(0);
        
        System.out.println("\n价格统计：");
        System.out.println("平均价格：¥" + String.format("%.2f", avgPrice));
        System.out.println("最高价格：¥" + String.format("%.2f", maxPrice));
        System.out.println("最低价格：¥" + String.format("%.2f", minPrice));
    }
    
    private void showOrderStats(User admin) {
        repository.DataCenter dc = repository.DataCenter.getInstance();
        List<entity.Order> orders = dc.getAllOrders();
        
        ConsoleUtil.printTitle("订单详细统计");
        
        // 统计各状态数量
        long pendingCount = orders.stream()
            .filter(o -> o.getStatus() == enums.OrderStatus.PENDING).count();
        long confirmedCount = orders.stream()
            .filter(o -> o.getStatus() == enums.OrderStatus.CONFIRMED).count();
        long completedCount = orders.stream()
            .filter(o -> o.getStatus() == enums.OrderStatus.COMPLETED).count();
        long cancelledCount = orders.stream()
            .filter(o -> o.getStatus() == enums.OrderStatus.CANCELLED).count();
        
        System.out.println("待确认：" + pendingCount);
        System.out.println("已确认：" + confirmedCount);
        System.out.println("已完成：" + completedCount);
        System.out.println("已取消：" + cancelledCount);
        
        // 交易金额统计
        double totalAmount = orders.stream()
            .filter(o -> o.getStatus() == enums.OrderStatus.COMPLETED)
            .mapToDouble(entity.Order::getPrice).sum();
        double avgAmount = orders.stream()
            .filter(o -> o.getStatus() == enums.OrderStatus.COMPLETED)
            .mapToDouble(entity.Order::getPrice).average().orElse(0);
        
        System.out.println("\n交易金额统计（已完成订单）：");
        System.out.println("总交易额：¥" + String.format("%.2f", totalAmount));
        System.out.println("平均订单金额：¥" + String.format("%.2f", avgAmount));
        
        // 订单完成率
        double completionRate = orders.isEmpty() ? 0 : 
            (double) completedCount / orders.size() * 100;
        System.out.println("\n订单完成率：" + String.format("%.1f%%", completionRate));
    }
    
    private void handleLogout() {
        userService.logout();
        ConsoleUtil.printSuccess("已退出登录");
    }
}

