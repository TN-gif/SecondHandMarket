package handler;

import dto.SearchCriteria;
import entity.Product;
import entity.User;
import enums.UserRole;
import service.ProductService;
import service.UserService;
import strategy.ProductSortStrategies;
import util.ConsoleUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * 主菜单处理器
 * 职责：处理未登录状态下的用户交互
 */
public class MainMenuHandler implements MenuHandler {
    private final UserService userService;
    private final ProductService productService;
    private final Scanner scanner;
    
    public MainMenuHandler(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
        this.scanner = new Scanner(System.in);
    }
    
    @Override
    public void displayAndHandle() {
        ConsoleUtil.printDivider();
        System.out.println("【主菜单】");
        System.out.println("[1] 用户登录");
        System.out.println("[2] 用户注册");
        System.out.println("[3] 浏览商品（游客）");
        System.out.println("[4] 系统说明");
        System.out.println("[0] 退出系统");
        ConsoleUtil.printDivider();
        
        System.out.print("请选择：");
        String choice = scanner.nextLine();
        
        try {
            switch (choice) {
                case "1" -> handleLogin();
                case "2" -> handleRegister();
                case "3" -> handleBrowseProducts();
                case "4" -> handleSystemInfo();
                case "0" -> handleExit();
                default -> ConsoleUtil.printError("无效选项");
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
    }
    
    /**
     * 处理登录
     */
    private void handleLogin() {
        ConsoleUtil.printTitle("用户登录");
        System.out.print("用户名：");
        String username = scanner.nextLine();
        System.out.print("密码：");
        String password = scanner.nextLine();
        
        User user = userService.login(username, password);
        ConsoleUtil.printSuccess("登录成功！欢迎，" + user.getUsername());
    }
    
    /**
     * 处理注册
     */
    private void handleRegister() {
        ConsoleUtil.printTitle("用户注册");
        System.out.print("用户名（4-20位字母数字）：");
        String username = scanner.nextLine();
        System.out.print("密码（6-20位）：");
        String password = scanner.nextLine();
        System.out.print("确认密码：");
        String confirmPassword = scanner.nextLine();
        
        if (!password.equals(confirmPassword)) {
            ConsoleUtil.printError("两次密码不一致");
            return;
        }
        
        System.out.print("是否注册为买家？(y/n)：");
        boolean isBuyer = scanner.nextLine().equalsIgnoreCase("y");
        System.out.print("是否注册为卖家？(y/n)：");
        boolean isSeller = scanner.nextLine().equalsIgnoreCase("y");
        
        if (!isBuyer && !isSeller) {
            ConsoleUtil.printError("至少选择一个角色");
            return;
        }
        
        // 构建角色集合
        Set<UserRole> roles = EnumSet.noneOf(UserRole.class);
        if (isBuyer) roles.add(UserRole.BUYER);
        if (isSeller) roles.add(UserRole.SELLER);
        
        userService.register(username, password, roles);
        ConsoleUtil.printSuccess("注册成功！请登录");
    }
    
    /**
     * 处理浏览商品（游客）
     */
    private void handleBrowseProducts() {
        List<Product> products = productService.searchProducts(
            new SearchCriteria.Builder().build());
        
        if (products.isEmpty()) {
            ConsoleUtil.printInfo("暂无商品");
            return;
        }
        
        // 按时间降序排序（最新的在前）
        productService.sortProducts(products, ProductSortStrategies.BY_TIME_DESC);
        
        ConsoleUtil.printTitle("商品列表（游客模式）");
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            System.out.printf("[%d] %s - ¥%.2f [%s] [%s]%n",
                i + 1, p.getTitle(), p.getPrice(),
                p.getCategory().getDisplayName(),
                p.getCondition().getDescription());
        }
        
        ConsoleUtil.printInfo("提示：登录后可购买商品");
    }
    
    /**
     * 处理系统说明
     */
    private void handleSystemInfo() {
        ConsoleUtil.printTitle("系统说明");
        System.out.println("📦 校园二手商品交易管理系统");
        System.out.println("版本：1.0");
        System.out.println();
        System.out.println("功能模块：");
        System.out.println("  • 用户管理：注册、登录、多角色支持");
        System.out.println("  • 商品管理：发布、编辑、搜索、排序");
        System.out.println("  • 订单管理：下单、确认、收货、取消");
        System.out.println("  • 评价管理：评分、评价、信誉系统");
        System.out.println();
        System.out.println("技术亮点：");
        System.out.println("  • EnumSet角色管理");
        System.out.println("  • 观察者模式消息通知");
        System.out.println("  • 依赖注入（IoC）");
        System.out.println("  • 5种设计模式应用");
        System.out.println("  • RESERVED中间状态流转");
    }
    
    /**
     * 处理退出
     */
    private void handleExit() {
        ConsoleUtil.printInfo("感谢使用，再见！");
        System.exit(0);
    }
}

