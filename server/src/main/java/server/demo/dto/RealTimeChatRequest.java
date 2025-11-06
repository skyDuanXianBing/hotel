package server.demo.dto;

import jakarta.validation.constraints.NotBlank;
import server.demo.entity.RealTimeChatMessage;

/**
 * 实时聊天请求DTO
 */
public class RealTimeChatRequest {
    
    @NotBlank(message = "聊天室ID不能为空")
    private String roomId;
    
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    private RealTimeChatMessage.SenderType senderType = RealTimeChatMessage.SenderType.GUEST;
    
    private String senderName;
    
    private String guestRoomNumber;
    
    private RealTimeChatMessage.MessageType messageType = RealTimeChatMessage.MessageType.TEXT;
    
    // 构造函数
    public RealTimeChatRequest() {}
    
    public RealTimeChatRequest(String roomId, String message, RealTimeChatMessage.SenderType senderType, String senderName) {
        this.roomId = roomId;
        this.message = message;
        this.senderType = senderType;
        this.senderName = senderName;
    }
    
    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public RealTimeChatMessage.SenderType getSenderType() {
        return senderType;
    }
    
    public void setSenderType(RealTimeChatMessage.SenderType senderType) {
        this.senderType = senderType;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getGuestRoomNumber() {
        return guestRoomNumber;
    }
    
    public void setGuestRoomNumber(String guestRoomNumber) {
        this.guestRoomNumber = guestRoomNumber;
    }
    
    public RealTimeChatMessage.MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(RealTimeChatMessage.MessageType messageType) {
        this.messageType = messageType;
    }
    
    @Override
    public String toString() {
        return "RealTimeChatRequest{" +
                "roomId='" + roomId + '\'' +
                ", message='" + message + '\'' +
                ", senderType=" + senderType +
                ", senderName='" + senderName + '\'' +
                ", guestRoomNumber='" + guestRoomNumber + '\'' +
                ", messageType=" + messageType +
                '}';
    }
}