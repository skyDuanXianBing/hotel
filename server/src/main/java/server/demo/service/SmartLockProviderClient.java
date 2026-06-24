package server.demo.service;

import server.demo.enums.SmartLockProvider;
import server.demo.enums.SmartLockTaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface SmartLockProviderClient {
    SmartLockProvider getProvider();

    void testConnection(SmartLockCredentialData credentials);

    SmartLockCredentialData refreshToken(SmartLockCredentialData credentials);

    List<DeviceSnapshot> listDevices(SmartLockCredentialData credentials);

    LockStatusSnapshot getStatus(SmartLockCredentialData credentials, String providerLockId);

    ProviderTaskResult unlock(SmartLockCredentialData credentials, String providerLockId);

    ProviderTaskResult lock(SmartLockCredentialData credentials, String providerLockId);

    ProviderTaskResult createPasscode(
            SmartLockCredentialData credentials,
            String providerLockId,
            PasscodeCommand command
    );

    ProviderTaskResult deletePasscode(
            SmartLockCredentialData credentials,
            String providerLockId,
            String providerPasscodeId
    );

    default List<ProviderPasscodeSnapshot> listPasscodes(
            SmartLockCredentialData credentials,
            String providerLockId
    ) {
        return List.of();
    }

    default ProviderPasscodeListSnapshot inspectPasscodes(
            SmartLockCredentialData credentials,
            String providerLockId
    ) {
        return new ProviderPasscodeListSnapshot(
                true,
                true,
                null,
                null,
                null,
                null,
                listPasscodes(credentials, providerLockId)
        );
    }

    ProviderTaskResult queryTask(SmartLockCredentialData credentials, String providerTaskId);

    record DeviceSnapshot(
            String providerLockId,
            String lockName,
            String deviceType,
            String auxiliaryDeviceId,
            Integer battery,
            String lockStatus,
            Boolean online,
            String rawJson
    ) {
    }

    record LockStatusSnapshot(
            String lockStatus,
            Integer battery,
            Boolean online,
            String rawJson
    ) {
    }

    record PasscodeCommand(
            String passcodeName,
            String passcode,
            LocalDateTime validFrom,
            LocalDateTime validUntil
    ) {
    }

    record ProviderTaskResult(
            SmartLockTaskStatus status,
            String providerTaskId,
            String providerPasscodeId,
            String message
    ) {
    }

    record ProviderPasscodeSnapshot(
            String providerPasscodeId,
            String passcodeName,
            String status
    ) {
    }

    record ProviderPasscodeListSnapshot(
            boolean deviceFound,
            boolean keyListReadable,
            String deviceType,
            Boolean online,
            String linkedLockDeviceId,
            String hubDeviceId,
            List<ProviderPasscodeSnapshot> passcodes
    ) {
        public ProviderPasscodeListSnapshot {
            if (passcodes == null) {
                passcodes = List.of();
            }
        }
    }
}
