package com.trading.user_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false,unique = true)
    private String email;
    private String password;
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false,precision = 15,scale = 2)
    private BigDecimal walletBalance = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
