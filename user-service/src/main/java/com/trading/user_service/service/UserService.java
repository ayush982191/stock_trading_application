package com.trading.user_service.service;


import com.trading.user_service.dto.AuthResponse;
import com.trading.user_service.dto.LoginRequest;
import com.trading.user_service.dto.RegisterRequest;
import com.trading.user_service.dto.UserResponse;
import com.trading.user_service.model.User;
import com.trading.user_service.model.UserStatus;
import com.trading.user_service.repository.UserRepository;
// import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private static final String USER_REGISTERED_TOPIC = "user.register";
    private final PasswordEncoder passwordEncoder;

    @Value("${initial_deposit}")
    private BigDecimal initialDeposit;

    @Value("${jwt.secret:myDefaultSecretKeyThatIsAtLeast32CharsLong}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;


    /**
     * register
     * @param request
     * @return
     */
    public AuthResponse register(@Valid RegisterRequest request) {
        log.info("registering user {} ",request.getEmail());
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if(userRepository.existsByEmail(normalizedEmail)){
            throw new RuntimeException("Email already registered "+request.getEmail());
        }
//        User user = User.builder()
//                .firstName(request.getFirstName())
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setWalletBalance(initialDeposit
                );
        user.setStatus(UserStatus.ACTIVE);
//        User saveUser = userRepository.save(user);
        User saveUser;
        try{
            saveUser = userRepository.saveAndFlush(user);
            log.info("User registered : {} ",saveUser.getId());
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent registration collision detected for email: {}", request.getEmail());
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }


        // publish use.regitered event.
        Map<String ,Object> userRegisterEvent = new HashMap<>();
        userRegisterEvent.put("userId",saveUser.getId());
        userRegisterEvent.put("email",saveUser.getEmail());
        userRegisterEvent.put("firstName",saveUser.getFirstName());
        userRegisterEvent.put("lastName",saveUser.getLastName());
        userRegisterEvent.put("walletBalance",saveUser.getWalletBalance());

        kafkaTemplate.send(USER_REGISTERED_TOPIC,saveUser.getId(),userRegisterEvent);
        String token =   generateAccessToken(saveUser.getId(),saveUser.getEmail());
        String refreshToken=   generateRefreshToken(saveUser.getId(),saveUser.getEmail());
        return buildAuthResponse(saveUser,token,refreshToken);

    }
    /**
     * login
     */
    public AuthResponse login(LoginRequest request){
        log.info("Login attempt {} ",request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()->new RuntimeException("User not found"));
        log.info("request password "+request.getPassword()+" and user password "+user.getPassword());
        if(!passwordEncoder.matches(
                request.getPassword(), user.getPassword()
        )){
            throw new RuntimeException("Invalid credentials");
        }
        log.info("Login successful {} ",user.getId());
        String token = generateAccessToken(user.getId(),user.getEmail());
        String refreshToken=  generateRefreshToken(user.getId(),user.getEmail());
        return buildAuthResponse(user,token,refreshToken);
    }

    private String generateRefreshToken(String id, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(email)
                .claim("userId", id)
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    private String generateAccessToken(String id, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(email)
                .claim("userId", id)
                .claim("tokenType", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }


    /**
     * get user by id,
     * called by other services by feign, internal
     * @param userId
     * @return
     */
    public UserResponse getUserById(
            @PathVariable String userId){
        User user= userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("user not found "+userId))
                ;
        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setInitialDeposit(user.getWalletBalance());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        // Never set password here to prevent leaking credentials
        return response;
    }
    private AuthResponse buildAuthResponse(User user, String token, String refreshToken) {
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setWalletBalance(user.getWalletBalance());
        response.setAccessToken(token);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        return response;
    }
    public UserResponse addFunds(String userId, BigDecimal amount){
        User user= userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("user not found "+userId))
                ;
        user.setWalletBalance(user.getWalletBalance().add(amount));
        User savedUser = userRepository.save(user);
        log.info("funds added {} to user {} ",amount,user);
        return mapToResponse(savedUser);

    }

    /**
     * deduct funds on wallet and call on buy order.
     * @param userId
     * @param amount
     * @return
     */
    @Transactional
    public UserResponse deductFunds(
            String userId, BigDecimal amount
    ){
         if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("Deduction amount must be strictly positive");
    }
        User user= userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("user not found "+userId))
                ;
        int rowsUpdated = userRepository.deductBalance(userId,amount);
        if(rowsUpdated == 0){
            throw new RuntimeException(String.format("Insufficient balance. Required: %s, Current: %s", amount, user.getWalletBalance()));

        }
        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found " + userId));
        log.info("Successfully deducted {} from user {}", amount, userId);
        return mapToResponse(updatedUser);

    }
    /*
        credit funds to wallet called on SELL order
     */
@Transactional
    public UserResponse creditFunds(String userId, BigDecimal amount){
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deduction amount must be strictly positive");
        }
        User user= userRepository.findById(userId)
                .orElseThrow(()->new RuntimeException("user not found "+userId))
                ;
        int rowsUpdated = userRepository.creditBalance(userId,amount);
        if(rowsUpdated == 0){
            throw new RuntimeException(String.format("user not found: %s", user));

        }
        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("user not found " + userId));
        log.info("Successfully credited {} from user {}", amount, userId);
        return mapToResponse(updatedUser);
    }
}
