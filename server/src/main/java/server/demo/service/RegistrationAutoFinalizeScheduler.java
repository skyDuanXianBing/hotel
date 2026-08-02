package server.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import server.demo.entity.RegistrationForm;
import server.demo.entity.RegistrationReviewSettings;
import server.demo.entity.Store;
import server.demo.repository.RegistrationFormRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * 登记表两段式审查：自动终审定时任务。
 *
 * <p>门店将表单初审通过（REVIEWED）后，系统在门店本地日期到达
 * 入住日 - leadDays 时，把表单翻成 APPROVED 并向客人发送预设终审消息。
 * 每 30 分钟滚动扫描一次；条件更新保证重复触发/并发触发不会重复发消息。
 */
@Service
public class RegistrationAutoFinalizeScheduler {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationAutoFinalizeScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final RegistrationFormRepository registrationFormRepository;
    private final StoreRepository storeRepository;
    private final RegistrationReviewSettingsService registrationReviewSettingsService;
    private final RegistrationAdminService registrationAdminService;
    private final Clock clock;

    public RegistrationAutoFinalizeScheduler(
            RegistrationFormRepository registrationFormRepository,
            StoreRepository storeRepository,
            RegistrationReviewSettingsService registrationReviewSettingsService,
            RegistrationAdminService registrationAdminService,
            Clock clock
    ) {
        this.registrationFormRepository = registrationFormRepository;
        this.storeRepository = storeRepository;
        this.registrationReviewSettingsService = registrationReviewSettingsService;
        this.registrationAdminService = registrationAdminService;
        this.clock = clock;
    }

    @Scheduled(initialDelay = 120_000, fixedDelay = 1_800_000)
    public void tick() {
        try {
            dispatchAllStores();
        } catch (Exception e) {
            logger.error("[RegistrationAutoFinalize] tick failed. err={}", e.getMessage(), e);
        }
    }

    private void dispatchAllStores() {
        List<Store> stores = storeRepository.findAll();
        for (Store store : stores) {
            if (store == null || store.getId() == null) {
                continue;
            }
            try {
                dispatchStore(store);
            } catch (Exception e) {
                logger.warn("[RegistrationAutoFinalize] dispatch store failed. storeId={}, err={}",
                        store.getId(), e.getMessage(), e);
            }
        }
    }

    private void dispatchStore(Store store) {
        RegistrationReviewSettings settings = registrationReviewSettingsService.getEffective(store.getId());
        if (!settings.isAutoFinalizeEnabled()) {
            return;
        }
        ZoneId zone = StoreTimeZoneUtil.resolveZoneId(store);
        LocalDate today = LocalDate.ofInstant(clock.instant(), zone);
        LocalDate thresholdDate = today.plusDays(settings.effectiveLeadDays());

        List<RegistrationForm> dueForms = registrationFormRepository.findDueReviewedForFinalize(
                store.getId(), thresholdDate, PageRequest.of(0, BATCH_SIZE));
        for (RegistrationForm form : dueForms) {
            if (form == null || form.getId() == null) {
                continue;
            }
            try {
                registrationAdminService.autoFinalizeForm(store.getId(), form.getId(), settings);
            } catch (Exception e) {
                logger.warn("[RegistrationAutoFinalize] finalize form failed. storeId={}, formId={}, err={}",
                        store.getId(), form.getId(), e.getMessage(), e);
            }
        }
    }
}
