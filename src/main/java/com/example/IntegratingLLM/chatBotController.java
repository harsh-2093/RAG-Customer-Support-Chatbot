package com.example.IntegratingLLM;

import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1")
public class chatBotController {
    private final chatBotService chatBotService;

    public chatBotController(chatBotService chatBotService) {
        this.chatBotService = chatBotService;
    }
    @GetMapping("/query")
    public String query(@RequestParam String question){
        return chatBotService.answerUserQuery(question);
    }
}
