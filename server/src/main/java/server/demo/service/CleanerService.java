package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Cleaner;
import server.demo.repository.CleanerRepository;

import java.util.List;
import java.util.Optional;

/**
 * 保洁员 Service
 */
@Service
public class CleanerService {

    @Autowired
    private CleanerRepository cleanerRepository;

    /**
     * 根据用户ID和门店ID获取保洁员列表
     */
    public List<Cleaner> getCleanersByUserIdAndStoreId(Long userId, Long storeId) {
        return cleanerRepository.findByUserIdAndStoreId(userId, storeId);
    }

    /**
     * 根据用户ID获取保洁员列表
     */
    public List<Cleaner> getCleanersByUserId(Long userId) {
        return cleanerRepository.findByUserId(userId);
    }

    /**
     * 根据ID获取保洁员
     */
    public Optional<Cleaner> getCleanerById(Long id) {
        return cleanerRepository.findById(id);
    }

    /**
     * 创建保洁员
     */
    @Transactional
    public Cleaner createCleaner(Cleaner cleaner) {
        return cleanerRepository.save(cleaner);
    }

    /**
     * 更新保洁员
     */
    @Transactional
    public Cleaner updateCleaner(Long id, Cleaner cleaner) {
        Optional<Cleaner> existingCleaner = cleanerRepository.findById(id);
        if (existingCleaner.isEmpty()) {
            throw new RuntimeException("保洁员不存在");
        }

        Cleaner c = existingCleaner.get();
        c.setName(cleaner.getName());
        c.setEmail(cleaner.getEmail());

        return cleanerRepository.save(c);
    }

    /**
     * 删除保洁员
     */
    @Transactional
    public void deleteCleaner(Long id) {
        cleanerRepository.deleteById(id);
    }
}
