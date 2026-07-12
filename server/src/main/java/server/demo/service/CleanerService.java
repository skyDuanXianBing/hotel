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

    @Autowired
    private CleanerIdentityService cleanerIdentityService;

    public List<Cleaner> getCleanersByUserIdAndStoreId(Long userId, Long storeId) {
        return cleanerRepository.findByUserIdAndStoreId(userId, storeId);
    }

    public List<Cleaner> getCleanersByUserId(Long userId) {
        return cleanerRepository.findByUserId(userId);
    }

    public List<Cleaner> getCleanersByStoreId(Long storeId) {
        return cleanerRepository.findByStoreIdAndIsActiveTrue(storeId);
    }

    public Optional<Cleaner> getCleanerById(Long storeId, Long id) {
        return cleanerRepository.findById(id).filter(cleaner -> storeId.equals(cleaner.getStoreId()));
    }

    @Transactional
    public Cleaner createCleaner(Cleaner cleaner) {
        if (cleaner.getPassword() == null || cleaner.getPassword().isBlank()) {
            throw new RuntimeException("保洁员密码不能为空，请通过邀请邮件完成注册");
        }

        throw new RuntimeException("请通过保洁员邀请流程创建账号，禁止直接创建不完整身份");
    }

    @Transactional
    public Cleaner updateCleaner(Long storeId, Long id, Cleaner cleaner) {
        Optional<Cleaner> existingCleaner = cleanerRepository.findById(id)
                .filter(existing -> storeId.equals(existing.getStoreId()));
        if (existingCleaner.isEmpty()) {
            throw new RuntimeException("保洁员不存在");
        }

        Cleaner currentCleaner = existingCleaner.get();
        if (cleaner.getEmail() != null && !cleaner.getEmail().trim().equalsIgnoreCase(currentCleaner.getEmail())) {
            throw new RuntimeException("保洁员邮箱不可直接修改，请重新邀请并完成身份核验");
        }
        currentCleaner.setName(cleaner.getName());
        Cleaner saved = cleanerRepository.save(currentCleaner);
        return cleanerIdentityService.ensureCleanerIdentity(saved);
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
