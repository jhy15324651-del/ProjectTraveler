package org.zerock.projecttraveler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("activePage", "index");
        model.addAttribute("username", "사용자"); // 로그인 붙이면 교체
        return "index";
    }

    @GetMapping("/main")
    public String main(Model model) {
        model.addAttribute("activePage", "main");
        model.addAttribute("username", "사용자");
        return "main";
    }

    @GetMapping("/guide")
    public String guide(Model model) {
        model.addAttribute("activePage", "guide");
        model.addAttribute("username", "사용자");
        return "guide";
    }

    @GetMapping("/attendance")
    public String attendance(Model model) {
        model.addAttribute("activePage", "attendance");
        model.addAttribute("username", "사용자");
        return "attendance";
    }

    @GetMapping("/my-classroom")
    public String myClassroom(Model model) {
        model.addAttribute("activePage", "myClassroom");
        model.addAttribute("username", "사용자");
        return "my-classroom";
    }

    @GetMapping("/online-learning")
    public String onlineLearning(Model model) {
        model.addAttribute("activePage", "online-learning");
        model.addAttribute("username", "사용자");
        return "online-learning";
    }

    @GetMapping("/select")
    public String select(Model model) {
        model.addAttribute("activePage", "select");
        model.addAttribute("username", "사용자");
        return "select";
    }
}
