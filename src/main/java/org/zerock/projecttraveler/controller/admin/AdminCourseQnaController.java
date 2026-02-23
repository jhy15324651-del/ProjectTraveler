package org.zerock.projecttraveler.controller.admin;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.entity.CourseAnswer;
import org.zerock.projecttraveler.entity.CourseQuestion;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.CourseQnaService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/course-qna")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminCourseQnaController {

    private final CourseQnaService qnaService;

    @GetMapping
    public String qnaListPage(Model model) {
        List<CourseQuestion> questions = qnaService.getAllQuestions();

        model.addAttribute("questions", questions);
        model.addAttribute("activePage", "admin-qna");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);

        return "admin/qna-list";
    }

    @GetMapping("/{id}")
    public String qnaDetailPage(@PathVariable Long id, Model model) {
        CourseQuestion question = qnaService.getQuestionWithAnswers(id)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));

        model.addAttribute("question", question);
        model.addAttribute("activePage", "admin-qna");
        model.addAttribute("username", SecurityUtils.getCurrentUserDetails()
                .map(u -> u.getFullName()).orElse("관리자"));
        model.addAttribute("isAdmin", true);

        return "admin/qna-detail";
    }

    @PostMapping("/api/{questionId}/answer")
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> createAnswer(
            @PathVariable Long questionId,
            @RequestBody AnswerRequest request) {
        try {
            Long userId = SecurityUtils.getCurrentUserIdOrThrow();
            CourseAnswer answer = qnaService.createAnswer(questionId, userId, request.getContent(), request.getImageUrl());
            return ResponseEntity.ok(ApiResponse.success("답변이 등록되었습니다.", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/api/answer/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(@PathVariable Long id) {
        try {
            qnaService.deleteAnswer(id);
            return ResponseEntity.ok(ApiResponse.success("답변이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/api/question/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long id) {
        try {
            qnaService.deleteQuestion(id);
            return ResponseEntity.ok(ApiResponse.success("질문이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/api/upload-image")
    @ResponseBody
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("이미지 파일만 업로드 가능합니다."));
            }
            String imageUrl = qnaService.uploadImage(file);
            Map<String, String> data = new HashMap<>();
            data.put("imageUrl", imageUrl);
            return ResponseEntity.ok(ApiResponse.success("이미지가 업로드되었습니다.", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("이미지 업로드에 실패했습니다."));
        }
    }

    @Data
    public static class AnswerRequest {
        private String content;
        private String imageUrl;
    }
}
