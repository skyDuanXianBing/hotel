# 后端卡死应急处置 SOP（先抓现场，再重启）

适用：PMS 后端出现"线程打完入口日志后永远静默""某类请求集体超时但其他请求正常"等疑似线程挂死。

## 0. 铁律

**不要直接 pkill / 重启。** 2026-08-05 事故最大的遗憾就是没有线程转储，根因只能靠推断。先抓现场（1~3 分钟），再发版/重启。

## 1. 抓现场（按顺序，30 秒 × 3 次）

```bash
PID=$(pgrep -f 'server.demo.DemoApplication' | head -1)

# 线程转储 ×3（间隔 10s），点在同一路径即实锤
for i in 1 2 3; do jstack $PID > /tmp/jstack.$i.txt; sleep 10; done

# 快速看挂死线程在等什么（socket read / 锁 / DB）
grep -n 'exec-\|scheduling-\|task-' /tmp/jstack.1.txt | head -40
grep -B2 -A15 'RUNNABLE' /tmp/jstack.1.txt | grep -A15 'SocketInputStream\|socketRead\|lock\|park' | head -60
```

现场判读：
- 栈在 `SocketInputStream.socketRead0` + 持有业务事务 → 无超时的出站/DB 调用被黑洞（核对 `application.properties` 超时配置是否生效）。
- 栈在 `park`/`Unsafe.park` + `ReentrantLock` → 进程内锁。
- 栈在 MySQL 驱动 `ReadAheadInputStream` → DB 连接黑洞（远程 DB 重点怀疑中间层/conntrack）。

## 2. DB 侧辅助确认

```sql
-- 语句级现场（重启后消失，尽快执行）
SELECT * FROM information_schema.innodb_trx ORDER BY trx_started;
SELECT * FROM performance_schema.events_statements_current WHERE THREAD_ID IN ( ... );
SHOW ENGINE INNODB STATUS\G   -- LATEST DETECTED DEADLOCK

-- 慢查询日志默认关闭，建议生产常开（开销极小）
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 5;
-- 持久化：my.cnf 加 slow_query_log=1, long_query_time=5
```

## 3. 重启后必做

1. 保存 `/tmp/jstack.*.txt`、`application.log`、`su-reservation.log`、nginx access.log 对应窗口。
2. 核对事件表 `su_reservation_webhook_events`（RECEIVED/FAILED 残留会由补偿调度器自动追平，确认 `retry_count`/`last_error` 有留痕）。
3. 复盘时间线时先查：`Unexpected error occurred in scheduled task`、`[WebhookCompensate]`、`[ReservationUpsert] failed` 三组关键字。

## 4. 已知的无超时雷区（2026-08-05 后已加超时，回归时核对）

| 位置 | 配置 |
|---|---|
| JDBC | URL `connectTimeout=10000&socketTimeout=120000`（**生产 `DB_URL` env 覆盖默认 URL 时必须自带这两个参数**） |
| Su / PriceLabs / Geocoding / SmartLock HTTP | `TimeoutRestTemplateFactory`（connect 5s / read 30s） |
| SMTP | `mail.smtp.connectiontimeout/timeout/writetimeout`（application.properties） |
| APNs | `ApnsPushService` `setConnectionTimeout(10s)` + idle ping 15min |
| Embedding (OpenAI) | `LangChain4jMessageKnowledgeEmbeddingProvider` timeout 60s |
| Redis | `spring.data.redis.timeout=5000ms`（命令级） |
