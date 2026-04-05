package com.workflow.favorite.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.favorite.service.FavoriteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorite")
public class FavoriteController {
    
    private final FavoriteService favoriteService;
    
    // 즐겨찾기 등록, 해제 (토글)
    @PostMapping("/{taskId}")
    public ResponseEntity<Void> toggleFavorite(
            @PathVariable("taskId") Long taskId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) return ResponseEntity.status(401).build();

        Long userId = Long.parseLong(principal.getUsername());

        favoriteService.toggleFavorite(userId, taskId); 

        return ResponseEntity.ok().build();
    }
}