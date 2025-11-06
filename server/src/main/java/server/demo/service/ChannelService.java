package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.demo.dto.ChannelDTO;
import server.demo.dto.CreateChannelRequest;
import server.demo.entity.Channel;
import server.demo.entity.User;
import server.demo.repository.ChannelRepository;
import server.demo.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChannelService {

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ChannelDTO> getAllChannels(Long userId) {
        List<Channel> channels = channelRepository.findByUserId(userId);
        return channels.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<ChannelDTO> getChannelById(Long userId, Long id) {
        return channelRepository.findById(id)
                .filter(channel -> channel.getUser().getId().equals(userId))
                .map(this::convertToDTO);
    }

    public ChannelDTO createChannel(Long userId, CreateChannelRequest request) {
        // 检查渠道代码是否已存在（用户级别）
        if (channelRepository.existsByUserIdAndCode(userId, request.getCode())) {
            throw new RuntimeException("渠道代码已存在");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Channel channel = new Channel();
        channel.setUser(user);
        channel.setName(request.getName());
        channel.setCode(request.getCode());
        channel.setType(request.getType());
        channel.setColor(request.getColor());
        channel.setEnabled(request.getEnabled());
        channel.setDescription(request.getDescription());

        Channel savedChannel = channelRepository.save(channel);
        return convertToDTO(savedChannel);
    }

    public Optional<ChannelDTO> updateChannel(Long userId, Long id, CreateChannelRequest request) {
        return channelRepository.findById(id)
                .filter(channel -> channel.getUser().getId().equals(userId))
                .map(channel -> {
                    // 检查新代码是否与其他渠道冲突
                    if (!channel.getCode().equals(request.getCode()) &&
                        channelRepository.existsByUserIdAndCode(userId, request.getCode())) {
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

    public boolean deleteChannel(Long userId, Long id) {
        Optional<Channel> channel = channelRepository.findById(id);
        if (channel.isPresent() && channel.get().getUser().getId().equals(userId)) {
            channelRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<ChannelDTO> toggleChannelStatus(Long userId, Long id, Boolean enabled) {
        return channelRepository.findById(id)
                .filter(channel -> channel.getUser().getId().equals(userId))
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