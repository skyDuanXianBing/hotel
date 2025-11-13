package server.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import server.demo.entity.Cleaner;
import server.demo.repository.CleanerRepository;

import java.util.List;

/**
 * 保洁员数据迁移 - 为现有的Cleaner记录添加密码字段
 * 由于新增了password字段,需要为现有记录设置默认密码
 */
@Component
public class CleanerDataMigration implements CommandLineRunner {

    @Autowired
    private CleanerRepository cleanerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 查找所有password为null的Cleaner记录
        List<Cleaner> cleanersWithoutPassword = cleanerRepository.findAll().stream()
                .filter(c -> c.getPassword() == null || c.getPassword().isEmpty())
                .toList();

        if (!cleanersWithoutPassword.isEmpty()) {
            System.out.println("===== 保洁员数据迁移：为现有记录添加密码 =====");
            System.out.println("发现 " + cleanersWithoutPassword.size() + " 个保洁员需要迁移");

            // 为每个保洁员设置默认密码: password123
            String defaultPassword = "password123";
            String encodedPassword = passwordEncoder.encode(defaultPassword);

            for (Cleaner cleaner : cleanersWithoutPassword) {
                cleaner.setPassword(encodedPassword);
                // 确保isActive已设置
                if (cleaner.getIsActive() == null) {
                    cleaner.setIsActive(true);
                }
            }

            cleanerRepository.saveAll(cleanersWithoutPassword);
            System.out.println("✓ 已为 " + cleanersWithoutPassword.size() + " 个保洁员添加默认密码");
            System.out.println("默认密码: " + defaultPassword);
            System.out.println("建议保洁员首次登录后立即修改密码");
        }
    }
}
