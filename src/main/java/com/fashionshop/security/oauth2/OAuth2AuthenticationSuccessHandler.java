package com.fashionshop.security.oauth2;

import com.fashionshop.dto.response.AuthResponse;
import com.fashionshop.entity.User;
import com.fashionshop.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        User user;
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcOAuth2UserPrincipal oidcPrincipal) {
            user = oidcPrincipal.getUser();
        } else {
            user = ((OAuth2UserPrincipal) principal).getUser();
        }

        AuthResponse authResponse = authService.handleOAuth2Login(user);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("accessToken", authResponse.getAccessToken())
                .queryParam("refreshToken", authResponse.getRefreshToken())
                .build().toUriString();

        log.info("OAuth2 login success for: {}", user.getEmail());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
