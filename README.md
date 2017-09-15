# Spring Security Addon for JWT
[![Build Status](https://travis-ci.org/bratkartoffel/security-jwt.svg?branch=develop)](https://travis-ci.org/bratkartoffel/security-jwt)
[![Code Coverage](https://img.shields.io/codecov/c/github/bratkartoffel/security-jwt/develop.svg)](https://codecov.io/github/bratkartoffel/security-jwt?branch=develop)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/3632c543331d4fe3b98c96a2964d43ae)](https://www.codacy.com/app/bratkartoffel/security-jwt?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=bratkartoffel/security-jwt&amp;utm_campaign=Badge_Grade)
[![License](http://img.shields.io/:license-mit-blue.svg?style=flat)](http://doge.mit-license.org)

Providing a simple way to integrate [JWT](https://jwt.io/introduction/) into your spring boot application.

This project is split into 4 parts:

* base: Basic integration of JWT into spring security (without refresh tokens)
* internal: Support for an in-memory cache (ExpiringMap) for refresh tokens
* memcache: Support for memcache to store refresh tokens
* hibernate: Support for hibernate to store refresh tokens

Simply use the dependencies within your build script, spring boot takes care of the rest.
The default configuration should be sufficient for the most use cases.

# Contents
* base:
  * JWT Integration into Spring Security (including a [REST-Controller](base/src/main/java/eu/fraho/spring/securityJwt/controller/AuthenticationRestController.java) to authenticate against)
  * A [CryptPasswordEncoder](base/src/main/java/eu/fraho/spring/securityJwt/password/CryptPasswordEncoder.java) to generate / use linux system crypt(1)-hashes (supporting new $5$ and $6$ hashes and rounds)
  * Full support for [Swagger 2](https://github.com/springfox/springfox) documentation (REST Controller and DTO are annotated and described)
* module [internal](internal/):
  * Refresh token support through an internal, in-memory map
* module [memcache](memcache/):
  * Refresh token support through an external memcache server
* module [hibernate](hibernate/):
	* Refresh token support using hibernate and a database table

# Dependencies
```xml
<dependency>
    <groupId>eu.fraho.spring</groupId>
    <artifactId>security-jwt-base</artifactId>
    <version>1.0.0</version>
</dependency>
```

When you want to add refresh token support, then choose one of the following dependencies:
```xml
<dependencies>
    <dependency>
        <groupId>eu.fraho.spring</groupId>
        <artifactId>security-jwt-internal</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>eu.fraho.spring</groupId>
        <artifactId>security-jwt-memcache</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>eu.fraho.spring</groupId>
        <artifactId>security-jwt-hibernate</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```
For details on the usage of the plugins please see the README within the relevant module directories.

# Usage
Starting with version 1.0.0 there are two ways on how to use these libraries.

The old way is by directly using the libraries as dependencies and doing some manual configuration.
The newer way used spring boot autoconfiguration and reduced the needed configuration a lot.

To see this library "in action", please take a look at [the examples](https://github.com/bratkartoffel/security-jwt-examples).

## Spring Boot Autoconfig (recommended):
* Use any *-spring-boot-starter dependency you like
* Bouncycastle will be automagically loaded and installed if on classpath
* My enhanced PasswordEncoder will be used as default

## Manual configuration (legacy):
* Add the dependencies to your build script
* Configure your boot application to pick up our components (add "eu.fraho.spring.securityJwt" to the scanBasePackages field of your ```@SpringBootApplication```)
* Optionally add the BouncyCastle Provider (e.g. within the [main-Method](base/src/test/java/eu/fraho/spring/securityJwt/util/CreateEcdsaJwtKeys.java))
  * **Hint:** This is required if you would like to use the ECDSA signature algorithm!
* Optionally use my enhanced PasswordEncoder as a ```@Bean```
* Optionally choose a refresh token store implementation and set it as ```fraho.jwt.refresh.cache-impl```

## General steps for both methos:
* Create an implementation of UserDetailsService that returns an instance of [JwtUser](base/src/main/java/eu/fraho/spring/securityJwt/dto/JwtUser.java)
* By default, this service will create a random hmac secret for signatures on each startup. To stay consistent accress service restarts and not kicking clients out please either change the algorithm (recommended) or specifiy at least an hmac keyfile.

# Usage for clients
* Request new tokens by sending an authentication request to ```/auth/login```
* Somehow store the received token(s)
* To access a secured controller set the retrieved token as ```Authorization: <TokenType> <Token>```

# Configuration
This library will not run out-of the box, it needs at least some information for the token security.
Either you create an ECDSA keypair for the tokens (you can use [this](base/src/test/java/eu/fraho/spring/securityJwt/util/CreateEcdsaJwtKeys.java) class for that)
or you change the used algorithm to HMAC and specify a secret keyfile.

This library is customizable by the following properties:

| Property                                       | Default                     | Description   |
|------------------------------------------------|-----------------------------|---------------|
| fraho.jwt.token.algorithm                      | HS256                       | The signature algorithm used for the tokens. For a list of valid algorithms please see either the [JWT spec](https://tools.ietf.org/html/rfc7518#section-3) or [JWSAlgorithm](https://bitbucket.org/connect2id/nimbus-jose-jwt/src/master/src/main/java/com/nimbusds/jose/JWSAlgorithm.java)|
| fraho.jwt.token.cookie.enabled                 | false                       | Enables support for tokens sent as a cookie|
| fraho.jwt.token.cookie.names                   | JWT-ACCESSTOKEN, XSRF-TOKEN | Sets the name of the cookie with the token. The first entry in this list is used when sending out the cookie, any other names are optionally taken when validating incoming requests.|
| fraho.jwt.token.cookie.domain                  | null                        | see [javax.servlet.http.Cookie#setDomain(String)](https://docs.oracle.com/javaee/7/api/javax/servlet/http/Cookie.html#setDomain-java.lang.String-)|
| fraho.jwt.token.cookie.httpOnly                | true                        | see [javax.servlet.http.Cookie#setHttpOnly(boolean)](https://docs.oracle.com/javaee/7/api/javax/servlet/http/Cookie.html#setHttpOnly-boolean-)|
| fraho.jwt.token.cookie.secure                  | true                        | see [javax.servlet.http.Cookie#setSecure(boolean)](https://docs.oracle.com/javaee/7/api/javax/servlet/http/Cookie.html#setSecure-boolean-)|
| fraho.jwt.token.expiration                     | 1 hour                      | The validity period of issued tokens. For details on how this field has to specified see [TimeWithPeriod](base/src/main/java/eu/fraho/spring/securityJwt/dto/TimeWithPeriod.java)|
| fraho.jwt.token.header.enabled                 | true                        | Enables support for tokens sent as a header.|
| fraho.jwt.token.header.names                   | Authorization               | Sets the name of the headers which may contain the token.|
| fraho.jwt.token.hmac                           | null                        | Defines the key file when using a hmac signature method|
| fraho.jwt.token.issuer                         | fraho-security              | Sets the issuer of the token. The issuer is used in the tokens ```iss``` field|
| fraho.jwt.token.path                           | /auth/login                 | Sets the path for the RestController, defining the endpoint for login requests.|
| fraho.jwt.token.priv                           | null                        | Defines the private key file when using a public / private key signature method. May be null if this service should only verify, but not issue tokens. In this case, any calls to ```generateToken``` or ```generateRefreshToken``` will throw an FeatureNotConfiguredException. To the caller, it will be shown as a UNAUTHORIZED Http StatusCode.|
| fraho.jwt.token.pub                            | null                        | Defines the public key file when using a public / private key signature method|
| fraho.jwt.refresh.cache-impl                   | null                        | Defines the implemenation for refresh token storage. The specified class has to implement the [RefreshTokenStore](base/src/main/java/eu/fraho/spring/securityJwt/service/RefreshTokenStore.java) Interface. To disable the refresh tokens at all use null as value.<br>You have to add at least one of the optional dependencies below to add refresh token support.<br>Please see module READMEs for valid values.|
| fraho.jwt.refresh.cookie.enabled               | false                       | Enables support for tokens sent as a cookie|
| fraho.jwt.refresh.cookie.names                 | JWT-REFRESHTOKEN            | Sets the name of the cookie with the token. The first entry in this list is used when sending out the cookie, any other names are optionally taken when validating incoming requests.|
| fraho.jwt.refresh.cookie.domain                | null                        | see [javax.servlet.http.Cookie#setDomain(String)](https://docs.oracle.com/javaee/7/api/javax/servlet/http/Cookie.html#setDomain-java.lang.String-)|
| fraho.jwt.refresh.cookie.httpOnly              | true                        | see [javax.servlet.http.Cookie#setHttpOnly(boolean)](https://docs.oracle.com/javaee/7/api/javax/servlet/http/Cookie.html#setHttpOnly-boolean-)|
| fraho.jwt.refresh.cookie.secure                | true                        | see [javax.servlet.http.Cookie#setSecure(boolean)](https://docs.oracle.com/javaee/7/api/javax/servlet/http/Cookie.html#setSecure-boolean-)|
| fraho.jwt.refresh.cookie.fallbackRedirectUrl   | /                           | After using the refresh token redirect user to his referer or (if unavailable for any reason) to this path. The path may be a full URI.|
| fraho.jwt.refresh.cookie.path                  | /auth/refreshCookie         | Sets the path for the RestController, defining the endpoint for refresh requests. This endpoint has to be different to fraho.jwt.refresh.path|
| fraho.jwt.refresh.expiration                   | 1 day                       | How long are refresh tokens valid? For details on how this field has to specified see [TimeWithPeriod](base/src/main/java/eu/fraho/spring/securityJwt/dto/TimeWithPeriod.java)|
| fraho.jwt.refresh.length                       | 24                          | Defines the length of refresh tokens in bytes, without the base64 encoding|
| fraho.jwt.refresh.path                         | /auth/refresh               | Sets the path for the RestController, defining the endpoint for refresh requests.|
| fraho.totp.length                              | 16                          | Defines the length of the generated TOTP secrets|
| fraho.totp.variance                            | 3                           | Defines the allowed variance / validity of TOTP pins. The number defines how many "old / expired" pins will be considered valid. A value of "3" is the official suggestion for TOTP. This value is used to consider small clock-differences between the client and server.|
| fraho.crypt.algorithm                          | SHA512                      | Configure the used crypt algorithm. For a list of possible values see [CryptAlgorithm](base/src/main/java/eu/fraho/spring/securityJwt/dto/CryptAlgorithm.java) Please be aware that changing this parameter has a major effect on the strength of the hashed password! Do not use insecure algorithms (as DES or MD5 as time of writing) unless you really know what you do!|
| fraho.crypt.rounds                             | 10,000                      | Defines the "strength" of the hashing function. The more rounds used, the more secure the generated hash. But beware that more rounds mean more cpu-load and longer computation times! This parameter is only used if the specified algorithm supports hashing rounds.|

# Building
```bash
# on linux:
./gradlew assemble
# on windows:
gradlew.bat assemble
```

# Hacking
* This repository uses the git flow layout
* Changes are welcome, but please use pull requests with separate branches
* TravisCI has to pass before merging
* Code coverage should stay about the same level (please write tests for new features!)
* When writing new modules please use my abstract testclasses which provide a great base (see [internal](internal/src/test/java/eu/fraho/spring/securityJwt/service) for an example)

# Releasing
```bash
# to local repository:
./gradlew install
# to central:
./gradlew -Prelease check uploadArchives
```

# JWT Request Flow
## UML
[![Sequence diagram](doc/sequence.png)](doc/sequence.png)

## HTTP-Requests
Request a token (login):
```
> POST /auth/login HTTP/1.1
> Host: localhost:8080
> User-Agent: insomnia/5.0.20
> Accept: */*
> Accept-Encoding: deflate, gzip
> Content-Type: application/json
> Content-Length: 63
>
> {
>   "username": "userA",
>   "password": "userA"
> }
>
< HTTP/1.1 200
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< X-Request-Id: 732521a6-be29-41e8-9f1c-d3722ac0bcf3
< Content-Type: application/json;charset=UTF-8
< Content-Length: 491
< Date: Fri, 19 May 2017 08:04:14 GMT
<
< {
<   "accessToken": {
<     "token": "eyJhbGciOiJFUzI1NiJ9.<some more token code>",
<     "expiresIn": 3600,
<     "type": "Bearer"
<   },
<   "refreshToken": {
<     "token": "5bYNdXEGQRzz6xD4yFmw2wNPjXAh+wMc",
<     "expiresIn": 604800
<   }
< }
```

Usage of token to access some (secured) data:
```
> GET /users HTTP/1.1
> Host: localhost:8080
> User-Agent: insomnia/5.0.20
> Accept: */*
> Accept-Encoding: deflate, gzip
> Authorization: Bearer eyJhbGciOiJFUzI1NiJ9...
>
< HTTP/1.1 200
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< X-Request-Id: 4724811e-c2f0-492f-8d90-ab42901edcb5
< Content-Type: application/json;charset=UTF-8
< Content-Length: 1099
< Date: Fri, 19 May 2017 08:04:42 GMT
<
< (some json content, result of request)
```

Use refresh token when token expired:
```
> POST /auth/refresh HTTP/1.1
> Host: localhost:8080
> User-Agent: insomnia/5.0.20
> Accept: */*
> Accept-Encoding: deflate, gzip
> Content-Type: application/json
> Content-Length: 94
>
> {
>   "username": "userA",
>   "refreshToken": "5bYNdXEGQRzz6xD4yFmw2wNPjXAh+wMc"
> }
>
< HTTP/1.1 200
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Cache-Control: no-cache, no-store, max-age=0, must-revalidate
< Pragma: no-cache
< Expires: 0
< X-Frame-Options: DENY
< X-Request-Id: 23b61002-a37b-4bfb-9b69-be31b67cf5f3
< Content-Type: application/json;charset=UTF-8
< Content-Length: 491
< Date: Fri, 19 May 2017 08:05:19 GMT
<
< {
<   "accessToken": {
<     "token": "eyJhbGciOiJFUzI1NiJ9.<some more token code>",
<     "expiresIn": 3600,
<     "type": "Bearer"
<   },
<   "refreshToken": {
<     "token": "U3LFL8dVZAeAp8Js6db2zrHGPfGslIeQ",
<     "expiresIn": 604800
<   }
< }
```
