package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zerock.projecttraveler.entity.Course;
import org.zerock.projecttraveler.entity.CourseResource;
import org.zerock.projecttraveler.entity.CourseUnit;
import org.zerock.projecttraveler.entity.CourseEnrollment;
import org.zerock.projecttraveler.repository.CourseEnrollmentRepository;
import org.zerock.projecttraveler.repository.CourseRepository;
import org.zerock.projecttraveler.repository.CourseResourceRepository;
import org.zerock.projecttraveler.repository.CourseUnitRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseResourceService {

    private final CourseResourceRepository resourceRepository;
    private final CourseRepository courseRepository;
    private final CourseUnitRepository unitRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    @Value("${app.upload.image-path:C:/lms-uploads/images}")
    private String imageUploadPath;

    @Transactional
    public CourseResource uploadResource(Long courseId, Long unitId, String title, String description,
                                         MultipartFile file, Integer sortOrder) throws IOException {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("강좌를 찾을 수 없습니다."));

        CourseUnit unit = null;
        if (unitId != null) {
            unit = unitRepository.findById(unitId).orElse(null);
        }

        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String storedFileName = UUID.randomUUID() + extension;

        Path uploadDir = Paths.get(imageUploadPath, "course-resources");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        Path filePath = uploadDir.resolve(storedFileName);
        file.transferTo(filePath.toFile());

        CourseResource resource = CourseResource.builder()
                .course(course)
                .unit(unit)
                .title(title)
                .description(description)
                .originalFileName(originalFileName != null ? originalFileName : "unknown")
                .storedFileName(storedFileName)
                .filePath("/uploads/course-resources/" + storedFileName)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build();

        return resourceRepository.save(resource);
    }

    public List<CourseResource> getActiveResources(Long courseId) {
        return resourceRepository.findActiveByCourseId(courseId);
    }

    public List<CourseResource> getAllResources(Long courseId) {
        return resourceRepository.findAllByCourseIdWithUnit(courseId);
    }

    public Map<String, List<CourseResource>> groupByUnit(List<CourseResource> resources) {
        Map<String, List<CourseResource>> grouped = new LinkedHashMap<>();
        for (CourseResource r : resources) {
            String key = r.getUnit() != null ? r.getUnit().getTitle() : "공통 자료";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        return grouped;
    }

    @Transactional
    public CourseResource downloadResource(Long resourceId) {
        CourseResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("자료를 찾을 수 없습니다."));
        resource.incrementDownloadCount();
        return resource;
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        CourseResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("자료를 찾을 수 없습니다."));
        resource.setActive(false);
    }

    @Transactional
    public CourseResource updateResource(Long resourceId, String title, String description,
                                          Long unitId, Integer sortOrder) {
        CourseResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("자료를 찾을 수 없습니다."));

        resource.setTitle(title);
        resource.setDescription(description);
        if (sortOrder != null) resource.setSortOrder(sortOrder);

        if (unitId != null) {
            CourseUnit unit = unitRepository.findById(unitId).orElse(null);
            resource.setUnit(unit);
        } else {
            resource.setUnit(null);
        }

        return resource;
    }

    public boolean hasAccess(Long userId, Long courseId) {
        Optional<CourseEnrollment> enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId);
        return enrollment.isPresent() && enrollment.get().isAccessible();
    }

    public String getStoragePath() {
        return imageUploadPath + "/course-resources/";
    }
}
