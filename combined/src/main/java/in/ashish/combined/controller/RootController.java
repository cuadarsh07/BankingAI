package in.ashish.combined.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Root Controller for uptime monitoring and cold-start wake-up pings.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Aethel Banking AI Security System",
                "timestamp", Instant.now().toString()
        ));
    }
}
