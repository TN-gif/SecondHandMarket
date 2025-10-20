package handler;

import dto.SearchCriteria;
import entity.Appeal;
import entity.Product;
import entity.User;
import enums.UserRole;
import enums.UserStatus;
import exception.AuthenticationException;
import repository.DataCenter;
import service.ProductService;
import service.UserService;
import strategy.ProductSortStrategies;
import util.ConsoleUtil;
import util.IdGenerator;
import util.InputValidator;
import util.PerfectTableFormatter;
import util.TranslationUtil;
import util.PasswordEncoder;

import java.time.format.DateTimeFormatter;
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
        System.out.println("[1] 登录");
        System.out.println("[2] 注册");
        System.out.println("[3] 浏览商品（游客模式）");
        System.out.println("[4] 提交申诉");
        System.out.println("[5] 查看申诉状态");
        System.out.println("[0] 退出系统");
        ConsoleUtil.printDivider();
        
        System.out.print("请选择：");
        String choice = scanner.nextLine();
        
        try {
            switch (choice) {
                case "1" -> handleLogin();
                case "2" -> handleRegister();
                case "3" -> handleBrowseProducts();
                case "4" -> handleAppeal();
                case "5" -> handleCheckAppealStatus();
                case "0" -> handleExit();
                default -> ConsoleUtil.printError("无效选项");
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
    }
    
    /**
     * 处理申诉
     */
    private void handleAppeal() {
        ConsoleUtil.printTitle("提交申诉");
        ConsoleUtil.printInfo("提示：如果您的账号被封禁，可以在此提交申诉");
        
        System.out.print("\n用户名：");
        String username = scanner.nextLine();
        
        if (username.equals("0")) {
            return;
        }
        
        System.out.print("密码：");
        String password = scanner.nextLine();
        
        if (password.equals("0")) {
            return;
        }
        
        try {
            // 验证身份
            User user = DataCenter.getInstance().findUserByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("用户名或密码错误"));
            
            if (!PasswordEncoder.matches(password, user.getPassword())) {
                throw new AuthenticationException("用户名或密码错误");
            }
            
            // 检查是否被封禁
            if (user.getStatus() != UserStatus.BANNED) {
                ConsoleUtil.printInfo("您的账号未被封禁，无需申诉");
                return;
            }
            
            // 检查是否已有未处理的申诉
            List<Appeal> existingAppeals = DataCenter.getInstance()
                    .findAppealsByUserId(user.getUserId()).stream()
                    .filter(a -> !a.isProcessed())
                    .toList();
            
            if (!existingAppeals.isEmpty()) {
                ConsoleUtil.printInfo("您的申诉正在处理中，请等待管理员审核");
                ConsoleUtil.printInfo("您可以通过菜单选项 [5] 查看申诉状态");
                return;
            }
            
            // 提交申诉
            System.out.print("\n请输入申诉理由（100字以内）：");
            String reason = scanner.nextLine();
            
            if (reason.trim().isEmpty()) {
                ConsoleUtil.printError("申诉理由不能为空");
                return;
            }
            
            if (reason.length() > 100) {
                ConsoleUtil.printError("申诉理由过长");
                return;
            }
            
            String appealId = IdGenerator.generateAppealId();
            Appeal appeal = new Appeal(appealId, user.getUserId(), reason);
            DataCenter.getInstance().addAppeal(appeal);
            
            ConsoleUtil.printSuccess("申诉已提交！申诉ID：" + appealId);
            ConsoleUtil.printInfo("您可以通过菜单选项 [5] 查看申诉状态");
            
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
    }
    
    /**
     * 查看申诉状态
     */
    private void handleCheckAppealStatus() {
        ConsoleUtil.printTitle("查看申诉状态");
        ConsoleUtil.printInfo("提示：输入您的凭证以查看申诉状态");
        
        System.out.print("\n用户名：");
        String username = scanner.nextLine();
        
        if (username.equals("0")) {
            return;
        }
        
        System.out.print("密码：");
        String password = scanner.nextLine();
        
        if (password.equals("0")) {
            return;
        }
        
        try {
            // 验证身份
            User user = DataCenter.getInstance().findUserByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("用户名或密码错误"));
            
            if (!PasswordEncoder.matches(password, user.getPassword())) {
                throw new AuthenticationException("用户名或密码错误");
            }
            
            // 获取该用户的所有申诉
            List<Appeal> appeals = DataCenter.getInstance().findAppealsByUserId(user.getUserId());
            
            if (appeals.isEmpty()) {
                ConsoleUtil.printInfo("您没有申诉记录");
                return;
            }
            
            // 显示申诉列表
            ConsoleUtil.printTitle("我的申诉");
            
                PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                    .setHeaders("No.", "Appeal ID", "Submit Time", "Status", "Result");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
            
            for (int i = 0; i < appeals.size(); i++) {
                Appeal appeal = appeals.get(i);
                String status = appeal.isProcessed() ? "Processed" : "Pending";
                String result = appeal.isProcessed() ? appeal.getResult() : "Waiting for review";
                
                table.addRow(
                    String.valueOf(i + 1),
                    appeal.getAppealId(),
                    appeal.getCreateTime().format(formatter),
                    TranslationUtil.toEnglish(status),
                    TranslationUtil.toEnglish(result)
                );
            }
            
            table.print();
            
            // 显示当前账号状态
            System.out.println("\n当前账号状态：" + user.getStatus().getDisplayName());
            
            if (user.getStatus() == UserStatus.BANNED) {
                // 检查是否可以重新提交申诉
                boolean hasUnprocessed = appeals.stream().anyMatch(a -> !a.isProcessed());
                if (!hasUnprocessed) {
                    ConsoleUtil.printInfo("您的所有申诉已处理完毕。您可以通过菜单选项 [4] 提交新的申诉");
                }
            } else {
                ConsoleUtil.printSuccess("您的账号已解封！");
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
        ConsoleUtil.printInfo("提示：输入 '0' 返回主菜单");
        
        while (true) {
            System.out.print("\n用户名：");
            String username = scanner.nextLine();
            
            if (username.equals("0")) {
                ConsoleUtil.printInfo("已取消登录");
                return;
            }
            
            if (username.trim().isEmpty()) {
                ConsoleUtil.printError("用户名不能为空，请重新输入");
                continue;
            }
            
            System.out.print("密码：");
            String password = scanner.nextLine();
            
            if (password.equals("0")) {
                ConsoleUtil.printInfo("已取消登录");
                return;
            }
            
            if (password.trim().isEmpty()) {
                ConsoleUtil.printError("密码不能为空，请重新输入");
                continue;
            }
            
            try {
                User user = userService.login(username, password);
                ConsoleUtil.printSuccess("登录成功！欢迎，" + user.getUsername());
                return;
            } catch (Exception e) {
                ConsoleUtil.printError(e.getMessage());
                System.out.print("\n是否重试？(y/n)：");
                String retry = scanner.nextLine();
                if (!retry.equalsIgnoreCase("y")) {
                    ConsoleUtil.printInfo("已取消登录");
                    return;
                }
            }
        }
    }
    
    /**
     * 处理注册
     */
    private void handleRegister() {
        ConsoleUtil.printTitle("用户注册");
        ConsoleUtil.printInfo("提示：输入 '0' 返回主菜单");
        
        String username = null;
        String password = null;
        Set<UserRole> roles = null;
        
        // 1. 输入用户名（带验证和重试）
        while (username == null) {
            System.out.print("\n用户名（4-20位字母数字）：");
            String input = scanner.nextLine();
            
            if (input.equals("0")) {
                ConsoleUtil.printInfo("已取消注册");
                return;
            }
            
            // 验证用户名格式
            if (!InputValidator.isValidUsername(input)) {
                ConsoleUtil.printError("用户名格式无效（需要4-20位字母数字）");
                System.out.print("是否重试？(y/n)：");
                if (!scanner.nextLine().equalsIgnoreCase("y")) {
                    ConsoleUtil.printInfo("已取消注册");
                    return;
                }
                continue;
            }
            
            // 检查用户名是否已存在
            if (DataCenter.getInstance().existsUsername(input)) {
                ConsoleUtil.printError("用户名已存在，请尝试其他用户名");
                System.out.print("是否重试？(y/n)：");
                if (!scanner.nextLine().equalsIgnoreCase("y")) {
                    ConsoleUtil.printInfo("已取消注册");
                    return;
                }
                continue;
            }
            
            username = input;
        }
        
        // 2. 输入密码（带验证和重试）
        while (password == null) {
            System.out.print("密码（6-20位字符）：");
            String pwd = scanner.nextLine();
            
            if (pwd.equals("0")) {
                ConsoleUtil.printInfo("已取消注册");
                return;
            }
            
            // 验证密码格式
            if (!InputValidator.isValidPassword(pwd)) {
                ConsoleUtil.printError("密码格式无效（需要6-20位字符）");
                System.out.print("是否重试？(y/n)：");
                if (!scanner.nextLine().equalsIgnoreCase("y")) {
                    ConsoleUtil.printInfo("已取消注册");
                    return;
                }
                continue;
            }
            
            System.out.print("确认密码：");
            String confirmPassword = scanner.nextLine();
            
            if (confirmPassword.equals("0")) {
                ConsoleUtil.printInfo("已取消注册");
                return;
            }
            
            if (!pwd.equals(confirmPassword)) {
                ConsoleUtil.printError("两次密码不一致");
                System.out.print("是否重试？(y/n)：");
                if (!scanner.nextLine().equalsIgnoreCase("y")) {
                    ConsoleUtil.printInfo("已取消注册");
                    return;
                }
                continue;
            }
            
            password = pwd;
        }
        
        // 3. 选择角色
        while (roles == null) {
            System.out.print("注册为买家？(y/n)：");
            String buyerInput = scanner.nextLine();
            
            if (buyerInput.equals("0")) {
                ConsoleUtil.printInfo("已取消注册");
                return;
            }
            
            boolean isBuyer = buyerInput.equalsIgnoreCase("y");
            
            System.out.print("注册为卖家？(y/n)：");
            String sellerInput = scanner.nextLine();
            
            if (sellerInput.equals("0")) {
                ConsoleUtil.printInfo("已取消注册");
                return;
            }
            
            boolean isSeller = sellerInput.equalsIgnoreCase("y");
            
            if (!isBuyer && !isSeller) {
                ConsoleUtil.printError("至少需要选择一个角色");
                System.out.print("是否重试？(y/n)：");
                if (!scanner.nextLine().equalsIgnoreCase("y")) {
                    ConsoleUtil.printInfo("已取消注册");
                    return;
                }
                continue;
            }
            
            // 构建角色集合
            roles = EnumSet.noneOf(UserRole.class);
            if (isBuyer) roles.add(UserRole.BUYER);
            if (isSeller) roles.add(UserRole.SELLER);
        }
        
        // 4. 确认注册
        System.out.println("\n=== 注册信息确认 ===");
        System.out.println("用户名：" + username);
        System.out.print("角色：");
        if (roles.contains(UserRole.BUYER) && roles.contains(UserRole.SELLER)) {
            System.out.println("买家 + 卖家");
        } else if (roles.contains(UserRole.BUYER)) {
            System.out.println("买家");
        } else {
            System.out.println("卖家");
        }
        System.out.print("\n确认注册？(y/n)：");
        
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            try {
                userService.register(username, password, roles);
                ConsoleUtil.printSuccess("注册成功！请登录");
            } catch (Exception e) {
                ConsoleUtil.printError("注册失败：" + e.getMessage());
            }
        } else {
            ConsoleUtil.printInfo("已取消注册");
        }
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
        
        // 使用FlipTables专业库创建表格
        PerfectTableFormatter.Table table = PerfectTableFormatter.createTable()
                .setHeaders("No.", "Product Name", "Price", "Category", "Condition");
        
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            
            table.addRow(
                String.valueOf(i + 1),
                TranslationUtil.toEnglish(p.getTitle()),
                String.format("%.2f", p.getPrice()),
                TranslationUtil.toEnglish(p.getCategory().getDisplayName()),
                TranslationUtil.toEnglish(p.getCondition().getDescription())
            );
        }
        
        table.print();
        
        ConsoleUtil.printInfo("提示：登录后可购买商品");
    }
    
    /**
     * 处理退出
     */
    private void handleExit() {
        ConsoleUtil.printInfo("感谢使用，再见！");
        System.exit(0);
    }
    
    /**
     * 计算字符串的实际显示宽度（考虑全角半角）
     * 改进版：更准确地判断字符宽度
     */
    private int getDisplayWidth(String str) {
        if (str == null) return 0;
        int width = 0;
        for (char c : str.toCharArray()) {
            // 判断字符是否为全角（占2个显示位置）
            if (isFullWidth(c)) {
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }
    
    /**
     * 判断字符是否为全角字符
     * 基于 Unicode East Asian Width 标准实现
     * 这是最准确的字符宽度判断方法
     */
    private boolean isFullWidth(char c) {
        // 使用 Character.UnicodeBlock 进行精确判断
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        
        if (block == null) return false;
        
        // 所有CJK相关字符块（全角）
        if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
            || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || block == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT
            || block == Character.UnicodeBlock.KANGXI_RADICALS) {
            return true;
        }
        
        // 日韩文字（全角）
        if (block == Character.UnicodeBlock.HIRAGANA
            || block == Character.UnicodeBlock.KATAKANA
            || block == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HANGUL_JAMO
            || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
            return true;
        }
        
        // 全角ASCII和全角标点
        if (block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            // 全角字符范围：0xFF01-0xFF60 和 0xFFE0-0xFFE6
            return (c >= 0xFF01 && c <= 0xFF60) || (c >= 0xFFE0 && c <= 0xFFE6);
        }
        
        // 中文标点符号等
        if (block == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            // 一些特定的全角标点
            return c >= 0x2000 && c <= 0x206F;
        }
        
        // 特殊符号（根据实际终端渲染调整）
        // 这些符号在Windows终端中通常显示为双宽
        if (c == '★' || c == '☆' || c == '●' || c == '○' || 
            c == '■' || c == '□' || c == '▲' || c == '△' ||
            c == '◆' || c == '◇' || c == '※' || c == '√' ||
            c == '✓' || c == '✗' || c == '✕') {
            return true;
        }
        
        return false;
    }
    
    /**
     * 填充字符串到指定显示宽度（精确对齐）
     * @param str 原始字符串
     * @param targetWidth 目标显示宽度
     * @return 填充后的字符串
     */
    private String padToWidth(String str, int targetWidth) {
        if (str == null) str = "";
        
        // 计算当前显示宽度
        int currentWidth = getDisplayWidth(str);
        
        // 如果超长，截断
        if (currentWidth > targetWidth) {
            int width = 0;
            int cutIndex = 0;
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                // 修复：使用 isFullWidth 方法保持一致性
                int charWidth = isFullWidth(c) ? 2 : 1;
                if (width + charWidth > targetWidth) {
                    break;
                }
                width += charWidth;
                cutIndex = i + 1;
            }
            str = str.substring(0, cutIndex);
            currentWidth = width;
        }
        
        // 补齐空格到目标宽度
        StringBuilder sb = new StringBuilder(str);
        for (int i = currentWidth; i < targetWidth; i++) {
            sb.append(" ");
        }
        
        return sb.toString();
    }
}

