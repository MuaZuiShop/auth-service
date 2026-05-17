package com.us.quy.authservice.services;

import com.quy.common.core.exception.ErrorCode;
import com.quy.common.core.security.ERole;
import com.quy.common.util.jwt.JwtUtil;
import com.us.quy.authservice.dtos.AccountRegistrationRequest;
import com.us.quy.authservice.dtos.LoginRequest;
import com.us.quy.authservice.dtos.LoginResponse;
import com.us.quy.authservice.dtos.RefreshTokenRequest;
import com.us.quy.authservice.entities.AccountEntity;
import com.us.quy.authservice.kafka.providers.AccountPublisher;
import com.us.quy.authservice.mapper.AccountMapper;
import com.us.quy.authservice.repositories.AccountRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private static final String PREFIX_KEY = "auth:refresh_token:";
    private static final String TOPIC = "auth.user-onboarding";
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountPublisher accountPublisher;
    private final PasswordEncoder encoder;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    @Override
    public void register(AccountRegistrationRequest request) {
        accountRepository.findByUsername(request.getUsername())
            .ifPresent(entity -> {
                throw ErrorCode.AUTH_USERNAME_EXISTED.toException();
            });

        accountRepository.findByEmail(request.getEmail())
            .ifPresent(entity -> {
                throw ErrorCode.AUTH_EMAIL_EXISTED.toException();
            });

        request.setPassword(encoder.encode(request.getPassword()));
        AccountEntity account = accountRepository.save(accountMapper.fromRegisterRequest(request));
        accountPublisher.registerEvent(TOPIC, account);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        AccountEntity account = accountRepository
            .findByUsernameOrEmail(request.getUsername(), request.getUsername())
            .orElseThrow(ErrorCode.CUSTOMER_NOT_FOUND::toException);

        if (!encoder.matches(request.getPassword(), account.getPassword())) {
            throw ErrorCode.AUTH_PASSWORD_WRONG.toException();
        }

        switch (account.getStatus()) {
            case PENDING_VERIFICATION -> throw ErrorCode.AUTH_ACCOUNT_DISABLED.toException();
            case BANNED -> throw ErrorCode.AUTH_ACCOUNT_LOCKED.toException();
            case DELETED -> throw ErrorCode.NOT_FOUND.toException();
            default -> {
            }
        }

        LoginResponse response = new LoginResponse();
        response.setAccessToken(jwtUtil.generateAccessToken(account.getId(),
            account.getUsername(),
            account.getRoles()));
        response.setRefreshToken(jwtUtil.generateRefreshToken(account.getId(),
            account.getUsername(),
            account.getRoles()));

        saveRefreshTokenToRedis(account.getId(), response.getRefreshToken());

        return response;
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String oldRefreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(oldRefreshToken)) {
            throw ErrorCode.AUTH_REFRESH_TOKEN_INVALID.toException();
        }

        if (jwtUtil.isTokenExpired(oldRefreshToken)) {
            throw ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED.toException();
        }

        String username = jwtUtil.extractUsername(oldRefreshToken);
        UUID userId = jwtUtil.extractUserId(oldRefreshToken);
        List<ERole> roles = jwtUtil.extractRoles(oldRefreshToken);

        String redisKey = PREFIX_KEY + userId;

        String storedToken = redisTemplate.opsForValue().get(redisKey);

        if (storedToken == null || !storedToken.equals(oldRefreshToken)) {
            throw ErrorCode.AUTH_LOGIN_FAILED.toException();
        }

        LoginResponse response = new LoginResponse();
        response.setAccessToken(jwtUtil.generateAccessToken(userId, username, roles));
        response.setRefreshToken(jwtUtil.generateRefreshToken(userId, username, roles));

        saveRefreshTokenToRedis(userId, response.getRefreshToken());

        return response;
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (jwtUtil.validateToken(refreshToken)) {
            String userId = jwtUtil.extractUserId(refreshToken).toString();
            String redisKey = PREFIX_KEY + userId;

            redisTemplate.delete(redisKey);
        }
    }

    private void saveRefreshTokenToRedis(UUID userId, String refreshToken) {
        String redisKey = PREFIX_KEY + userId;

        redisTemplate.opsForValue().set(
            redisKey,
            refreshToken,
            Duration.ofMillis(jwtUtil.getRefreshTokenExp())
        );
    }
}
