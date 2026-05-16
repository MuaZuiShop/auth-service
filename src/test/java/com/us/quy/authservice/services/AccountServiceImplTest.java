package com.us.quy.authservice.services;

import com.us.quy.authservice.BaseIntegrationTest;
import com.us.quy.authservice.dtos.AccountRegistrationRequest;
import com.us.quy.authservice.entities.AccountEntity;
import com.us.quy.authservice.kafka.providers.AccountPublisher;
import com.us.quy.authservice.repositories.AccountRepository;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class AccountServiceImplTest extends BaseIntegrationTest {

    @Autowired
    private AccountServiceImpl accountService;

    @Autowired
    private AccountRepository accountRepository;

    @MockBean
    private AccountPublisher accountPublisher;

    @MockBean
    private PasswordEncoder encoder;

    @BeforeEach
    void init() {
        accountRepository.deleteAll();
        when(encoder.encode(any())).thenReturn("encoded_password_123");
    }

    private AccountRegistrationRequest initBaseValidRequest() {
        return Instancio.of(AccountRegistrationRequest.class)
            .set(Select.field(AccountRegistrationRequest::getUsername), "test")
            .set(Select.field(AccountRegistrationRequest::getEmail), "test1108@gmail.com")
            .set(Select.field(AccountRegistrationRequest::getPassword), "Valid123!")
            .create();
    }

    @Test
    void register_WhenRequestIsValid_ThenSaveToDbAndPublishEvent() {
        // Arrange
        AccountRegistrationRequest request = initBaseValidRequest();

        // Act
        accountService.register(request);

        // Assert
        Optional<AccountEntity> savedAccount = accountRepository.findAll().stream()
            .filter(a -> a.getUsername().equals("test"))
            .findFirst();

        assertThat(savedAccount).isPresent();
        assertThat(savedAccount.get().getEmail()).isEqualTo("test1108@gmail.com");
        assertThat(savedAccount.get().getPassword()).isEqualTo("encoded_password_123");

        verify(accountPublisher).registerEvent(eq("auth.user-onboarding"), any(AccountEntity.class));
    }

    @Nested
    class ValidateFieldNotNullTests {

        @Test
        void register_WhenUsernameIsEmpty_ThenThrowResponseStatusException() {
            // Arrange
            AccountRegistrationRequest request = initBaseValidRequest();
            request.setUsername("");

            // Act & Assert
            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        void register_WhenPasswordIsEmpty_ThenThrowResponseStatusException() {
            // Arrange
            AccountRegistrationRequest request = initBaseValidRequest();
            request.setPassword("");

            // Act & Assert
            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        void register_WhenEmailIsEmpty_ThenThrowResponseStatusException() {
            // Arrange
            AccountRegistrationRequest request = initBaseValidRequest();
            request.setEmail("");

            // Act & Assert
            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    class ValidateEmailFormatTests {

        @Test
        void register_WhenEmailMissingAtSign_ThenThrowResponseStatusException() {
            // Arrange
            AccountRegistrationRequest request = initBaseValidRequest();
            request.setEmail("test.gmail.com");

            // Act & Assert
            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        void register_WhenEmailMissingDot_ThenThrowResponseStatusException() {
            // Arrange
            AccountRegistrationRequest request = initBaseValidRequest();
            request.setEmail("test@gmailcom");

            // Act & Assert
            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    class ValidatePasswordConditionTests {

        @Test
        void register_WhenPasswordLengthLessThan8_ThenThrowResponseStatusException() {
            // Arrange
            AccountRegistrationRequest request = initBaseValidRequest();
            request.setPassword("Val1!56");

            // Act & Assert
            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        void register_WhenPasswordMissingLowerCase_ThenThrowResponseStatusException() {
            // Arrange
            AccountRegistrationRequest request = initBaseValidRequest();
            request.setPassword("VALID123!");

            // Act & Assert
            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        void register_WhenPasswordMissingUpperCase_ThenThrowResponseStatusException() {
            // Arrange
            AccountRegistrationRequest request = initBaseValidRequest();
            request.setPassword("valid123!");

            // Act & Assert
            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        void register_WhenPasswordMissingDigit_ThenThrowResponseStatusException() {
            // Arrange
            AccountRegistrationRequest request = initBaseValidRequest();
            request.setPassword("ValidABC!");

            // Act & Assert
            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        void register_WhenPasswordMissingSpecialChar_ThenThrowResponseStatusException() {
            // Arrange
            AccountRegistrationRequest request = initBaseValidRequest();
            request.setPassword("Valid12345");

            // Act & Assert
            assertThatThrownBy(() -> accountService.register(request))
                .isInstanceOf(ResponseStatusException.class);
        }
    }
}