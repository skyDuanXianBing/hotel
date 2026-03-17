package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContextHolder;
import server.demo.dto.ChannelDTO;
import server.demo.dto.CreateChannelRequest;
import server.demo.entity.Channel;
import server.demo.repository.ChannelRepository;
import server.demo.repository.ChannelPriceRepository;
import server.demo.repository.ReservationRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChannelService {
    private static final Set<String> PROTECTED_CHANNEL_CODES = Set.of("DIRECT", "AIRBNB", "BOOKING");

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelPriceRepository channelPriceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

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

    @Transactional
    public boolean deleteChannel(Long id) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        Optional<Channel> channel = channelRepository.findById(id);
        if (channel.isEmpty() || !channel.get().getStoreId().equals(storeId)) {
            return false;
        }
        if (PROTECTED_CHANNEL_CODES.contains(channel.get().getCode())) {
            throw new RuntimeException("默认渠道不可删除");
        }

        long reservationCount = reservationRepository.countByStoreIdAndChannelId(storeId, id);
        if (reservationCount > 0) {
            throw new RuntimeException("该渠道已被订单引用，无法删除。请先停用渠道。");
        }

        channelPriceRepository.deleteByStoreIdAndChannelId(storeId, id);
        channelRepository.deleteById(id);
        return true;
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
