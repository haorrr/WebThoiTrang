package com.fashionshop.security.oauth2;

import com.fashionshop.entity.User;
import com.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email;
        String name;
        String avatar;
        String providerId;
        User.Provider provider;

        if ("google".equals(registrationId)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            avatar = (String) attributes.get("picture");
            providerId = (String) attributes.get("sub");
            provider = User.Provider.GOOGLE;
        } else if ("github".equals(registrationId)) {
            // GitHub: email might be null if user's email is private
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            if (name == null) name = (String) attributes.get("login");
            avatar = (String) attributes.get("avatar_url");
            providerId = String.valueOf(attributes.get("id"));
            provider = User.Provider.GITHUB;

            if (email == null) {
                // Use login@github.noreply.com as fallback email
                email = attributes.get("login") + "@users.noreply.github.com";
            }
        } else {
            throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
        }

        User user = upsertUser(email, name, avatar, provider, providerId);
        return new OAuth2UserPrincipal(user, attributes);
    }

    private User upsertUser(String email, String name, String avatar,
                             User.Provider provider, String providerId) {
        // First try to find by provider + providerId
        Optional<User> byProvider = userRepository.findByProviderAndProviderId(provider, providerId);
        if (byProvider.isPresent()) {
            User user = byProvider.get();
            user.setName(name);
            user.setAvatar(avatar);
            return userRepository.save(user);
        }

        // Then try by email (user may have local account with same email)
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            // Link OAuth2 to existing account
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setAvatar(avatar);
            return userRepository.save(user);
        }

        // Create new user
        User newUser = User.builder()
                .email(email)
                .name(name)
                .avatar(avatar)
                .role(User.Role.USER)
                .status(User.Status.ACTIVE)
                .provider(provider)
                .providerId(providerId)
                .build();
        return userRepository.save(newUser);
    }
}
