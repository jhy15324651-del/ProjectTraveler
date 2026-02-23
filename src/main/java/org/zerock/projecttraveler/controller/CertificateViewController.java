package org.zerock.projecttraveler.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.zerock.projecttraveler.dto.CertificateDto;
import org.zerock.projecttraveler.service.CertificateService;

import java.util.Optional;

@Controller
@RequestMapping("/certificate")
@RequiredArgsConstructor
public class CertificateViewController {

    private final CertificateService certificateService;

    /**
     * 수료증 보딩패스 뷰
     * GET /certificate/view/{id}
     */
    @GetMapping("/view/{id}")
    public String viewCertificate(@PathVariable Long id, Model model) {
        Optional<CertificateDto.CertificateInfo> cert = certificateService.getCertificateForView(id);
        if (cert.isEmpty()) {
            return "redirect:/mypage";
        }
        model.addAttribute("cert", cert.get());
        return "certificate/boarding-pass-certificate";
    }

    /**
     * 수료증 공개 검증 페이지 (비로그인 접근 가능)
     * GET /certificate/verify/{code}
     */
    @GetMapping("/verify/{code}")
    public String verifyCertificate(@PathVariable String code, Model model) {
        Optional<CertificateDto.CertificateInfo> cert = certificateService.getCertificateByNumber(code);
        model.addAttribute("cert", cert.orElse(null));
        model.addAttribute("valid", cert.isPresent());
        model.addAttribute("code", code);
        return "certificate/verify";
    }
}
