package com.trading.user_service.repository;

import com.trading.user_service.model.User;
import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String > {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(@Email(message = "Invalid email format") String email);
}
