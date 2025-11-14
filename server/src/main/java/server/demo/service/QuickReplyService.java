package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.QuickReply;
import server.demo.repository.QuickReplyRepository;

import java.util.List;
import java.util.Optional;

@Service
public class QuickReplyService {

    @Autowired
    private QuickReplyRepository quickReplyRepository;

    /**
     * 获取当前门店ID
     */
    private Long getCurrentStoreId() {
        StoreContext context = StoreContextHolder.getContext();
        if (context == null || context.getStoreId() == null) {
            throw new RuntimeException("无法获取当前门店信息");
        }
        return context.getStoreId();
    }

    /**
     * 获取所有快捷回复(门店级)
     */
    public List<QuickReply> getAllQuickReplies() {
        Long storeId = getCurrentStoreId();
        return quickReplyRepository.findByStoreId(storeId);
    }

    /**
     * 根据用户ID获取快捷回复列表(已废弃,使用getAllQuickReplies)
     */
    @Deprecated
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
     * 创建快捷回复(门店级)
     */
    @Transactional
    public QuickReply createQuickReply(QuickReply quickReply) {
        Long storeId = getCurrentStoreId();
        quickReply.setStoreId(storeId);
        return quickReplyRepository.save(quickReply);
    }

    /**
     * 更新快捷回复(门店级)
     */
    @Transactional
    public QuickReply updateQuickReply(Long id, QuickReply quickReplyDetails) {
        Long storeId = getCurrentStoreId();

        Optional<QuickReply> existingReply = quickReplyRepository.findById(id);
        if (existingReply.isEmpty()) {
            throw new RuntimeException("快捷回复不存在");
        }

        QuickReply quickReply = existingReply.get();

        // 验证快捷回复属于当前门店
        if (!storeId.equals(quickReply.getStoreId())) {
            throw new RuntimeException("无权限修改此快捷回复");
        }

        quickReply.setTitle(quickReplyDetails.getTitle());
        quickReply.setMessage(quickReplyDetails.getMessage());

        return quickReplyRepository.save(quickReply);
    }

    /**
     * 删除快捷回复(门店级)
     */
    @Transactional
    public void deleteQuickReply(Long id) {
        Long storeId = getCurrentStoreId();

        Optional<QuickReply> existingReply = quickReplyRepository.findById(id);
        if (existingReply.isEmpty()) {
            throw new RuntimeException("快捷回复不存在");
        }

        QuickReply quickReply = existingReply.get();

        // 验证快捷回复属于当前门店
        if (!storeId.equals(quickReply.getStoreId())) {
            throw new RuntimeException("无权限删除此快捷回复");
        }

        quickReplyRepository.deleteById(id);
    }
}
