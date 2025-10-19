package handler;

/**
 * Handler接口
 * 所有视图处理器的统一接口
 * 
 * 设计说明：
 * Handler层负责处理用户交互，将业务逻辑委托给Service层
 */
public interface MenuHandler {
    /**
     * 显示菜单并处理用户输入
     */
    void displayAndHandle();
}


