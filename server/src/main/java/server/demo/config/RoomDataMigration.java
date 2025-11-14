package server.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import server.demo.entity.Room;
import server.demo.entity.User;
import server.demo.repository.RoomRepository;
import server.demo.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 旧数据兼容：为缺少 userId 的房间补齐拥有者，保持向后兼容。
 */
@Component
public class RoomDataMigration implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(RoomDataMigration.class);

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public RoomDataMigration(RoomRepository roomRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        List<Room> roomsWithoutUser = roomRepository.findAll().stream()
                .filter(room -> room.getUserId() == null || room.getUserId() == 0L)
                .toList();

        if (roomsWithoutUser.isEmpty()) {
            return;
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("[RoomDataMigration] 系统中没有用户，跳过房间 userId 迁移");
            return;
        }

        log.info("[RoomDataMigration] 准备为 {} 个房间补齐 userId", roomsWithoutUser.size());
        List<Room> toSave = new ArrayList<>(roomsWithoutUser.size());
        int index = 0;
        for (Room room : roomsWithoutUser) {
            User assignedUser = users.get(index % users.size());
            room.setUserId(assignedUser.getId());
            toSave.add(room);
            index++;
        }

        roomRepository.saveAll(toSave);
        log.info("[RoomDataMigration] 已为 {} 个房间重新分配 userId", toSave.size());
    }
}
