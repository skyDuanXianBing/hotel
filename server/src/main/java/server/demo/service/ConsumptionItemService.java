package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.ConsumptionItem;
import server.demo.repository.ConsumptionItemRepository;

import java.util.List;

@Service
public class ConsumptionItemService {

    @Autowired
    private ConsumptionItemRepository consumptionItemRepository;

    /**
     * 获取用户的所有消费项
     */
    public List<ConsumptionItem> getAllByUserId(Long userId) {
        return consumptionItemRepository.findByUserId(userId);
    }

    /**
     * 根据ID获取消费项
     */
    public ConsumptionItem getById(Long id) {
        return consumptionItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("消费项不存在"));
    }

    /**
     * 根据分类获取消费项
     */
    public List<ConsumptionItem> getByCategory(Long userId, String category) {
        return consumptionItemRepository.findByUserIdAndCategory(userId, category);
    }

    /**
     * 根据启用状态获取消费项
     */
    public List<ConsumptionItem> getByEnabled(Long userId, Boolean enabled) {
        return consumptionItemRepository.findByUserIdAndEnabled(userId, enabled);
    }

    /**
     * 创建消费项
     */
    @Transactional
    public ConsumptionItem create(ConsumptionItem item) {
        return consumptionItemRepository.save(item);
    }

    /**
     * 更新消费项
     */
    @Transactional
    public ConsumptionItem update(Long id, ConsumptionItem updates) {
        ConsumptionItem item = getById(id);

        if (updates.getCategory() != null) {
            item.setCategory(updates.getCategory());
        }
        if (updates.getName() != null) {
            item.setName(updates.getName());
        }
        if (updates.getPrice() != null) {
            item.setPrice(updates.getPrice());
        }
        if (updates.getEnabled() != null) {
            item.setEnabled(updates.getEnabled());
        }
        if (updates.getDescription() != null) {
            item.setDescription(updates.getDescription());
        }

        return consumptionItemRepository.save(item);
    }

    /**
     * 更新消费项启用状态
     */
    @Transactional
    public ConsumptionItem updateEnabled(Long id, Boolean enabled) {
        ConsumptionItem item = getById(id);
        item.setEnabled(enabled);
        return consumptionItemRepository.save(item);
    }

    /**
     * 删除消费项
     */
    @Transactional
    public void delete(Long id) {
        if (!consumptionItemRepository.existsById(id)) {
            throw new RuntimeException("消费项不存在");
        }
        consumptionItemRepository.deleteById(id);
    }
}
