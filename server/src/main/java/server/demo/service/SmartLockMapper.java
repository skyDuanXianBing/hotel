package server.demo.service;

import org.springframework.stereotype.Component;
import server.demo.dto.SmartLockBindingDTO;
import server.demo.dto.SmartLockDeviceDTO;
import server.demo.dto.SmartLockIntegrationDTO;
import server.demo.dto.SmartLockPasscodeDTO;
import server.demo.dto.SmartLockRoomDTO;
import server.demo.dto.SmartLockStatusDTO;
import server.demo.dto.SmartLockTaskDTO;
import server.demo.entity.Room;
import server.demo.entity.SmartLockDevice;
import server.demo.entity.SmartLockIntegration;
import server.demo.entity.SmartLockPasscodeRecord;
import server.demo.entity.SmartLockRoomBinding;
import server.demo.entity.SmartLockTask;

@Component
public class SmartLockMapper {
    public SmartLockIntegrationDTO toIntegrationDto(
            SmartLockIntegration integration,
            SmartLockCredentialData credentials
    ) {
        SmartLockIntegrationDTO dto = new SmartLockIntegrationDTO();
        dto.setId(integration.getId());
        dto.setProvider(integration.getProvider());
        dto.setName(integration.getName());
        dto.setEnabled(integration.getEnabled());
        dto.setConnectionStatus(integration.getConnectionStatus());
        dto.setCredentialsConfigured(credentials != null && credentials.isConfigured());
        dto.setMaskedCredentials(credentials != null ? credentials.maskedFields() : null);
        dto.setTokenExpiresAt(integration.getTokenExpiresAt());
        dto.setLastTestAt(integration.getLastTestAt());
        dto.setLastSyncAt(integration.getLastSyncAt());
        dto.setLastError(integration.getLastError());
        dto.setCreatedAt(integration.getCreatedAt());
        dto.setUpdatedAt(integration.getUpdatedAt());
        return dto;
    }

    public SmartLockDeviceDTO toDeviceDto(SmartLockDevice device) {
        SmartLockDeviceDTO dto = new SmartLockDeviceDTO();
        dto.setId(device.getId());
        if (device.getIntegration() != null) {
            dto.setIntegrationId(device.getIntegration().getId());
        }
        dto.setProvider(device.getProvider());
        dto.setProviderLockId(device.getProviderLockId());
        dto.setLockName(device.getLockName());
        dto.setDeviceType(device.getDeviceType());
        dto.setAuxiliaryDeviceId(device.getAuxiliaryDeviceId());
        dto.setBattery(device.getBattery());
        dto.setLockStatus(device.getLockStatus());
        dto.setOnline(device.getOnline());
        dto.setLastSyncedAt(device.getLastSyncedAt());
        dto.setLastStatusAt(device.getLastStatusAt());
        return dto;
    }

    public SmartLockBindingDTO toBindingDto(SmartLockRoomBinding binding) {
        SmartLockBindingDTO dto = new SmartLockBindingDTO();
        dto.setId(binding.getId());
        Room room = binding.getRoom();
        if (room != null) {
            dto.setRoomId(room.getId());
            dto.setRoomNumber(room.getRoomNumber());
            dto.setRoomTypeId(room.getRoomTypeId());
            dto.setRoomTypeName(room.getRoomTypeName());
        }
        if (binding.getIntegration() != null) {
            dto.setIntegrationId(binding.getIntegration().getId());
        }
        if (binding.getDevice() != null) {
            dto.setDeviceId(binding.getDevice().getId());
            dto.setLockName(binding.getDevice().getLockName());
        }
        dto.setProvider(binding.getProvider());
        dto.setProviderLockId(binding.getProviderLockId());
        dto.setStatus(binding.getStatus());
        dto.setCreatedAt(binding.getCreatedAt());
        dto.setUpdatedAt(binding.getUpdatedAt());
        return dto;
    }

    public SmartLockRoomDTO toRoomDto(Room room, SmartLockRoomBinding binding) {
        SmartLockRoomDTO dto = new SmartLockRoomDTO();
        dto.setRoomId(room.getId());
        dto.setRoomNumber(room.getRoomNumber());
        dto.setRoomTypeId(room.getRoomTypeId());
        dto.setRoomTypeName(room.getRoomTypeName());
        if (binding != null) {
            dto.setBinding(toBindingDto(binding));
        }
        return dto;
    }

    public SmartLockStatusDTO toStatusDto(SmartLockRoomBinding binding) {
        SmartLockStatusDTO dto = new SmartLockStatusDTO();
        Room room = binding.getRoom();
        SmartLockDevice device = binding.getDevice();
        if (room != null) {
            dto.setRoomId(room.getId());
        }
        dto.setBindingId(binding.getId());
        if (device != null) {
            dto.setDeviceId(device.getId());
            dto.setLockName(device.getLockName());
            dto.setLockStatus(device.getLockStatus());
            dto.setBattery(device.getBattery());
            dto.setOnline(device.getOnline());
            dto.setLastStatusAt(device.getLastStatusAt());
        }
        dto.setProvider(binding.getProvider());
        dto.setProviderLockId(binding.getProviderLockId());
        return dto;
    }

    public SmartLockPasscodeDTO toPasscodeDto(SmartLockPasscodeRecord record, String oneTimePasscode) {
        SmartLockPasscodeDTO dto = new SmartLockPasscodeDTO();
        dto.setId(record.getId());
        if (record.getRoom() != null) {
            dto.setRoomId(record.getRoom().getId());
        }
        if (record.getBinding() != null) {
            dto.setBindingId(record.getBinding().getId());
        }
        dto.setProvider(record.getProvider());
        dto.setProviderLockId(record.getProviderLockId());
        dto.setProviderPasscodeId(record.getProviderPasscodeId());
        dto.setProviderTaskId(record.getProviderTaskId());
        dto.setPasscodeName(record.getPasscodeName());
        dto.setPasscodeMasked(record.getPasscodeMasked());
        dto.setOneTimePasscode(oneTimePasscode);
        dto.setValidFrom(record.getValidFrom());
        dto.setValidUntil(record.getValidUntil());
        dto.setStatus(record.getStatus());
        dto.setLastError(record.getLastError());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }

    public SmartLockTaskDTO toTaskDto(SmartLockTask task) {
        SmartLockTaskDTO dto = new SmartLockTaskDTO();
        dto.setId(task.getId());
        dto.setTaskType(task.getTaskType());
        dto.setProvider(task.getProvider());
        if (task.getRoom() != null) {
            dto.setRoomId(task.getRoom().getId());
        }
        if (task.getBinding() != null) {
            dto.setBindingId(task.getBinding().getId());
        }
        if (task.getPasscodeRecord() != null) {
            dto.setPasscodeRecordId(task.getPasscodeRecord().getId());
        }
        dto.setProviderTaskId(task.getProviderTaskId());
        dto.setStatus(task.getStatus());
        dto.setResultMessage(task.getResultMessage());
        dto.setErrorMessage(task.getErrorMessage());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        return dto;
    }
}
