package com.wk.ti.config;

import com.wk.ti.user.service.UserDetailExtractor;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
public class CustomGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    public static final String ROLE_TEMPLATE = "ROLE_%s";
    private final UserDetailExtractor userDetailExtractor;

    @Override
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        // Extract roles from custom claim

        List<String> roles = userDetailExtractor.extractor(jwt).getRoles();
        // If roles claim is not present, return empty list of authorities
        if (roles == null) {
            return Collections.emptyList();
        }

        // Map roles to GrantedAuthorities
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(format(ROLE_TEMPLATE, role.toUpperCase())))
                .collect(Collectors.toList());
    }
}

