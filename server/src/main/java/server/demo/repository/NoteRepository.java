package server.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import server.demo.entity.Note;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * 根据门店ID查询所有记一笔
     */
    List<Note> findByStoreId(Long storeId);

    /**
     * 根据门店ID和时间范围查询记一笔列表
     */
    @Query("SELECT n FROM Note n WHERE n.storeId = :storeId AND n.datetime >= :startDate AND n.datetime < :endDate ORDER BY n.datetime DESC")
    List<Note> findByStoreIdAndDateRange(@Param("storeId") Long storeId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 根据门店ID、时间范围和类型查询
     */
    @Query("SELECT n FROM Note n WHERE n.storeId = :storeId AND n.datetime >= :startDate AND n.datetime < :endDate AND n.type = :type ORDER BY n.datetime DESC")
    List<Note> findByStoreIdAndDateRangeAndType(@Param("storeId") Long storeId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 @Param("type") String type);

    /**
     * 根据门店ID、时间范围和项目类别查询
     */
    @Query("SELECT n FROM Note n WHERE n.storeId = :storeId AND n.datetime >= :startDate AND n.datetime < :endDate AND n.category = :category ORDER BY n.datetime DESC")
    List<Note> findByStoreIdAndDateRangeAndCategory(@Param("storeId") Long storeId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     @Param("category") String category);

    /**
     * 根据门店ID、时间范围和支付方式查询
     */
    @Query("SELECT n FROM Note n WHERE n.storeId = :storeId AND n.datetime >= :startDate AND n.datetime < :endDate AND n.paymentMethod = :paymentMethod ORDER BY n.datetime DESC")
    List<Note> findByStoreIdAndDateRangeAndPaymentMethod(@Param("storeId") Long storeId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate,
                                                          @Param("paymentMethod") String paymentMethod);

    /**
     * 根据门店ID和房间ID查询
     */
    @Query("SELECT n FROM Note n WHERE n.storeId = :storeId AND n.room.id = :roomId ORDER BY n.datetime DESC")
    List<Note> findByStoreIdAndRoomId(@Param("storeId") Long storeId,
                                       @Param("roomId") Long roomId);

    /**
     * 根据门店ID、时间范围和房间ID查询
     */
    @Query("SELECT n FROM Note n WHERE n.storeId = :storeId AND n.datetime >= :startDate AND n.datetime < :endDate AND n.room.id = :roomId ORDER BY n.datetime DESC")
    List<Note> findByStoreIdAndDateRangeAndRoomId(@Param("storeId") Long storeId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate,
                                                   @Param("roomId") Long roomId);
}
