package handler;

import entity.Order;
import entity.Product;
import entity.User;
import enums.OrderStatus;
import service.*;
import util.UIHelper;

import java.util.Scanner;

/**
 * 菜单处理器基类
 * 
 * 提供所有菜单处理器共用的功能，避免代码重复。
 * 子类可以直接使用这些通用方法，专注于各自的业务逻辑。
 */
public abstract class BaseMenuHandler implements MenuHandler {
    
    protected final UserService userService;
    protected final Scanner scanner;
    
    /**
     * 构造基础菜单处理器
     * 
     * @param userService 用户服务
     */
    protected BaseMenuHandler(UserService userService) {
        this.userService = userService;
        this.scanner = new Scanner(System.in);
    }
    
    /**
     * 安全读取整数输入
     * 
     * 委托给UIHelper统一处理。
     * 
     * @param prompt 提示信息
     * @param errorMsg 错误提示信息
     * @return 解析后的整数，失败返回null
     */
    protected Integer readIntSafely(String prompt, String errorMsg) {
        return UIHelper.readIntSafely(prompt, errorMsg);
    }
    
    /**
     * 读取字符串输入
     * 
     * @param prompt 提示信息
     * @return 用户输入的字符串
     */
    protected String readLine(String prompt) {
        return UIHelper.readLine(prompt);
    }
    
    /**
     * 读取确认输入
     * 
     * @param prompt 提示信息
     * @return 如果输入y或Y返回true
     */
    protected boolean readConfirmation(String prompt) {
        return UIHelper.readConfirmation(prompt);
    }
    
    /**
     * 获取订单状态的买家视角描述
     * 
     * @param status 订单状态
     * @return 买家视角的状态描述
     */
    protected String getOrderStatusForBuyer(OrderStatus status) {
        return UIHelper.getOrderStatusForBuyer(status);
    }
    
    /**
     * 获取订单状态的卖家视角描述
     * 
     * @param status 订单状态
     * @return 卖家视角的状态描述
     */
    protected String getOrderStatusForSeller(OrderStatus status) {
        return UIHelper.getOrderStatusForSeller(status);
    }
}



