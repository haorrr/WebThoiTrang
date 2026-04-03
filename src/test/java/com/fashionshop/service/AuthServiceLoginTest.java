package com.fashionshop.service;

import com.fashionshop.dto.request.LoginRequest;
import com.fashionshop.dto.response.AuthResponse;
import com.fashionshop.dto.response.UserSummaryResponse;
import com.fashionshop.entity.User;
import com.fashionshop.exception.BadRequestException;
import com.fashionshop.exception.UnauthorizedException;
import com.fashionshop.repository.UserRepository;
import com.fashionshop.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test – Đăng nhập (Path Coverage)
 * 
 * Tương ứng mục 5.2 trong báo cáo KTPM.
 * Kỹ thuật: Path Coverage (V(G) = 4, 5 đường đi)
 * Framework: JUnit 5 + Mockito
 * 
 * Method được test: AuthService.login(LoginRequest request)
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private EmailService emailService;

    @Mock
    private ReferralService referralService;

    @Mock
    private com.fashionshop.repository.PasswordResetTokenRepository resetTokenRepository;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private User activeLocalUser;

    @BeforeEach
    void setUp() {
        // Chuẩn bị LoginRequest mặc định
        loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password123");

        // Chuẩn bị User hợp lệ mặc định (LOCAL, ACTIVE, chưa bị xóa)
        activeLocalUser = User.builder()
                .name("Test User")
                .email("user@example.com")
                .password("encoded_password")
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .provider(User.Provider.LOCAL)
                .build();
        activeLocalUser.setId(1L);
    }

    // ==================== PATH 1 ====================
    /**
     * TC_WB_PC_01: Email không tồn tại trong hệ thống
     * Path: B1 → Exception (email not found)
     * Kết quả mong đợi: BadCredentialsException("Invalid email or password")
     */
    @Test
    @DisplayName("TC_WB_PC_01: Email không tồn tại → BadCredentialsException")
    void testLogin_EmailNotFound_ThrowsBadCredentials() {
        // Arrange
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest));
        assertEquals("Invalid email or password", exception.getMessage());

        // Verify: không gọi thêm bất kỳ thao tác nào
        verify(userRepository).findByEmail("user@example.com");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
    }

    // ==================== PATH 2 ====================
    /**
     * TC_WB_PC_02: Tài khoản bị vô hiệu hóa (INACTIVE)
     * Path: B1 → B2(true) → Exception (inactive)
     * Kết quả mong đợi: UnauthorizedException("Account is inactive or disabled")
     */
    @Test
    @DisplayName("TC_WB_PC_02: Tài khoản INACTIVE → UnauthorizedException")
    void testLogin_AccountInactive_ThrowsUnauthorized() {
        // Arrange - User có status INACTIVE
        activeLocalUser.setStatus(User.Status.INACTIVE);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(activeLocalUser));

        // Act & Assert
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authService.login(loginRequest));
        assertEquals("Account is inactive or disabled", exception.getMessage());

        verify(userRepository).findByEmail("user@example.com");
        verifyNoInteractions(jwtService);
    }

    // ==================== PATH 3 ====================
    /**
     * TC_WB_PC_03: Tài khoản đăng nhập bằng OAuth2 (Google/GitHub)
     * Path: B1 → B2(false) → B3(true) → Exception (OAuth2 provider)
     * Kết quả mong đợi: BadRequestException("Please login with google")
     */
    @Test
    @DisplayName("TC_WB_PC_03: Tài khoản OAuth2 (Google) → BadRequestException")
    void testLogin_OAuth2Provider_ThrowsBadRequest() {
        // Arrange - User dùng provider GOOGLE
        activeLocalUser.setProvider(User.Provider.GOOGLE);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(activeLocalUser));

        // Act & Assert
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.login(loginRequest));
        assertEquals("Please login with google", exception.getMessage());

        verify(userRepository).findByEmail("user@example.com");
        verifyNoInteractions(jwtService);
    }

    // ==================== PATH 4 ====================
    /**
     * TC_WB_PC_04: Mật khẩu không đúng
     * Path: B1 → B2(false) → B3(false) → B4(true) → Exception (wrong password)
     * Kết quả mong đợi: BadCredentialsException("Invalid email or password")
     */
    @Test
    @DisplayName("TC_WB_PC_04: Sai mật khẩu → BadCredentialsException")
    void testLogin_WrongPassword_ThrowsBadCredentials() {
        // Arrange
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(activeLocalUser));
        when(passwordEncoder.matches("password123", "encoded_password"))
                .thenReturn(false);

        // Act & Assert
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest));
        assertEquals("Invalid email or password", exception.getMessage());

        verify(userRepository).findByEmail("user@example.com");
        verify(passwordEncoder).matches("password123", "encoded_password");
        verifyNoInteractions(jwtService);
    }

    // ==================== PATH 5 ====================
    /**
     * TC_WB_PC_05: Đăng nhập thành công
     * Path: B1 → B2(false) → B3(false) → B4(false) → B5 (success)
     * Kết quả mong đợi: Trả về AuthResponse với accessToken và refreshToken
     */
    @Test
    @DisplayName("TC_WB_PC_05: Đăng nhập thành công → AuthResponse")
    void testLogin_Success_ReturnsAuthResponse() {
        // Arrange
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(activeLocalUser));
        when(passwordEncoder.matches("password123", "encoded_password"))
                .thenReturn(true);
        when(jwtService.generateRefreshToken())
                .thenReturn("new-refresh-token");
        when(passwordEncoder.encode("new-refresh-token"))
                .thenReturn("encoded-refresh-token");
        when(userRepository.save(any(User.class)))
                .thenReturn(activeLocalUser);

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("user@example.com"))
                .thenReturn(mockUserDetails);
        when(jwtService.generateAccessToken(mockUserDetails))
                .thenReturn("new-access-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertNotNull(response.getUser());

        // Verify các bước đã được gọi đúng thứ tự
        verify(userRepository).findByEmail("user@example.com");
        verify(passwordEncoder).matches("password123", "encoded_password");
        verify(jwtService).generateRefreshToken();
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateAccessToken(mockUserDetails);
    }
}
