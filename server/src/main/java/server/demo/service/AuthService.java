package server.demo.service;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import server.demo.dto.StoreDTO;
import server.demo.dto.auth.*;
import server.demo.entity.Cleaner;
import server.demo.entity.StoreUser;
import server.demo.entity.User;
import server.demo.repository.StoreUserRepository;
import server.demo.repository.UserRepository;
import server.demo.util.JwtUtil;
import server.demo.util.RedisUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 认证服务
 * 负责用户注册、登录、登出等业务逻辑
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private StoreUserRepository storeUserRepository;

    @Autowired
    private CleanerIdentityService cleanerIdentityService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final Set<String> ALLOWED_GENDERS = Set.of("male", "female", "private");

    /**
     * 发送验证码
     *
     * @param request 发送验证码请求
     */
    public void sendVerificationCode(SendVerificationCodeRequest request) {
        String email = request.getEmail();
        String type = request.getType();

        logger.info("收到验证码发送请求: email={}, type={}", email, type);

        // 检查是否在冷却时间内
        if (redisUtil.isInCooldown(email, type)) {
            logger.warn("验证码发送失败: 操作频繁 - email={}, type={}", email, type);
            throw new RuntimeException("操作频繁,请稍后再试");
        }

        // 对于注册,检查邮箱是否已存在
        if ("register".equals(type) && userRepository.existsByEmail(email)) {
            logger.warn("验证码发送失败: 邮箱已注册 - email={}", email);
            throw new RuntimeException("该邮箱已注册");
        }

        // 对于登录和重置密码,检查用户是否存在
        if (("login".equals(type) || "reset_password".equals(type)) && !userRepository.existsByEmail(email)) {
            logger.warn("验证码发送失败: 邮箱未注册 - email={}", email);
            throw new RuntimeException("该邮箱未注册");
        }

        // 生成验证码
        String code = redisUtil.generateVerificationCode();
        logger.info("生成验证码: email={}, code={}", email, code);

        // 存储到Redis(同时设置验证码和冷却时间)
        redisUtil.saveVerificationCode(email, code, type);
        logger.info("验证码已存储到Redis: email={}, type={}", email, type);

        // 发送邮件
        try {
            emailService.sendVerificationCode(email, code, type);
            logger.info("验证码邮件发送成功: email={}, code={}", email, code);
        } catch (MessagingException e) {
            logger.error("验证码邮件发送失败: email={}, error={}", email, e.getMessage());
            throw new RuntimeException("验证码发送失败,请稍后重试");
        }
    }

    /**
     * 注册
     *
     * @param request 注册请求
     * @return 用户DTO
     */
    public UserDTO register(RegisterRequest request) {
        // 验证验证码
        if (!redisUtil.verifyCode(request.getEmail(), request.getVerificationCode(), "register")) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("该邮箱已注册");
        }

        // 创建用户
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getEmail().split("@")[0]); // 使用邮箱前缀作为用户名
        user.setName(request.getEmail().split("@")[0]); // 设置name字段为邮箱前缀
        user.setPassword(passwordEncoder.encode(request.getPassword())); // 密码加密
        user.setNickname(request.getEmail().split("@")[0]); // 默认昵称为邮箱前缀

        // 保存用户
        User savedUser = userRepository.save(user);

        return new UserDTO(savedUser);
    }

    /**
     * 密码登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    public LoginResponse loginByPassword(LoginByPasswordRequest request) {
        // 查询用户 - 仅查询User表中的管理员
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("邮箱或密码错误"));

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("邮箱或密码错误");
        }

        // 生成token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        // 返回登录响应 - 管理员的isCleaner固定为false
        UserDTO userDTO = new UserDTO(user);
        userDTO.setIsCleaner(false);

        // 获取用户的门店列表
        List<StoreDTO> stores = storeService.getUserStores(user.getId());

        return buildLoginResponse(token, userDTO, stores);
    }

    /**
     * 验证码登录
     *
     * @param request 验证码登录请求
     * @return 登录响应
     */
    public LoginResponse loginByCode(LoginByCodeRequest request) {
        // 验证验证码
        if (!redisUtil.verifyCode(request.getEmail(), request.getVerificationCode(), "login")) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 查询用户 - 仅查询User表中的管理员
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("该邮箱未注册"));

        // 生成token
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        // 返回登录响应 - 管理员的isCleaner固定为false
        UserDTO userDTO = new UserDTO(user);
        userDTO.setIsCleaner(false);

        // 获取用户的门店列表
        List<StoreDTO> stores = storeService.getUserStores(user.getId());

        return buildLoginResponse(token, userDTO, stores);
    }

    /**
     * 为已完成认证的用户构建统一登录响应，供旧 cleaner 兼容端点复用目标解析逻辑。
     */
    public LoginResponse buildAuthenticatedLoginResponse(Long userId, String token) {
        if (userId == null) {
            throw new RuntimeException("用户不存在");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        UserDTO userDTO = new UserDTO(user);
        userDTO.setIsCleaner(false);
        List<StoreDTO> stores = storeService.getUserStores(user.getId());
        return buildLoginResponse(token, userDTO, stores);
    }

    /**
     * 更新个人资料
     *
     * @param userId 用户ID
     * @param request 更新请求
     * @return 用户DTO
     */
    public UserDTO updateProfile(Long userId, UpdateProfileRequest request) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getNickname() != null) {
            String nickname = request.getNickname().trim();
            if (nickname.isEmpty()) {
                throw new RuntimeException("昵称不能为空");
            }
            user.setNickname(nickname);
        }

        if (request.getAvatar() != null) {
            String avatar = request.getAvatar().trim();
            user.setAvatar(avatar.isEmpty() ? null : avatar);
        }

        if (request.getGender() != null) {
            String gender = request.getGender().trim();
            if (gender.isEmpty()) {
                user.setGender(null);
            } else if (!ALLOWED_GENDERS.contains(gender)) {
                throw new RuntimeException("性别参数不合法");
            } else {
                user.setGender(gender);
            }
        }

        User savedUser = userRepository.save(user);
        return new UserDTO(savedUser);
    }

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param token 当前token
     * @param request 修改密码请求
     */
    public void changePassword(Long userId, String token, ChangePasswordRequest request) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("当前密码不正确");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new RuntimeException("新密码不能与当前密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        if (token != null && !token.isBlank()) {
            Date expirationDate = jwtUtil.getExpirationDate(token);
            long expirationTime = expirationDate != null
                    ? expirationDate.getTime()
                    : System.currentTimeMillis() + 86400000;
            redisUtil.addTokenToBlacklist(token, expirationTime);
        }
    }

    /**
     * 登出
     *
     * @param token JWT token
     */
    public void logout(String token) {
        try {
            if (token == null || token.isBlank()) {
                return;
            }
            Date expirationDate = jwtUtil.getExpirationDate(token);
            long expirationTime = expirationDate != null
                    ? expirationDate.getTime()
                    : System.currentTimeMillis() + 86400000;
            redisUtil.addTokenToBlacklist(token, expirationTime);
        } catch (Exception e) {
            throw new RuntimeException("登出失败");
        }
    }

    /**
     * 获取当前用户信息
     *
     * @param userId 用户ID
     * @return 用户DTO
     */
    public UserDTO getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return new UserDTO(user);
    }

    /**
     * 重置密码（忘记密码）
     *
     * @param request 重置密码请求
     */
    public void resetPassword(ResetPasswordRequest request) {
        // 验证验证码
        if (!redisUtil.verifyCode(request.getEmail(), request.getVerificationCode(), "reset_password")) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 查询用户（必须已注册）
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("该邮箱未注册，请先注册"));

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private LoginResponse buildLoginResponse(String token, UserDTO userDTO, List<StoreDTO> stores) {
        LoginResponse response = new LoginResponse(token, userDTO, stores);
        applyLoginTarget(response, stores);
        return response;
    }

    private void applyLoginTarget(LoginResponse response, List<StoreDTO> stores) {
        response.setLoginTarget(LoginTarget.PMS);
        response.setCleaner(null);
        response.setCurrentStore(null);
        response.setTargetStoreId(null);

        if (response.getUser() == null || response.getUser().getId() == null) {
            return;
        }

        List<CleanerTargetCandidate> candidates = resolveCleanerTargetCandidates(response.getUser().getId());
        if (candidates.isEmpty()) {
            return;
        }

        sortCleanerCandidates(candidates);
        CleanerTargetCandidate target = candidates.get(0);
        response.setLoginTarget(LoginTarget.CLEANER);
        response.setCleaner(new CleanerDTO(target.cleaner));
        response.setTargetStoreId(target.storeId);
        response.setCurrentStore(findStoreDto(stores, target.storeId));
    }

    private List<CleanerTargetCandidate> resolveCleanerTargetCandidates(Long userId) {
        List<CleanerTargetCandidate> candidates = new ArrayList<>();
        List<StoreUser> storeUsers = storeUserRepository.findByUserIdWithStoreAndRoles(userId);
        if (storeUsers == null || storeUsers.isEmpty()) {
            return candidates;
        }

        for (StoreUser storeUser : storeUsers) {
            if (!isUsableActiveStoreUser(storeUser)) {
                continue;
            }

            Long storeId = storeUser.getStore().getId();
            Cleaner cleaner = cleanerIdentityService.findCleanerByUserIdAndStoreId(userId, storeId)
                    .filter(candidate -> candidate != null && Boolean.TRUE.equals(candidate.getIsActive()))
                    .orElse(null);
            if (cleaner == null) {
                continue;
            }
            candidates.add(new CleanerTargetCandidate(storeId, cleaner));
        }

        return candidates;
    }

    private boolean isUsableActiveStoreUser(StoreUser storeUser) {
        return storeUser != null
                && storeUser.getId() != null
                && Boolean.TRUE.equals(storeUser.getIsActive())
                && storeUser.getStore() != null
                && storeUser.getStore().getId() != null;
    }

    private StoreDTO findStoreDto(List<StoreDTO> stores, Long storeId) {
        if (stores == null || storeId == null) {
            return null;
        }

        for (StoreDTO store : stores) {
            if (store != null && storeId.equals(store.getId())) {
                return store;
            }
        }
        return null;
    }

    private void sortCleanerCandidates(List<CleanerTargetCandidate> candidates) {
        candidates.sort((left, right) -> {
            int storeCompare = compareLongValues(left.storeId, right.storeId);
            if (storeCompare != 0) {
                return storeCompare;
            }
            return compareLongValues(left.cleaner.getId(), right.cleaner.getId());
        });
    }

    private int compareLongValues(Long left, Long right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private static class CleanerTargetCandidate {
        private final Long storeId;
        private final Cleaner cleaner;

        private CleanerTargetCandidate(Long storeId, Cleaner cleaner) {
            this.storeId = storeId;
            this.cleaner = cleaner;
        }
    }
}
