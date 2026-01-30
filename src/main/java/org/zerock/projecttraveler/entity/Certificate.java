package org.zerock.projecttraveler.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "certificate",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent;

    @Column(name = "quiz_percent", nullable = false)
    private Integer quizPercent;

    @Column(name = "certificate_number", length = 50, unique = true)
    private String certificateNumber;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (certificateNumber == null) {
            certificateNumber = generateCertificateNumber();
        }
    }

    private String generateCertificateNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomStr = String.format("%04d", (int)(Math.random() * 10000));
        return "CERT-" + dateStr + "-" + randomStr;
    }

    public String getIssuedDateDisplay() {
        if (issuedAt == null) return "";
        return issuedAt.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
    }

    public boolean isPdfGenerated() {
        return pdfPath != null && !pdfPath.isEmpty();
    }
}
