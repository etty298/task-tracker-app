package ru.home.authentication.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserEventProducer {

    private static final String TOPIC = "user_event";

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendUserRegisteredEvent(String email, String username) {
        sendEvent("user_registered", email, username);
    }

    private void sendEvent(String type, String email, String username) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", type);
            event.put("email", email);
            if (username != null)
                event.put("username", username);

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, json);
        } catch (Exception e) {
            // TODO
        }
    }
}
