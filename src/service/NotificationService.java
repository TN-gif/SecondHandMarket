package service;

import entity.Message;
import observer.MessageObserver;
import repository.DataCenter;
import util.IdGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知服务 - 观察者模式核心
 * 
 * 设计模式：观察者模式（Observer Pattern）
 * 
 * 设计说明：
 * 1. NotificationService是Subject（主题）
 * 2. UserMessageReceiver是Observer（观察者）
 * 3. 当业务事件发生时，通知所有订阅的观察者
 * 
 * 完整实现将在Day 5完成
 */
public class NotificationService {
    
    private final DataCenter dataCenter;
    
    /**
     * 观察者列表（key: userId, value: List of observers）
     */
    private final Map<String, List<MessageObserver>> observers = new ConcurrentHashMap<>();
    
    public NotificationService() {
        this.dataCenter = DataCenter.getInstance();
    }
    
    /**
     * 订阅通知
     */
    public void subscribe(String userId, MessageObserver observer) {
        observers.computeIfAbsent(userId, k -> new ArrayList<>()).add(observer);
    }
    
    /**
     * 取消订阅
     */
    public void unsubscribe(String userId, MessageObserver observer) {
        List<MessageObserver> userObservers = observers.get(userId);
        if (userObservers != null) {
            userObservers.remove(observer);
            if (userObservers.isEmpty()) {
                observers.remove(userId);
            }
        }
    }
    
    /**
     * 发送通知给指定用户
     */
    public void notify(String userId, String messageContent) {
        // 1. 持久化消息到数据库
        String messageId = IdGenerator.generateMessageId();
        Message message = new Message(messageId, userId, messageContent);
        dataCenter.addMessage(message);
        
        // 2. 如果用户在线，通过观察者模式实时通知
        List<MessageObserver> userObservers = observers.get(userId);
        if (userObservers != null) {
            for (MessageObserver observer : userObservers) {
                observer.onMessage(messageContent);
            }
        }
    }
    
    /**
     * 广播通知给所有用户
     */
    public void notifyAll(String messageContent) {
        // 1. 持久化消息到所有用户
        for (String userId : observers.keySet()) {
            String messageId = IdGenerator.generateMessageId();
            Message message = new Message(messageId, userId, messageContent);
            dataCenter.addMessage(message);
        }
        
        // 2. 实时通知在线用户
        observers.values().forEach(observerList -> 
            observerList.forEach(observer -> observer.onMessage(messageContent))
        );
    }
    
    /**
     * 获取用户的历史消息
     */
    public List<Message> getMessageHistory(String userId) {
        return dataCenter.findMessagesByUserId(userId);
    }
}


