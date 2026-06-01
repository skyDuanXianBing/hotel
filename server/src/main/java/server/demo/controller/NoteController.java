package server.demo.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import server.demo.annotation.StoreScoped;
import server.demo.dto.ApiResponse;
import server.demo.dto.CreateNoteRequest;
import server.demo.dto.NoteDTO;
import server.demo.dto.NotesStatisticsDTO;
import server.demo.service.NoteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notes")
@StoreScoped
public class NoteController {

    @Autowired
    private NoteService noteService;

    /**
     * 创建记一笔
     */
    @PostMapping
    public ApiResponse<NoteDTO> createNote(@Valid @RequestBody CreateNoteRequest request) {
        try {
            NoteDTO note = noteService.createNote(request);
            return ApiResponse.success("创建成功", note);
        } catch (Exception e) {
            return ApiResponse.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 获取记一笔列表
     */
    @GetMapping
    public ApiResponse<List<NoteDTO>> getNotesList(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) Long roomId) {

        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

            List<NoteDTO> notes = noteService.getNotesList(start, endExclusive, type, category, paymentMethod, roomId);
            return ApiResponse.success(notes);
        } catch (Exception e) {
            return ApiResponse.error("获取列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取记一笔统计数据
     */
    @GetMapping("/statistics")
    public ApiResponse<NotesStatisticsDTO> getNotesStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();

            NotesStatisticsDTO statistics = noteService.getNotesStatistics(start, endExclusive);
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error("获取统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取记一笔详情
     */
    @GetMapping("/{id}")
    public ApiResponse<NoteDTO> getNoteById(@PathVariable Long id) {
        try {
            NoteDTO note = noteService.getNoteById(id);
            return ApiResponse.success(note);
        } catch (Exception e) {
            return ApiResponse.error("获取详情失败: " + e.getMessage());
        }
    }

    /**
     * 删除记一笔
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteNote(@PathVariable Long id) {
        try {
            noteService.deleteNote(id);
            return ApiResponse.success("删除成功", "删除成功");
        } catch (Exception e) {
            return ApiResponse.error("删除失败: " + e.getMessage());
        }
    }
}
