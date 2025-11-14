package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.*;
import server.demo.entity.*;
import server.demo.repository.*;

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

        Store store = new Store();
        store.setUserId(userId);
        store.setName(request.getName());
        store.setPhone(request.getPhone());
        store.setType(request.getType());
        store.setTimezone(request.getTimezone());
        store.setManager(request.getManager());
        store.setOwnerEmail(user.getEmail());
        store.setCountry(request.getCountry());
        store.setCity(request.getCity());
        store.setState(request.getState());
        store.setAddress(request.getAddress());

        Store savedStore = storeRepository.save(store);

        StoreUser storeUser = new StoreUser(savedStore, user, "owner");
        storeUserRepository.save(storeUser);

        return convertToDTO(savedStore, "owner");
    }

    public Optional<StoreDTO> getStoreById(Long storeId, Long userId) {
        Optional<StoreUser> storeUser = storeUserRepository.findByStoreIdAndUserId(storeId, userId);
        return storeUser.map(su -> convertToDTO(su.getStore(), su.getRole()));
    }

    @Transactional
    public StoreDTO updateStore(Long storeId, Long userId, CreateStoreRequest request) {
        StoreUser storeUser = storeUserRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new RuntimeException("No permission"));

        if (!"owner".equals(storeUser.getRole()) && !"admin".equals(storeUser.getRole())) {
            throw new RuntimeException("No permission to update store");
        }

        Store store = storeUser.getStore();
        store.setName(request.getName());
        store.setPhone(request.getPhone());
        store.setType(request.getType());
        store.setTimezone(request.getTimezone());
        store.setManager(request.getManager());
        store.setCountry(request.getCountry());
        store.setCity(request.getCity());
        store.setState(request.getState());
        store.setAddress(request.getAddress());

        Store updatedStore = storeRepository.save(store);
        return convertToDTO(updatedStore, storeUser.getRole());
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
     * 添加门店成员（支持权限角色）
     */
    @Transactional
    public StoreUserDTO addStoreMember(Long storeId, Long operatorUserId, AddStoreMemberRequest request) {
        // 验证操作者权限
        StoreUser operator = storeUserRepository.findByStoreIdAndUserId(storeId, operatorUserId)
                .orElseThrow(() -> new RuntimeException("无权限"));

        if (!"owner".equals(operator.getRole()) && !"admin".equals(operator.getRole())) {
            throw new RuntimeException("只有管理员可以添加成员");
        }

        // 查找被邀请用户
        User invitedUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("用户不存在，请先让该用户注册账号"));

        // 检查是否已是成员
        if (storeUserRepository.existsByStoreIdAndUserId(storeId, invitedUser.getId())) {
            throw new RuntimeException("该用户已是门店成员");
        }

        // 创建门店用户关联
        StoreUser storeUser = new StoreUser(operator.getStore(), invitedUser, request.getRole());
        storeUser.setInvitedBy(operatorUserId);

        // 分配权限角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (Long roleId : request.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new RuntimeException("角色不存在: " + roleId));

                // 验证角色属于当前门店
                if (!storeId.equals(role.getStoreId())) {
                    throw new RuntimeException("角色不属于当前门店");
                }

                roles.add(role);
            }
            storeUser.setRoles(roles);
        }

        StoreUser savedStoreUser = storeUserRepository.save(storeUser);
        return convertToStoreUserDTO(savedStoreUser);
    }

    /**
     * 更新门店成员权限
     */
    @Transactional
    public StoreUserDTO updateStoreMemberPermission(Long storeId, Long operatorUserId, Long targetUserId,
                                                     UpdateStoreMemberPermissionRequest request) {
        // 验证操作者权限
        StoreUser operator = storeUserRepository.findByStoreIdAndUserId(storeId, operatorUserId)
                .orElseThrow(() -> new RuntimeException("无权限"));

        if (!"owner".equals(operator.getRole()) && !"admin".equals(operator.getRole())) {
            throw new RuntimeException("只有管理员可以修改成员权限");
        }

        // 查找目标成员
        StoreUser target = storeUserRepository.findByStoreIdAndUserId(storeId, targetUserId)
                .orElseThrow(() -> new RuntimeException("目标用户不是门店成员"));

        // 不能修改owner权限
        if ("owner".equals(target.getRole()) && !"owner".equals(operator.getRole())) {
            throw new RuntimeException("只有所有者可以修改所有者权限");
        }

        // 更新基础角色
        if (request.getRole() != null) {
            target.setRole(request.getRole());
        }

        // 更新权限角色
        if (request.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>();
            for (Long roleId : request.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new RuntimeException("角色不存在: " + roleId));

                // 验证角色属于当前门店
                if (!storeId.equals(role.getStoreId())) {
                    throw new RuntimeException("角色不属于当前门店");
                }

                roles.add(role);
            }
            target.setRoles(roles);
        }

        // 更新激活状态
        if (request.getIsActive() != null) {
            target.setIsActive(request.getIsActive());
        }

        StoreUser updatedStoreUser = storeUserRepository.save(target);
        return convertToStoreUserDTO(updatedStoreUser);
    }

    /**
     * 获取门店成员详细信息
     */
    public StoreUserDTO getStoreMemberDetail(Long storeId, Long userId) {
        StoreUser storeUser = storeUserRepository.findByStoreIdAndUserId(storeId, userId)
                .orElseThrow(() -> new RuntimeException("成员不存在"));
        return convertToStoreUserDTO(storeUser);
    }

    /**
     * 获取门店成员列表（返回DTO）
     */
    public List<StoreUserDTO> getStoreMembersDTO(Long storeId) {
        List<StoreUser> storeUsers = storeUserRepository.findActiveUsersByStoreId(storeId);
        return storeUsers.stream()
                .map(this::convertToStoreUserDTO)
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

    private StoreDTO convertToDTO(Store store, String userRole) {
        StoreDTO dto = new StoreDTO();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setPhone(store.getPhone());
        dto.setType(store.getType());
        dto.setTimezone(store.getTimezone());
        dto.setManager(store.getManager());
        dto.setOwnerEmail(store.getOwnerEmail());
        dto.setAddress(store.getAddress());
        dto.setCity(store.getCity());
        dto.setState(store.getState());
        dto.setCountry(store.getCountry());
        dto.setCurrency(store.getCurrency());
        dto.setLogo(store.getLogo());
        dto.setDescription(store.getDescription());
        dto.setEmail(store.getEmail());
        dto.setWechat(store.getWechat());
        dto.setWhatsapp(store.getWhatsapp());
        dto.setLine(store.getLine());
        dto.setLanguage(store.getLanguage());
        dto.setUserRole(userRole);
        dto.setCreatedAt(store.getCreatedAt());
        dto.setUpdatedAt(store.getUpdatedAt());
        return dto;
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

        return dto;
    }
}
