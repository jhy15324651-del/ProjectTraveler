package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.projecttraveler.entity.*;
import org.zerock.projecttraveler.repository.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseQnaService {

    private final CourseQuestionRepository questionRepository;
    private final CourseAnswerRepository answerRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    @Value("${app.upload.image-path:C:/lms-uploads/images}")
    private String imageUploadPath;

    @Transactional
    public CourseQuestion createQuestion(Long courseId, Long userId, String title, String content, String imageUrl) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        CourseQuestion question = CourseQuestion.builder()
                .course(course)
                .user(user)
                .title(title)
                .content(content)
                .imageUrl(imageUrl)
                .build();

        return questionRepository.save(question);
    }

    public List<CourseQuestion> getQuestionsByCourse(Long courseId) {
        return questionRepository.findByCourseIdWithUser(courseId);
    }

    public Optional<CourseQuestion> getQuestionWithAnswers(Long questionId) {
        return questionRepository.findByIdWithUserAndAnswers(questionId);
    }

    public List<CourseQuestion> getAllQuestions() {
        return questionRepository.findAllWithUserAndCourse();
    }

    @Transactional
    public CourseAnswer createAnswer(Long questionId, Long userId, String content, String imageUrl) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        CourseAnswer answer = CourseAnswer.builder()
                .question(question)
                .user(user)
                .content(content)
                .imageUrl(imageUrl)
                .build();

        CourseAnswer saved = answerRepository.save(answer);
        question.setAnswered(true);
        return saved;
    }

    @Transactional
    public void deleteAnswer(Long answerId) {
        CourseAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("답변을 찾을 수 없습니다."));
        Long questionId = answer.getQuestion().getId();
        answerRepository.delete(answer);

        // 답변이 더 이상 없으면 answered=false
        long remaining = answerRepository.countByQuestionId(questionId);
        if (remaining == 0) {
            questionRepository.findById(questionId).ifPresent(q -> q.setAnswered(false));
        }
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다."));
        questionRepository.delete(question);
    }

    public String uploadImage(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String storedFileName = UUID.randomUUID() + extension;

        Path uploadDir = Paths.get(imageUploadPath, "qna");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        Path filePath = uploadDir.resolve(storedFileName);
        file.transferTo(filePath.toFile());

        return "/uploads/qna/" + storedFileName;
    }

    public boolean hasAccess(Long userId, Long courseId) {
        Optional<CourseEnrollment> enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        return enrollment.isPresent() && enrollment.get().isAccessible();
    }
}
