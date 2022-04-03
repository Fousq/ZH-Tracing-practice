package kz.zhanbolat.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ServerController {

    @Autowired
    private Tracer tracer;

    @GetMapping("/message")
    public ResponseEntity<MessageResponse> getMessage() {
        Span span = tracer.nextSpan().name("getMessage");
        try (Tracer.SpanInScope spanInScope = tracer.withSpan(span.start())) {
            MessageResponse messageResponse = new MessageResponse("hello");
            span.tag("message", messageResponse.getMessage());
            span.event("getMessage");
            return ResponseEntity.ok(messageResponse);
        } finally {
            span.end();
        }
    }

    private static class MessageResponse {
        private String message;

        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
