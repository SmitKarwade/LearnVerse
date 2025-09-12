package com.example.learnverse.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GeneralController {

    @GetMapping("/ping")
    public Object ping(Authentication auth) {
        return new Object() {
            public final String message = "ok";
            public final String userId = (String) auth.getPrincipal();
            public final Object authorities = auth.getAuthorities();
        };
    }

    @GetMapping("/hello")
    public Object hello() {
        return new Object() {
            public final String message = "Hi developers!";
        };
    }

    @GetMapping("/debug/token")
    public Object debugToken(Authentication auth) {
        return auth;
    }
}

