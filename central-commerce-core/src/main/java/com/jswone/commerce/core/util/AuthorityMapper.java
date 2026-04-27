package com.jswone.commerce.core.util;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuthorityMapper {

    /**
     * Maps a permission map to a collection of Spring Security GrantedAuthorities.
     * Format generated: {RESOURCE}_{MASK} e.g. ORDER_11000000
     * @param permissions The extracted JWT permissions map
     * @return Collection of GrantedAuthority
     */
    public Collection<GrantedAuthority> mapPermissions(Map<String, String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }

        return permissions.entrySet().stream()
                .map(entry -> new SimpleGrantedAuthority(entry.getKey() + "_" + entry.getValue()))
                .collect(Collectors.toList());
    }
}
