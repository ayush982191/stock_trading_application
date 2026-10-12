package com.trading.user_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String email;
//    private String password;
    private String firstName;
    private String lastName;
    private BigDecimal initialDeposit ;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
