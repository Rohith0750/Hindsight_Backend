package com.hindsight.backend.controller;

import com.hindsight.backend.dto.ChatDto;
import com.hindsight.backend.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/v1/chat/message")
    public ResponseEntity<ChatDto.MessageResponse> handleSocraticMessage(@RequestBody ChatDto.MessageRequest request) {
        ChatDto.MessageResponse response = chatService.handleSocraticMessage(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/api/v1/chatbot/chat", "/api/v1/chat/chat"})
    public ResponseEntity<ChatDto.ChatbotResponse> policeChat(@RequestBody ChatDto.ChatbotRequest request) {
        ChatDto.ChatbotResponse response = chatService.policeChat(request.getMessage());
        return ResponseEntity.ok(response);
    }
}
