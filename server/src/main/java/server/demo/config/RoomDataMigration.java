package server.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import server.demo.entity.Room;
import server.demo.entity.User;
import server.demo.repository.RoomRepository;
import server.demo.repository.UserRepository;

import java.util.List;
import java.util.Optional;

/**
 * 房间数据迁移 - 为现有的Room记录添加user_id字段
 * 由于新增了user_id字段用于数据隔离,需要为现有记录分配用户
 */
@Component
public class RoomDataMigration implements CommandLineRunner {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // 查找所有user_id为null或0的Room记录
        List<Room> roomsWithoutUser = roomRepository.findAll().stream()
                .filter(r -> r.getUserId() == null || r.getUserId() == 0L)
                .toList();

        if (!roomsWithoutUser.isEmpty()) {
            System.out.println("===== 房间数据迁移：为现有房间分配用户 =====");
            System.out.println("发现 " + roomsWithoutUser.size() + " 个房间需要迁移(userId为null或0)");

            // 获取系统中的第一个用户(假设为管理员)
            Optional<User> firstUser = userRepository.findAll().stream().findFirst();

            if (firstUser.isEmpty()) {
                System.out.println("⚠ 系统中没有用户,跳过房间迁移");
                return;
            }

            Long adminUserId = firstUser.get().getId();
            String adminEmail = firstUser.get().getEmail();

            System.out.println("将为房间分配给用户: " + adminEmail + " (ID: " + adminUserId + ")");

            // 为每个房间分配用户
            for (Room room : roomsWithoutUser) {
                room.setUserId(adminUserId);
            }

            roomRepository.saveAll(roomsWithoutUser);
            System.out.println("✓ 已为 " + roomsWithoutUser.size() + " 个房间分配用户");
            System.out.println("所有房间现在已与用户关联,支持多用户数据隔离");
        }
    }
}
