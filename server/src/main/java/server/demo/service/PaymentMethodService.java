package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContextHolder;
import server.demo.dto.PaymentMethodDTO;
import server.demo.dto.PaymentMethodOrderRequest;
import server.demo.dto.UpsertPaymentMethodRequest;
import server.demo.entity.PaymentMethod;
import server.demo.repository.PaymentMethodRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class PaymentMethodService {

    private static final List<String> DEFAULT_METHOD_NAMES = List.of(
            "携程代收",
            "飞猪代收",
            "美团酒店代收",
            "美团民宿代收",
            "爱彼迎代收",
            "Booking.com代收",
            "小猪民宿代收",
            "木鸟民宿代收",
            "Agoda代收",
            "Expedia代收",
            "小红书代收",
            "途家代收",
            "抖音代收",
            "现金",
            "支付宝",
            "微信"
    );

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    public List<PaymentMethodDTO> getAll() {
        Long storeId = getCurrentStoreId();
        ensureDefaultMethodsIfNeeded(storeId);
        return toDTOList(paymentMethodRepository.findByStoreIdOrderByDisplayOrderAscIdAsc(storeId));
    }

    public List<PaymentMethodDTO> getEnabled() {
        Long storeId = getCurrentStoreId();
        ensureDefaultMethodsIfNeeded(storeId);
        return toDTOList(paymentMethodRepository.findByStoreIdAndEnabledTrueOrderByDisplayOrderAscIdAsc(storeId));
    }

    @Transactional
    public PaymentMethodDTO create(UpsertPaymentMethodRequest request) {
        Long storeId = getCurrentStoreId();
        String normalizedName = normalizeName(request.getName());
        if (paymentMethodRepository.existsByStoreIdAndNameIgnoreCase(storeId, normalizedName)) {
            throw new RuntimeException("收款方式名称已存在");
        }

        List<PaymentMethod> currentMethods = paymentMethodRepository.findByStoreIdOrderByDisplayOrderAscIdAsc(storeId);
        int nextOrder = currentMethods.size();

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setName(normalizedName);
        paymentMethod.setDisplayOrder(nextOrder);
        paymentMethod.setEnabled(request.getEnabled() == null || request.getEnabled());

        return toDTO(paymentMethodRepository.save(paymentMethod));
    }

    @Transactional
    public PaymentMethodDTO update(Long id, UpsertPaymentMethodRequest request) {
        Long storeId = getCurrentStoreId();
        PaymentMethod paymentMethod = paymentMethodRepository.findByStoreIdAndId(storeId, id)
                .orElseThrow(() -> new RuntimeException("收款方式不存在"));

        String normalizedName = normalizeName(request.getName());
        paymentMethodRepository.findByStoreIdAndNameIgnoreCase(storeId, normalizedName)
                .ifPresent(existing -> {
                    if (!Objects.equals(existing.getId(), id)) {
                        throw new RuntimeException("收款方式名称已存在");
                    }
                });

        paymentMethod.setName(normalizedName);
        if (request.getEnabled() != null) {
            paymentMethod.setEnabled(request.getEnabled());
        }

        return toDTO(paymentMethodRepository.save(paymentMethod));
    }

    @Transactional
    public PaymentMethodDTO updateEnabled(Long id, Boolean enabled) {
        Long storeId = getCurrentStoreId();
        PaymentMethod paymentMethod = paymentMethodRepository.findByStoreIdAndId(storeId, id)
                .orElseThrow(() -> new RuntimeException("收款方式不存在"));
        paymentMethod.setEnabled(Boolean.TRUE.equals(enabled));
        return toDTO(paymentMethodRepository.save(paymentMethod));
    }

    @Transactional
    public void delete(Long id) {
        Long storeId = getCurrentStoreId();
        PaymentMethod paymentMethod = paymentMethodRepository.findByStoreIdAndId(storeId, id)
                .orElseThrow(() -> new RuntimeException("收款方式不存在"));
        paymentMethodRepository.delete(paymentMethod);
        reindexDisplayOrder(storeId);
    }

    @Transactional
    public List<PaymentMethodDTO> updateOrder(List<PaymentMethodOrderRequest> requests) {
        Long storeId = getCurrentStoreId();
        if (requests == null || requests.isEmpty()) {
            return getAll();
        }

        List<PaymentMethod> existingMethods = paymentMethodRepository.findByStoreIdOrderByDisplayOrderAscIdAsc(storeId);
        if (existingMethods.isEmpty()) {
            return List.of();
        }

        for (PaymentMethodOrderRequest request : requests) {
            PaymentMethod method = paymentMethodRepository.findByStoreIdAndId(storeId, request.getId())
                    .orElseThrow(() -> new RuntimeException("收款方式不存在: " + request.getId()));
            method.setDisplayOrder(request.getDisplayOrder());
            paymentMethodRepository.save(method);
        }

        reindexDisplayOrder(storeId);
        return getAll();
    }

    @Transactional
    protected void ensureDefaultMethodsIfNeeded(Long storeId) {
        if (paymentMethodRepository.countByStoreId(storeId) > 0) {
            return;
        }

        List<PaymentMethod> defaults = new ArrayList<>();
        for (int i = 0; i < DEFAULT_METHOD_NAMES.size(); i++) {
            PaymentMethod method = new PaymentMethod();
            method.setName(DEFAULT_METHOD_NAMES.get(i));
            method.setDisplayOrder(i);
            method.setEnabled(true);
            method.setStoreId(storeId);
            defaults.add(method);
        }
        paymentMethodRepository.saveAll(defaults);
    }

    @Transactional
    protected void reindexDisplayOrder(Long storeId) {
        List<PaymentMethod> methods = paymentMethodRepository.findByStoreIdOrderByDisplayOrderAscIdAsc(storeId);
        methods.sort(Comparator
                .comparing((PaymentMethod item) -> item.getDisplayOrder() == null ? Integer.MAX_VALUE : item.getDisplayOrder())
                .thenComparing(item -> item.getId() == null ? Long.MAX_VALUE : item.getId()));

        for (int i = 0; i < methods.size(); i++) {
            PaymentMethod method = methods.get(i);
            method.setDisplayOrder(i);
        }
        paymentMethodRepository.saveAll(methods);
    }

    private Long getCurrentStoreId() {
        if (StoreContextHolder.getContext() == null || StoreContextHolder.getContext().getStoreId() == null) {
            throw new RuntimeException("缺少门店上下文");
        }
        return StoreContextHolder.getContext().getStoreId();
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw new RuntimeException("收款方式名称不能为空");
        }
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            throw new RuntimeException("收款方式名称不能为空");
        }
        return normalized;
    }

    private List<PaymentMethodDTO> toDTOList(List<PaymentMethod> entities) {
        List<PaymentMethodDTO> result = new ArrayList<>();
        for (PaymentMethod entity : entities) {
            result.add(toDTO(entity));
        }
        return result;
    }

    private PaymentMethodDTO toDTO(PaymentMethod entity) {
        PaymentMethodDTO dto = new PaymentMethodDTO();
        dto.setId(entity.getId());
        dto.setStoreId(entity.getStoreId());
        dto.setName(entity.getName());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setEnabled(entity.getEnabled());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
