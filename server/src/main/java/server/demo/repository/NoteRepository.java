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
     * 根据时间范围查询记一笔列表
     */
    @Query("SELECT n FROM Note n WHERE n.datetime BETWEEN :startDate AND :endDate ORDER BY n.datetime DESC")
    List<Note> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * 根据时间范围和类型查询
     */
    @Query("SELECT n FROM Note n WHERE n.datetime BETWEEN :startDate AND :endDate AND n.type = :type ORDER BY n.datetime DESC")
    List<Note> findByDateRangeAndType(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       @Param("type") String type);

    /**
     * 根据时间范围和项目类别查询
     */
    @Query("SELECT n FROM Note n WHERE n.datetime BETWEEN :startDate AND :endDate AND n.category = :category ORDER BY n.datetime DESC")
    List<Note> findByDateRangeAndCategory(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           @Param("category") String category);

    /**
     * 根据时间范围和支付方式查询
     */
    @Query("SELECT n FROM Note n WHERE n.datetime BETWEEN :startDate AND :endDate AND n.paymentMethod = :paymentMethod ORDER BY n.datetime DESC")
    List<Note> findByDateRangeAndPaymentMethod(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate,
                                                @Param("paymentMethod") String paymentMethod);

    /**
     * 根据房间ID查询
     */
    @Query("SELECT n FROM Note n WHERE n.room.id = :roomId ORDER BY n.datetime DESC")
    List<Note> findByRoomId(@Param("roomId") Long roomId);

    /**
     * 根据时间范围和房间ID查询
     */
    @Query("SELECT n FROM Note n WHERE n.datetime BETWEEN :startDate AND :endDate AND n.room.id = :roomId ORDER BY n.datetime DESC")
    List<Note> findByDateRangeAndRoomId(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate,
                                         @Param("roomId") Long roomId);
}