package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ChannelDTO;
import server.demo.dto.CreateChannelRequest;
import server.demo.entity.Channel;
import server.demo.repository.ChannelRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChannelService {

    @Autowired
    private ChannelRepository channelRepository;

    public List<ChannelDTO> getAllChannels() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        List<Channel> channels = channelRepository.findByStoreId(storeId);
        return channels.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<ChannelDTO> getChannelById(Long id) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        return channelRepository.findById(id)
                .filter(channel -> channel.getStoreId().equals(storeId))
                .map(this::convertToDTO);
    }

    public ChannelDTO createChannel(CreateChannelRequest request) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        // 检查渠道代码是否已存在（门店级别）
        if (channelRepository.existsByStoreIdAndCode(storeId, request.getCode())) {
            throw new RuntimeException("渠道代码已存在");
        }

        Channel channel = new Channel();
        // storeId由StoreScopedEntityListener自动注入
        channel.setName(request.getName());
        channel.setCode(request.getCode());
        channel.setType(request.getType());
        channel.setColor(request.getColor());
        channel.setEnabled(request.getEnabled());
        channel.setDescription(request.getDescription());

        Channel savedChannel = channelRepository.save(channel);
        return convertToDTO(savedChannel);
    }

    public Optional<ChannelDTO> updateChannel(Long id, CreateChannelRequest request) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        return channelRepository.findById(id)
                .filter(channel -> channel.getStoreId().equals(storeId))
                .map(channel -> {
                    // 检查新代码是否与其他渠道冲突
                    if (!channel.getCode().equals(request.getCode()) &&
                        channelRepository.existsByStoreIdAndCode(storeId, request.getCode())) {
                        throw new RuntimeException("渠道代码已存在");
                    }

                    channel.setName(request.getName());
                    channel.setCode(request.getCode());
                    channel.setType(request.getType());
                    channel.setColor(request.getColor());
                    channel.setEnabled(request.getEnabled());
                    channel.setDescription(request.getDescription());

                    Channel updatedChannel = channelRepository.save(channel);
                    return convertToDTO(updatedChannel);
                });
    }

    public boolean deleteChannel(Long id) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        Optional<Channel> channel = channelRepository.findById(id);
        if (channel.isPresent() && channel.get().getStoreId().equals(storeId)) {
            channelRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<ChannelDTO> toggleChannelStatus(Long id, Boolean enabled) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        return channelRepository.findById(id)
                .filter(channel -> channel.getStoreId().equals(storeId))
                .map(channel -> {
                    channel.setEnabled(enabled);
                    Channel updatedChannel = channelRepository.save(channel);
                    return convertToDTO(updatedChannel);
                });
    }

    private ChannelDTO convertToDTO(Channel channel) {
        ChannelDTO dto = new ChannelDTO();
        dto.setId(channel.getId());
        dto.setName(channel.getName());
        dto.setCode(channel.getCode());
        dto.setType(channel.getType());
        dto.setColor(channel.getColor());
        dto.setEnabled(channel.getEnabled());
        dto.setDescription(channel.getDescription());
        dto.setContactPerson(channel.getContactPerson());
        dto.setContactPhone(channel.getContactPhone());
        dto.setContactEmail(channel.getContactEmail());
        dto.setCommissionRate(channel.getCommissionRate());
        dto.setIsActive(channel.getIsActive());
        dto.setNotes(channel.getNotes());
        dto.setCreatedAt(channel.getCreatedAt());
        dto.setUpdatedAt(channel.getUpdatedAt());
        return dto;
    }
}