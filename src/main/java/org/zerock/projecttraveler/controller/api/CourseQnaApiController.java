package org.zerock.projecttraveler.controller.api;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.entity.CourseAnswer;
import org.zerock.projecttraveler.entity.CourseQuestion;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.CourseQnaService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/course-qna")
@RequiredArgsConstructor
@Slf4j
public class CourseQnaApiController {

    private final CourseQnaService qnaService;

    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Object>> getQuestions(@PathVariable Long courseId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!SecurityUtils.isAdmin() && !qnaService.hasAccess(userId, courseId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("수강 권한이 없습니다."));
        }

        List<CourseQuestion> questions = qnaService.getQuestionsByCourse(courseId);
        List<Map<String, Object>> result = questions.stream().map(this::toQuestionMap).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/question/{id}")
    public ResponseEntity<ApiResponse<Object>> getQuestionDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        CourseQuestion question = qnaService.getQuestionWithAnswers(id)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));

        if (!SecurityUtils.isAdmin() && !qnaService.hasAccess(userId, question.getCourse().getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("수강 권한이 없습니다."));
        }

        Map<String, Object> result = toQuestionMap(question);
        result.put("content", question.getContent());
        result.put("imageUrl", question.getImageUrl());
        result.put("answers", question.getAnswers().stream().map(a -> {
            Map<String, Object> answerMap = new LinkedHashMap<>();
            answerMap.put("id", a.getId());
            answerMap.put("content", a.getContent());
            answerMap.put("imageUrl", a.getImageUrl());
            answerMap.put("userName", a.getUser().getFullName() != null ? a.getUser().getFullName() : a.getUser().getUsername());
            answerMap.put("userRole", a.getUser().getRole().name());
            answerMap.put("createdAt", a.getCreatedAt().toString());
            return answerMap;
        }).collect(Collectors.toList()));

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Object>> createQuestion(
            @PathVariable Long courseId,
            @RequestBody QuestionRequest request) {
        try {
            Long userId = SecurityUtils.getCurrentUserIdOrThrow();
            if (!SecurityUtils.isAdmin() && !qnaService.hasAccess(userId, courseId)) {
                return ResponseEntity.status(403).body(ApiResponse.error("수강 권한이 없습니다."));
            }

            CourseQuestion question = qnaService.createQuestion(
                    courseId, userId, request.getTitle(), request.getContent(), request.getImageUrl());

            return ResponseEntity.ok(ApiResponse.success("질문이 등록되었습니다.", toQuestionMap(question)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/question/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable Long id) {
        try {
            Long userId = SecurityUtils.getCurrentUserIdOrThrow();
            CourseQuestion question = qnaService.getQuestionWithAnswers(id)
                    .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));

            // 본인 또는 관리자만 삭제 가능
            if (!SecurityUtils.isAdmin() && !question.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(ApiResponse.error("삭제 권한이 없습니다."));
            }

            qnaService.deleteQuestion(id);
            return ResponseEntity.ok(ApiResponse.success("질문이 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 이미지 파일만 허용
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

    private Map<String, Object> toQuestionMap(CourseQuestion q) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", q.getId());
        map.put("title", q.getTitle());
        map.put("userName", q.getUser().getFullName() != null ? q.getUser().getFullName() : q.getUser().getUsername());
        map.put("answered", q.getAnswered());
        map.put("createdAt", q.getCreatedAt().toString());
        return map;
    }

    @Data
    public static class QuestionRequest {
        private String title;
        private String content;
        private String imageUrl;
    }
}
