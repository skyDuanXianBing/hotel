package server.demo.service;

import server.demo.dto.ChatMessageRequest;
import server.demo.dto.ChatMessageResponse;

/**
 * 聊天服务接口
 * 定义AI聊天的核心业务逻辑
 * 
 * @author AI Assistant
 */
public interface ChatService {

    /**
     * 处理用户消息并生成AI回复
     * 
     * @param request 聊天消息请求
     * @return 聊天消息响应
     */
    ChatMessageResponse processMessage(ChatMessageRequest request);

    /**
     * 生成欢迎消息
     * 
     * @param sessionId 会话ID
     * @return 欢迎消息响应
     */
    ChatMessageResponse generateWelcomeMessage(String sessionId);

    /**
     * 检查服务健康状态
     * 
     * @return 是否可用
     */
    boolean isServiceAvailable();

    /**
     * 获取服务信息
     * 
     * @return 服务信息
     */
    String getServiceInfo();
}