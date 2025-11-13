package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.CreateStoreRequest;
import server.demo.dto.StoreDTO;
import server.demo.dto.StorePolicyDTO;
import server.demo.entity.Store;
import server.demo.entity.StorePolicy;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.repository.StoreRepository;
import server.demo.repository.StorePolicyRepository;
import server.demo.repository.StoreUserRepository;
import server.demo.repository.UserRepository;

import java.util.List;
import java.util.Optional;
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
}
