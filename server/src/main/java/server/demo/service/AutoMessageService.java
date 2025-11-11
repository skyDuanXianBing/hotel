package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.AutoMessage;
import server.demo.repository.AutoMessageRepository;

import java.util.List;
import java.util.Optional;

/**
 * 自动化消息 Service
 */
@Service
public class AutoMessageService {

    @Autowired
    private AutoMessageRepository autoMessageRepository;

    /**
     * 获取所有自动化消息
     */
    public List<AutoMessage> getAllAutoMessages() {
        return autoMessageRepository.findAll();
    }

    /**
     * 根据用户ID获取自动化消息列表
     */
    public List<AutoMessage> getAutoMessagesByUserId(Long userId) {
        return autoMessageRepository.findByUserId(userId);
    }

    /**
     * 根据用户ID和启用状态获取自动化消息列表
     */
    public List<AutoMessage> getEnabledAutoMessages(Long userId) {
        return autoMessageRepository.findByUserIdAndEnabled(userId, true);
    }

    /**
     * 根据ID获取自动化消息
     */
    public Optional<AutoMessage> getAutoMessageById(Long id) {
        return autoMessageRepository.findById(id);
    }

    /**
     * 创建自动化消息
     */
    @Transactional
    public AutoMessage createAutoMessage(AutoMessage autoMessage) {
        return autoMessageRepository.save(autoMessage);
    }

    /**
     * 更新自动化消息
     */
    @Transactional
    public AutoMessage updateAutoMessage(Long id, AutoMessage autoMessage) {
        Optional<AutoMessage> existingMessage = autoMessageRepository.findById(id);
        if (existingMessage.isEmpty()) {
            throw new RuntimeException("自动化消息不存在");
        }

        AutoMessage message = existingMessage.get();
        message.setTitle(autoMessage.getTitle());
        message.setMessage(autoMessage.getMessage());
        message.setAutomationRule(autoMessage.getAutomationRule());
        message.setChannel(autoMessage.getChannel());
        message.setRoom(autoMessage.getRoom());
        message.setEnabled(autoMessage.getEnabled());

        return autoMessageRepository.save(message);
    }

    /**
     * 删除自动化消息
     */
    @Transactional
    public void deleteAutoMessage(Long id) {
        autoMessageRepository.deleteById(id);
    }

    /**
     * 切换自动化消息启用状态
     */
    @Transactional
    public AutoMessage toggleAutoMessage(Long id) {
        Optional<AutoMessage> existingMessage = autoMessageRepository.findById(id);
        if (existingMessage.isEmpty()) {
            throw new RuntimeException("自动化消息不存在");
        }

        AutoMessage message = existingMessage.get();
        message.setEnabled(!message.getEnabled());
        return autoMessageRepository.save(message);
    }
}
