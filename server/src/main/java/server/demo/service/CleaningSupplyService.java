package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.CleaningSupply;
import server.demo.repository.CleaningSupplyRepository;

import java.util.List;
import java.util.Optional;

/**
 * 保洁易耗品 Service
 */
@Service
public class CleaningSupplyService {

    @Autowired
    private CleaningSupplyRepository cleaningSupplyRepository;

    /**
     * 根据用户ID获取易耗品列表
     */
    public List<CleaningSupply> getSuppliesByUserId(Long userId) {
        return cleaningSupplyRepository.findByUserId(userId);
    }

    /**
     * 根据ID获取易耗品
     */
    public Optional<CleaningSupply> getSupplyById(Long id) {
        return cleaningSupplyRepository.findById(id);
    }

    /**
     * 创建易耗品
     */
    @Transactional
    public CleaningSupply createSupply(CleaningSupply supply) {
        return cleaningSupplyRepository.save(supply);
    }

    /**
     * 更新易耗品
     */
    @Transactional
    public CleaningSupply updateSupply(Long id, CleaningSupply supply) {
        Optional<CleaningSupply> existingSupply = cleaningSupplyRepository.findById(id);
        if (existingSupply.isEmpty()) {
            throw new RuntimeException("易耗品不存在");
        }

        CleaningSupply s = existingSupply.get();
        s.setRoomType(supply.getRoomType());
        s.setSupplies(supply.getSupplies());

        return cleaningSupplyRepository.save(s);
    }

    /**
     * 删除易耗品
     */
    @Transactional
    public void deleteSupply(Long id) {
        cleaningSupplyRepository.deleteById(id);
    }

    /**
     * 清空易耗品内容
     */
    @Transactional
    public CleaningSupply clearSupply(Long id) {
        Optional<CleaningSupply> existingSupply = cleaningSupplyRepository.findById(id);
        if (existingSupply.isEmpty()) {
            throw new RuntimeException("易耗品不存在");
        }

        CleaningSupply s = existingSupply.get();
        s.setSupplies("-");

        return cleaningSupplyRepository.save(s);
    }
}
