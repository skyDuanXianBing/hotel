package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.*;
import server.demo.entity.*;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;
import server.demo.exception.SuPropertyDeleteFailedException;
import server.demo.repository.*;
import server.demo.util.SuHotelIdUtil;
import server.demo.util.StoreTimeZoneUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoreService {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreUserRepository storeUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorePolicyRepository storePolicyRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ChannelBootstrapService channelBootstrapService;

    @Autowired
    private SuPropertyService suPropertyService;

    @Autowired
    private SuImageSyncService suImageSyncService;

    @Autowired
    private StoreUserPermissionRepository storeUserPermissionRepository;

    @Autowired
    private PriceLabsSyncService priceLabsSyncService;

    public List<StoreDTO> getUserStores(Long userId) {
        List<StoreUser> storeUsers = storeUserRepository.findActiveStoresByUserId(userId);
        return storeUsers.stream()
                .map(su -> convertToDTO(su.getStore(), su.getRole()))
                .collect(Collectors.toList());
    }

    @Transactional
    public StoreDTO createStore(Long userId, CreateStoreRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String normalizedTimezone = StoreTimeZoneUtil.normalizeOrNull(request.getTimezone());

        Store store = new Store();
        store.setUserId(userId);
        store.setName(request.getName());
        store.setPhone(request.getPhone());
        store.setPhoneTechType(request.getPhoneTechType());
        store.setType(request.getType());
        store.setTimezone(normalizedTimezone);
        store.setManager(request.getManager());
        store.setOwnerEmail(user.getEmail());
        store.setCountry(request.getCountry());
        store.setCity(request.getCity());
        store.setState(request.getState());
        store.setAddress(request.getAddress());
        if (request.getCurrency() != null) {
            store.setCurrency(request.getCurrency());
        }
        store.setLogo(request.getLogo());
        store.setDescription(request.getDescription());
        store.setEmail(request.getEmail());
        store.setWechat(request.getWechat());
        store.setWhatsapp(request.getWhatsapp());
        store.setLine(request.getLine());
        store.setLanguage(request.getLanguage());
        store.setFacilities(request.getFacilities());
        store.setDesktopPhotoUrls(request.getDesktopPhotoUrls());
        store.setMobilePhotoUrls(request.getMobilePhotoUrls());
        store.setLocalizedContent(request.getLocalizedContent());

        Store savedStore = storeRepository.save(store);

        // 生成并保存 Su hotelid（支持多门店分别配置）
        String requestedHotelId = SuHotelIdUtil.normalize(request.getSuHotelId());
        String suHotelId = requestedHotelId != null ? requestedHotelId : generateUniqueRandomSuHotelId();
        if (!SuHotelIdUtil.isValid(suHotelId)) {
            throw new RuntimeException("渠道酒店ID仅支持 A-Z/0-9，长度<=15");
        }
        savedStore.setSuHotelId(suHotelId);
        savedStore = storeRepository.save(savedStore);
        upsertStorePolicy(savedStore, request);

        StoreUser storeUser = new StoreUser(savedStore, user, "owner");
        storeUserRepository.save(storeUser);

        // 为新门店补齐默认渠道（避免渠道价格比例/OTA 同步空数据）
        channelBootstrapService.ensureDefaultChannelsForStore(savedStore.getId());

        return convertToDTO(savedStore, "owner");
    }

    public Optional<StoreDTO> getStoreById(Long storeId, Long userId) {
        Optional<StoreUser> storeUser = storeUserRepository.findByStoreIdAndUserId(storeId, userId);
        return storeUser.map(su -> convertToDTO(su.getStore(), su.getRole()));
    }

    public List<PermissionDTO> getCurrentUserEffectivePermissions(Long storeId, Long userId) {
        StoreUser storeUser = storeUserRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new RuntimeException("No permission"));

        if (isStoreManager(storeUser)) {
            return buildManagerPermissions();
        }

        return mergeEffectivePermissions(
                storeUser.getRoles(),
                loadStoreUserExtraPermissions(storeUser.getId())
        );
    }

    @Transactional
    public StoreDTO updateStore(Long storeId, Long userId, CreateStoreRequest request) {
        StoreUser storeUser = storeUserRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new RuntimeException("No permission"));

        if (!"owner".equals(storeUser.getRole()) && !"admin".equals(storeUser.getRole())) {
            throw new RuntimeException("No permission to update store");
        }

        Store store = storeUser.getStore();
        String normalizedTimezone = StoreTimeZoneUtil.normalizeOrNull(request.getTimezone());

        boolean fallbackAddressChanged = hasStoreFallbackAddressChanged(store, request, normalizedTimezone);

        store.setName(request.getName());
        store.setPhone(request.getPhone());
        store.setPhoneTechType(request.getPhoneTechType());
        store.setType(request.getType());
        store.setTimezone(normalizedTimezone);
        store.setManager(request.getManager());
        store.setCountry(request.getCountry());
        store.setCity(request.getCity());
        store.setState(request.getState());
        store.setAddress(request.getAddress());
        store.setLogo(request.getLogo());
        store.setDescription(request.getDescription());
        store.setEmail(request.getEmail());
        store.setWechat(request.getWechat());
        store.setWhatsapp(request.getWhatsapp());
        store.setLine(request.getLine());
        store.setLanguage(request.getLanguage());
        store.setFacilities(request.getFacilities());
        store.setDesktopPhotoUrls(request.getDesktopPhotoUrls());
        store.setMobilePhotoUrls(request.getMobilePhotoUrls());
        store.setLocalizedContent(request.getLocalizedContent());
        if (request.getCurrency() != null) {
            store.setCurrency(request.getCurrency());
        }
        if (request.getSuHotelId() != null) {
            String normalized = SuHotelIdUtil.normalize(request.getSuHotelId());
            if (normalized != null && !SuHotelIdUtil.isValid(normalized)) {
                throw new RuntimeException("渠道酒店ID仅支持 A-Z/0-9，长度<=15");
            }
            store.setSuHotelId(normalized);
        }

        Store updatedStore = storeRepository.save(store);
        if (fallbackAddressChanged) {
            priceLabsSyncService.syncListingsUsingStoreAddressAsync(updatedStore.getId());
        }
        upsertStorePolicy(updatedStore, request);
        syncStoreToSu(updatedStore);
        return convertToDTO(updatedStore, storeUser.getRole());
    }

    private boolean hasStoreFallbackAddressChanged(
            Store store,
            CreateStoreRequest request,
            String normalizedRequestTimezone
    ) {
        return !Objects.equals(normalizeOptionalText(store.getAddress()), normalizeOptionalText(request.getAddress()))
                || !Objects.equals(normalizeOptionalText(store.getCity()), normalizeOptionalText(request.getCity()))
                || !Objects.equals(normalizeOptionalText(store.getState()), normalizeOptionalText(request.getState()))
                || !Objects.equals(normalizeOptionalText(store.getCountry()), normalizeOptionalText(request.getCountry()))
                || !Objects.equals(normalizeOptionalText(store.getTimezone()), normalizedRequestTimezone);
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Transactional
    public void softDeleteStore(Long storeId, Long operatorUserId) {
        StoreUser operator = storeUserRepository.findByStoreIdAndUserId(storeId, operatorUserId)
                .orElseThrow(() -> new RuntimeException("No permission"));

        if (!"owner".equals(operator.getRole())) {
            throw new RuntimeException("Only owner can delete store");
        }

        List<StoreUser> storeUsers = storeUserRepository.findByStoreId(storeId);
        if (storeUsers == null || storeUsers.isEmpty()) {
            return;
        }

        storeUsers.forEach(storeUser -> storeUser.setIsActive(false));
        storeUserRepository.saveAll(storeUsers);
    }

    @Transactional
    public void deleteStoreWithSuRemoveProperty(Long storeId, Long operatorUserId) {
        StoreUser operator = storeUserRepository.findByStoreIdAndUserId(storeId, operatorUserId)
                .orElseThrow(() -> new RuntimeException("No permission"));

        if (!"owner".equals(operator.getRole())) {
            throw new RuntimeException("Only owner can delete store");
        }

        SuPropertyService.RemoveResult result = suPropertyService.removeStoreProperty(storeId, false);
        if (result != null && !result.success() && !SuPropertyService.isPropertyAlreadyMissing(result)) {
            throw new SuPropertyDeleteFailedException(
                    result.message() != null ? result.message() : "Delete store failed",
                    result.errorCode()
            );
        }

        softDeleteStore(storeId, operatorUserId);
    }

    @Transactional
    public void inviteUser(Long storeId, Long inviterUserId, String invitedEmail, String role) {
        StoreUser inviter = storeUserRepository.findByStoreIdAndUserId(storeId, inviterUserId)
                .orElseThrow(() -> new RuntimeException("No permission"));

        if (!"owner".equals(inviter.getRole()) && !"admin".equals(inviter.getRole())) {
            throw new RuntimeException("Only admin can invite user");
        }

        User invitedUser = userRepository.findByEmail(invitedEmail)
                .orElseThrow(() -> new RuntimeException("Invited user not found"));

        if (storeUserRepository.existsByStoreIdAndUserId(storeId, invitedUser.getId())) {
            throw new RuntimeException("User already member");
        }

        StoreUser storeUser = new StoreUser(inviter.getStore(), invitedUser, role);
        storeUser.setInvitedBy(inviterUserId);
        storeUserRepository.save(storeUser);
    }

    /**
     * 添加门店成员（支持权限角色）。
     */
    @Transactional
    public StoreUserDTO addStoreMember(Long storeId, Long operatorUserId, AddStoreMemberRequest request) {
        StoreUser operator = storeUserRepository.findByStoreIdAndUserId(storeId, operatorUserId)
                .orElseThrow(() -> new RuntimeException("无权限"));

        if (!"owner".equals(operator.getRole()) && !"admin".equals(operator.getRole())) {
            throw new RuntimeException("只有管理员可以添加成员");
        }

        User invitedUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("用户不存在，请先让该用户注册账号"));

        Optional<StoreUser> existingStoreUser = storeUserRepository.findByStoreIdAndUserId(storeId, invitedUser.getId());
        if (existingStoreUser.isPresent() && !Boolean.TRUE.equals(existingStoreUser.get().getIsActive())) {
            StoreUser inactiveStoreUser = existingStoreUser.get();
            inactiveStoreUser.setStore(operator.getStore());
            inactiveStoreUser.setUser(invitedUser);
            inactiveStoreUser.setRole(request.getRole());
            inactiveStoreUser.setIsActive(true);
            inactiveStoreUser.setInvitedBy(operatorUserId);

            Set<Role> roles = new HashSet<>();
            if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
                for (Long roleId : request.getRoleIds()) {
                    Role role = roleRepository.findById(roleId)
                            .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

                    if (!storeId.equals(role.getStoreId())) {
                        throw new RuntimeException("Role does not belong to current store");
                    }

                    roles.add(role);
                }
            }
            inactiveStoreUser.setRoles(roles);

            StoreUser restoredStoreUser = storeUserRepository.save(inactiveStoreUser);
            replaceStoreUserExtraPermissions(restoredStoreUser, request.getExtraPermissions());
            return convertToStoreUserDTO(
                    restoredStoreUser,
                    loadStoreUserExtraPermissions(restoredStoreUser.getId())
            );
        }

        if (storeUserRepository.existsByStoreIdAndUserId(storeId, invitedUser.getId())) {
            throw new RuntimeException("该用户已是门店成员");
        }

        StoreUser storeUser = new StoreUser(operator.getStore(), invitedUser, request.getRole());
        storeUser.setInvitedBy(operatorUserId);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (Long roleId : request.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new RuntimeException("角色不存在: " + roleId));

                if (!storeId.equals(role.getStoreId())) {
                    throw new RuntimeException("角色不属于当前门店");
                }

                roles.add(role);
            }
            storeUser.setRoles(roles);
        }

        StoreUser savedStoreUser = storeUserRepository.save(storeUser);

        if (request.getExtraPermissions() != null) {
            replaceStoreUserExtraPermissions(savedStoreUser, request.getExtraPermissions());
        }

        return convertToStoreUserDTO(
                savedStoreUser,
                loadStoreUserExtraPermissions(savedStoreUser.getId())
        );
    }

    /**
     * 更新门店成员权限。
     */
    @Transactional
    public StoreUserDTO updateStoreMemberPermission(Long storeId, Long operatorUserId, Long targetUserId,
                                                     UpdateStoreMemberPermissionRequest request) {
        StoreUser operator = storeUserRepository.findByStoreIdAndUserId(storeId, operatorUserId)
                .orElseThrow(() -> new RuntimeException("无权限"));

        if (!"owner".equals(operator.getRole()) && !"admin".equals(operator.getRole())) {
            throw new RuntimeException("只有管理员可以修改成员权限");
        }

        StoreUser target = storeUserRepository.findByStoreIdAndUserId(storeId, targetUserId)
                .orElseThrow(() -> new RuntimeException("目标用户不是门店成员"));

        if ("owner".equals(target.getRole()) && !"owner".equals(operator.getRole())) {
            throw new RuntimeException("只有所有者可以修改所有者权限");
        }

        if (request.getRole() != null && !"owner".equals(target.getRole()) && "owner".equals(request.getRole())) {
            throw new RuntimeException("Please use transfer owner API");
        }

        if ("owner".equals(target.getRole()) && request.getRole() != null && !"owner".equals(request.getRole())) {
            throw new RuntimeException("Please use transfer owner API");
        }

        if ("owner".equals(target.getRole()) && Boolean.FALSE.equals(request.getIsActive())) {
            throw new RuntimeException("Store owner must remain active");
        }

        if (request.getName() != null) {
            String normalizedName = request.getName().trim();
            if (normalizedName.isEmpty()) {
                throw new RuntimeException("员工姓名不能为空");
            }
            target.getUser().setName(normalizedName);
            userRepository.save(target.getUser());
        }

        if (request.getRole() != null) {
            target.setRole(request.getRole());
        }

        if (request.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>();
            for (Long roleId : request.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new RuntimeException("角色不存在: " + roleId));

                if (!storeId.equals(role.getStoreId())) {
                    throw new RuntimeException("角色不属于当前门店");
                }

                roles.add(role);
            }
            target.setRoles(roles);
        }

        if (request.getIsActive() != null) {
            target.setIsActive(request.getIsActive());
        }

        StoreUser updatedStoreUser = storeUserRepository.save(target);

        if (request.getExtraPermissions() != null) {
            replaceStoreUserExtraPermissions(updatedStoreUser, request.getExtraPermissions());
        }

        return convertToStoreUserDTO(
                updatedStoreUser,
                loadStoreUserExtraPermissions(updatedStoreUser.getId())
        );
    }

    /**
     * Transfer store owner in a single transaction to avoid transient invalid states.
     */
    @Transactional
    public void transferStoreOwner(Long storeId, Long operatorUserId, Long targetUserId) {
        StoreUser currentOwner = storeUserRepository.findByStoreIdAndUserId(storeId, operatorUserId)
                .orElseThrow(() -> new RuntimeException("No permission"));

        if (!"owner".equals(currentOwner.getRole())) {
            throw new RuntimeException("Only owner can transfer store owner");
        }

        if (Objects.equals(operatorUserId, targetUserId)) {
            throw new RuntimeException("New owner cannot be current owner");
        }

        StoreUser target = storeUserRepository.findByStoreIdAndUserId(storeId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user is not a store member"));

        if ("owner".equals(target.getRole())) {
            throw new RuntimeException("Target user is already owner");
        }

        currentOwner.setRole("admin");
        target.setRole("owner");
        target.setIsActive(true);

        Store store = currentOwner.getStore();
        if (store == null) {
            throw new RuntimeException("Store not found");
        }
        if (target.getUser() != null) {
            store.setOwnerEmail(target.getUser().getEmail());
        }

        storeUserRepository.save(currentOwner);
        storeUserRepository.save(target);
        storeRepository.save(store);
    }

    /**
     * ??????????
     */
    public StoreUserDTO getStoreMemberDetail(Long storeId, Long userId) {
        StoreUser storeUser = storeUserRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new RuntimeException("成员不存在"));
        List<StoreUserPermission> extraPermissions = loadStoreUserExtraPermissions(storeUser.getId());
        return convertToStoreUserDTO(storeUser, extraPermissions);
    }

    /**
     * 获取门店成员列表（返回DTO）
     */
    public List<StoreUserDTO> getStoreMembersDTO(Long storeId) {
        List<StoreUser> storeUsers = storeUserRepository.findAllUsersByStoreId(storeId);
        if (storeUsers == null || storeUsers.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> storeUserIds = storeUsers.stream().map(StoreUser::getId).toList();
        Map<Long, List<StoreUserPermission>> extraPermissionsByStoreUserId = new HashMap<>();

        List<StoreUserPermission> extraPermissions = storeUserPermissionRepository.findByStoreUser_IdIn(storeUserIds);
        if (extraPermissions != null && !extraPermissions.isEmpty()) {
            for (StoreUserPermission p : extraPermissions) {
                if (p == null || p.getStoreUser() == null || p.getStoreUser().getId() == null) {
                    continue;
                }
                extraPermissionsByStoreUserId
                        .computeIfAbsent(p.getStoreUser().getId(), k -> new ArrayList<>())
                        .add(p);
            }
        }

        return storeUsers.stream()
                .map(su -> convertToStoreUserDTO(su, extraPermissionsByStoreUserId.getOrDefault(su.getId(), new ArrayList<>())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeStoreMember(Long storeId, Long operatorUserId, Long targetUserId) {
        StoreUser operator = storeUserRepository.findByStoreIdAndUserId(storeId, operatorUserId)
                .orElseThrow(() -> new RuntimeException("No permission"));

        if (!"owner".equals(operator.getRole()) && !"admin".equals(operator.getRole())) {
            throw new RuntimeException("Only admin can remove member");
        }

        StoreUser target = storeUserRepository.findByStoreIdAndUserId(storeId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not member"));

        if ("owner".equals(target.getRole())) {
            throw new RuntimeException("Cannot remove owner");
        }

        storeUserRepository.delete(target);
    }

    public List<StoreUser> getStoreMembers(Long storeId) {
        return storeUserRepository.findActiveUsersByStoreId(storeId);
    }

    public StorePolicyDTO getStorePolicy(Long storeId) {
        Optional<StorePolicy> policy = storePolicyRepository.findByStoreId(storeId);
        if (policy.isPresent()) {
            return convertPolicyToDTO(policy.get());
        } else {
            // 返回空的政策对象
            StorePolicyDTO dto = new StorePolicyDTO();
            dto.setStoreId(storeId);
            return dto;
        }
    }

    @Transactional
    public StorePolicyDTO updateStorePolicy(Long storeId, Long userId, StorePolicyDTO policyDTO) {
        // 验证用户权限
        StoreUser storeUser = storeUserRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new RuntimeException("No permission"));

        if (!"owner".equals(storeUser.getRole()) && !"admin".equals(storeUser.getRole())) {
            throw new RuntimeException("No permission to update store policy");
        }

        Store store = storeUser.getStore();

        // 查找或创建政策
        StorePolicy policy = storePolicyRepository.findByStoreId(storeId)
                .orElseGet(() -> {
                    StorePolicy newPolicy = new StorePolicy();
                    newPolicy.setStore(store);
                    return newPolicy;
                });

        // 更新政策字段
        policy.setCheckinTime(policyDTO.getCheckinTime());
        policy.setCheckoutTime(policyDTO.getCheckoutTime());
        policy.setChildPolicy(policyDTO.getChildPolicy());
        policy.setSmokingPolicy(policyDTO.getSmokingPolicy());
        policy.setPetPolicy(policyDTO.getPetPolicy());
        policy.setAdditionalRules(policyDTO.getAdditionalRules());
        policy.setHotelTerms(policyDTO.getHotelTerms());

        StorePolicy savedPolicy = storePolicyRepository.save(policy);
        return convertPolicyToDTO(savedPolicy);
    }

    private String generateUniqueRandomSuHotelId() {
        for (int i = 0; i < 50; i++) {
            String candidate = SuHotelIdUtil.generateRandom();
            if (storeRepository.findBySuHotelId(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new RuntimeException("渠道酒店ID生成失败，请手动填写");
    }

    private StoreDTO convertToDTO(Store store, String userRole) {
        Optional<StorePolicy> storePolicy = storePolicyRepository.findByStoreId(store.getId());
        StoreDTO dto = new StoreDTO();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setPhone(store.getPhone());
        dto.setPhoneTechType(store.getPhoneTechType());
        dto.setType(store.getType());
        dto.setTimezone(store.getTimezone());
        dto.setManager(store.getManager());
        dto.setOwnerEmail(store.getOwnerEmail());
        dto.setAddress(store.getAddress());
        dto.setCity(store.getCity());
        dto.setState(store.getState());
        dto.setCountry(store.getCountry());
        dto.setCurrency(store.getCurrency());
        dto.setSuHotelId(store.getSuHotelId());
        dto.setLogo(store.getLogo());
        dto.setDescription(store.getDescription());
        dto.setEmail(store.getEmail());
        dto.setWechat(store.getWechat());
        dto.setWhatsapp(store.getWhatsapp());
        dto.setLine(store.getLine());
        dto.setLanguage(store.getLanguage());
        dto.setFacilities(store.getFacilities());
        dto.setDesktopPhotoUrls(store.getDesktopPhotoUrls());
        dto.setMobilePhotoUrls(store.getMobilePhotoUrls());
        dto.setLocalizedContent(store.getLocalizedContent());
        dto.setCheckinTime(storePolicy.map(StorePolicy::getCheckinTime).orElse(null));
        dto.setCheckoutTime(storePolicy.map(StorePolicy::getCheckoutTime).orElse(null));
        dto.setUserRole(userRole);
        dto.setCreatedAt(store.getCreatedAt());
        dto.setUpdatedAt(store.getUpdatedAt());
        return dto;
    }

    private void upsertStorePolicy(Store store, CreateStoreRequest request) {
        if (store == null || store.getId() == null || request == null) {
            return;
        }

        boolean hasPolicyValue = request.getCheckinTime() != null || request.getCheckoutTime() != null;
        if (!hasPolicyValue) {
            return;
        }

        StorePolicy policy = storePolicyRepository.findByStoreId(store.getId())
                .orElseGet(() -> {
                    StorePolicy newPolicy = new StorePolicy();
                    newPolicy.setStore(store);
                    return newPolicy;
                });
        policy.setCheckinTime(request.getCheckinTime());
        policy.setCheckoutTime(request.getCheckoutTime());
        storePolicyRepository.save(policy);
    }

    private void syncStoreToSu(Store store) {
        if (store == null || store.getId() == null) {
            return;
        }
        SuPropertyService.UpsertResult propertyResult = suPropertyService.updateStoreProperty(store.getId());
        if (!propertyResult.success()) {
            throw new RuntimeException(propertyResult.message() != null ? propertyResult.message() : "Su 门店同步失败");
        }
        suImageSyncService.syncStoreImagesStrict(store.getId());
    }

    private StorePolicyDTO convertPolicyToDTO(StorePolicy policy) {
        StorePolicyDTO dto = new StorePolicyDTO();
        dto.setId(policy.getId());
        dto.setStoreId(policy.getStore().getId());
        dto.setCheckinTime(policy.getCheckinTime());
        dto.setCheckoutTime(policy.getCheckoutTime());
        dto.setChildPolicy(policy.getChildPolicy());
        dto.setSmokingPolicy(policy.getSmokingPolicy());
        dto.setPetPolicy(policy.getPetPolicy());
        dto.setAdditionalRules(policy.getAdditionalRules());
        dto.setHotelTerms(policy.getHotelTerms());
        return dto;
    }

    /**
     * 转换StoreUser到DTO
     */
    private StoreUserDTO convertToStoreUserDTO(StoreUser storeUser) {
        return convertToStoreUserDTO(storeUser, Collections.emptyList());
    }

    private StoreUserDTO convertToStoreUserDTO(StoreUser storeUser, List<StoreUserPermission> extraPermissions) {
        StoreUserDTO dto = new StoreUserDTO();
        dto.setId(storeUser.getId());
        dto.setRole(storeUser.getRole());
        dto.setIsActive(storeUser.getIsActive());
        dto.setInvitedBy(storeUser.getInvitedBy());
        dto.setJoinedAt(storeUser.getJoinedAt());

        // 转换用户信息
        User user = storeUser.getUser();
        StoreUserDTO.UserSimpleDTO userDTO = new StoreUserDTO.UserSimpleDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setNickname(user.getNickname());
        userDTO.setAvatar(user.getAvatar());
        userDTO.setIsActive(user.getIsActive());
        dto.setUser(userDTO);

        // 转换权限角色列表
        Set<Role> roles = storeUser.getRoles();
        if (roles != null && !roles.isEmpty()) {
            List<RoleDTO> roleDTOs = roles.stream()
                    .map(role -> {
                        RoleDTO roleDTO = new RoleDTO();
                        roleDTO.setId(role.getId());
                        roleDTO.setName(role.getName());
                        roleDTO.setDescription(role.getDescription());
                        roleDTO.setIsSystem(role.getIsSystem());
                        return roleDTO;
                    })
                    .collect(Collectors.toList());
            dto.setRoles(roleDTOs);
        } else {
            dto.setRoles(new ArrayList<>());
        }

        dto.setExtraPermissions(convertToPermissionDTOs(extraPermissions));

        return dto;
    }

    private List<StoreUserPermission> loadStoreUserExtraPermissions(Long storeUserId) {
        if (storeUserId == null) {
            return Collections.emptyList();
        }
        List<StoreUserPermission> permissions = storeUserPermissionRepository.findByStoreUser_Id(storeUserId);
        return permissions != null ? permissions : Collections.emptyList();
    }

    private static List<PermissionDTO> convertToPermissionDTOs(List<StoreUserPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }
        List<PermissionDTO> out = new ArrayList<>();
        for (StoreUserPermission p : permissions) {
            if (p == null || p.getModule() == null || p.getAction() == null) {
                continue;
            }
            PermissionDTO dto = new PermissionDTO();
            dto.setModule(p.getModule());
            dto.setAction(p.getAction());
            dto.setRoomTypeId(p.getRoomTypeId());
            dto.setAllRoomTypes(p.getAllRoomTypes());
            out.add(dto);
        }
        return out;
    }

    private static boolean isStoreManager(StoreUser storeUser) {
        if (storeUser == null || storeUser.getRole() == null) {
            return false;
        }
        return "owner".equals(storeUser.getRole()) || "admin".equals(storeUser.getRole());
    }

    private static List<PermissionDTO> buildManagerPermissions() {
        List<PermissionDTO> permissions = new ArrayList<>();
        for (PermissionModule module : PermissionModule.values()) {
            for (PermissionAction action : PermissionAction.values()) {
                if (!belongsToModule(module, action)) {
                    continue;
                }

                PermissionDTO dto = new PermissionDTO();
                dto.setModule(module);
                dto.setAction(action);
                if (module == PermissionModule.ACCOMMODATION && action == PermissionAction.VIEW_ROOM_STATUS) {
                    dto.setRoomTypeId(0L);
                    dto.setAllRoomTypes(true);
                } else {
                    dto.setRoomTypeId(0L);
                    dto.setAllRoomTypes(false);
                }
                permissions.add(dto);
            }
        }
        return permissions;
    }

    private static boolean belongsToModule(PermissionModule module, PermissionAction action) {
        return switch (module) {
            case ACCOMMODATION -> switch (action) {
                case VIEW_ROOM_STATUS,
                        EDIT_ROOM_STATUS,
                        VIEW_ROOM_OPERATION_LOG,
                        VIEW_ROOM_INFO,
                        ROOM_SHARE,
                        VIEW_ROOM_PRICE,
                        EDIT_ROOM_PRICE,
                        VIEW_PRICE_LOG,
                        BATCH_CHANGE_PRICE,
                        BREAKFAST_PACKAGE,
                        RESERVATION_CALENDAR,
                        TASK_LIST -> true;
                default -> false;
            };
            case ORDER -> switch (action) {
                case VIEW_ORDERS, CREATE_ORDER, MODIFY_ORDER, DELETE_ORDER, CANCEL_ORDER -> true;
                default -> false;
            };
            case CHANNEL -> action == PermissionAction.VIEW_CHANNELS || action == PermissionAction.MANAGE_CHANNELS;
            case CUSTOMER -> action == PermissionAction.VIEW_CUSTOMERS || action == PermissionAction.MANAGE_CUSTOMERS;
            case STATISTICS -> action == PermissionAction.VIEW_STATS || action == PermissionAction.EXPORT_STATS;
            case SETTINGS -> switch (action) {
                case VIEW_SETTINGS,
                        MODIFY_SETTINGS,
                        MODIFY_STORE_SETTINGS,
                        MANAGE_EMPLOYEE_ACCOUNTS,
                        MANAGE_PAYMENT_METHODS -> true;
                default -> false;
            };
            case DATA_CENTER -> action == PermissionAction.VIEW_DATA || action == PermissionAction.EXPORT_DATA;
            case SENSITIVE -> action == PermissionAction.VIEW_FINANCIAL_DATA
                    || action == PermissionAction.DELETE_IMPORTANT_DATA;
        };
    }

    private static List<PermissionDTO> mergeEffectivePermissions(
            Set<Role> roles,
            List<StoreUserPermission> extraPermissions
    ) {
        LinkedHashMap<String, PermissionDTO> merged = new LinkedHashMap<>();
        mergeRolePermissions(merged, roles);
        mergeStoreUserPermissions(merged, extraPermissions);
        return new ArrayList<>(merged.values());
    }

    private static void mergeRolePermissions(LinkedHashMap<String, PermissionDTO> merged, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return;
        }

        for (Role role : roles) {
            if (role == null || role.getRolePermissions() == null || role.getRolePermissions().isEmpty()) {
                continue;
            }

            for (RolePermission permission : role.getRolePermissions()) {
                if (permission == null || permission.getModule() == null || permission.getAction() == null) {
                    continue;
                }

                putPermission(
                        merged,
                        permission.getModule(),
                        permission.getAction(),
                        permission.getRoomTypeId(),
                        permission.getAllRoomTypes(),
                        permission.getRoomTypeId(),
                        null
                );
            }
        }
    }

    private static void mergeStoreUserPermissions(
            LinkedHashMap<String, PermissionDTO> merged,
            List<StoreUserPermission> extraPermissions
    ) {
        if (extraPermissions == null || extraPermissions.isEmpty()) {
            return;
        }

        for (StoreUserPermission permission : extraPermissions) {
            if (permission == null || permission.getModule() == null || permission.getAction() == null) {
                continue;
            }

            putPermission(
                    merged,
                    permission.getModule(),
                    permission.getAction(),
                    permission.getRoomTypeId(),
                    permission.getAllRoomTypes(),
                    permission.getRoomTypeId(),
                    null
            );
        }
    }

    private static void putPermission(
            LinkedHashMap<String, PermissionDTO> merged,
            PermissionModule module,
            PermissionAction action,
            Long rawRoomTypeId,
            Boolean rawAllRoomTypes,
            Long roomTypeId,
            String roomTypeName
    ) {
        boolean isRoomStatusPermission =
                module == PermissionModule.ACCOMMODATION && action == PermissionAction.VIEW_ROOM_STATUS;
        boolean allRoomTypes = isRoomStatusPermission
                && (Boolean.TRUE.equals(rawAllRoomTypes) || rawRoomTypeId == null || rawRoomTypeId == 0L);
        long normalizedRoomTypeId = allRoomTypes || roomTypeId == null ? 0L : roomTypeId;

        if (allRoomTypes) {
            merged.entrySet().removeIf(entry -> entry.getValue().getModule() == module
                    && entry.getValue().getAction() == action);
        }

        String key = module + "|" + action + "|" + normalizedRoomTypeId;
        if (allRoomTypes && merged.containsKey(key)) {
            return;
        }
        if (!allRoomTypes && merged.containsKey(module + "|" + action + "|0")) {
            return;
        }

        PermissionDTO dto = new PermissionDTO();
        dto.setModule(module);
        dto.setAction(action);
        dto.setRoomTypeId(normalizedRoomTypeId);
        dto.setAllRoomTypes(allRoomTypes);
        dto.setRoomTypeName(roomTypeName);
        merged.putIfAbsent(key, dto);
    }

    private void replaceStoreUserExtraPermissions(StoreUser storeUser, List<PermissionDTO> extraPermissionDTOs) {
        if (storeUser == null || storeUser.getId() == null) {
            return;
        }

        List<StoreUserPermission> existingPermissions = loadStoreUserExtraPermissions(storeUser.getId());
        List<StoreUserPermission> normalizedPermissions = extraPermissionDTOs == null
                ? Collections.emptyList()
                : normalizeExtraPermissions(storeUser, extraPermissionDTOs);
        Map<String, StoreUserPermission> existingByKey = new LinkedHashMap<>();
        for (StoreUserPermission permission : existingPermissions) {
            String key = buildStoreUserPermissionKey(permission);
            if (key != null) {
                existingByKey.putIfAbsent(key, permission);
            }
        }

        Map<String, StoreUserPermission> requestedByKey = new LinkedHashMap<>();
        for (StoreUserPermission permission : normalizedPermissions) {
            String key = buildStoreUserPermissionKey(permission);
            if (key != null) {
                requestedByKey.putIfAbsent(key, permission);
            }
        }

        List<StoreUserPermission> permissionsToDelete = new ArrayList<>();
        for (Map.Entry<String, StoreUserPermission> entry : existingByKey.entrySet()) {
            if (!requestedByKey.containsKey(entry.getKey())) {
                permissionsToDelete.add(entry.getValue());
            }
        }
        if (!permissionsToDelete.isEmpty()) {
            storeUserPermissionRepository.deleteAll(permissionsToDelete);
        }

        List<StoreUserPermission> permissionsToSave = new ArrayList<>();
        for (Map.Entry<String, StoreUserPermission> entry : requestedByKey.entrySet()) {
            StoreUserPermission existing = existingByKey.get(entry.getKey());
            StoreUserPermission requested = entry.getValue();
            if (existing == null) {
                permissionsToSave.add(requested);
                continue;
            }

            if (!Objects.equals(existing.getAllRoomTypes(), requested.getAllRoomTypes())) {
                existing.setAllRoomTypes(requested.getAllRoomTypes());
                permissionsToSave.add(existing);
            }
        }

        if (permissionsToSave.isEmpty()) {
            return;
        }

        storeUserPermissionRepository.saveAll(permissionsToSave);
    }

    private static String buildStoreUserPermissionKey(StoreUserPermission permission) {
        if (permission == null || permission.getModule() == null || permission.getAction() == null) {
            return null;
        }

        Long roomTypeId = permission.getRoomTypeId();
        long normalizedRoomTypeId = roomTypeId == null ? 0L : roomTypeId;
        return permission.getModule() + "|" + permission.getAction() + "|" + normalizedRoomTypeId;
    }

    private static List<StoreUserPermission> normalizeExtraPermissions(StoreUser storeUser, List<PermissionDTO> extraPermissionDTOs) {
        LinkedHashMap<String, StoreUserPermission> deduped = new LinkedHashMap<>();
        boolean roomStatusAll = false;

        for (PermissionDTO dto : extraPermissionDTOs) {
            if (dto == null || dto.getModule() == null || dto.getAction() == null) {
                continue;
            }

            PermissionModule module = dto.getModule();
            PermissionAction action = dto.getAction();
            Long roomTypeId = dto.getRoomTypeId();
            boolean allRoomTypes = Boolean.TRUE.equals(dto.getAllRoomTypes());

            StoreUserPermission p = new StoreUserPermission(storeUser, module, action);

            if (module == PermissionModule.ACCOMMODATION && action == PermissionAction.VIEW_ROOM_STATUS) {
                if (roomStatusAll) {
                    continue;
                }
                if (allRoomTypes || roomTypeId == null || roomTypeId == 0L) {
                    roomStatusAll = true;
                    p.setRoomTypeId(0L);
                    p.setAllRoomTypes(true);
                    deduped.entrySet().removeIf(e -> e.getValue().getModule() == module && e.getValue().getAction() == action);
                    deduped.put(module + "|" + action + "|0", p);
                    continue;
                }

                p.setRoomTypeId(roomTypeId);
                p.setAllRoomTypes(false);
            } else {
                p.setRoomTypeId(0L);
                p.setAllRoomTypes(false);
            }

            String key = module + "|" + action + "|" + p.getRoomTypeId();
            deduped.putIfAbsent(key, p);
        }

        return new ArrayList<>(deduped.values());
    }
}
