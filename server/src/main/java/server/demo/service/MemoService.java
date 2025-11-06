package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.Memo;
import server.demo.entity.User;
import server.demo.repository.MemoRepository;
import server.demo.repository.UserRepository;

import java.util.Optional;

@Service
public class MemoService {

    @Autowired
    private MemoRepository memoRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 保存或更新用户备忘录
     * 每个用户只有一条备忘录记录
     */
    @Transactional
    public Memo saveMemo(Long userId, String content) {
        // 查找用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 查找是否已有备忘录
        Optional<Memo> existingMemo = memoRepository.findByUserId(userId);

        Memo memo;
        if (existingMemo.isPresent()) {
            // 更新现有备忘录
            memo = existingMemo.get();
            memo.setContent(content);
        } else {
            // 创建新备忘录
            memo = new Memo(user, content);
        }

        return memoRepository.save(memo);
    }

    /**
     * 获取用户备忘录
     */
    public String getMemo(Long userId) {
        Optional<Memo> memo = memoRepository.findByUserId(userId);
        return memo.map(Memo::getContent).orElse("");
    }

    /**
     * 获取用户备忘录实体
     */
    public Optional<Memo> getMemoEntity(Long userId) {
        return memoRepository.findByUserId(userId);
    }
}
