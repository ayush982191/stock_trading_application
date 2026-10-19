package com.trading.portfolioservice.controller;

import com.trading.portfolioservice.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioService portfolioService;

    /**
     * get my portfolio with live data
     * @param userId
     * @return
     */
    @GetMapping("/me")
    private ResponseEntity<Map<String ,Object>> getMyPortfolio(
            @RequestHeader("X-User-Id") String userId
    ){
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }

    /**
     * for internal calls.
     * get portfolio by user id
     * @param userId
     * @return
     */
    @GetMapping("/{userId}")
    private ResponseEntity<Map<String ,Object>> getPortfolio(
        @PathVariable String userId
    ){
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }








}
