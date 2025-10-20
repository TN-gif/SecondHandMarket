import entity.User;
import enums.UserRole;
import enums.UserStatus;
import exception.AuthenticationException;
import handler.AdminMenuHandler;
import handler.BannedUserMenuHandler;
import handler.MainMenuHandler;
import handler.UserMenuHandler;
import util.ConsoleUtil;
import java.util.NoSuchElementException;

/**
 * 主程序入口
 * 
 * 职责：
 * 1. 初始化应用上下文（AppContext）
 * 2. 主循环：根据登录状态选择不同的Handler
 * 3. 程序退出时保存数据
 * 
 * 设计说明：
 * Main类不再承担任何业务逻辑和视图渲染，只负责程序的启动、
 * 循环和关闭。所有的视图逻辑都委托给Handler类处理。
 * 
 * 这个Main类只有不到50行核心代码，职责单一，体现了良好的设计！
 */
public class Main {
    
    // 应用上下文（依赖注入容器）
    private static AppContext context;
    
    // Handler实例
    private static MainMenuHandler mainMenuHandler;
    private static UserMenuHandler userMenuHandler;
    private static AdminMenuHandler adminMenuHandler;
    private static BannedUserMenuHandler bannedUserMenuHandler;
    
    public static void main(String[] args) {
        // 1. 初始化应用
        initialize();
        
        // 2. 显示欢迎信息
        ConsoleUtil.printTitle("校园二手市场管理系统 v1.0");
        System.out.println("欢迎使用！系统已启动。");
        System.out.println();
        
        // 3. 主循环
        mainLoop();
    }
    
    /**
     * 初始化应用
     */
    private static void initialize() {
        // 设置控制台编码为UTF-8（支持中文显示）
        try {
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("sun.jnu.encoding", "UTF-8");
            // 对于Windows系统，使用chcp命令设置控制台代码页
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                try {
                    Runtime.getRuntime().exec("cmd /c chcp 65001");
                } catch (Exception e) {
                    // 忽略错误，继续执行
                }
            }
        } catch (Exception e) {
            // 如果设置失败，继续执行（使用默认编码）
        }
        
        // 创建应用上下文（所有Service在这里被创建和注入）
        context = new AppContext();
        
        // 加载数据
        context.loadData();
        
        // 首次运行时创建示例数据
        InitialDataSetup.setupInitialData();
        
        // 创建Handler（从context获取Service实例）
        mainMenuHandler = new MainMenuHandler(context.userService, context.productService);
        userMenuHandler = new UserMenuHandler(context.userService, context.productService,
                                             context.orderService, context.reviewService);
        adminMenuHandler = new AdminMenuHandler(context.userService, context.adminService);
        bannedUserMenuHandler = new BannedUserMenuHandler(context.userService);
        
        // 注册关闭钩子（程序退出时自动保存数据）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConsoleUtil.printInfo("正在保存数据...");
            context.saveData();
        }));
    }
    
    /**
     * 主循环
     */
    private static void mainLoop() {
        while (true) {
            try {
                // 根据登录状态选择Handler
                if (!context.userService.isLoggedIn()) {
                    // 未登录：显示主菜单
                    mainMenuHandler.displayAndHandle();
                } else {
                    User currentUser = context.userService.getCurrentUser();
                    
                    // 检查用户状态
                    if (currentUser.getStatus() == UserStatus.BANNED) {
                        // 被封禁用户：只能访问受限菜单
                        bannedUserMenuHandler.displayAndHandle();
                    } else if (currentUser.hasRole(UserRole.ADMIN)) {
                        // 管理员：显示管理员菜单
                        adminMenuHandler.displayAndHandle();
                    } else {
                        // 普通用户：显示用户菜单
                        userMenuHandler.displayAndHandle();
                    }
                }
            } catch (AuthenticationException e) {
                // 未登录异常，继续显示主菜单（静默处理）
            } catch (NoSuchElementException e) {
                // Scanner输入流关闭或无可用输入，退出程序
                ConsoleUtil.printError("输入流错误，系统将退出");
                context.saveData();
                System.exit(0);
            } catch (Exception e) {
                // 其他异常，显示错误信息
                ConsoleUtil.printError("操作失败：" + e.getMessage());
            }
        }
    }
}
