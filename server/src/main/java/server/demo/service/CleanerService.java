package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Cleaner;
import server.demo.entity.StoreUser;
import server.demo.entity.StoreUserPermission;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.CleanerRepository;
import server.demo.repository.CleaningTaskRepository;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CleanerService {

    @Autowired
    private CleanerRepository cleanerRepository;

    @Autowired
    private CleaningTaskRepository cleaningTaskRepository;

    @Autowired
    private StoreUserRepository storeUserRepository;

    @Autowired
    private StoreUserPermissionRepository storeUserPermissionRepository;

    public List<Cleaner> getCleanersByUserIdAndStoreId(Long userId, Long storeId) {
        return cleanerRepository.findByUserIdAndStoreId(userId, storeId);
    }

    public List<Cleaner> getCleanersByUserId(Long userId) {
        return cleanerRepository.findByUserId(userId);
    }

    public List<Cleaner> getCleanersByStoreId(Long storeId) {
        return cleanerRepository.findByStoreIdAndIsActiveTrue(storeId);
    }

    public Optional<Cleaner> getCleanerById(Long id) {
        return cleanerRepository.findById(id);
    }

    @Transactional
    public Cleaner createCleaner(Cleaner cleaner) {
        return cleanerRepository.save(cleaner);
    }

    @Transactional
    public Cleaner updateCleaner(Long id, Cleaner cleaner) {
        Optional<Cleaner> existingCleaner = cleanerRepository.findById(id);
        if (existingCleaner.isEmpty()) {
            throw new RuntimeException("保洁员不存在");
        }

        Cleaner currentCleaner = existingCleaner.get();
        currentCleaner.setName(cleaner.getName());
        currentCleaner.setEmail(cleaner.getEmail());

        return cleanerRepository.save(currentCleaner);
    }

    @Transactional
    public void deleteCleaner(Long storeId, Long id) {
        Cleaner cleaner = cleanerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("保洁员不存在"));

        if (!storeId.equals(cleaner.getStoreId())) {
            throw new RuntimeException("保洁员不存在");
        }

        if (cleaningTaskRepository.existsByCleanerId(id)) {
            throw new RuntimeException("该保洁员仍有关联任务，请先处理任务后再删除");
        }

        cleaner.setIsActive(false);
        cleanerRepository.save(cleaner);

        if (cleaner.getUserId() == null) {
            return;
        }

        storeUserRepository.findByStoreIdAndUserId(storeId, cleaner.getUserId())
                .filter(this::shouldDisableStoreMembershipWhenCleanerDeleted)
                .ifPresent(this::disableStoreMembership);
    }

    private void disableStoreMembership(StoreUser storeUser) {
        storeUser.setIsActive(false);
        storeUserRepository.save(storeUser);
    }

    private boolean shouldDisableStoreMembershipWhenCleanerDeleted(StoreUser storeUser) {
        if (storeUser == null || !Boolean.TRUE.equals(storeUser.getIsActive())) {
            return false;
        }

        if (!"member".equals(storeUser.getRole())) {
            return false;
        }

        if (storeUser.getRoles() != null && !storeUser.getRoles().isEmpty()) {
            return false;
        }

        if (storeUser.getId() == null) {
            return false;
        }

        List<StoreUserPermission> permissions =
                storeUserPermissionRepository.findByStoreUser_Id(storeUser.getId());
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        return permissions.stream().allMatch(this::isCleanerTaskListPermission);
    }

    private boolean isCleanerTaskListPermission(StoreUserPermission permission) {
        return permission != null
                && permission.getModule() == PermissionModule.ACCOMMODATION
                && permission.getAction() == PermissionAction.TASK_LIST;
    }
}
