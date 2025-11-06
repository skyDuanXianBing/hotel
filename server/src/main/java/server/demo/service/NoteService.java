package server.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
                    .orElseThrow(() -> new RuntimeException("房间不存在"));
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
        List<Note> notes;

        if (type != null && !type.isEmpty()) {
            notes = noteRepository.findByDateRangeAndType(startDate, endDate, type);
        } else if (category != null && !category.isEmpty()) {
            notes = noteRepository.findByDateRangeAndCategory(startDate, endDate, category);
        } else if (paymentMethod != null && !paymentMethod.isEmpty()) {
            notes = noteRepository.findByDateRangeAndPaymentMethod(startDate, endDate, paymentMethod);
        } else if (roomId != null) {
            notes = noteRepository.findByDateRangeAndRoomId(startDate, endDate, roomId);
        } else {
            notes = noteRepository.findByDateRange(startDate, endDate);
        }

        return notes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取记一笔统计数据
     */
    public NotesStatisticsDTO getNotesStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Note> notes = noteRepository.findByDateRange(startDate, endDate);

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
        categoryNames.put("catering", "餐饮美食");
        categoryNames.put("tobacco_alcohol", "烟酒饮料");
        categoryNames.put("compensation", "物品赔付");
        categoryNames.put("ticket", "景点门票");
        categoryNames.put("souvenir", "特色纪念品");
        categoryNames.put("other", "其他杂项");
        // 支出项目
        categoryNames.put("utilities", "水电燃气");
        categoryNames.put("rent_property", "房租物业费");
        categoryNames.put("salary", "支付工资");
        categoryNames.put("maintenance", "房间维修");
        categoryNames.put("communication_transport", "通讯交通");
        categoryNames.put("daily_misc", "日常杂项");
        return categoryNames.getOrDefault(category, category);
    }

    /**
     * 获取支付方式显示名称
     */
    private String getPaymentDisplayName(String paymentMethod) {
        Map<String, String> paymentNames = new HashMap<>();
        paymentNames.put("wechat", "微信");
        paymentNames.put("alipay", "支付宝");
        paymentNames.put("cash", "现金");
        return paymentNames.getOrDefault(paymentMethod, paymentMethod);
    }

    /**
     * 获取记一笔详情
     */
    public NoteDTO getNoteById(Long id) {
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("记账记录不存在"));
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