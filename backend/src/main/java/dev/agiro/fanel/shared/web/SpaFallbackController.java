package dev.agiro.fanel.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaFallbackController {
    @GetMapping({"/", "/menu", "/calendari", "/compra", "/tasques", "/households",
            "/households/{id}", "/**/{path:[^\\.]*}"})
    public String index() {
        return "forward:/index.html";
    }
}
