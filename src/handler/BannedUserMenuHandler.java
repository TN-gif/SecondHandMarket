package handler;

import entity.Appeal;
import entity.User;
import enums.UserStatus;
import repository.DataCenter;
import service.UserService;
import util.ConsoleUtil;
import util.IdGenerator;
import util.PerfectTableFormatter;
import util.TranslationUtil;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * 被封禁用户菜单处理器
 * 职责：处理被封禁用户的受限交互
 * 
 * 被封禁用户只能：
 * 1. 查看自己的申诉记录
 * 2. 提交新的申诉（如果之前的申诉已处理）
 * 3. 查看消息通知
 * 4. 登出
 */
public class BannedUserMenuHandler implements MenuHandler {
    private final UserService userService;
    private final Scanner scanner;
    
    public BannedUserMenuHandler(UserService userService) {
        this.userService = userService;
        this.scanner = new Scanner(System.in);
    }
    
    @Override
    public void displayAndHandle() {
        User user = userService.getCurrentUser();
        
        // 显示封禁警告
        ConsoleUtil.printDivider();
        System.out.println("  [!] 账号已封禁 [!]");
        System.out.println("您的账号已被封禁。访问受限。");
        System.out.println("用户名：" + user.getUsername());
        System.out.println("信誉：" + user.getReputation());
        ConsoleUtil.printDivider();
        
        System.out.println("【被封禁用户菜单】");
        System.out.println("[1] 查看我的申诉");
        System.out.println("[2] 提交新申诉");
        System.out.println("[3] 查看消息");
        System.out.println("[0] 退出登录");
        ConsoleUtil.printDivider();
        
        System.out.print("请选择：");
        String choice = scanner.nextLine();
        
        try {
            switch (choice) {
                case "1" -> handleViewMyAppeals();
                case "2" -> handleSubmitAppeal();
                case "3" -> handleViewMessages();
                case "0" -> handleLogout();
                default -> ConsoleUtil.printError("无效选项");
            }
        } catch (Exception e) {
            ConsoleUtil.printError(e.getMessage());
        }
    }
    
    /**
     * 查看我的申诉
     */
    private void handleViewMyAppeals() {
        User user = userService.getCurrentUser();
        List<Appeal> appeals = DataCenter.getInstance().findAppealsByUserId(user.getUserId());
        
        if (appeals.isEmpty()) {
            ConsoleUtil.printInfo("您还没有申诉记录");
            ConsoleUtil.printInfo("您可以通过选项 [2] 提交申诉");
            return;
        }
        
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
        
        // 检查是否可以提交新申诉
        boolean hasUnprocessed = appeals.stream().anyMatch(a -> !a.isProcessed());
        if (hasUnprocessed) {
            ConsoleUtil.printInfo("您有一个申诉正在处理中。请等待管理员审核。");
        } else {
            ConsoleUtil.printInfo("所有申诉已处理完毕。您可以通过选项 [2] 提交新申诉");
        }
    }
    
    /**
     * 提交新申诉
     */
    private void handleSubmitAppeal() {
        User user = userService.getCurrentUser();
        
        // 检查是否有未处理的申诉
        List<Appeal> unprocessedAppeals = DataCenter.getInstance()
                .findAppealsByUserId(user.getUserId()).stream()
                .filter(a -> !a.isProcessed())
                .toList();
        
        if (!unprocessedAppeals.isEmpty()) {
            ConsoleUtil.printError("您已有待处理的申诉。请等待管理员审核。");
            ConsoleUtil.printInfo("您可以通过选项 [1] 查看您的申诉");
            return;
        }
        
        ConsoleUtil.printTitle("提交新申诉");
        ConsoleUtil.printInfo("提示：输入'0'取消");
        
        System.out.print("\n请输入申诉理由（100字以内）：");
        String reason = scanner.nextLine();
        
        if (reason.equals("0")) {
            ConsoleUtil.printInfo("已取消申诉提交");
            return;
        }
        
        if (reason.trim().isEmpty()) {
            ConsoleUtil.printError("申诉理由不能为空");
            return;
        }
        
        if (reason.length() > 100) {
            ConsoleUtil.printError("申诉理由过长（最多100字）");
            return;
        }
        
        String appealId = IdGenerator.generateAppealId();
        Appeal appeal = new Appeal(appealId, user.getUserId(), reason);
        DataCenter.getInstance().addAppeal(appeal);
        
        ConsoleUtil.printSuccess("申诉提交成功！申诉ID：" + appealId);
        ConsoleUtil.printInfo("请等待管理员审核。您可以通过选项 [1] 查看状态");
    }
    
    /**
     * 查看消息
     */
    private void handleViewMessages() {
        User user = userService.getCurrentUser();
        
        // 显示运行时消息
        var receiver = userService.getCurrentReceiver();
        List<String> messages = receiver.getMessages();
        
        if (messages.isEmpty()) {
            ConsoleUtil.printInfo("暂无新消息");
        } else {
            ConsoleUtil.printTitle("我的消息");
            System.out.println("共 " + messages.size() + " 条消息\n");
            
            for (String msg : messages) {
                System.out.println("• " + msg);
            }
        }
    }
    
    /**
     * 登出
     */
    private void handleLogout() {
        userService.logout();
        ConsoleUtil.printSuccess("退出登录成功");
        ConsoleUtil.printInfo("您可以从主菜单查看申诉状态而无需登录");
    }
}

