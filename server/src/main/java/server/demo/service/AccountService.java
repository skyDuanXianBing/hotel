package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.dto.*;
import server.demo.entity.Role;
import server.demo.entity.User;
import server.demo.repository.RoleRepository;
import server.demo.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 账号管理服务
 */
@Service
public class AccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RolePermissionService rolePermissionService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "password123"; // 默认密码

    /**
     * 获取所有账号
     */
    public List<AccountDTO> getAllAccounts() {
        List<User> users = userRepository.findAll();
        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 根据筛选条件查询账号
     */
    public List<AccountDTO> getAccountsWithFilters(String keyword, Long roleId, Boolean isActive) {
        List<User> users;

        if (keyword != null || roleId != null || isActive != null) {
            users = userRepository.findAccountsWithFilters(keyword, roleId, isActive);
        } else {
            users = userRepository.findAll();
        }

        return users.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 根据ID获取账号详情
     */
    public AccountDTO getAccountById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        return convertToDTO(user);
    }

    /**
     * 创建账号
     */
    @Transactional
    public AccountDTO createAccount(CreateAccountRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // 设置密码(如果未提供则使用默认密码)
        String password = request.getPassword() != null ? request.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(password));

        user.setIsActive(true);

        // 设置角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
    }

    /**
     * 更新账号
     */
    @Transactional
    public AccountDTO updateAccount(Long id, UpdateAccountRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        // 更新基本信息
        if (request.getName() != null) {
            user.setName(request.getName());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("邮箱已存在");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        // 更新角色
        if (request.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            user.setRoles(roles);
        }

        User updatedUser = userRepository.save(user);

        return convertToDTO(updatedUser);
    }

    /**
     * 删除账号
     */
    @Transactional
    public void deleteAccount(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("账号不存在");
        }
        userRepository.deleteById(id);
    }

    /**
     * 批量删除账号
     */
    @Transactional
    public void batchDeleteAccounts(List<Long> accountIds) {
        userRepository.deleteAllById(accountIds);
    }

    /**
     * 更新账号状态
     */
    @Transactional
    public AccountDTO updateAccountStatus(Long id, Boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        user.setIsActive(isActive);
        User updatedUser = userRepository.save(user);

        return convertToDTO(updatedUser);
    }

    /**
     * 批量更新账号状态
     */
    @Transactional
    public void batchUpdateStatus(BatchUpdateStatusRequest request) {
        List<User> users = userRepository.findAllById(request.getAccountIds());

        for (User user : users) {
            user.setIsActive(request.getIsActive());
        }

        userRepository.saveAll(users);
    }

    /**
     * 批量调整角色
     */
    @Transactional
    public void batchUpdateRoles(BatchUpdateRolesRequest request) {
        List<User> users = userRepository.findAllById(request.getAccountIds());
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));

        for (User user : users) {
            user.setRoles(roles);
        }

        userRepository.saveAll(users);
    }

    /**
     * 转换为DTO
     */
    private AccountDTO convertToDTO(User user) {
        AccountDTO dto = new AccountDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setGender(user.getGender());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        // 转换角色
        if (user.getRoles() != null) {
            List<RoleDTO> roleDTOs = user.getRoles().stream()
                    .map(role -> {
                        RoleDTO roleDTO = new RoleDTO();
                        roleDTO.setId(role.getId());
                        roleDTO.setName(role.getName());
                        roleDTO.setDescription(role.getDescription());
                        return roleDTO;
                    })
                    .collect(Collectors.toList());
            dto.setRoles(roleDTOs);
        }

        return dto;
    }
}
