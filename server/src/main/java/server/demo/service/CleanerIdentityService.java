package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Cleaner;
import server.demo.entity.Store;
import server.demo.entity.StoreUser;
import server.demo.entity.StoreUserPermission;
import server.demo.entity.User;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.repository.CleanerRepository;
import server.demo.repository.StoreRepository;
import server.demo.repository.StoreUserPermissionRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.repository.UserRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CleanerIdentityService {

    private static final String CLEANER_STORE_ROLE = "member";

    private final CleanerRepository cleanerRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final StoreUserRepository storeUserRepository;
    private final StoreUserPermissionRepository storeUserPermissionRepository;

    public CleanerIdentityService(
            CleanerRepository cleanerRepository,
            UserRepository userRepository,
            StoreRepository storeRepository,
            StoreUserRepository storeUserRepository,
            StoreUserPermissionRepository storeUserPermissionRepository
    ) {
        this.cleanerRepository = cleanerRepository;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.storeUserRepository = storeUserRepository;
        this.storeUserPermissionRepository = storeUserPermissionRepository;
    }

    @Transactional
    public User createCleanerUserAccount(
            String email,
            String name,
            String encodedPassword,
            Long storeId,
            Long invitedByUserId
    ) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("该邮箱已存在系统账号，请更换邮箱后再注册保洁员");
        }

        return saveCleanerUserAccount(
                null,
                email,
                name,
                encodedPassword,
                storeId,
                invitedByUserId,
                true
        );
    }

    @Transactional
    public User createOrReuseCleanerUserAccount(
            String email,
            String name,
            String encodedPassword,
            Long storeId,
            Long invitedByUserId
    ) {
        User existingUser = userRepository.findByEmail(email).orElse(null);
        return saveCleanerUserAccount(
                existingUser,
                email,
                name,
                encodedPassword,
                storeId,
                invitedByUserId,
                true
        );
    }

    @Transactional
    public Cleaner ensureCleanerIdentity(Cleaner cleaner) {
        if (cleaner == null) {
            throw new RuntimeException("保洁员信息不存在");
        }

        Store store = loadStore(cleaner.getStoreId());
        User user = resolveMatchingUser(cleaner);
        syncUserProfile(user, cleaner);
        User savedUser = userRepository.save(user);

        if (!Objects.equals(cleaner.getUserId(), savedUser.getId())) {
            cleaner.setUserId(savedUser.getId());
            cleaner = cleanerRepository.save(cleaner);
        }

        ensureStoreMembership(store, savedUser, store.getUserId(), Boolean.TRUE.equals(cleaner.getIsActive()));
        return cleaner;
    }

    public Optional<Cleaner> findCleanerByUserIdAndStoreId(Long userId, Long storeId) {
        if (userId == null || storeId == null) {
            return Optional.empty();
        }

        List<Cleaner> cleaners = cleanerRepository.findByUserIdAndStoreId(userId, storeId);
        if (cleaners == null || cleaners.isEmpty()) {
            return Optional.empty();
        }
        if (cleaners.size() > 1) {
            throw new RuntimeException("同一门店下存在重复的保洁员身份，请联系管理员检查数据");
        }
        return Optional.of(cleaners.get(0));
    }

    @Transactional
    public Cleaner ensureCleanerIdentityByEmail(String email) {
        Cleaner cleaner = cleanerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("该邮箱未注册保洁员账号"));
        return ensureCleanerIdentity(cleaner);
    }

    public Cleaner getRequiredCleanerByUserIdAndStoreId(Long userId, Long storeId) {
        return findCleanerByUserIdAndStoreId(userId, storeId)
                .orElseThrow(() -> new RuntimeException("当前账号未绑定保洁员身份"));
    }

    private User saveCleanerUserAccount(
            User user,
            String email,
            String name,
            String encodedPassword,
            Long storeId,
            Long invitedByUserId,
            boolean active
    ) {
        Store store = loadStore(storeId);

        User targetUser = user == null ? new User() : user;
        if (targetUser.getId() == null) {
            targetUser.setUsername(generateUniqueUsername(email));
        }

        targetUser.setEmail(email);
        targetUser.setName(name);
        targetUser.setNickname(name);
        targetUser.setPassword(encodedPassword);
        targetUser.setIsActive(active);

        User savedUser = userRepository.save(targetUser);
        ensureStoreMembership(store, savedUser, invitedByUserId, active);
        return savedUser;
    }

    private User resolveMatchingUser(Cleaner cleaner) {
        if (cleaner.getUserId() != null) {
            Optional<User> linkedUser = userRepository.findById(cleaner.getUserId());
            if (linkedUser.isPresent() && emailEquals(linkedUser.get().getEmail(), cleaner.getEmail())) {
                return linkedUser.get();
            }
        }

        Optional<User> sameEmailUser = userRepository.findByEmail(cleaner.getEmail());
        if (sameEmailUser.isPresent()) {
            return sameEmailUser.get();
        }

        User user = new User();
        user.setUsername(generateUniqueUsername(cleaner.getEmail()));
        user.setEmail(cleaner.getEmail());
        user.setPassword(cleaner.getPassword());
        user.setName(cleaner.getName());
        user.setNickname(cleaner.getName());
        user.setIsActive(cleaner.getIsActive());
        return user;
    }

    private void syncUserProfile(User user, Cleaner cleaner) {
        user.setEmail(cleaner.getEmail());
        user.setName(cleaner.getName());
        user.setNickname(cleaner.getName());
        user.setIsActive(cleaner.getIsActive());
    }

    private StoreUser ensureStoreMembership(Store store, User user, Long invitedByUserId, boolean active) {
        StoreUser storeUser = storeUserRepository.findByStoreIdAndUserId(store.getId(), user.getId())
                .orElseGet(() -> {
                    StoreUser created = new StoreUser(store, user, CLEANER_STORE_ROLE);
                    created.setInvitedBy(invitedByUserId);
                    return created;
                });

        storeUser.setStore(store);
        storeUser.setUser(user);
        storeUser.setRole(CLEANER_STORE_ROLE);
        storeUser.setIsActive(active);
        if (storeUser.getInvitedBy() == null) {
            storeUser.setInvitedBy(invitedByUserId);
        }

        StoreUser savedStoreUser = storeUserRepository.save(storeUser);
        ensureTaskListPermission(savedStoreUser);
        return savedStoreUser;
    }

    private void ensureTaskListPermission(StoreUser storeUser) {
        List<StoreUserPermission> permissions =
                storeUserPermissionRepository.findByStoreUser_Id(storeUser.getId());

        boolean hasTaskListPermission = permissions.stream().anyMatch(permission ->
                permission.getModule() == PermissionModule.ACCOMMODATION
                        && permission.getAction() == PermissionAction.TASK_LIST
        );
        if (hasTaskListPermission) {
            return;
        }

        StoreUserPermission permission = new StoreUserPermission(
                storeUser,
                PermissionModule.ACCOMMODATION,
                PermissionAction.TASK_LIST
        );
        permission.setRoomTypeId(0L);
        permission.setAllRoomTypes(false);
        storeUserPermissionRepository.save(permission);
    }

    private Store loadStore(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("门店不存在"));
    }

    private String generateUniqueUsername(String email) {
        String localPart = email == null ? "cleaner" : email;
        int atIndex = localPart.indexOf('@');
        if (atIndex > 0) {
            localPart = localPart.substring(0, atIndex);
        }

        String normalized = localPart
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9._-]", "");

        if (normalized.length() < 3) {
            normalized = normalized + "cleaner";
        }
        if (normalized.length() > 50) {
            normalized = normalized.substring(0, 50);
        }

        String candidate = normalized;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            String suffixValue = String.valueOf(suffix);
            int maxBaseLength = Math.max(1, 50 - suffixValue.length());
            String base = normalized.length() > maxBaseLength
                    ? normalized.substring(0, maxBaseLength)
                    : normalized;
            candidate = base + suffixValue;
            suffix++;
        }

        return candidate;
    }

    private boolean emailEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }
}
