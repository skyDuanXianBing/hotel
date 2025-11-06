package server.demo.service.impl;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Mock聊天语言模型实现类
 * 用于在DashScope依赖问题解决前提供演示功能
 * 
 * @author AI Assistant
 */
public class MockChatLanguageModel implements ChatLanguageModel {

    private static final Logger logger = LoggerFactory.getLogger(MockChatLanguageModel.class);

    /**
     * 预定义的智能回复模板
     */
    private static final Map<String, String> REPLY_TEMPLATES = new HashMap<>();

    static {
        // 酒店管理相关问题
        REPLY_TEMPLATES.put("房间", "关于房间管理，您可以在房态页面查看和管理所有房间的状态。可以进行入住、退房、清洁等操作。");
        REPLY_TEMPLATES.put("预订", "预订管理功能支持创建新预订、修改现有预订、取消预订等操作。您可以在订单管理页面查看所有预订信息。");
        REPLY_TEMPLATES.put("房型", "房型管理允许您创建和配置不同类型的房间，设置房间数量、价格等信息。请在设置-房型管理中操作。");
        REPLY_TEMPLATES.put("价格", "房价管理支持单独设置和批量修改房价，还可以区分平日和周末价格。请前往设置-房价管理进行配置。");
        REPLY_TEMPLATES.put("渠道", "渠道管理用于配置不同的预订来源，如官网、OTA平台等。您可以在设置-渠道设置中管理。");
        REPLY_TEMPLATES.put("统计", "系统提供详细的经营统计数据，包括入住率、收入分析等。请查看业务概览页面。");
        REPLY_TEMPLATES.put("设置", "系统设置包括房型管理、房价管理、渠道设置等功能模块，都在设置页面中。");
        
        // 系统操作相关
        REPLY_TEMPLATES.put("如何", "我可以为您提供详细的操作指导。请告诉我您想了解哪个具体功能的使用方法。");
        REPLY_TEMPLATES.put("帮助", "我是乐迪酒店管理系统的智能客服，可以帮您解答房间管理、预订处理、价格设置等相关问题。");
        REPLY_TEMPLATES.put("问题", "请详细描述您遇到的问题，我会尽力为您提供解决方案。");
        REPLY_TEMPLATES.put("错误", "如果遇到系统错误，请先尝试刷新页面。如果问题持续存在，请联系技术支持。");
        
        // 通用问候
        REPLY_TEMPLATES.put("你好", "您好！我是乐迪酒店管理系统的智能客服助手，很高兴为您服务！");
        REPLY_TEMPLATES.put("您好", "您好！我是乐迪酒店管理系统的智能客服助手，很高兴为您服务！");
        REPLY_TEMPLATES.put("hello", "您好！我是乐迪酒店管理系统的智能客服助手，很高兴为您服务！");
        REPLY_TEMPLATES.put("hi", "您好！欢迎使用乐迪酒店管理系统，有什么可以帮助您的吗？");
    }

    /**
     * 默认回复消息
     */
    private static final List<String> DEFAULT_REPLIES = Arrays.asList(
        "感谢您的咨询！我是乐迪酒店管理系统的智能客服。我可以帮您了解房间管理、预订处理、价格设置、渠道管理等功能。请告诉我您需要什么帮助？",
        "我理解您的问题。作为酒店管理系统的客服，我主要协助您解决系统使用相关问题。请描述一下您遇到的具体情况？",
        "很高兴为您服务！我可以为您介绍系统的各项功能，包括房态管理、订单处理、统计报表等。请问您想了解哪方面的内容？",
        "我会尽力帮助您解决酒店管理系统相关的问题。您可以询问我关于房型设置、价格管理、渠道配置等任何功能的使用方法。",
        "作为您的智能助手，我专门负责酒店管理系统的咨询服务。无论是日常操作还是功能介绍，我都很乐意为您解答。请问有什么可以帮您的？"
    );

    @Override
    public String generate(String userMessage) {
        logger.info("MockChatLanguageModel 收到消息: {}", userMessage);
        
        try {
            // 模拟AI处理时间
            Thread.sleep(500 + new Random().nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        String reply = generateReply(userMessage);
        logger.info("MockChatLanguageModel 生成回复: {}", reply);
        
        return reply;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        // 从消息列表中提取用户消息
        String userMessage = "";
        if (messages != null && !messages.isEmpty()) {
            ChatMessage lastMessage = messages.get(messages.size() - 1);
            userMessage = lastMessage.text();
        }
        
        String reply = generateReply(userMessage);
        AiMessage aiMessage = AiMessage.from(reply);
        return Response.from(aiMessage);
    }

    /**
     * 根据用户消息生成智能回复
     */
    private String generateReply(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "请输入您的问题，我会尽力为您解答。";
        }
        
        String message = userMessage.trim();
        String lowerMessage = message.toLowerCase();
        
        // 数学计算相关
        if (lowerMessage.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*") || 
            lowerMessage.contains("计算") || lowerMessage.contains("等于")) {
            return "我是酒店管理系统的客服助手，主要帮助您解决酒店管理相关问题。如需数学计算，建议使用计算器。有什么酒店管理方面的问题可以咨询我。";
        }
        
        // 优先检查完整词组匹配
        if (containsWord(lowerMessage, "房间管理") || containsWord(lowerMessage, "房间状态")) {
            return REPLY_TEMPLATES.get("房间");
        }
        
        if (containsWord(lowerMessage, "预订管理") || containsWord(lowerMessage, "订单管理")) {
            return REPLY_TEMPLATES.get("预订");
        }
        
        if (containsWord(lowerMessage, "房型管理") || containsWord(lowerMessage, "房间类型")) {
            return REPLY_TEMPLATES.get("房型");
        }
        
        if (containsWord(lowerMessage, "价格管理") || containsWord(lowerMessage, "房价设置")) {
            return REPLY_TEMPLATES.get("价格");
        }
        
        if (containsWord(lowerMessage, "渠道管理") || containsWord(lowerMessage, "渠道设置")) {
            return REPLY_TEMPLATES.get("渠道");
        }
        
        if (containsWord(lowerMessage, "数据统计") || containsWord(lowerMessage, "经营报表")) {
            return REPLY_TEMPLATES.get("统计");
        }
        
        if (containsWord(lowerMessage, "系统设置") || containsWord(lowerMessage, "设置页面")) {
            return REPLY_TEMPLATES.get("设置");
        }
        
        // 检查单个关键词（更严格的匹配）
        for (Map.Entry<String, String> entry : REPLY_TEMPLATES.entrySet()) {
            if (containsWord(lowerMessage, entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // 特殊关键词组合处理
        if (lowerMessage.contains("批量") && lowerMessage.contains("价格")) {
            return "批量修改房价功能在设置-房价管理页面。您可以选择多个房型，设置统一价格或按比例调整。";
        }
        
        if (lowerMessage.contains("房态") && lowerMessage.contains("管理")) {
            return "房态管理页面显示所有房间的实时状态。您可以查看房间占用情况，进行入住、退房等操作。";
        }
        
        if (lowerMessage.contains("统计") && lowerMessage.contains("数据")) {
            return "业务概览页面提供详细的统计分析，包括入住率、收入趋势、渠道分析等数据报表。";
        }
        
        // 返回随机默认回复
        Random random = new Random();
        return DEFAULT_REPLIES.get(random.nextInt(DEFAULT_REPLIES.size()));
    }
    
    /**
     * 检查消息中是否包含完整的词语（避免部分匹配）
     */
    private boolean containsWord(String message, String word) {
        // 对于中文，直接检查包含关系，但要求前后不是字母数字
        int index = message.indexOf(word);
        if (index == -1) {
            return false;
        }
        
        // 检查前一个字符
        if (index > 0) {
            char prevChar = message.charAt(index - 1);
            if (Character.isLetterOrDigit(prevChar)) {
                return false;
            }
        }
        
        // 检查后一个字符
        int endIndex = index + word.length();
        if (endIndex < message.length()) {
            char nextChar = message.charAt(endIndex);
            if (Character.isLetterOrDigit(nextChar)) {
                return false;
            }
        }
        
        return true;
    }
}