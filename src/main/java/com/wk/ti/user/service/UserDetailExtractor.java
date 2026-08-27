package com.wk.ti.user.service;

import com.wk.ti.config.GroupConfig;
import com.wk.ti.exception.NotAuthorizedException;
import com.wk.ti.user.model.UserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.wk.ti.config.CustomGrantedAuthoritiesConverter.ROLE_TEMPLATE;
import static java.lang.String.format;

@SuppressWarnings({"unchecked", "unused", "SuspiciousMethodCalls"})
@Component
@RequiredArgsConstructor
public class UserDetailExtractor {
    public static final String USER_ROLE = "user";
    private static final String MODERATOR_ROLE = "moderator";
    private static final String ADMIN_ROLE = "admin";
    private static final String USER_ID = "sub";
    private static final String DEFAULT_EMAIL = "";
    private static final String DEFAULT_GIVEN_NAME = "google";
    private static final String DEFAULT_FAMILY_NAME = "";
    private static final String EMAIL_ATTRIBUTE = "email";
    private static final String GIVEN_NAME_ATTRIBUTE = "given_name";
    private static final String FAMILY_NAME_ATTRIBUTE = "family_name";
    private static final String GROUPS_ATTRIBUTE = "groups";
    private static final String NICKNAME_ATTRIBUTE = "nickname";

    private final GroupConfig groupConfig;

    public UserDetail extractor(Jwt jwt) {
        Assert.notNull(jwt, "jwt can't be null");
        Map<String, Object> attributes = jwt.getClaims();

        List<String> authorities = extractRoles(attributes);

        return UserDetail.builder()
                .username(getUserId(attributes))
                .email(getEmail(attributes))
                .givenName(getGivenName(attributes))
                .familyName(getFamilyName(attributes))
                .roles(authorities)
                .build();
    }

    private List<String> extractRoles(Map<String, Object> attributes) {
        List<String> authorities = (List<String>) attributes.get(GROUPS_ATTRIBUTE);
        if (authorities == null) {
            authorities = new ArrayList<>();
        }
        if (!authorities.contains(USER_ROLE)) {
            authorities.add(USER_ROLE);
        }
        String nickname = getNickname(attributes);
        boolean isAdmin = groupConfig.getAdmin().stream()
                .anyMatch(item -> item.equalsIgnoreCase(nickname));
        if (isAdmin) {
            authorities.add(ADMIN_ROLE);
        }
        return authorities;
    }

    private String getNickname(Map<String, Object> attributes) {
        return (String) attributes.get(NICKNAME_ATTRIBUTE);
    }

    private String getEmail(Map<String, Object> attributes) {
        return attributes.get(EMAIL_ATTRIBUTE) == null ? DEFAULT_EMAIL : (String) attributes.get(EMAIL_ATTRIBUTE);
    }

    private String getGivenName(Map<String, Object> attributes) {
        return attributes.get(GIVEN_NAME_ATTRIBUTE) == null ? DEFAULT_GIVEN_NAME : (String) attributes.get(GIVEN_NAME_ATTRIBUTE);
    }

    private String getFamilyName(Map<String, Object> attributes) {
        return attributes.get(FAMILY_NAME_ATTRIBUTE) == null ? DEFAULT_FAMILY_NAME : (String) attributes.get(FAMILY_NAME_ATTRIBUTE);
    }

    private String getUserId(Map<String, Object> attributes) {
        return attributes.get(USER_ID) == null ? "" : (String) attributes.get(USER_ID);
    }

    public static String getUser() {
        if (auth() == null) {
            throw new NotAuthorizedException("User is not authorized.");
        }

        return (String) ((Jwt) Objects.requireNonNull(auth().getPrincipal())).getClaims().get("nickname");
    }

    public static boolean isAdmin() {
        if (auth() == null) {
            throw new NotAuthorizedException("User is not authorized.");
        }
        return auth().getAuthorities().contains(format(ROLE_TEMPLATE, ADMIN_ROLE.toUpperCase()));
    }

    private static Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
