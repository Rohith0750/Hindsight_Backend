package com.hindsight.backend.controller;

import com.hindsight.backend.model.Sos;
import com.hindsight.backend.model.User;
import com.hindsight.backend.repository.UserRepository;
import com.hindsight.backend.security.JwtTokenProvider;
import com.hindsight.backend.service.SosService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
public class SosController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SosService sosService;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public SosController(SimpMessagingTemplate messagingTemplate,
                         SosService sosService,
                         UserRepository userRepository,
                         JwtTokenProvider tokenProvider) {
        this.messagingTemplate = messagingTemplate;
        this.sosService = sosService;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
    }

    @MessageMapping("/sendSOS")
    public void handleSendSos(SosPayload payload) {
        try {
            log.info("Received WebSocket SOS payload: {}", payload);
            String token = payload.getToken();

            if (token != null && tokenProvider.validateToken(token)) {
                Map<String, String> userData = tokenProvider.getUserDataFromToken(token);
                String userId = userData.get("id");

                if (userId != null) {
                    User user = userRepository.findById(userId).orElse(null);
                    String stationId = user != null ? user.getRole() : "default"; // Mock/real station resolving

                    Sos.Location location = Sos.Location.builder()
                            .latitude(payload.getLocation().getLatitude())
                            .longitude(payload.getLocation().getLongitude())
                            .build();

                    Sos sos = Sos.builder()
                            .triggeredBy(userId)
                            .stationId(stationId)
                            .location(location)
                            .emergencyType(payload.getEmergency_type())
                            .build();

                    Sos savedSos = sosService.saveSos(sos);

                    // Broadcast to topic
                    messagingTemplate.convertAndSend("/topic/newSOS", savedSos);
                    log.info("Broadcasted SOS to /topic/newSOS");
                }
            } else {
                log.warn("Invalid token received in WebSocket SOS");
            }
        } catch (Exception e) {
            log.error("Failed to handle WebSocket SOS message", e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SosPayload {
        private String token;
        private LocationPayload location;
        private String emergency_type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationPayload {
        private Double latitude;
        private Double longitude;
    }
}
