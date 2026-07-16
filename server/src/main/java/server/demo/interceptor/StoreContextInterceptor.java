package server.demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import server.demo.annotation.StoreScoped;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.StoreUser;
import server.demo.exception.StoreAccessDeniedException;
import server.demo.repository.StoreUserRepository;
import server.demo.service.ChannelPriceWarmupService;

import java.io.IOException;
import java.util.Optional;

/**
 * 根据请求头 X-Store-Id 建立门店上下文并做权限校验。
 */
@Component
public class StoreContextInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(StoreContextInterceptor.class);

    private final StoreUserRepository storeUserRepository;
    private final ChannelPriceWarmupService channelPriceWarmupService;

    public StoreContextInterceptor(StoreUserRepository storeUserRepository, ChannelPriceWarmupService channelPriceWarmupService) {
        this.storeUserRepository = storeUserRepository;
        this.channelPriceWarmupService = channelPriceWarmupService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        StoreContextHolder.clear();

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        StoreScoped storeScoped = resolveAnnotation(handlerMethod);
        if (storeScoped == null) {
            return true;
        }

        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr == null) {
            return writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "未获取到用户信息");
        }
        Long userId = (Long) userIdAttr;

        String storeIdHeader = request.getHeader("X-Store-Id");
        if ((storeIdHeader == null || storeIdHeader.isBlank())) {
            if (storeScoped.required()) {
                return writeError(response, HttpServletResponse.SC_BAD_REQUEST, "缺少门店标识");
            }
            return true;
        }

        Long storeId;
        try {
            storeId = Long.parseLong(storeIdHeader);
        } catch (NumberFormatException ex) {
            return writeError(response, HttpServletResponse.SC_BAD_REQUEST, "门店标识格式错误");
        }

        Optional<StoreUser> storeUserOpt = storeUserRepository.findByStoreIdAndUserId(storeId, userId);
        StoreUser storeUser = storeUserOpt.orElseThrow(() -> new StoreAccessDeniedException("无权访问该门店"));

        StoreContextHolder.setContext(new StoreContext(userId, storeId, storeUser.getRole()));
        request.setAttribute("storeId", storeId);

        // 方案A：第一次收到带 X-Store-Id 的 StoreScoped 请求时自动预热渠道价格（仅当未来区间内还没有任何 channel_prices 时才生成）
        if (storeScoped.warmupChannelPrices() && !"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            try {
                channelPriceWarmupService.warmupIfNeeded(storeId);
            } catch (Exception ex) {
                logger.warn("[StoreContext] channel price warmup failed. storeId={}, message={}", storeId, ex.getMessage(), ex);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        StoreContextHolder.clear();
    }

    private StoreScoped resolveAnnotation(HandlerMethod handlerMethod) {
        StoreScoped methodAnnotation = handlerMethod.getMethodAnnotation(StoreScoped.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(StoreScoped.class);
    }

    private boolean writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
        return false;
    }
}
