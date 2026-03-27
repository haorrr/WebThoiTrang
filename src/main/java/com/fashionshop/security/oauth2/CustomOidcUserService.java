package com.fashionshop.security.oauth2;

import com.fashionshop.entity.User;
import com.fashionshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        Map<String, Object> attributes = oidcUser.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatar = (String) attributes.get("picture");
        String providerId = (String) attributes.get("sub");

        User user = upsertUser(email, name, avatar, User.Provider.GOOGLE, providerId);
        return new OidcOAuth2UserPrincipal(user, attributes, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private User upsertUser(String email, String name, String avatar,
                             User.Provider provider, String providerId) {
        Optional<User> byProvider = userRepository.findByProviderAndProviderId(provider, providerId);
        if (byProvider.isPresent()) {
            User user = byProvider.get();
            user.setName(name);
            user.setAvatar(avatar);
            return userRepository.save(user);
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setAvatar(avatar);
            return userRepository.save(user);
        }

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
