package server.demo.service.impl;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.dto.ChatMessageRequest;
import server.demo.dto.ChatMessageResponse;
import server.demo.service.ChatService;

import java.util.UUID;

/**
 * 聊天服务实现类
 * 使用LangChain4j和DashScope实现AI聊天功能
 * 
 * @author AI Assistant
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    /**
     * LangChain4j聊天模型
     */
    @Autowired
    private ChatLanguageModel chatLanguageModel;

    /**
     * 系统提示词，定义AI助手的角色和行为
     */
    private static final String SYSTEM_PROMPT = """
        你是房东智控中心（THE HOST HUB）的智能客服助手。请遵循以下规则：
        
        1. 角色定位：
        - 你是一个专业、友好、耐心的酒店客服助手
        - 主要为酒店客人提供住宿相关的服务和帮助
        - 你代表房东智控中心（THE HOST HUB）为客人解决各种问题和需求
        
        2. 服务范围：
        - 房间预订咨询（房型介绍、价格查询、入住退房时间等）
        - 酒店设施介绍（餐厅、健身房、停车场、WiFi等）
        - 客房服务（清洁、维修、用品补充等）
        - 周边信息（交通、景点、购物、餐饮推荐）
        - 入住期间的各种问题和投诉处理
        - 账单查询和结算相关问题
        - 特殊需求安排（加床、无烟房、接机等）
        
        3. 回复风格：
        - 保持热情、专业和礼貌的服务态度
        - 用温馨、亲切的语言与客人交流
        - 回复简洁明了，重点突出
        - 主动关心客人的住宿体验
        - 及时回应客人的问题和需求
        
        4. 特殊处理：
        - 对于紧急问题（如安全、医疗），立即建议联系前台或拨打紧急电话
        - 对于投诉，表示歉意并承诺尽快解决
        - 对于无法直接解决的问题，承诺转达给相关部门并跟进
        - 始终保持以客人为中心的服务理念
        
        请用中文回复，语言温馨自然，体现房东智控中心（THE HOST HUB）的优质服务品质。
        """;

    @Override
    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : generateSessionId();
        
        try {
            logger.info("处理聊天消息: sessionId={}, message={}", sessionId, request.getMessage());
            
            // 构建完整的提示词
            String fullPrompt = buildFullPrompt(request.getMessage());
            
            // 调用AI模型生成回复
            String aiReply = chatLanguageModel.generate(fullPrompt);
            
            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("AI回复生成成功: sessionId={}, processingTime={}ms", sessionId, processingTime);
            
            // 创建响应
            ChatMessageResponse response = ChatMessageResponse.success(aiReply, sessionId);
            response.setProcessingTime(processingTime);
            
            return response;
            
        } catch (Exception e) {
            logger.error("处理聊天消息失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            
            long processingTime = System.currentTimeMillis() - startTime;
            ChatMessageResponse errorResponse = ChatMessageResponse.error(
                "抱歉，我现在遇到了一些技术问题，请稍后再试或联系技术支持。", 
                sessionId
            );
            errorResponse.setProcessingTime(processingTime);
            
            return errorResponse;
        }
    }

    @Override
    public ChatMessageResponse generateWelcomeMessage(String sessionId) {
        if (sessionId == null) {
            sessionId = generateSessionId();
        }
        
        String welcomeMessage = "您好！欢迎入住乐迪酒店！我是您的专属智能客服助手。" +
                "我可以为您提供房间服务、酒店设施介绍、周边信息查询等各种帮助。" +
                "如果您在住宿期间有任何需要，请随时告诉我，我会竭诚为您服务！";
        
        return ChatMessageResponse.success(welcomeMessage, sessionId);
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            // 发送一个简单的测试消息来检查服务状态
            String testResponse = chatLanguageModel.generate("你好");
            return testResponse != null && !testResponse.trim().isEmpty();
        } catch (Exception e) {
            logger.warn("AI聊天服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getServiceInfo() {
        return "LangChain4j + DashScope AI聊天服务 - 乐迪酒店智能客服";
    }

    /**
     * 构建完整的提示词
     */
    private String buildFullPrompt(String userMessage) {
        return SYSTEM_PROMPT + "\n\n用户问题: " + userMessage + "\n\n请根据上述规则回复:";
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "chat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 设置聊天模型（用于测试）
     */
    public void setChatLanguageModel(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }
}