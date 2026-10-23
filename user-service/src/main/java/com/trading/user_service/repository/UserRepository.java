package com.trading.user_service.repository;

import com.trading.user_service.model.User;
import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String > {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(@Email(message = "Invalid email format") String email);

    // to handle concurrency
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.walletBalance = u.walletBalance - :amount " +
           "WHERE u.id = :userId AND u.walletBalance >= :amount")
    int deductBalance(@Param("userId") String userId, @Param("amount") BigDecimal amount);

    // to handle concurrency
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.walletBalance = u.walletBalance + :amount WHERE u.id = :userId")
    int creditBalance(@Param("userId") String userId, @Param("amount") BigDecimal amount);
}
