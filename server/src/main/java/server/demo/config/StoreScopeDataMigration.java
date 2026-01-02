package server.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.*;
import server.demo.repository.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 为历史数据补齐 store_id，保证门店级数据隔离生效。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class StoreScopeDataMigration implements org.springframework.boot.CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StoreScopeDataMigration.class);

    private final StoreUserRepository storeUserRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final RoomPriceRepository roomPriceRepository;
    private final ChannelRepository channelRepository;
    private final RoomGroupRepository roomGroupRepository;
    private final RoomGroupMemberRepository roomGroupMemberRepository;
    private final PricePlanRepository pricePlanRepository;
    private final RoomTypePricePlanRepository roomTypePricePlanRepository;
    private final ReservationRepository reservationRepository;
    private final RoomStatusShareRepository roomStatusShareRepository;

    @Autowired
    public StoreScopeDataMigration(StoreUserRepository storeUserRepository,
                                   RoomTypeRepository roomTypeRepository,
                                   RoomRepository roomRepository,
                                   RoomPriceRepository roomPriceRepository,
                                   ChannelRepository channelRepository,
                                   RoomGroupRepository roomGroupRepository,
                                   RoomGroupMemberRepository roomGroupMemberRepository,
                                   PricePlanRepository pricePlanRepository,
                                   RoomTypePricePlanRepository roomTypePricePlanRepository,
                                   ReservationRepository reservationRepository,
                                   RoomStatusShareRepository roomStatusShareRepository) {
        this.storeUserRepository = storeUserRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.roomPriceRepository = roomPriceRepository;
        this.channelRepository = channelRepository;
        this.roomGroupRepository = roomGroupRepository;
        this.roomGroupMemberRepository = roomGroupMemberRepository;
        this.pricePlanRepository = pricePlanRepository;
        this.roomTypePricePlanRepository = roomTypePricePlanRepository;
        this.reservationRepository = reservationRepository;
        this.roomStatusShareRepository = roomStatusShareRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<Long, Store> userStoreMap = buildUserStoreMap();
        if (userStoreMap.isEmpty()) {
            log.info("[StoreScopeMigration] 没有可用的门店-用户映射，跳过 store_id 迁移");
            return;
        }

        log.info("[StoreScopeMigration] 开始执行历史数据 store_id 迁移");
        migrateChannels(userStoreMap);
        migrateRoomTypes(userStoreMap);
        migrateRooms(userStoreMap);
        migrateRoomPrices();
        migrateRoomGroups(userStoreMap);
        migrateRoomGroupMembers();
        migratePricePlans(userStoreMap);
        migrateRoomTypePricePlans();
        migrateReservations(userStoreMap);
        migrateRoomStatusShares(userStoreMap);
        log.info("[StoreScopeMigration] store_id 迁移完成");
    }

    private Map<Long, Store> buildUserStoreMap() {
        List<StoreUser> storeUsers = storeUserRepository.findAll();
        if (storeUsers.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, StoreUser> bestByUser = new HashMap<>();
        for (StoreUser membership : storeUsers) {
            if (membership.getStore() == null || membership.getUser() == null) {
                continue;
            }
            if (Boolean.FALSE.equals(membership.getIsActive())) {
                continue;
            }
            Long userId = membership.getUser().getId();
            StoreUser existing = bestByUser.get(userId);
            if (existing == null || compareMembership(membership, existing) < 0) {
                bestByUser.put(userId, membership);
            }
        }

        return bestByUser.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getStore()));
    }

    private int compareMembership(StoreUser current, StoreUser existing) {
        int roleCompare = Integer.compare(rolePriority(current.getRole()), rolePriority(existing.getRole()));
        if (roleCompare != 0) {
            return roleCompare;
        }
        LocalDateTime currentJoin = current.getJoinedAt();
        LocalDateTime existingJoin = existing.getJoinedAt();
        if (currentJoin == null && existingJoin == null) {
            return 0;
        }
        if (currentJoin == null) {
            return 1;
        }
        if (existingJoin == null) {
            return -1;
        }
        return currentJoin.compareTo(existingJoin);
    }

    private int rolePriority(String role) {
        if (role == null) {
            return 3;
        }
        switch (role.toLowerCase()) {
            case "owner":
                return 0;
            case "admin":
                return 1;
            default:
                return 2;
        }
    }

    private void migrateRoomTypes(Map<Long, Store> storeMap) {
        List<RoomType> pending = roomTypeRepository.findAll().stream()
                .filter(rt -> rt.getStoreId() == null)
                .collect(Collectors.toList());
        List<RoomType> toSave = new ArrayList<>();
        for (RoomType roomType : pending) {
            Long userId = roomType.getUser() != null ? roomType.getUser().getId() : null;
            Store store = userId != null ? storeMap.get(userId) : null;
            if (store == null) {
                logMissingStore("RoomType", roomType.getId(), userId);
                continue;
            }
            roomType.setStoreId(store.getId());
            toSave.add(roomType);
        }
        saveIfNeeded("RoomType", toSave, roomTypeRepository);
    }

    private void migrateChannels(Map<Long, Store> storeMap) {
        List<Channel> pending = channelRepository.findAll().stream()
                .filter(channel -> channel.getStoreId() == null)
                .collect(Collectors.toList());
        List<Channel> toSave = new ArrayList<>();

        Set<String> reservedKeys = channelRepository.findAll().stream()
                .filter(c -> c.getStoreId() != null && c.getCode() != null && !c.getCode().isBlank())
                .map(c -> storeCodeKey(c.getStoreId(), c.getCode()))
                .collect(Collectors.toSet());

        for (Channel channel : pending) {
            Long userId = channel.getUser() != null ? channel.getUser().getId() : null;
            Store store = userId != null ? storeMap.get(userId) : null;
            if (store == null) {
                logMissingStore("Channel", channel.getId(), userId);
                continue;
            }

            String code = channel.getCode() != null ? channel.getCode().trim() : null;
            if (code == null || code.isBlank()) {
                code = "CH" + (channel.getId() != null ? channel.getId() : "");
            }

            Long storeId = store.getId();
            String key = storeCodeKey(storeId, code);
            if (reservedKeys.contains(key)) {
                String newCode = dedupeChannelCode(code, channel.getId(), reservedKeys, storeId);
                log.warn("[StoreScopeMigration] Channel code duplicate under store. storeId={}, oldCode={}, newCode={}, channelId={}",
                        storeId, code, newCode, channel.getId());
                channel.setCode(newCode);
                // 避免重复渠道影响业务逻辑：默认禁用历史重复数据
                channel.setEnabled(false);
                channel.setIsActive(false);
                if (channel.getName() != null && !channel.getName().contains("历史重复")) {
                    channel.setName(channel.getName() + "（历史重复）");
                }
                key = storeCodeKey(storeId, newCode);
            } else {
                channel.setCode(code);
            }

            channel.setStoreId(storeId);
            reservedKeys.add(key);
            toSave.add(channel);
        }
        saveIfNeeded("Channel", toSave, channelRepository);
    }

    private static String storeCodeKey(Long storeId, String code) {
        return storeId + "-" + code;
    }

    private static String dedupeChannelCode(String baseCode, Long channelId, Set<String> reservedKeys, Long storeId) {
        String normalized = baseCode != null ? baseCode.trim() : "CH";
        String idPart = channelId != null ? String.valueOf(channelId) : "0";

        // code 字段长度=20，尽量保留原 code + 短后缀
        for (int attempt = 0; attempt < 1000; attempt++) {
            String suffix = attempt == 0 ? "_D" + idPart : "_D" + idPart + "_" + attempt;
            String candidate = shortenToMax(normalized, suffix, 20);
            if (!reservedKeys.contains(storeCodeKey(storeId, candidate))) {
                return candidate;
            }
        }

        // 理论不会走到这里，兜底返回截断后的 code
        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }

    private static String shortenToMax(String base, String suffix, int maxLen) {
        if (suffix.length() >= maxLen) {
            // 极端情况：只保留后缀的尾部
            return suffix.substring(suffix.length() - maxLen);
        }
        int maxBaseLen = maxLen - suffix.length();
        String trimmedBase = base != null ? base : "";
        if (trimmedBase.length() > maxBaseLen) {
            trimmedBase = trimmedBase.substring(0, maxBaseLen);
        }
        return trimmedBase + suffix;
    }

    private void migrateRooms(Map<Long, Store> storeMap) {
        List<Room> pending = roomRepository.findAll().stream()
                .filter(room -> room.getStoreId() == null)
                .collect(Collectors.toList());
        List<Room> toSave = new ArrayList<>();
        for (Room room : pending) {
            Long storeId = null;
            if (room.getRoomType() != null) {
                storeId = room.getRoomType().getStoreId();
            }
            if (storeId == null && room.getUserId() != null) {
                Store store = storeMap.get(room.getUserId());
                if (store != null) {
                    storeId = store.getId();
                }
            }
            if (storeId == null) {
                logMissingStore("Room", room.getId(), room.getUserId());
                continue;
            }
            room.setStoreId(storeId);
            toSave.add(room);
        }
        saveIfNeeded("Room", toSave, roomRepository);
    }

    private void migrateRoomPrices() {
        List<RoomPrice> pending = roomPriceRepository.findAll().stream()
                .filter(price -> price.getStoreId() == null)
                .collect(Collectors.toList());
        List<RoomPrice> toSave = new ArrayList<>();
        for (RoomPrice price : pending) {
            if (price.getRoomType() == null || price.getRoomType().getStoreId() == null) {
                log.warn("[StoreScopeMigration] RoomPrice id={} 无法确定 storeId（缺少房型信息）", price.getId());
                continue;
            }
            price.setStoreId(price.getRoomType().getStoreId());
            toSave.add(price);
        }
        saveIfNeeded("RoomPrice", toSave, roomPriceRepository);
    }

    private void migrateRoomGroups(Map<Long, Store> storeMap) {
        List<RoomGroup> pending = roomGroupRepository.findAll().stream()
                .filter(group -> group.getStoreId() == null)
                .collect(Collectors.toList());
        List<RoomGroup> toSave = new ArrayList<>();
        for (RoomGroup group : pending) {
            Long userId = group.getUserId();
            Store store = userId != null ? storeMap.get(userId) : null;
            if (store == null) {
                logMissingStore("RoomGroup", group.getId(), userId);
                continue;
            }
            group.setStoreId(store.getId());
            toSave.add(group);
        }
        saveIfNeeded("RoomGroup", toSave, roomGroupRepository);
    }

    private void migrateRoomGroupMembers() {
        Map<Long, RoomGroup> groupCache = roomGroupRepository.findAll().stream()
                .collect(Collectors.toMap(RoomGroup::getId, g -> g));
        List<RoomGroupMember> pending = roomGroupMemberRepository.findAll().stream()
                .filter(member -> member.getStoreId() == null)
                .collect(Collectors.toList());
        List<RoomGroupMember> toSave = new ArrayList<>();
        for (RoomGroupMember member : pending) {
            RoomGroup group = groupCache.get(member.getGroupId());
            if (group == null || group.getStoreId() == null) {
                log.warn("[StoreScopeMigration] RoomGroupMember id={} 无法确定 storeId（分组缺失）", member.getId());
                continue;
            }
            member.setStoreId(group.getStoreId());
            toSave.add(member);
        }
        saveIfNeeded("RoomGroupMember", toSave, roomGroupMemberRepository);
    }

    private void migratePricePlans(Map<Long, Store> storeMap) {
        List<PricePlan> pending = pricePlanRepository.findAll().stream()
                .filter(plan -> plan.getStoreId() == null)
                .collect(Collectors.toList());
        List<PricePlan> toSave = new ArrayList<>();
        for (PricePlan plan : pending) {
            Long userId = plan.getUser() != null ? plan.getUser().getId() : null;
            Store store = userId != null ? storeMap.get(userId) : null;
            if (store == null) {
                logMissingStore("PricePlan", plan.getId(), userId);
                continue;
            }
            plan.setStoreId(store.getId());
            toSave.add(plan);
        }
        saveIfNeeded("PricePlan", toSave, pricePlanRepository);
    }

    private void migrateRoomTypePricePlans() {
        Map<Long, RoomType> roomTypeCache = roomTypeRepository.findAll().stream()
                .collect(Collectors.toMap(RoomType::getId, rt -> rt));
        Map<Long, PricePlan> pricePlanCache = pricePlanRepository.findAll().stream()
                .collect(Collectors.toMap(PricePlan::getId, plan -> plan));
        List<RoomTypePricePlan> pending = roomTypePricePlanRepository.findAll().stream()
                .filter(rtp -> rtp.getStoreId() == null)
                .collect(Collectors.toList());
        List<RoomTypePricePlan> toSave = new ArrayList<>();
        for (RoomTypePricePlan rtp : pending) {
            Long storeId = null;
            if (rtp.getRoomType() != null && rtp.getRoomType().getStoreId() != null) {
                storeId = rtp.getRoomType().getStoreId();
            } else if (rtp.getRoomType() != null) {
                RoomType cached = roomTypeCache.get(rtp.getRoomType().getId());
                if (cached != null) {
                    storeId = cached.getStoreId();
                }
            }
            if (storeId == null && rtp.getPricePlan() != null) {
                PricePlan cachedPlan = pricePlanCache.get(rtp.getPricePlan().getId());
                if (cachedPlan != null) {
                    storeId = cachedPlan.getStoreId();
                }
            }
            if (storeId == null) {
                log.warn("[StoreScopeMigration] RoomTypePricePlan id={} 无法确定 storeId", rtp.getId());
                continue;
            }
            rtp.setStoreId(storeId);
            toSave.add(rtp);
        }
        saveIfNeeded("RoomTypePricePlan", toSave, roomTypePricePlanRepository);
    }

    private void migrateReservations(Map<Long, Store> storeMap) {
        List<Reservation> pending = reservationRepository.findAll().stream()
                .filter(reservation -> reservation.getStoreId() == null)
                .collect(Collectors.toList());
        List<Reservation> toSave = new ArrayList<>();
        for (Reservation reservation : pending) {
            Long storeId = null;
            if (reservation.getRoom() != null) {
                storeId = reservation.getRoom().getStoreId();
            }
            if (storeId == null && reservation.getUser() != null) {
                Store store = storeMap.get(reservation.getUser().getId());
                if (store != null) {
                    storeId = store.getId();
                }
            }
            if (storeId == null) {
                logMissingStore("Reservation", reservation.getId(),
                        reservation.getUser() != null ? reservation.getUser().getId() : null);
                continue;
            }
            reservation.setStoreId(storeId);
            toSave.add(reservation);
        }
        saveIfNeeded("Reservation", toSave, reservationRepository);
    }

    private void migrateRoomStatusShares(Map<Long, Store> storeMap) {
        List<RoomStatusShare> pending = roomStatusShareRepository.findAll().stream()
                .filter(share -> share.getStoreId() == null)
                .collect(Collectors.toList());
        List<RoomStatusShare> toSave = new ArrayList<>();
        for (RoomStatusShare share : pending) {
            Long storeId = share.getStoreId();
            if (storeId == null && share.getUserId() != null) {
                Store store = storeMap.get(share.getUserId());
                if (store != null) {
                    storeId = store.getId();
                }
            }
            if (storeId == null) {
                logMissingStore("RoomStatusShare", share.getId(), share.getUserId());
                continue;
            }
            share.setStoreId(storeId);
            toSave.add(share);
        }
        saveIfNeeded("RoomStatusShare", toSave, roomStatusShareRepository);
    }

    private void logMissingStore(String entity, Long entityId, Long userId) {
        log.warn("[StoreScopeMigration] {} id={} 无法确定 storeId (userId={})", entity, entityId, userId);
    }

    private <T> void saveIfNeeded(String entityName, List<T> entities, org.springframework.data.jpa.repository.JpaRepository<T, Long> repository) {
        if (entities.isEmpty()) {
            return;
        }
        repository.saveAll(entities);
        log.info("[StoreScopeMigration] 已为 {} 条 {} 记录补齐 store_id", entities.size(), entityName);
    }
}
