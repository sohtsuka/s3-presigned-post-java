package com.example.demo;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SignedPostController {
    private UploaderService uploaderService;

    public SignedPostController(UploaderService uploaderService) {
        this.uploaderService = uploaderService;
    }

    @GetMapping("/signed-post")
    public PresignedPost get() {
        String key = UUID.randomUUID().toString();
        return uploaderService.createPresignedPost(key);
    }
}
