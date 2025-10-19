package observer;

/**
 * 消息观察者接口
 * 
 * 设计模式：观察者模式
 */
public interface MessageObserver {
    
    /**
     * 接收消息
     */
    void onMessage(String message);
}


