package com.mud.api.controller;

import com.mud.service.DigestSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/digest")
@RequiredArgsConstructor
public class DigestController {

    private final DigestSubscriptionService subscriptionService;

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribe(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "이메일을 입력해주세요."));
        }
        String result = subscriptionService.subscribe(email.trim().toLowerCase());
        return ResponseEntity.ok(Map.of("message", result));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token) {
        String result = subscriptionService.verify(token);
        return ResponseEntity.ok("<html><body style='font-family:sans-serif;text-align:center;padding:60px;'>"
            + "<h2>⚗️ Mud</h2><p>" + result + "</p></body></html>");
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestParam String token) {
        String result = subscriptionService.unsubscribe(token);
        return ResponseEntity.ok("<html><body style='font-family:sans-serif;text-align:center;padding:60px;'>"
            + "<h2>⚗️ Mud</h2><p>" + result + "</p></body></html>");
    }
}
