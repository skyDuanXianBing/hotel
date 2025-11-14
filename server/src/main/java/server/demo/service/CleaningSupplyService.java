package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
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
     * 获取易耗品列表(门店级)
     */
    public List<CleaningSupply> getAllSupplies() {
        Long storeId = getCurrentStoreId();
        return cleaningSupplyRepository.findByStoreId(storeId);
    }

    /**
     * 根据用户ID获取易耗品列表(已废弃,使用getAllSupplies)
     */
    @Deprecated
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
     * 创建易耗品(门店级)
     */
    @Transactional
    public CleaningSupply createSupply(CleaningSupply supply) {
        Long storeId = getCurrentStoreId();
        supply.setStoreId(storeId);
        return cleaningSupplyRepository.save(supply);
    }

    /**
     * 更新易耗品(门店级)
     */
    @Transactional
    public CleaningSupply updateSupply(Long id, CleaningSupply supply) {
        Long storeId = getCurrentStoreId();

        Optional<CleaningSupply> existingSupply = cleaningSupplyRepository.findById(id);
        if (existingSupply.isEmpty()) {
            throw new RuntimeException("易耗品不存在");
        }

        CleaningSupply s = existingSupply.get();

        // 验证易耗品属于当前门店
        if (!storeId.equals(s.getStoreId())) {
            throw new RuntimeException("无权限修改此易耗品");
        }

        s.setRoomType(supply.getRoomType());
        s.setSupplies(supply.getSupplies());

        return cleaningSupplyRepository.save(s);
    }

    /**
     * 删除易耗品(门店级)
     */
    @Transactional
    public void deleteSupply(Long id) {
        Long storeId = getCurrentStoreId();

        Optional<CleaningSupply> existingSupply = cleaningSupplyRepository.findById(id);
        if (existingSupply.isEmpty()) {
            throw new RuntimeException("易耗品不存在");
        }

        CleaningSupply s = existingSupply.get();

        // 验证易耗品属于当前门店
        if (!storeId.equals(s.getStoreId())) {
            throw new RuntimeException("无权限删除此易耗品");
        }

        cleaningSupplyRepository.deleteById(id);
    }

    /**
     * 清空易耗品内容(门店级)
     */
    @Transactional
    public CleaningSupply clearSupply(Long id) {
        Long storeId = getCurrentStoreId();

        Optional<CleaningSupply> existingSupply = cleaningSupplyRepository.findById(id);
        if (existingSupply.isEmpty()) {
            throw new RuntimeException("易耗品不存在");
        }

        CleaningSupply s = existingSupply.get();

        // 验证易耗品属于当前门店
        if (!storeId.equals(s.getStoreId())) {
            throw new RuntimeException("无权限修改此易耗品");
        }

        s.setSupplies("-");

        return cleaningSupplyRepository.save(s);
    }
}
