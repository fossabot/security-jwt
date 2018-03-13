/*
 * MIT Licence
 * Copyright (c) 2017 Simon Frankenberger
 *
 * Please see LICENCE.md for complete licence text.
 */
package eu.fraho.spring.securityJwt.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import net.jcip.annotations.Immutable;

import java.util.Optional;

@Immutable
@Getter
@AllArgsConstructor
@JsonDeserialize(builder = AuthenticationRequest.AuthenticationRequestBuilder.class)
@Builder
@Value
public final class AuthenticationRequest {
    private String username;

    private String password;

    private Integer totp;

    public Optional<Integer> getTotp() {
        return Optional.ofNullable(totp);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class AuthenticationRequestBuilder {
    }
}
