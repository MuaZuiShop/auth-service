package com.us.quy.authservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quy.common.core.exception.AppException;
import com.quy.common.core.security.ERole;
import com.quy.common.util.jwt.JwtUtil;
import com.us.quy.authservice.BaseIntegrationTest;
import com.us.quy.authservice.dtos.AccountRegistrationRequest;
import com.us.quy.authservice.dtos.LoginRequest;
import com.us.quy.authservice.dtos.LoginResponse;
import com.us.quy.authservice.dtos.RefreshTokenRequest;
import com.us.quy.authservice.entities.AccountEntity;
import com.us.quy.authservice.enums.EStatus;
import com.us.quy.authservice.kafka.providers.AccountPublisher;
import com.us.quy.authservice.repositories.AccountRepository;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class AccountServiceImplTest extends BaseIntegrationTest {

    private static final String FAKE_ACCESS_TOKEN = "fake.access.token";
    private static final String FAKE_REFRESH_TOKEN = "fake.refresh.token";
    private static final String ENCODED_PASSWORD = "encoded_password_123";
    private static final String RAW_PASSWORD = "Valid123!";
    private static final String PREFIX_KEY = "auth:refresh_token:";
    @Autowired
    private AccountServiceImpl accountService;
    @Autowired
    private AccountRepository accountRepository;
    @MockBean
    private AccountPublisher accountPublisher;
    @MockBean
    private PasswordEncoder encoder;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private StringRedisTemplate redisTemplate;
    @MockBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void init() {
        accountRepository.deleteAll();

        when(encoder.encode(any())).thenReturn(ENCODED_PASSWORD);
        when(encoder.matches(eq(RAW_PASSWORD), eq(ENCODED_PASSWORD))).thenReturn(true);

        when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn(FAKE_ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn(FAKE_REFRESH_TOKEN);
        when(jwtUtil.getRefreshTokenExp()).thenReturn(86400000L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== Helper ====================

    private AccountRegistrationRequest buildRegisterRequest(String username, String email) {
        return Instancio.of(AccountRegistrationRequest.class)
            .set(Select.field(AccountRegistrationRequest::getUsername), username)
            .set(Select.field(AccountRegistrationRequest::getEmail), email)
            .set(Select.field(AccountRegistrationRequest::getPassword), RAW_PASSWORD)
            .create();
    }

    private AccountEntity saveAccount(String username, String email, EStatus status) {
        return accountRepository.save(
            AccountEntity.builder()
                .username(username)
                .email(email)
                .password(ENCODED_PASSWORD)
                .roles(List.of(ERole.USER))
                .status(status)
                .build()
        );
    }

    private LoginRequest buildLoginRequest(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    // ==================== register ====================

    @Nested
    class Register {

        @Test
        void register_WhenRequestIsValid_ThenSaveToDbAndPublishEvent() {
            AccountRegistrationRequest request = buildRegisterRequest("newuser", "newuser@gmail.com");

            accountService.register(request);

            AccountEntity saved = accountRepository.findByUsername("newuser").orElseThrow();
            assertThat(saved.getEmail()).isEqualTo("newuser@gmail.com");
            assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(saved.getRoles()).containsExactly(ERole.USER);
            assertThat(saved.getStatus()).isEqualTo(EStatus.PENDING_VERIFICATION);

            verify(accountPublisher).registerEvent(eq("auth.user-onboarding"), any(AccountEntity.class));
        }

        @Test
        void register_WhenRequestIsValid_ThenPasswordIsEncoded() {
            AccountRegistrationRequest request = buildRegisterRequest("newuser", "newuser@gmail.com");

            accountService.register(request);

            verify(encoder).encode(RAW_PASSWORD);
            AccountEntity saved = accountRepository.findByUsername("newuser").orElseThrow();
            assertThat(saved.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(saved.getPassword()).isNotEqualTo(RAW_PASSWORD);
        }

        @Test
        void register_WhenUsernameAlreadyExists_ThenThrowAppException() {
            saveAccount("existuser", "exist@gmail.com", EStatus.ACTIVE);
            AccountRegistrationRequest request = buildRegisterRequest("existuser", "other@gmail.com");

            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo("AUTH_USERNAME_EXISTED");
                    assertThat(appEx.getHttpStatus().value()).isEqualTo(409);
                });

            verify(accountPublisher, never()).registerEvent(any(), any());
        }

        @Test
        void register_WhenEmailAlreadyExists_ThenThrowAppException() {
            saveAccount("otheruser", "duplicate@gmail.com", EStatus.ACTIVE);
            AccountRegistrationRequest request = buildRegisterRequest("newuser", "duplicate@gmail.com");

            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo("AUTH_EMAIL_EXISTED");
                    assertThat(appEx.getHttpStatus().value()).isEqualTo(409);
                });

            verify(accountPublisher, never()).registerEvent(any(), any());
        }

        @Test
        void register_WhenUsernameAndEmailBothExist_ThenThrowUsernameExistedFirst() {
            // username check đến trước email check trong code
            saveAccount("dupuser", "dup@gmail.com", EStatus.ACTIVE);
            AccountRegistrationRequest request = buildRegisterRequest("dupuser", "dup@gmail.com");

            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                    .isEqualTo("AUTH_USERNAME_EXISTED"));
        }
    }

    // ==================== login ====================

    @Nested
    class Login {

        @Test
        void login_WhenCredentialsAreValid_ThenReturnTokens() {
            saveAccount("loginuser", "login@gmail.com", EStatus.ACTIVE);
            LoginRequest request = buildLoginRequest("loginuser", RAW_PASSWORD);

            LoginResponse response = accountService.login(request);

            assertThat(response.getAccessToken()).isEqualTo(FAKE_ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(FAKE_REFRESH_TOKEN);
        }

        @Test
        void login_WhenCredentialsAreValid_ThenSaveRefreshTokenToRedis() {
            AccountEntity account = saveAccount("loginuser", "login@gmail.com", EStatus.ACTIVE);
            LoginRequest request = buildLoginRequest("loginuser", RAW_PASSWORD);

            accountService.login(request);

            String expectedKey = PREFIX_KEY + account.getId();
            verify(valueOperations).set(eq(expectedKey), eq(FAKE_REFRESH_TOKEN), any());
        }

        @Test
        void login_WhenCredentialsAreValid_ThenGenerateTokensWithCorrectData() {
            AccountEntity account = saveAccount("loginuser", "login@gmail.com", EStatus.ACTIVE);
            LoginRequest request = buildLoginRequest("loginuser", RAW_PASSWORD);

            accountService.login(request);

            verify(jwtUtil).generateAccessToken(
                eq(account.getId()),
                eq("loginuser"),
                eq(List.of(ERole.USER))
            );
            verify(jwtUtil).generateRefreshToken(
                eq(account.getId()),
                eq("loginuser"),
                eq(List.of(ERole.USER))
            );
        }

        @Test
        void login_WhenUsernameNotFound_ThenThrowAppException() {
            LoginRequest request = buildLoginRequest("nonexistent", RAW_PASSWORD);

            assertThatThrownBy(() -> accountService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo("CUSTOMER_NOT_FOUND");
                    assertThat(appEx.getHttpStatus().value()).isEqualTo(404);
                });
        }

        @Test
        void login_WhenPasswordIsWrong_ThenThrowAppException() {
            saveAccount("loginuser", "login@gmail.com", EStatus.ACTIVE);
            when(encoder.matches(eq("wrongpassword"), eq(ENCODED_PASSWORD))).thenReturn(false);
            LoginRequest request = buildLoginRequest("loginuser", "wrongpassword");

            assertThatThrownBy(() -> accountService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo("AUTH_PASSWORD_WRONG");
                    assertThat(appEx.getHttpStatus().value()).isEqualTo(400);
                });
        }

        @Test
        void login_WhenAccountIsPendingVerification_ThenThrowAppException() {
            saveAccount("pendinguser", "pending@gmail.com", EStatus.PENDING_VERIFICATION);
            LoginRequest request = buildLoginRequest("pendinguser", RAW_PASSWORD);

            assertThatThrownBy(() -> accountService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo("AUTH_ACCOUNT_DISABLED");
                    assertThat(appEx.getHttpStatus().value()).isEqualTo(403);
                });
        }

        @Test
        void login_WhenAccountIsBanned_ThenThrowAppException() {
            saveAccount("banneduser", "banned@gmail.com", EStatus.BANNED);
            LoginRequest request = buildLoginRequest("banneduser", RAW_PASSWORD);

            assertThatThrownBy(() -> accountService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo("AUTH_ACCOUNT_LOCKED");
                    assertThat(appEx.getHttpStatus().value()).isEqualTo(403);
                });
        }

        @Test
        void login_WhenAccountIsDeleted_ThenThrowAppException() {
            saveAccount("deleteduser", "deleted@gmail.com", EStatus.DELETED);
            LoginRequest request = buildLoginRequest("deleteduser", RAW_PASSWORD);

            assertThatThrownBy(() -> accountService.login(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo("NOT_FOUND");
                    assertThat(appEx.getHttpStatus().value()).isEqualTo(404);
                });
        }

        @Test
        void login_WhenEmailUsedAsUsername_ThenReturnTokens() {
            // findByUsernameOrEmail cho phép login bằng email
            saveAccount("loginuser", "login@gmail.com", EStatus.ACTIVE);
            LoginRequest request = buildLoginRequest("login@gmail.com", RAW_PASSWORD);

            LoginResponse response = accountService.login(request);

            assertThat(response.getAccessToken()).isEqualTo(FAKE_ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(FAKE_REFRESH_TOKEN);
        }
    }

    // ==================== refreshToken ====================

    @Nested
    class RefreshToken {

        @Test
        void refreshToken_WhenTokenIsValid_ThenReturnNewTokens() {
            UUID userId = UUID.randomUUID();
            when(jwtUtil.validateToken(FAKE_REFRESH_TOKEN)).thenReturn(true);
            when(jwtUtil.isTokenExpired(FAKE_REFRESH_TOKEN)).thenReturn(false);
            when(jwtUtil.extractUsername(FAKE_REFRESH_TOKEN)).thenReturn("loginuser");
            when(jwtUtil.extractUserId(FAKE_REFRESH_TOKEN)).thenReturn(userId);
            when(jwtUtil.extractRoles(FAKE_REFRESH_TOKEN)).thenReturn(List.of(ERole.USER));
            when(valueOperations.get(PREFIX_KEY + userId)).thenReturn(FAKE_REFRESH_TOKEN);

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(FAKE_REFRESH_TOKEN);

            LoginResponse response = accountService.refreshToken(request);

            assertThat(response.getAccessToken()).isEqualTo(FAKE_ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isEqualTo(FAKE_REFRESH_TOKEN);
        }

        @Test
        void refreshToken_WhenTokenIsValid_ThenSaveNewRefreshTokenToRedis() {
            UUID userId = UUID.randomUUID();
            String newRefreshToken = "new.refresh.token";
            when(jwtUtil.validateToken(FAKE_REFRESH_TOKEN)).thenReturn(true);
            when(jwtUtil.isTokenExpired(FAKE_REFRESH_TOKEN)).thenReturn(false);
            when(jwtUtil.extractUsername(FAKE_REFRESH_TOKEN)).thenReturn("loginuser");
            when(jwtUtil.extractUserId(FAKE_REFRESH_TOKEN)).thenReturn(userId);
            when(jwtUtil.extractRoles(FAKE_REFRESH_TOKEN)).thenReturn(List.of(ERole.USER));
            when(valueOperations.get(PREFIX_KEY + userId)).thenReturn(FAKE_REFRESH_TOKEN);
            when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn(newRefreshToken);

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(FAKE_REFRESH_TOKEN);

            accountService.refreshToken(request);

            verify(valueOperations).set(eq(PREFIX_KEY + userId), eq(newRefreshToken), any());
        }

        @Test
        void refreshToken_WhenTokenIsInvalid_ThenThrowAppException() {
            when(jwtUtil.validateToken(anyString())).thenReturn(false);

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalid.token");

            assertThatThrownBy(() -> accountService.refreshToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo("AUTH_REFRESH_TOKEN_INVALID");
                    assertThat(appEx.getHttpStatus().value()).isEqualTo(401);
                });
        }

        @Test
        void refreshToken_WhenTokenIsExpired_ThenThrowAppException() {
            when(jwtUtil.validateToken(FAKE_REFRESH_TOKEN)).thenReturn(true);
            when(jwtUtil.isTokenExpired(FAKE_REFRESH_TOKEN)).thenReturn(true);

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(FAKE_REFRESH_TOKEN);

            assertThatThrownBy(() -> accountService.refreshToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo("AUTH_REFRESH_TOKEN_EXPIRED");
                    assertThat(appEx.getHttpStatus().value()).isEqualTo(401);
                });
        }

        @Test
        void refreshToken_WhenTokenNotInRedis_ThenThrowAppException() {
            UUID userId = UUID.randomUUID();
            when(jwtUtil.validateToken(FAKE_REFRESH_TOKEN)).thenReturn(true);
            when(jwtUtil.isTokenExpired(FAKE_REFRESH_TOKEN)).thenReturn(false);
            when(jwtUtil.extractUserId(FAKE_REFRESH_TOKEN)).thenReturn(userId);
            when(jwtUtil.extractUsername(FAKE_REFRESH_TOKEN)).thenReturn("loginuser");
            when(jwtUtil.extractRoles(FAKE_REFRESH_TOKEN)).thenReturn(List.of(ERole.USER));
            when(valueOperations.get(PREFIX_KEY + userId)).thenReturn(null); // không có trong redis

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(FAKE_REFRESH_TOKEN);

            assertThatThrownBy(() -> accountService.refreshToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                    .isEqualTo("AUTH_LOGIN_FAILED"));
        }

        @Test
        void refreshToken_WhenTokenMismatchInRedis_ThenThrowAppException() {
            UUID userId = UUID.randomUUID();
            when(jwtUtil.validateToken(FAKE_REFRESH_TOKEN)).thenReturn(true);
            when(jwtUtil.isTokenExpired(FAKE_REFRESH_TOKEN)).thenReturn(false);
            when(jwtUtil.extractUserId(FAKE_REFRESH_TOKEN)).thenReturn(userId);
            when(jwtUtil.extractUsername(FAKE_REFRESH_TOKEN)).thenReturn("loginuser");
            when(jwtUtil.extractRoles(FAKE_REFRESH_TOKEN)).thenReturn(List.of(ERole.USER));
            when(valueOperations.get(PREFIX_KEY + userId)).thenReturn("different.token.in.redis");

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(FAKE_REFRESH_TOKEN);

            assertThatThrownBy(() -> accountService.refreshToken(request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                    .isEqualTo("AUTH_LOGIN_FAILED"));
        }
    }

    // ==================== logout ====================

    @Nested
    class Logout {

        @Test
        void logout_WhenTokenIsValid_ThenDeleteFromRedis() {
            UUID userId = UUID.randomUUID();
            when(jwtUtil.validateToken(FAKE_REFRESH_TOKEN)).thenReturn(true);
            when(jwtUtil.extractUserId(FAKE_REFRESH_TOKEN)).thenReturn(userId);

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(FAKE_REFRESH_TOKEN);

            accountService.logout(request);

            verify(redisTemplate).delete(PREFIX_KEY + userId);
        }

        @Test
        void logout_WhenTokenIsInvalid_ThenDoNotDeleteFromRedis() {
            when(jwtUtil.validateToken(anyString())).thenReturn(false);

            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalid.token");

            accountService.logout(request);

            verify(redisTemplate, never()).delete(anyString());
        }
    }
}