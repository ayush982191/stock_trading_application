package com.trading.user_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuthResponse {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private BigDecimal walletBalance;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";


}
