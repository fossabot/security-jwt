/*
 * MIT Licence
 * Copyright (c) 2017 Simon Frankenberger
 *
 * Please see LICENCE.md for complete licence text.
 */
package eu.fraho.spring.securityJwt.starter;

import eu.fraho.spring.securityJwt.config.*;
import eu.fraho.spring.securityJwt.controller.AuthenticationRestController;
import eu.fraho.spring.securityJwt.password.CryptPasswordEncoder;
import eu.fraho.spring.securityJwt.service.JwtTokenService;
import eu.fraho.spring.securityJwt.service.JwtTokenServiceImpl;
import eu.fraho.spring.securityJwt.service.TotpService;
import eu.fraho.spring.securityJwt.service.TotpServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@AutoConfigureAfter(SecurityAutoConfiguration.class)
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Slf4j
public class SecurityJwtBaseAutoConfiguration {
    @Bean
    public TotpConfiguration totpConfig() {
        log.debug("Register TotpConfiguration");
        return new TotpConfiguration();
    }

    @Bean
    public CryptConfiguration cryptConfiguration() {
        log.debug("Register CryptConfiguration");
        return new CryptConfiguration();
    }

    @Bean
    public TotpService totpService() {
        log.debug("Register TotpService");
        return new TotpServiceImpl(totpConfig());
    }

    @Bean
    public JwtTokenConfiguration jwtTokenConfiguration() {
        log.debug("Register JwtTokenConfiguration");
        return new JwtTokenConfiguration();
    }

    @Bean
    public JwtRefreshConfiguration jwtRefreshConfiguration() {
        log.debug("Register JwtRefreshConfiguration");
        return new JwtRefreshConfiguration();
    }

    @Bean
    public JwtTokenService jwtTokenService() {
        log.debug("Register JwtTokenService");
        return new JwtTokenServiceImpl(jwtTokenConfiguration(), jwtRefreshConfiguration());
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        log.debug("Register CryptPasswordEncoder");
        return new CryptPasswordEncoder(cryptConfiguration());
    }

    @Bean
    @ConditionalOnMissingBean
    public UserDetailsService defaultUserDetailsService() {
        log.debug("Register EmptyUserDetailsService");
        return new EmptyUserDetailsService();
    }

    @Bean
    public AuthenticationRestController authenticationRestController(final AuthenticationManager authenticationManager,
                                                                     final JwtTokenService jwtTokenService,
                                                                     final UserDetailsService userDetailsService,
                                                                     final TotpService totpService) {
        log.debug("Register AuthenticationRestController");
        return new AuthenticationRestController(authenticationManager, jwtTokenService, userDetailsService, totpService);
    }

    @Bean
    public JwtSecurityConfig webSecurityConfig(final UserDetailsService userDetailsService,
                                               final PasswordEncoder passwordEncoder,
                                               final JwtTokenService jwtTokenService) {
        log.debug("Register JwtSecurityConfig");
        return new JwtSecurityConfig(userDetailsService, passwordEncoder, jwtTokenService);
    }
}