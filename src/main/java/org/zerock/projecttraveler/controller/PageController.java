package org.zerock.projecttraveler.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/index"})
    public String index() {
        return "index"; // templates/index.html
    }

    @GetMapping("/main")
    public String main() {
        return "main";
    }

    @GetMapping("/guide")
    public String guide() {
        return "guide";
    }

    @GetMapping("/attendance")
    public String attendance() {
        return "attendance";
    }

    @GetMapping("/my-classroom")
    public String myClassroom() {
        return "my-classroom";
    }

    @GetMapping("/online-learning")
    public String onlineLearning() {
        return "online-learning";
    }

    @GetMapping("/select")
    public String select() {
        return "select";
    }
}
