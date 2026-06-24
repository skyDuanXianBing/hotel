package server.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.SmartLockDevice;
import server.demo.entity.SmartLockIntegration;
import server.demo.entity.SmartLockPasscodeRecord;
import server.demo.entity.SmartLockTask;
import server.demo.entity.Store;
import server.demo.enums.SmartLockPasscodeStatus;
import server.demo.enums.SmartLockProvider;
import server.demo.enums.SmartLockTaskStatus;
import server.demo.enums.SmartLockTaskType;
import server.demo.repository.SmartLockPasscodeRecordRepository;
import server.demo.repository.SmartLockTaskRepository;
import server.demo.repository.StoreRepository;
import server.demo.util.SmartLockCredentialCrypto;
import server.demo.util.SmartLockMaskingUtils;
import server.demo.util.StoreContextUtils;
import server.demo.util.StoreTimeZoneUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
public class SmartLockPasscodeReconciliationService {
    private static final Logger logger =
            LoggerFactory.getLogger(SmartLockPasscodeReconciliationService.class);
    private static final int PASSCODE_RECONCILE_MAX_BATCH_SIZE = 100;
    private static final String SWITCHBOT_KEYLIST_SYNCED_MESSAGE =
            "SwitchBot keyList 已同步门锁密码";
    private static final String SWITCHBOT_KEYLIST_DELETE_SYNCED_MESSAGE =
            "SwitchBot keyList 已确认远端密码删除";

    private final SmartLockPasscodeRecordRepository passcodeRepository;
    private final SmartLockTaskRepository taskRepository;
    private final SmartLockProviderClientRegistry providerRegistry;
    private final SmartLockCredentialCrypto credentialCrypto;
    private final ObjectMapper objectMapper;
    private final StoreRepository storeRepository;
    private final Clock clock;

    public SmartLockPasscodeReconciliationService(
            SmartLockPasscodeRecordRepository passcodeRepository,
            SmartLockTaskRepository taskRepository,
            SmartLockProviderClientRegistry providerRegistry,
            SmartLockCredentialCrypto credentialCrypto,
            ObjectMapper objectMapper,
            StoreRepository storeRepository,
            Clock clock
    ) {
        this.passcodeRepository = passcodeRepository;
        this.taskRepository = taskRepository;
        this.providerRegistry = providerRegistry;
        this.credentialCrypto = credentialCrypto;
        this.objectMapper = objectMapper;
        this.storeRepository = storeRepository;
        this.clock = clock;
    }

    @Transactional
    public ReconciliationSummary reconcilePendingSwitchBotPasscodesForCurrentStore(
            int batchSize,
            long timeoutMinutes
    ) {
        Long storeId = StoreContextUtils.requireStoreId();
        int effectiveBatchSize = Math.max(1, Math.min(batchSize, PASSCODE_RECONCILE_MAX_BATCH_SIZE));
        List<SmartLockPasscodeRecord> records = passcodeRepository.findSwitchBotReconciliationCandidates(
                storeId,
                SmartLockProvider.SWITCHBOT,
                SmartLockPasscodeStatus.PENDING,
                SmartLockPasscodeStatus.DELETE_PENDING,
                PageRequest.of(0, effectiveBatchSize)
        );
        int changed = 0;
        int timedOut = 0;
        int failed = 0;
        for (SmartLockPasscodeRecord record : records) {
            ReconciliationOutcome outcome = reconcileSwitchBotPasscodeRecord(storeId, record, timeoutMinutes);
            if (outcome.changed()) {
                changed += 1;
            }
            if (outcome.timedOut()) {
                timedOut += 1;
            }
            if (outcome.failed()) {
                failed += 1;
            }
        }
        return new ReconciliationSummary(records.size(), changed, timedOut, failed);
    }

    @Transactional
    public void reconcileSwitchBotPasscodeRecords(
            Long storeId,
            List<SmartLockPasscodeRecord> records,
            long timeoutMinutes
    ) {
        for (SmartLockPasscodeRecord record : records) {
            reconcileSwitchBotPasscodeRecord(storeId, record, timeoutMinutes);
        }
    }

    @Transactional
    public ReconciliationOutcome reconcileSwitchBotPendingPasscodeTask(
            SmartLockTask task,
            long timeoutMinutes
    ) {
        if (task == null || task.getProvider() != SmartLockProvider.SWITCHBOT) {
            return ReconciliationOutcome.skipped();
        }
        return reconcileSwitchBotPasscodeRecord(task.getStoreId(), task.getPasscodeRecord(), timeoutMinutes);
    }

    @Transactional
    public void completeMatchingCreateTasksWithoutProviderTaskId(
            SmartLockPasscodeRecord record,
            SmartLockTaskStatus status,
            String message
    ) {
        completeMatchingPasscodeTasksWithoutProviderTaskId(
                record,
                SmartLockTaskType.CREATE_PASSCODE,
                status,
                message
        );
    }

    private ReconciliationOutcome reconcileSwitchBotPasscodeRecord(
            Long storeId,
            SmartLockPasscodeRecord record,
            long timeoutMinutes
    ) {
        if (!shouldReconcileSwitchBotPasscode(record)) {
            return ReconciliationOutcome.skipped();
        }
        try {
            RoleTarget target = requirePasscodeSnapshotTarget(storeId, record);
            if (target.provider() != SmartLockProvider.SWITCHBOT) {
                return ReconciliationOutcome.skipped();
            }
            SmartLockCredentialData credentials = decryptCredentials(target.integration());
            SmartLockProviderClient.ProviderPasscodeListSnapshot inspection = providerRegistry
                    .getClient(target.provider())
                    .inspectPasscodes(credentials, target.providerLockId());
            if (inspection == null) {
                inspection = new SmartLockProviderClient.ProviderPasscodeListSnapshot(
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        List.of()
                );
            }
            logSwitchBotPasscodeReconciliationInspection(record, target, inspection);
            if (record.getStatus() == SmartLockPasscodeStatus.PENDING
                    && !hasText(record.getProviderPasscodeId())) {
                return applySwitchBotCreatePasscodeReconciliation(record, target, inspection, timeoutMinutes);
            }
            if (record.getStatus() == SmartLockPasscodeStatus.DELETE_PENDING
                    && hasText(record.getProviderPasscodeId())) {
                return applySwitchBotDeletePasscodeReconciliation(record, target, inspection, timeoutMinutes);
            }
            return ReconciliationOutcome.skipped();
        } catch (RuntimeException ex) {
            return applySwitchBotReconciliationReason(
                    record,
                    null,
                    null,
                    "api_exception",
                    "SwitchBot keyList 对账异常：" + safeError(ex),
                    0,
                    0,
                    timeoutMinutes,
                    ex
            );
        }
    }

    private boolean shouldReconcileSwitchBotPasscode(SmartLockPasscodeRecord record) {
        if (record == null || record.getProvider() != SmartLockProvider.SWITCHBOT) {
            return false;
        }
        if (record.getStatus() == SmartLockPasscodeStatus.PENDING) {
            return !hasText(record.getProviderPasscodeId()) && hasText(record.getPasscodeName());
        }
        if (record.getStatus() == SmartLockPasscodeStatus.DELETE_PENDING) {
            return hasText(record.getProviderPasscodeId());
        }
        return false;
    }

    private ReconciliationOutcome applySwitchBotCreatePasscodeReconciliation(
            SmartLockPasscodeRecord record,
            RoleTarget target,
            SmartLockProviderClient.ProviderPasscodeListSnapshot inspection,
            long timeoutMinutes
    ) {
        if (!inspection.deviceFound()) {
            return applySwitchBotReconciliationReason(
                    record,
                    target,
                    inspection,
                    "no_device",
                    "SwitchBot devices 响应中未找到密码设备，无法读取 keyList，请检查设备同步、Hub 与 Keypad 连接",
                    0,
                    keyCount(inspection),
                    timeoutMinutes,
                    null
            );
        }
        if (!inspection.keyListReadable()) {
            return applySwitchBotReconciliationReason(
                    record,
                    target,
                    inspection,
                    "keylist_unreadable",
                    "SwitchBot 密码设备未返回可读取的 keyList，请检查 Hub 在线、蓝牙范围和设备同步",
                    0,
                    keyCount(inspection),
                    timeoutMinutes,
                    null
            );
        }

        SwitchBotPasscodeMatchResult match = findSwitchBotCreatePasscodeMatch(record, inspection.passcodes());
        if (match.status() == SwitchBotPasscodeMatchStatus.MATCHED) {
            return activateSwitchBotCreatedPasscode(record, target, match);
        }

        String reasonCode = "no_match";
        String message = "SwitchBot keyList 尚未发现唯一匹配的临时密码，可能设备未同步或名称不匹配";
        if (match.status() == SwitchBotPasscodeMatchStatus.AMBIGUOUS) {
            reasonCode = "ambiguous_match";
            message = "SwitchBot keyList 中存在多条同名临时密码，已拒绝自动匹配以避免误绑定远端 ID";
        } else if (match.status() == SwitchBotPasscodeMatchStatus.CANDIDATE_MISSING_ID) {
            reasonCode = "candidate_missing_id";
            message = "SwitchBot keyList 找到同名临时密码但缺少远端 ID，暂不能用于后续删除";
        }
        return applySwitchBotReconciliationReason(
                record,
                target,
                inspection,
                reasonCode,
                message,
                match.candidateCount(),
                match.keyCount(),
                timeoutMinutes,
                null
        );
    }

    private ReconciliationOutcome activateSwitchBotCreatedPasscode(
            SmartLockPasscodeRecord record,
            RoleTarget target,
            SwitchBotPasscodeMatchResult match
    ) {
        ReconciliationTiming timing = reconciliationTiming(record, SmartLockTaskType.CREATE_PASSCODE);
        SmartLockProviderClient.ProviderPasscodeSnapshot snapshot = match.snapshot();
        record.setProviderPasscodeId(snapshot.providerPasscodeId());
        record.setStatus(SmartLockPasscodeStatus.ACTIVE);
        record.setLastError(null);
        passcodeRepository.save(record);
        markMatchingCreateTaskSucceeded(record);
        logger.info(
                "SmartLock SwitchBot passcode reconciliation succeeded action=create recordId={} storeId={} "
                        + "provider={} passcodeDeviceDbId={} passcodeDeviceIdSuffix={} providerLockIdSuffix={} "
                        + "matchedPasscodeIdSuffix={} keyCount={} candidateCount={} ageSeconds={} ageSource={} "
                        + "ageZone={}",
                record.getId(),
                record.getStoreId(),
                providerLabel(target, record),
                passcodeDeviceDbId(target, record),
                passcodeDeviceIdSuffix(target, record),
                providerLockIdSuffix(target, record),
                identifierSuffix(snapshot.providerPasscodeId()),
                match.keyCount(),
                match.candidateCount(),
                timing.ageSeconds(),
                timing.source(),
                timing.ageZone()
        );
        return ReconciliationOutcome.changedTerminal();
    }

    private ReconciliationOutcome applySwitchBotDeletePasscodeReconciliation(
            SmartLockPasscodeRecord record,
            RoleTarget target,
            SmartLockProviderClient.ProviderPasscodeListSnapshot inspection,
            long timeoutMinutes
    ) {
        if (!inspection.deviceFound()) {
            return applySwitchBotReconciliationReason(
                    record,
                    target,
                    inspection,
                    "no_device",
                    "SwitchBot devices 响应中未找到密码设备，无法确认远端密码是否删除",
                    0,
                    keyCount(inspection),
                    timeoutMinutes,
                    null
            );
        }
        if (!inspection.keyListReadable()) {
            return applySwitchBotReconciliationReason(
                    record,
                    target,
                    inspection,
                    "keylist_unreadable",
                    "SwitchBot 密码设备未返回可读取的 keyList，无法确认远端密码是否删除",
                    0,
                    keyCount(inspection),
                    timeoutMinutes,
                    null
            );
        }

        SwitchBotDeletePasscodeMatchResult match =
                findSwitchBotDeletePasscodeMatch(record.getProviderPasscodeId(), inspection.passcodes());
        if (match.status() == SwitchBotDeletePasscodeStatus.DELETED_OR_MISSING) {
            ReconciliationTiming timing = reconciliationTiming(record, SmartLockTaskType.DELETE_PASSCODE);
            record.setStatus(SmartLockPasscodeStatus.DELETED);
            record.setDeletedAt(now());
            record.setLastError(null);
            passcodeRepository.save(record);
            markMatchingDeleteTaskTerminal(record, SmartLockTaskStatus.SUCCESS, SWITCHBOT_KEYLIST_DELETE_SYNCED_MESSAGE);
            logger.info(
                    "SmartLock SwitchBot passcode reconciliation succeeded action=delete recordId={} storeId={} "
                            + "provider={} passcodeDeviceDbId={} passcodeDeviceIdSuffix={} "
                            + "providerLockIdSuffix={} deletedPasscodeIdSuffix={} keyCount={} ageSeconds={} "
                            + "ageSource={} ageZone={}",
                    record.getId(),
                    record.getStoreId(),
                    providerLabel(target, record),
                    passcodeDeviceDbId(target, record),
                    passcodeDeviceIdSuffix(target, record),
                    providerLockIdSuffix(target, record),
                    identifierSuffix(record.getProviderPasscodeId()),
                    match.keyCount(),
                    timing.ageSeconds(),
                    timing.source(),
                    timing.ageZone()
            );
            return ReconciliationOutcome.changedTerminal();
        }

        String reasonCode = "delete_still_exists";
        String message = "SwitchBot keyList 仍显示远端密码存在，等待设备删除同步";
        if (match.status() == SwitchBotDeletePasscodeStatus.KEYS_MISSING_ID) {
            reasonCode = "key_missing_id";
            message = "SwitchBot keyList 条目缺少远端 ID，无法确认目标密码是否已删除";
        }
        return applySwitchBotReconciliationReason(
                record,
                target,
                inspection,
                reasonCode,
                message,
                match.matchCount(),
                match.keyCount(),
                timeoutMinutes,
                null
        );
    }

    private ReconciliationOutcome applySwitchBotReconciliationReason(
            SmartLockPasscodeRecord record,
            RoleTarget target,
            SmartLockProviderClient.ProviderPasscodeListSnapshot inspection,
            String reasonCode,
            String message,
            int candidateCount,
            int keyCount,
            long timeoutMinutes,
            RuntimeException exception
    ) {
        if (record == null) {
            return ReconciliationOutcome.skipped();
        }
        SmartLockPasscodeStatus statusBefore = record.getStatus();
        SmartLockTaskType taskType = taskTypeForStatus(statusBefore);
        ReconciliationTiming timing = reconciliationTiming(record, taskType);
        boolean timedOut = hasReconciliationTimedOut(timing, timeoutMinutes);
        String finalMessage = message;
        if (timedOut) {
            finalMessage = message + "；已超过后台对账超时 " + timeoutMinutes + " 分钟";
        }
        record.setLastError(safeProviderMessage(finalMessage));
        boolean terminal = false;
        boolean failed = false;
        if (timedOut) {
            record.setStatus(SmartLockPasscodeStatus.FAILED);
            terminal = true;
            failed = true;
        }
        passcodeRepository.save(record);
        if (timedOut) {
            markMatchingPasscodeTaskFailed(record, statusBefore, finalMessage);
        }
        logSwitchBotPasscodeReconciliationReason(
                record,
                target,
                inspection,
                reasonCode,
                candidateCount,
                keyCount,
                timedOut,
                exception,
                switchBotReconciliationAction(statusBefore),
                timing
        );
        return new ReconciliationOutcome(true, timedOut, terminal, failed);
    }

    private SwitchBotPasscodeMatchResult findSwitchBotCreatePasscodeMatch(
            SmartLockPasscodeRecord record,
            List<SmartLockProviderClient.ProviderPasscodeSnapshot> snapshots
    ) {
        int keyCount = 0;
        int candidateCount = 0;
        SmartLockProviderClient.ProviderPasscodeSnapshot matchedSnapshot = null;
        for (SmartLockProviderClient.ProviderPasscodeSnapshot snapshot : safePasscodeSnapshots(snapshots)) {
            keyCount += 1;
            if (!hasText(snapshot.passcodeName()) || isDeletedRemotePasscode(snapshot.status())) {
                continue;
            }
            if (!snapshot.passcodeName().equals(record.getPasscodeName())) {
                continue;
            }
            candidateCount += 1;
            if (hasText(snapshot.providerPasscodeId())) {
                matchedSnapshot = snapshot;
            }
        }
        if (candidateCount == 0) {
            return new SwitchBotPasscodeMatchResult(
                    SwitchBotPasscodeMatchStatus.NO_MATCH,
                    null,
                    keyCount,
                    0
            );
        }
        if (candidateCount > 1) {
            return new SwitchBotPasscodeMatchResult(
                    SwitchBotPasscodeMatchStatus.AMBIGUOUS,
                    null,
                    keyCount,
                    candidateCount
            );
        }
        if (matchedSnapshot == null) {
            return new SwitchBotPasscodeMatchResult(
                    SwitchBotPasscodeMatchStatus.CANDIDATE_MISSING_ID,
                    null,
                    keyCount,
                    candidateCount
            );
        }
        return new SwitchBotPasscodeMatchResult(
                SwitchBotPasscodeMatchStatus.MATCHED,
                matchedSnapshot,
                keyCount,
                candidateCount
        );
    }

    private SwitchBotDeletePasscodeMatchResult findSwitchBotDeletePasscodeMatch(
            String providerPasscodeId,
            List<SmartLockProviderClient.ProviderPasscodeSnapshot> snapshots
    ) {
        int keyCount = 0;
        int matchCount = 0;
        int missingIdCount = 0;
        boolean hasIdBearingEntry = false;
        for (SmartLockProviderClient.ProviderPasscodeSnapshot snapshot : safePasscodeSnapshots(snapshots)) {
            keyCount += 1;
            if (!hasText(snapshot.providerPasscodeId())) {
                missingIdCount += 1;
                continue;
            }
            hasIdBearingEntry = true;
            if (!snapshot.providerPasscodeId().equals(providerPasscodeId)) {
                continue;
            }
            matchCount += 1;
            if (isDeletedRemotePasscode(snapshot.status())) {
                return new SwitchBotDeletePasscodeMatchResult(
                        SwitchBotDeletePasscodeStatus.DELETED_OR_MISSING,
                        keyCount,
                        matchCount
                );
            }
            return new SwitchBotDeletePasscodeMatchResult(
                    SwitchBotDeletePasscodeStatus.STILL_EXISTS,
                    keyCount,
                    matchCount
            );
        }
        if (keyCount > 0 && !hasIdBearingEntry && missingIdCount > 0) {
            return new SwitchBotDeletePasscodeMatchResult(
                    SwitchBotDeletePasscodeStatus.KEYS_MISSING_ID,
                    keyCount,
                    0
            );
        }
        return new SwitchBotDeletePasscodeMatchResult(
                SwitchBotDeletePasscodeStatus.DELETED_OR_MISSING,
                keyCount,
                0
        );
    }

    private void markMatchingCreateTaskSucceeded(SmartLockPasscodeRecord record) {
        if (!hasText(record.getProviderTaskId())) {
            completeMatchingCreateTasksWithoutProviderTaskId(
                    record,
                    SmartLockTaskStatus.SUCCESS,
                    SWITCHBOT_KEYLIST_SYNCED_MESSAGE
            );
            return;
        }
        List<SmartLockTask> tasks = taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.SWITCHBOT,
                record.getProviderTaskId()
        );
        for (SmartLockTask task : tasks) {
            if (task.getTaskType() != SmartLockTaskType.CREATE_PASSCODE
                    || task.getStatus() != SmartLockTaskStatus.PENDING
                    || !isSamePasscodeRecord(task.getPasscodeRecord(), record)) {
                continue;
            }
            SmartLockProviderClient.ProviderTaskResult result =
                    new SmartLockProviderClient.ProviderTaskResult(
                            SmartLockTaskStatus.SUCCESS,
                            record.getProviderTaskId(),
                            record.getProviderPasscodeId(),
                            SWITCHBOT_KEYLIST_SYNCED_MESSAGE
                    );
            completeTask(task, result);
        }
    }

    private void markMatchingPasscodeTaskFailed(
            SmartLockPasscodeRecord record,
            SmartLockPasscodeStatus statusBefore,
            String message
    ) {
        if (statusBefore == SmartLockPasscodeStatus.DELETE_PENDING) {
            markMatchingDeleteTaskTerminal(record, SmartLockTaskStatus.FAILED, message);
            return;
        }
        markMatchingCreateTaskTerminal(record, SmartLockTaskStatus.FAILED, message);
    }

    private void markMatchingCreateTaskTerminal(
            SmartLockPasscodeRecord record,
            SmartLockTaskStatus status,
            String message
    ) {
        if (!hasText(record.getProviderTaskId())) {
            completeMatchingCreateTasksWithoutProviderTaskId(record, status, message);
            return;
        }
        List<SmartLockTask> tasks = taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.SWITCHBOT,
                record.getProviderTaskId()
        );
        for (SmartLockTask task : tasks) {
            if (task.getTaskType() != SmartLockTaskType.CREATE_PASSCODE
                    || task.getStatus() != SmartLockTaskStatus.PENDING
                    || !isSamePasscodeRecord(task.getPasscodeRecord(), record)) {
                continue;
            }
            SmartLockProviderClient.ProviderTaskResult result =
                    new SmartLockProviderClient.ProviderTaskResult(
                            status,
                            record.getProviderTaskId(),
                            record.getProviderPasscodeId(),
                            message
                    );
            completeTask(task, result);
        }
    }

    private void markMatchingDeleteTaskTerminal(
            SmartLockPasscodeRecord record,
            SmartLockTaskStatus status,
            String message
    ) {
        if (!hasText(record.getProviderTaskId())) {
            completeMatchingPasscodeTasksWithoutProviderTaskId(
                    record,
                    SmartLockTaskType.DELETE_PASSCODE,
                    status,
                    message
            );
            return;
        }
        List<SmartLockTask> tasks = taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                SmartLockProvider.SWITCHBOT,
                record.getProviderTaskId()
        );
        for (SmartLockTask task : tasks) {
            if (task.getTaskType() != SmartLockTaskType.DELETE_PASSCODE
                    || task.getStatus() != SmartLockTaskStatus.PENDING
                    || !isSamePasscodeRecord(task.getPasscodeRecord(), record)) {
                continue;
            }
            SmartLockProviderClient.ProviderTaskResult result =
                    new SmartLockProviderClient.ProviderTaskResult(
                            status,
                            record.getProviderTaskId(),
                            record.getProviderPasscodeId(),
                            message
                    );
            completeTask(task, result);
        }
    }

    private void completeMatchingPasscodeTasksWithoutProviderTaskId(
            SmartLockPasscodeRecord record,
            SmartLockTaskType taskType,
            SmartLockTaskStatus status,
            String message
    ) {
        List<SmartLockTask> tasks = taskRepository.findPasscodeTasksWithoutProviderTaskId(
                record.getStoreId(),
                record.getId(),
                taskType,
                SmartLockTaskStatus.PENDING
        );
        for (SmartLockTask task : tasks) {
            if (!isPendingPasscodeTaskForRecordWithoutProviderTaskId(task, record, taskType)) {
                continue;
            }
            SmartLockProviderClient.ProviderTaskResult result =
                    new SmartLockProviderClient.ProviderTaskResult(
                            status,
                            null,
                            record.getProviderPasscodeId(),
                            message
                    );
            completeTask(task, result);
        }
    }

    private boolean isPendingPasscodeTaskForRecordWithoutProviderTaskId(
            SmartLockTask task,
            SmartLockPasscodeRecord record,
            SmartLockTaskType taskType
    ) {
        return task != null
                && record != null
                && task.getStoreId() != null
                && task.getStoreId().equals(record.getStoreId())
                && task.getTaskType() == taskType
                && task.getStatus() == SmartLockTaskStatus.PENDING
                && !hasText(task.getProviderTaskId())
                && isSamePasscodeRecord(task.getPasscodeRecord(), record);
    }

    private boolean isSamePasscodeRecord(
            SmartLockPasscodeRecord first,
            SmartLockPasscodeRecord second
    ) {
        return first != null
                && second != null
                && first.getId() != null
                && first.getId().equals(second.getId());
    }

    private void completeTask(SmartLockTask task, SmartLockProviderClient.ProviderTaskResult result) {
        task.setStatus(result.status());
        if (hasText(result.providerTaskId())) {
            task.setProviderTaskId(result.providerTaskId());
        }
        task.setResultMessage(safeProviderMessage(result.message()));
        if (result.status() == SmartLockTaskStatus.PENDING) {
            task.setCompletedAt(null);
        } else {
            task.setCompletedAt(now());
        }
        taskRepository.save(task);
    }

    private RoleTarget requirePasscodeSnapshotTarget(Long storeId, SmartLockPasscodeRecord record) {
        if (record.getRoom() == null || record.getRoom().getId() == null) {
            throw new IllegalArgumentException("门锁密码记录缺少房间快照");
        }
        if (!storeId.equals(record.getRoom().getStoreId())) {
            throw new IllegalArgumentException("门锁密码记录与当前门店不一致");
        }
        if (record.getBinding() == null || !storeId.equals(record.getBinding().getStoreId())) {
            throw new IllegalArgumentException("门锁密码记录缺少绑定快照");
        }
        SmartLockIntegration integration = record.getIntegration();
        if (integration == null || !storeId.equals(integration.getStoreId())) {
            throw new IllegalArgumentException("门锁密码记录缺少集成快照");
        }
        if (record.getProvider() != integration.getProvider()) {
            throw new IllegalArgumentException("门锁密码记录与集成服务商不一致，已拒绝删除远程密码");
        }
        String providerLockId = firstText(record.getPasscodeProviderLockId(), record.getProviderLockId());
        if (!hasText(providerLockId)) {
            throw new IllegalArgumentException("门锁密码记录缺少密码设备快照，无法删除远程密码");
        }
        SmartLockDevice passcodeDevice = record.getPasscodeDevice();
        if (passcodeDevice != null) {
            validateDeviceConsistency(storeId, passcodeDevice, integration);
            if (!providerLockId.equals(passcodeDevice.getProviderLockId())) {
                throw new IllegalArgumentException("门锁密码记录与密码设备快照不一致，已拒绝删除远程密码");
            }
        }
        return new RoleTarget(integration, record.getProvider(), passcodeDevice, providerLockId);
    }

    private void validateDeviceConsistency(
            Long storeId,
            SmartLockDevice device,
            SmartLockIntegration integration
    ) {
        if (device == null || !storeId.equals(device.getStoreId())) {
            throw new IllegalArgumentException("门锁设备不存在");
        }
        if (integration == null || !storeId.equals(integration.getStoreId())) {
            throw new IllegalArgumentException("门锁集成不存在");
        }
        if (device.getIntegration() == null || !integration.getId().equals(device.getIntegration().getId())) {
            throw new IllegalArgumentException("门锁设备与集成不一致");
        }
        if (device.getProvider() != integration.getProvider()) {
            throw new IllegalArgumentException("门锁设备与集成服务商不一致");
        }
    }

    private SmartLockCredentialData decryptCredentials(SmartLockIntegration integration) {
        try {
            String json = credentialCrypto.decrypt(integration.getCredentialCiphertext());
            SmartLockCredentialData data = objectMapper.readValue(json, SmartLockCredentialData.class);
            data.setProvider(integration.getProvider());
            return data;
        } catch (Exception ex) {
            throw new IllegalStateException("门锁凭证读取失败", ex);
        }
    }

    private void logSwitchBotPasscodeReconciliationInspection(
            SmartLockPasscodeRecord record,
            RoleTarget target,
            SmartLockProviderClient.ProviderPasscodeListSnapshot inspection
    ) {
        ReconciliationTiming timing = reconciliationTiming(record, taskTypeForRecord(record));
        logger.info(
                "SmartLock SwitchBot passcode reconciliation inspect recordId={} storeId={} action={} "
                        + "provider={} passcodeDeviceDbId={} passcodeDeviceIdSuffix={} providerLockIdSuffix={} "
                        + "deviceFound={} keyListReadable={} keyCount={} deviceType={} online={} "
                        + "linkedLockSuffix={} hubSuffix={} ageSeconds={} ageSource={} ageZone={}",
                record.getId(),
                record.getStoreId(),
                switchBotReconciliationAction(record),
                providerLabel(target, record),
                passcodeDeviceDbId(target, record),
                passcodeDeviceIdSuffix(target, record),
                providerLockIdSuffix(target, record),
                inspection.deviceFound(),
                inspection.keyListReadable(),
                keyCount(inspection),
                safeProviderMessage(inspection.deviceType()),
                inspection.online(),
                identifierSuffix(inspection.linkedLockDeviceId()),
                identifierSuffix(inspection.hubDeviceId()),
                timing.ageSeconds(),
                timing.source(),
                timing.ageZone()
        );
    }

    private void logSwitchBotPasscodeReconciliationReason(
            SmartLockPasscodeRecord record,
            RoleTarget target,
            SmartLockProviderClient.ProviderPasscodeListSnapshot inspection,
            String reasonCode,
            int candidateCount,
            int keyCount,
            boolean timedOut,
            RuntimeException exception,
            String action,
            ReconciliationTiming timing
    ) {
        String logMessage = "SmartLock SwitchBot passcode reconciliation pending reason recordId={} storeId={} "
                + "action={} reason={} provider={} passcodeDeviceDbId={} passcodeDeviceIdSuffix={} "
                + "providerLockIdSuffix={} deviceFound={} keyListReadable={} keyCount={} candidateCount={} "
                + "ageSeconds={} ageSource={} ageZone={} timedOut={} finalStatus={} errorClass={}";
        logger.warn(
                logMessage,
                record.getId(),
                record.getStoreId(),
                action,
                reasonCode,
                providerLabel(target, record),
                passcodeDeviceDbId(target, record),
                passcodeDeviceIdSuffix(target, record),
                providerLockIdSuffix(target, record),
                inspection != null && inspection.deviceFound(),
                inspection != null && inspection.keyListReadable(),
                keyCount,
                candidateCount,
                timing.ageSeconds(),
                timing.source(),
                timing.ageZone(),
                timedOut,
                record.getStatus(),
                exception != null ? exception.getClass().getSimpleName() : null
        );
    }

    private String switchBotReconciliationAction(SmartLockPasscodeRecord record) {
        if (record == null) {
            return "create";
        }
        return switchBotReconciliationAction(record.getStatus());
    }

    private String switchBotReconciliationAction(SmartLockPasscodeStatus status) {
        if (status == SmartLockPasscodeStatus.DELETE_PENDING) {
            return "delete";
        }
        return "create";
    }

    private int keyCount(SmartLockProviderClient.ProviderPasscodeListSnapshot inspection) {
        if (inspection == null || inspection.passcodes() == null) {
            return 0;
        }
        return inspection.passcodes().size();
    }

    private boolean hasReconciliationTimedOut(ReconciliationTiming timing, long timeoutMinutes) {
        if (timing == null || timeoutMinutes <= 0) {
            return false;
        }
        return timing.startedInstant() != null
                && !timing.startedInstant().plus(Duration.ofMinutes(timeoutMinutes)).isAfter(clock.instant());
    }

    private ReconciliationTiming reconciliationTiming(
            SmartLockPasscodeRecord record,
            SmartLockTaskType taskType
    ) {
        if (record == null) {
            ZoneId zoneId = StoreTimeZoneUtil.resolveZoneId((Store) null);
            return new ReconciliationTiming(null, null, "missing", zoneId.getId(), 0);
        }
        LocalDateTime startedAt = record.getCreatedAt();
        String source = "record_created_at";
        if (startedAt == null) {
            SmartLockTask task = findStableReconciliationTask(record, taskType);
            if (task != null) {
                startedAt = task.getCreatedAt();
                source = "task_created_at";
            }
        }
        ZoneId ageZone = resolveReconciliationAgeZone(record);
        if (startedAt == null) {
            return new ReconciliationTiming(null, null, "missing", ageZone.getId(), 0);
        }
        Instant startedInstant = startedAt.atZone(ageZone).toInstant();
        long seconds = Math.max(0, Duration.between(startedInstant, clock.instant()).getSeconds());
        return new ReconciliationTiming(startedAt, startedInstant, source, ageZone.getId(), seconds);
    }

    private ZoneId resolveReconciliationAgeZone(SmartLockPasscodeRecord record) {
        if (record == null || record.getStoreId() == null) {
            return StoreTimeZoneUtil.resolveZoneId((Store) null);
        }
        Store store = storeRepository.findById(record.getStoreId()).orElse(null);
        return StoreTimeZoneUtil.resolveZoneId(store);
    }

    private SmartLockTask findStableReconciliationTask(
            SmartLockPasscodeRecord record,
            SmartLockTaskType taskType
    ) {
        if (record == null || record.getStoreId() == null || record.getId() == null || taskType == null) {
            return null;
        }
        List<SmartLockTask> tasks;
        if (hasText(record.getProviderTaskId())) {
            tasks = taskRepository.findByProviderAndProviderTaskIdOrderByCreatedAtDesc(
                    SmartLockProvider.SWITCHBOT,
                    record.getProviderTaskId()
            );
        } else {
            tasks = taskRepository.findPasscodeTasksWithoutProviderTaskId(
                    record.getStoreId(),
                    record.getId(),
                    taskType,
                    SmartLockTaskStatus.PENDING
            );
        }
        SmartLockTask oldestMatchingTask = null;
        for (SmartLockTask task : safeTasks(tasks)) {
            if (!isPendingPasscodeTaskForRecord(task, record, taskType)) {
                continue;
            }
            if (task.getCreatedAt() == null) {
                continue;
            }
            if (oldestMatchingTask == null
                    || task.getCreatedAt().isBefore(oldestMatchingTask.getCreatedAt())) {
                oldestMatchingTask = task;
            }
        }
        return oldestMatchingTask;
    }

    private boolean isPendingPasscodeTaskForRecord(
            SmartLockTask task,
            SmartLockPasscodeRecord record,
            SmartLockTaskType taskType
    ) {
        return task != null
                && record != null
                && task.getStoreId() != null
                && task.getStoreId().equals(record.getStoreId())
                && task.getTaskType() == taskType
                && task.getStatus() == SmartLockTaskStatus.PENDING
                && isSamePasscodeRecord(task.getPasscodeRecord(), record);
    }

    private List<SmartLockTask> safeTasks(List<SmartLockTask> tasks) {
        if (tasks == null) {
            return List.of();
        }
        return tasks;
    }

    private SmartLockTaskType taskTypeForRecord(SmartLockPasscodeRecord record) {
        if (record == null) {
            return SmartLockTaskType.CREATE_PASSCODE;
        }
        return taskTypeForStatus(record.getStatus());
    }

    private SmartLockTaskType taskTypeForStatus(SmartLockPasscodeStatus status) {
        if (status == SmartLockPasscodeStatus.DELETE_PENDING) {
            return SmartLockTaskType.DELETE_PASSCODE;
        }
        return SmartLockTaskType.CREATE_PASSCODE;
    }

    private SmartLockProvider providerLabel(RoleTarget target, SmartLockPasscodeRecord record) {
        if (target != null && target.provider() != null) {
            return target.provider();
        }
        if (record == null) {
            return null;
        }
        return record.getProvider();
    }

    private Long passcodeDeviceDbId(RoleTarget target, SmartLockPasscodeRecord record) {
        SmartLockDevice device = passcodeDevice(target, record);
        if (device == null) {
            return null;
        }
        return device.getId();
    }

    private String passcodeDeviceIdSuffix(RoleTarget target, SmartLockPasscodeRecord record) {
        SmartLockDevice device = passcodeDevice(target, record);
        if (device == null) {
            return null;
        }
        return identifierSuffix(device.getProviderLockId());
    }

    private SmartLockDevice passcodeDevice(RoleTarget target, SmartLockPasscodeRecord record) {
        if (target != null && target.device() != null) {
            return target.device();
        }
        if (record == null) {
            return null;
        }
        return record.getPasscodeDevice();
    }

    private String providerLockIdSuffix(RoleTarget target, SmartLockPasscodeRecord record) {
        String providerLockId = null;
        if (target != null) {
            providerLockId = target.providerLockId();
        }
        if (!hasText(providerLockId) && record != null) {
            providerLockId = firstText(record.getPasscodeProviderLockId(), record.getProviderLockId());
        }
        return identifierSuffix(providerLockId);
    }

    private static String identifierSuffix(String value) {
        if (!hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "***" + trimmed.substring(trimmed.length() - 4);
    }

    private List<SmartLockProviderClient.ProviderPasscodeSnapshot> safePasscodeSnapshots(
            List<SmartLockProviderClient.ProviderPasscodeSnapshot> snapshots
    ) {
        if (snapshots == null) {
            return List.of();
        }
        return snapshots;
    }

    private boolean isDeletedRemotePasscode(String status) {
        if (!hasText(status)) {
            return false;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("deleted") || normalized.contains("delete");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String safeError(RuntimeException ex) {
        return SmartLockMaskingUtils.safeExceptionMessage(ex);
    }

    private String safeProviderMessage(String message) {
        return SmartLockMaskingUtils.redactSensitiveMessage(message);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public record ReconciliationSummary(
            int candidates,
            int changed,
            int timedOut,
            int failed
    ) {
    }

    public record ReconciliationOutcome(
            boolean changed,
            boolean timedOut,
            boolean terminal,
            boolean failed
    ) {
        private static ReconciliationOutcome skipped() {
            return new ReconciliationOutcome(false, false, false, false);
        }

        private static ReconciliationOutcome changedTerminal() {
            return new ReconciliationOutcome(true, false, true, false);
        }
    }

    private enum SwitchBotPasscodeMatchStatus {
        MATCHED,
        NO_MATCH,
        AMBIGUOUS,
        CANDIDATE_MISSING_ID
    }

    private enum SwitchBotDeletePasscodeStatus {
        DELETED_OR_MISSING,
        STILL_EXISTS,
        KEYS_MISSING_ID
    }

    private record SwitchBotPasscodeMatchResult(
            SwitchBotPasscodeMatchStatus status,
            SmartLockProviderClient.ProviderPasscodeSnapshot snapshot,
            int keyCount,
            int candidateCount
    ) {
    }

    private record SwitchBotDeletePasscodeMatchResult(
            SwitchBotDeletePasscodeStatus status,
            int keyCount,
            int matchCount
    ) {
    }

    private record ReconciliationTiming(
            LocalDateTime startedAt,
            Instant startedInstant,
            String source,
            String ageZone,
            long ageSeconds
    ) {
    }

    private record RoleTarget(
            SmartLockIntegration integration,
            SmartLockProvider provider,
            SmartLockDevice device,
            String providerLockId
    ) {
    }
}
