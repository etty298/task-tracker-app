package ru.home.notificationservice.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.home.notificationservice.service.EmailService;

@Service
@RequiredArgsConstructor
public class UserEventConsumer {

    private final EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "user_event", groupId = "consumer")
    public void consume(String message) {
        System.out.println(message);
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.get("type").asText();
            String email = event.get("email").asText();
            String username = event.has("username") ? event.get("username").asText() : "";
            switch (eventType) {
                case "user_registered":
                    emailService.sendEmail(email, "Welcome to Task Tracker!",
                            "Hello " + username + ",\n\nThank you for registering!");
                    System.out.println("Sent");
                    break;
                default:
            }
        } catch (Exception e) {
            e.printStackTrace();
            // TODO
        }
    }
}
