package server.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.demo.entity.AutoMessageSendLog;
import server.demo.repository.AutoMessageSendLogRepository;

/**
 * 自动消息发送日志的"抢占写入"服务。
 *
 * 必须放在独立 bean 上，REQUIRES_NEW 才会经过代理生效；
 * 唯一键冲突异常必须穿出本方法边界，Spring 才会回滚并丢弃这个独立 session，
 * 避免把外层业务事务（例如 webhook 预订入库）标记为 rollback-only。
 */
@Service
public class AutoMessageSendLogClaimService {

    private final AutoMessageSendLogRepository sendLogRepository;

    public AutoMessageSendLogClaimService(AutoMessageSendLogRepository sendLogRepository) {
        this.sendLogRepository = sendLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AutoMessageSendLog insertClaim(Long storeId, String action, String targetType,
                                          Long targetId, Long autoMessageId) {
        AutoMessageSendLog log = new AutoMessageSendLog();
        log.setStoreId(storeId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAutoMessageId(autoMessageId);
        return sendLogRepository.saveAndFlush(log);
    }
}
