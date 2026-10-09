package com.trading.user_service.service;


import com.trading.user_service.dto.AuthResponse;
import com.trading.user_service.dto.RegisterRequest;
import com.trading.user_service.model.User;
import com.trading.user_service.model.UserStatus;
import com.trading.user_service.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private static final String USER_REGISTERED_TOPIC = "user.registered";



    public AuthResponse register(@Valid RegisterRequest request) {
        log.info("registering user ",request.getEmail());
        if(userRepository.existsByEmail()){
            throw new RuntimeException("Email already registered "+request.getEmail());
        }
//        User user = User.builder()
//                .firstName(request.getFirstName())
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setWalletBalance(request.getInitialDeposit() != null ?
                request.getInitialDeposit() :
                BigDecimal.valueOf(1000)
                );
        user.setStatus(UserStatus.ACTIVE);
        User saveUser = userRepository.save(user);
        log.info("User registered : {} ",saveUser.getId());

        // publish use.regitered event.
        Map<String ,Object> userRegisterEvent = new HashMap<>();
        userRegisterEvent.put("userId",saveUser.getId());
        userRegisterEvent.put("email",saveUser.getEmail());
        userRegisterEvent.put("firstName",saveUser.getFirstName());
        userRegisterEvent.put("lastName",saveUser.getLastName());
        userRegisterEvent.put("walletBalance",saveUser.getWalletBalance());

        kafkaTemplate.send(USER_REGISTERED_TOPIC,saveUser.getId(),userRegisterEvent);



    }




}
