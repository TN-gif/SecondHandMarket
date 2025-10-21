package util;

import exception.BusinessException;

import java.util.Scanner;

/**
 * 输入验证辅助工具类
 * 
 * 提供带重试机制的输入验证功能，避免在Handler中重复编写验证逻辑。
 */
public class InputHelper {
    
    private static final Scanner scanner = new Scanner(System.in);
    
    /**
     * 读取并验证评分（1-5星）
     * 
     * 支持重试机制，输入0可取消操作。
     * 
     * @param prompt 提示信息
     * @return 验证通过的评分，取消则返回null
     */
    public static Integer readRating(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            
            if (input.equals("0")) {
                return null;
            }
            
            try {
                int rating = Integer.parseInt(input);
                if (rating >= 1 && rating <= 5) {
                    return rating;
                }
                ConsoleUtil.printError("评分必须在1-5星之间，请重新输入");
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("评分格式无效，请输入1-5之间的数字");
            }
            
            System.out.print("继续评价？(y/n)：");
            if (!scanner.nextLine().equalsIgnoreCase("y")) {
                ConsoleUtil.printInfo("已取消评价");
                return null;
            }
        }
    }
    
    /**
     * 读取并验证文本内容
     * 
     * 支持重试机制，输入0可取消操作。
     * 
     * @param prompt 提示信息
     * @param minLength 最小长度
     * @param maxLength 最大长度
     * @param fieldName 字段名称（用于错误提示）
     * @return 验证通过的文本，取消则返回null
     */
    public static String readText(String prompt, int minLength, int maxLength, String fieldName) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            
            if (input.equals("0")) {
                return null;
            }
            
            if (input.trim().isEmpty()) {
                ConsoleUtil.printError(fieldName + "不能为空");
            } else if (input.trim().length() < minLength) {
                ConsoleUtil.printError(fieldName + "至少需要" + minLength + "个字");
            } else if (input.trim().length() > maxLength) {
                ConsoleUtil.printError(fieldName + "最多" + maxLength + "个字");
            } else {
                return input;
            }
            
            System.out.print("是否继续？(y/n)：");
            if (!scanner.nextLine().equalsIgnoreCase("y")) {
                ConsoleUtil.printInfo("已取消操作");
                return null;
            }
        }
    }
    
    /**
     * 读取并验证价格
     * 
     * 支持重试机制，按回车跳过此输入。
     * 
     * @param prompt 提示信息
     * @return 验证通过的价格，跳过或取消则返回null
     */
    public static Double readPrice(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            
            if (input.trim().isEmpty()) {
                return null; // 跳过
            }
            
            if (!InputValidator.isValidDouble(input)) {
                ConsoleUtil.printError("价格格式无效，请输入有效数字");
                System.out.print("是否重试？(y/n)：");
                if (!scanner.nextLine().equalsIgnoreCase("y")) {
                    ConsoleUtil.printInfo("已取消价格筛选");
                    return null;
                }
                continue;
            }
            
            Double price = InputValidator.parseDoubleSafe(input);
            if (price == null || price <= 0) {
                ConsoleUtil.printError("价格必须大于0");
                System.out.print("是否重试？(y/n)：");
                if (!scanner.nextLine().equalsIgnoreCase("y")) {
                    ConsoleUtil.printInfo("已取消价格筛选");
                    return null;
                }
                continue;
            }
            
            return price;
        }
    }
    
    /**
     * 验证用户状态
     * 
     * 检查用户是否处于正常状态，被封禁或删除的用户无法执行操作。
     * 
     * @param user 要验证的用户
     * @param operationName 操作名称（用于错误提示）
     * @throws exception.PermissionDeniedException 如果用户状态异常
     */
    public static void validateUserStatus(entity.User user, String operationName) {
        if (user.getStatus() == enums.UserStatus.BANNED) {
            throw new exception.PermissionDeniedException("您的账号已被封禁，无法" + operationName);
        }
        if (user.getStatus() == enums.UserStatus.DELETED) {
            throw new exception.PermissionDeniedException("账号已被删除");
        }
    }
    
    /**
     * 验证用户角色
     * 
     * 检查用户是否拥有指定角色，没有则抛出权限异常。
     * 
     * @param user 要验证的用户
     * @param role 需要的角色
     * @param operationName 操作名称（用于错误提示）
     * @throws exception.PermissionDeniedException 如果用户没有指定角色
     */
    public static void validateUserRole(entity.User user, enums.UserRole role, String operationName) {
        if (!user.hasRole(role)) {
            throw new exception.PermissionDeniedException("只有" + role.getDisplayName() + "才能" + operationName);
        }
    }
}



