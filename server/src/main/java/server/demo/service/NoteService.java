package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.demo.context.StoreContextHolder;
import server.demo.dto.CreateNoteRequest;
import server.demo.dto.NoteDTO;
import server.demo.dto.NotesStatisticsDTO;
import server.demo.entity.Note;
import server.demo.entity.Room;
import server.demo.repository.NoteRepository;
import server.demo.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import server.demo.i18n.ApiMessages;
@Service
public class NoteService {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * 创建记一笔
     */
    @Transactional
    public NoteDTO createNote(CreateNoteRequest request) {
        Note note = new Note();
        note.setType(request.getType());
        note.setCategory(request.getCategory());
        note.setPaymentMethod(request.getPaymentMethod());
        note.setAmount(request.getAmount());
        note.setDatetime(request.getDatetime());
        note.setNotes(request.getNotes());

        // 关联房间
        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.57ab5ddad6df")));
            note.setRoom(room);
        }

        Note savedNote = noteRepository.save(note);
        return convertToDTO(savedNote);
    }

    /**
     * 获取记一笔列表
     */
    public List<NoteDTO> getNotesList(LocalDateTime startDate, LocalDateTime endDate,
                                      String type, String category, String paymentMethod, Long roomId) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        List<Note> notes;

        if (type != null && !type.isEmpty()) {
            notes = noteRepository.findByStoreIdAndDateRangeAndType(storeId, startDate, endDate, type);
        } else if (category != null && !category.isEmpty()) {
            notes = noteRepository.findByStoreIdAndDateRangeAndCategory(storeId, startDate, endDate, category);
        } else if (paymentMethod != null && !paymentMethod.isEmpty()) {
            notes = noteRepository.findByStoreIdAndDateRangeAndPaymentMethod(storeId, startDate, endDate, paymentMethod);
        } else if (roomId != null) {
            notes = noteRepository.findByStoreIdAndDateRangeAndRoomId(storeId, startDate, endDate, roomId);
        } else {
            notes = noteRepository.findByStoreIdAndDateRange(storeId, startDate, endDate);
        }

        return notes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取记一笔统计数据
     */
    public NotesStatisticsDTO getNotesStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        Long storeId = StoreContextHolder.getContext().getStoreId();
        List<Note> notes = noteRepository.findByStoreIdAndDateRange(storeId, startDate, endDate);

        NotesStatisticsDTO statistics = new NotesStatisticsDTO();

        // 计算总收入和总支出
        BigDecimal totalIncome = notes.stream()
                .filter(n -> "income".equals(n.getType()))
                .map(Note::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = notes.stream()
                .filter(n -> "expense".equals(n.getType()))
                .map(Note::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        statistics.setTotalIncome(totalIncome);
        statistics.setTotalExpense(totalExpense);
        statistics.setNetIncome(totalIncome.subtract(totalExpense));

        // 按项目分类统计
        statistics.setIncomeByProject(calculateCategoryStatistics(notes, "income"));
        statistics.setExpenseByProject(calculateCategoryStatistics(notes, "expense"));

        // 按支付方式统计
        statistics.setIncomeByPayment(calculatePaymentStatistics(notes, "income"));
        statistics.setExpenseByPayment(calculatePaymentStatistics(notes, "expense"));

        return statistics;
    }

    /**
     * 按项目分类统计
     */
    private List<NotesStatisticsDTO.CategoryStatistic> calculateCategoryStatistics(List<Note> notes, String type) {
        Map<String, BigDecimal> categoryMap = notes.stream()
                .filter(n -> type.equals(n.getType()))
                .collect(Collectors.groupingBy(
                        Note::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Note::getAmount, BigDecimal::add)
                ));

        return categoryMap.entrySet().stream()
                .map(e -> new NotesStatisticsDTO.CategoryStatistic(
                        getCategoryDisplayName(e.getKey()),
                        e.getValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 按支付方式统计
     */
    private List<NotesStatisticsDTO.PaymentStatistic> calculatePaymentStatistics(List<Note> notes, String type) {
        Map<String, BigDecimal> paymentMap = notes.stream()
                .filter(n -> type.equals(n.getType()))
                .collect(Collectors.groupingBy(
                        Note::getPaymentMethod,
                        Collectors.reducing(BigDecimal.ZERO, Note::getAmount, BigDecimal::add)
                ));

        return paymentMap.entrySet().stream()
                .map(e -> new NotesStatisticsDTO.PaymentStatistic(
                        getPaymentDisplayName(e.getKey()),
                        e.getValue()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取分类显示名称
     */
    private String getCategoryDisplayName(String category) {
        Map<String, String> categoryNames = new HashMap<>();
        // 收入项目
        categoryNames.put("catering", ApiMessages.get("api.t.647896b2c898"));
        categoryNames.put("tobacco_alcohol", ApiMessages.get("api.t.51e1d93f89c8"));
        categoryNames.put("compensation", ApiMessages.get("api.t.28a8136cef8f"));
        categoryNames.put("ticket", ApiMessages.get("api.t.f3c5df2035b7"));
        categoryNames.put("souvenir", ApiMessages.get("api.t.58486ddb4c6e"));
        categoryNames.put("other", ApiMessages.get("api.t.5f1596619d16"));
        // 支出项目
        categoryNames.put("utilities", ApiMessages.get("api.t.277bf57ca643"));
        categoryNames.put("rent_property", ApiMessages.get("api.t.c9453407c638"));
        categoryNames.put("salary", ApiMessages.get("api.t.13f3a58794d2"));
        categoryNames.put("maintenance", ApiMessages.get("api.t.86f001765057"));
        categoryNames.put("communication_transport", ApiMessages.get("api.t.9aa1271870b7"));
        categoryNames.put("daily_misc", ApiMessages.get("api.t.b6d8851f14a0"));
        return categoryNames.getOrDefault(category, category);
    }

    /**
     * 获取支付方式显示名称
     */
    private String getPaymentDisplayName(String paymentMethod) {
        Map<String, String> paymentNames = new HashMap<>();
        paymentNames.put("wechat", ApiMessages.get("api.t.68406df395e4"));
        paymentNames.put("alipay", ApiMessages.get("api.t.66f1177d677b"));
        paymentNames.put("cash", ApiMessages.get("api.t.6548450b8d16"));
        return paymentNames.getOrDefault(paymentMethod, paymentMethod);
    }

    /**
     * 获取记一笔详情
     */
    public NoteDTO getNoteById(Long id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(ApiMessages.get("api.t.70a84c454e2b")));
        return convertToDTO(note);
    }

    /**
     * 删除记一笔
     */
    @Transactional
    public void deleteNote(Long id) {
        noteRepository.deleteById(id);
    }

    /**
     * 转换为DTO
     */
    private NoteDTO convertToDTO(Note note) {
        NoteDTO dto = new NoteDTO();
        dto.setId(note.getId());
        dto.setType(note.getType());
        dto.setCategory(note.getCategory());
        dto.setPaymentMethod(note.getPaymentMethod());
        dto.setAmount(note.getAmount());
        dto.setDatetime(note.getDatetime());
        dto.setVoucherCount(note.getVoucherCount());
        dto.setNotes(note.getNotes());
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());

        if (note.getRoom() != null) {
            dto.setRoomId(note.getRoom().getId());
            dto.setRoomNumber(note.getRoom().getRoomNumber());
        }

        return dto;
    }
}