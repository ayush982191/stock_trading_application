package com.trading.user_service.repository;

import com.trading.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,String > {

    boolean existsByEmail();
}
