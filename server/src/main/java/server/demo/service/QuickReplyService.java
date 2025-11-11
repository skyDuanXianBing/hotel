package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.QuickReply;
import server.demo.repository.QuickReplyRepository;

import java.util.List;
import java.util.Optional;

@Service
public class QuickReplyService {

    @Autowired
    private QuickReplyRepository quickReplyRepository;

    /**
     * 获取所有快捷回复
     */
    public List<QuickReply> getAllQuickReplies() {
        return quickReplyRepository.findAll();
    }

    /**
     * 根据用户ID获取快捷回复列表
     */
    public List<QuickReply> getQuickRepliesByUserId(Long userId) {
        return quickReplyRepository.findByUserId(userId);
    }

    /**
     * 根据ID获取快捷回复
     */
    public Optional<QuickReply> getQuickReplyById(Long id) {
        return quickReplyRepository.findById(id);
    }

    /**
     * 创建快捷回复
     */
    @Transactional
    public QuickReply createQuickReply(QuickReply quickReply) {
        return quickReplyRepository.save(quickReply);
    }

    /**
     * 更新快捷回复
     */
    @Transactional
    public QuickReply updateQuickReply(Long id, QuickReply quickReplyDetails) {
        QuickReply quickReply = quickReplyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("快捷回复不存在"));

        quickReply.setTitle(quickReplyDetails.getTitle());
        quickReply.setMessage(quickReplyDetails.getMessage());

        return quickReplyRepository.save(quickReply);
    }

    /**
     * 删除快捷回复
     */
    @Transactional
    public void deleteQuickReply(Long id) {
        quickReplyRepository.deleteById(id);
    }
}
