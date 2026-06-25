package server.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.demo.entity.SmartLockDevice;
import server.demo.enums.SmartLockProvider;
import server.demo.util.SmartLockMaskingUtils;

import java.util.List;

public class SmartLockDeviceRoleResolver {
    private static final List<String> SWITCHBOT_CONTROL_DEVICE_TYPES = List.of(
            "Lock",
            "Lock Pro",
            "Lock Lite",
            "Lock Ultra",
            "Smart Lock",
            "Smart Lock Pro",
            "Smart Lock Ultra",
            "Smart Lock Pro Wifi",
            "Lock Vision",
            "Lock Vision Pro"
    );
    private static final List<String> SWITCHBOT_AUTHENTICATION_PANEL_TYPES = List.of(
            "Keypad",
            "Keypad Touch",
            "Keypad Vision",
            "Keypad Vision Pro"
    );
    private static final List<String> SWITCHBOT_PASSCODE_DEVICE_TYPES = List.of(
            "Keypad",
            "Keypad Touch",
            "Keypad Vision",
            "Keypad Vision Pro",
            "Lock Vision",
            "Lock Vision Pro"
    );

    private final ObjectMapper objectMapper;

    public SmartLockDeviceRoleResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean supportsControl(SmartLockDevice device) {
        if (device == null) {
            return false;
        }
        if (device.getProvider() == SmartLockProvider.TTLOCK) {
            Integer hasGateway = readRawDeviceInteger(device, "hasGateway");
            return hasGateway == null || hasGateway == 1;
        }
        if (device.getProvider() != SmartLockProvider.SWITCHBOT) {
            return true;
        }
        return matchesType(device.getDeviceType(), SWITCHBOT_CONTROL_DEVICE_TYPES);
    }

    public boolean supportsPasscode(SmartLockDevice device) {
        if (device == null) {
            return false;
        }
        if (device.getProvider() == SmartLockProvider.TTLOCK) {
            Integer keyboardPwdVersion = readRawDeviceInteger(device, "keyboardPwdVersion");
            return keyboardPwdVersion == null || keyboardPwdVersion == 4;
        }
        if (device.getProvider() != SmartLockProvider.SWITCHBOT) {
            return true;
        }
        return matchesType(device.getDeviceType(), SWITCHBOT_PASSCODE_DEVICE_TYPES);
    }

    public boolean isSwitchBotAuthenticationPanel(SmartLockDevice device) {
        if (device == null || device.getProvider() != SmartLockProvider.SWITCHBOT) {
            return false;
        }
        return matchesType(device.getDeviceType(), SWITCHBOT_AUTHENTICATION_PANEL_TYPES);
    }

    public String linkedControlProviderLockId(SmartLockDevice device) {
        if (!isSwitchBotAuthenticationPanel(device)) {
            return null;
        }

        String rawLockDeviceId = readRawDeviceField(device, "lockDeviceId");
        if (hasText(rawLockDeviceId)) {
            return rawLockDeviceId;
        }

        String auxiliaryDeviceId = SmartLockMaskingUtils.trimToNull(device.getAuxiliaryDeviceId());
        String rawHubDeviceId = readRawDeviceField(device, "hubDeviceId");
        if (hasText(rawHubDeviceId) && rawHubDeviceId.equals(auxiliaryDeviceId)) {
            return null;
        }
        return auxiliaryDeviceId;
    }

    public boolean hasConflictingSwitchBotLinkedControl(SmartLockDevice device) {
        if (!isSwitchBotAuthenticationPanel(device)) {
            return false;
        }
        String rawLockDeviceId = readRawDeviceField(device, "lockDeviceId");
        String auxiliaryDeviceId = SmartLockMaskingUtils.trimToNull(device.getAuxiliaryDeviceId());
        return hasText(rawLockDeviceId) && hasText(auxiliaryDeviceId) && !rawLockDeviceId.equals(auxiliaryDeviceId);
    }

    private String readRawDeviceField(SmartLockDevice device, String fieldName) {
        if (device == null || !hasText(device.getRawDataJson())) {
            return null;
        }
        try {
            return SmartLockMaskingUtils.trimToNull(
                    objectMapper.readTree(device.getRawDataJson()).path(fieldName).asText(null)
            );
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer readRawDeviceInteger(SmartLockDevice device, String fieldName) {
        if (device == null || !hasText(device.getRawDataJson())) {
            return null;
        }
        try {
            JsonNode value = objectMapper.readTree(device.getRawDataJson()).path(fieldName);
            if (value.isMissingNode() || value.isNull()) {
                return null;
            }
            return value.asInt();
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean matchesType(String deviceType, List<String> supportedTypes) {
        if (!hasText(deviceType)) {
            return false;
        }
        for (String supportedType : supportedTypes) {
            if (supportedType.equalsIgnoreCase(deviceType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
