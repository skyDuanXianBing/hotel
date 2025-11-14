package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContextHolder;
import server.demo.entity.Memo;
import server.demo.repository.MemoRepository;

import java.util.Optional;

@Service
public class MemoService {

    @Autowired
    private MemoRepository memoRepository;

    /**
     * 保存或更新门店备忘录（门店级架构）
     * 每个门店只有一条备忘录记录
     */
    @Transactional
    public Memo saveMemo(String content) {
        Long storeId = StoreContextHolder.getContext().getStoreId();

        // 查找是否已有备忘录
        Optional<Memo> existingMemo = memoRepository.findByStoreId(storeId);

        Memo memo;
        if (existingMemo.isPresent()) {
            // 更新现有备忘录
            memo = existingMemo.get();
            memo.setContent(content);
        } else {
            // 创建新备忘录（storeId由StoreScopedEntityListener自动注入）
            memo = new Memo();
            memo.setContent(content);
        }

        return memoRepository.save(memo);
    }

    /**
     * 获取门店备忘录内容
     */
    public String getMemo() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        Optional<Memo> memo = memoRepository.findByStoreId(storeId);
        return memo.map(Memo::getContent).orElse("");
    }

    /**
     * 获取门店备忘录实体
     */
    public Optional<Memo> getMemoEntity() {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        return memoRepository.findByStoreId(storeId);
    }
}
