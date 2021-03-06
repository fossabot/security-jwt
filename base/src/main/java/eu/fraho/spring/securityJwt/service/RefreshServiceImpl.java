/*
 * MIT Licence
 * Copyright (c) 2018 Simon Frankenberger
 *
 * Please see LICENCE.md for complete licence text.
 */
package eu.fraho.spring.securityJwt.service;

import com.nimbusds.jose.JOSEException;
import eu.fraho.spring.securityJwt.dto.AccessToken;
import eu.fraho.spring.securityJwt.dto.AuthenticationResponse;
import eu.fraho.spring.securityJwt.dto.JwtUser;
import eu.fraho.spring.securityJwt.dto.RefreshToken;
import eu.fraho.spring.securityJwt.exceptions.FeatureNotConfiguredException;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@NoArgsConstructor
public class RefreshServiceImpl implements RefreshService {
    @Setter(onMethod = @__({@Autowired, @NonNull}))
    private JwtTokenService jwtTokenService;

    @Override
    public AuthenticationResponse checkRefresh(@Nullable String token) throws AuthenticationException {
        if (!jwtTokenService.isRefreshTokenSupported()) {
            log.info("Refresh token support is disabled");
            throw new FeatureNotConfiguredException("Refresh token support is disabled");
        }

        if (token == null) {
            log.info("No refresh token found in request");
            throw new BadCredentialsException("No token found in request");
        }

        // use the refresh token to get the underlying userdetails
        log.debug("Using refresh token");
        Optional<JwtUser> jwtUser = jwtTokenService.useRefreshToken(token);
        if (!jwtUser.isPresent()) {
            log.info("Using refresh token failed (unknown refreshtoken?)");
            throw new BadCredentialsException("Unknown token");
        }

        final JwtUser userDetails = jwtUser.get();
        log.debug("User {} successfully used refresh token, checking database", userDetails.getUsername());

        if (!userDetails.isApiAccessAllowed()) {
            log.info("User {} may no longer access api", userDetails.getUsername());
            throw new BadCredentialsException("Insufficient accesss rights");
        }

        log.debug("User may access api, generating new access token");
        final AccessToken accessToken;
        try {
            accessToken = jwtTokenService.generateToken(userDetails);
        } catch (JOSEException e) {
            log.info("Error creating an access token for {}", userDetails.getUsername(), e);
            throw new BadCredentialsException("Token generation failed");
        }

        log.debug("Generating new refresh token");
        final RefreshToken refreshToken = jwtTokenService.generateRefreshToken(userDetails);

        return AuthenticationResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }
}
