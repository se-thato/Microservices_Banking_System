package com.banking.payment_api.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final JwtTokenExtractor jwtTokenExtractor;

    public UserDetailsServiceImpl(JwtTokenExtractor jwtTokenExtractor) {
        this.jwtTokenExtractor = jwtTokenExtractor;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        return User.builder()
                .username(email)
                .password("") //no passworde needed in this case the JWT already verified
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();
    }
}
