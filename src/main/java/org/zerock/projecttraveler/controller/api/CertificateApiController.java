package org.zerock.projecttraveler.controller.api;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.dto.CertificateDto;
import org.zerock.projecttraveler.security.SecurityUtils;
import org.zerock.projecttraveler.service.CertificateService;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateApiController {

    private final CertificateService certificateService;

    /**
     * 나의 수료증 목록
     */
    @GetMapping
    public ResponseEntity<?> getMyCertificates() {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        var certificates = certificateService.getMyCertificates(userId);
        return ResponseEntity.ok(ApiResponse.success(certificates));
    }

    /**
     * 수료증 상세 조회
     */
    @GetMapping("/{certificateId}")
    public ResponseEntity<?> getCertificate(@PathVariable Long certificateId) {
        return certificateService.getCertificate(certificateId)
                .map(cert -> ResponseEntity.ok(ApiResponse.success(cert)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 수료증 번호로 조회 (검증용)
     */
    @GetMapping("/verify/{certificateNumber}")
    public ResponseEntity<?> verifyCertificate(@PathVariable String certificateNumber) {
        return certificateService.getCertificateByNumber(certificateNumber)
                .map(cert -> ResponseEntity.ok(ApiResponse.success(cert)))
                .orElse(ResponseEntity.ok(ApiResponse.error("해당 수료증을 찾을 수 없습니다.")));
    }

    /**
     * 수료증 발급 자격 확인
     */
    @GetMapping("/eligibility/{courseId}")
    public ResponseEntity<?> checkEligibility(@PathVariable Long courseId) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();
        var eligibility = certificateService.checkEligibility(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success(eligibility));
    }

    /**
     * 수료증 발급 요청
     */
    @PostMapping("/issue")
    public ResponseEntity<?> issueCertificate(@RequestBody CertificateDto.IssueRequest request) {
        Long userId = SecurityUtils.getCurrentUserIdOrThrow();

        try {
            var result = certificateService.issueCertificate(userId, request.getCourseId());
            if (result.getSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error(result.getMessage()));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * QR 코드 이미지 생성 (PNG)
     * GET /api/certificates/qr/{certificateNumber}
     */
    @GetMapping(value = "/qr/{certificateNumber}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(@PathVariable String certificateNumber, HttpServletRequest request) {
        try {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
            String verifyUrl = baseUrl + "/certificate/verify/" + certificateNumber;

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(verifyUrl, BarcodeFormat.QR_CODE, 200, 200,
                    Map.of(EncodeHintType.MARGIN, 1));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
