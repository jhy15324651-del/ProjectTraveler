package org.zerock.projecttraveler.dto;

import lombok.*;
import org.zerock.projecttraveler.entity.Certificate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CertificateDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateInfo {
        private Long id;
        private Long courseId;
        private String courseTitle;
        private String courseThumbnail;
        private String certificateNumber;
        private Integer progressPercent;
        private Integer quizPercent;
        private String issuedAt;
        private Boolean pdfAvailable;
        private String userName;

        public static CertificateInfo from(Certificate certificate) {
            return CertificateInfo.builder()
                    .id(certificate.getId())
                    .courseId(certificate.getCourse().getId())
                    .courseTitle(certificate.getCourse().getTitle())
                    .courseThumbnail(certificate.getCourse().getThumbnailUrl())
                    .certificateNumber(certificate.getCertificateNumber())
                    .progressPercent(certificate.getProgressPercent())
                    .quizPercent(certificate.getQuizPercent())
                    .issuedAt(certificate.getIssuedAt() != null ?
                            certificate.getIssuedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) : null)
                    .pdfAvailable(certificate.isPdfGenerated())
                    .userName(certificate.getUser().getFullName() != null ?
                            certificate.getUser().getFullName() : certificate.getUser().getUsername())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EligibilityCheck {
        private Long courseId;
        private String courseTitle;
        private Boolean eligible;
        private Integer progressPercent;
        private Integer quizPercent;
        private Integer requiredProgress;
        private Integer requiredQuiz;
        private String message;
        private Boolean alreadyIssued;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueRequest {
        private Long courseId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueResult {
        private Boolean success;
        private String certificateNumber;
        private String message;
    }
}
