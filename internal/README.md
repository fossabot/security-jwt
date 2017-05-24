# In-Memory refresh token support for [security-jwt](../)

This module adds support for storing refresh tokens within an in-memory storage.

# Dependencies
```xml
<dependency>
    <groupId>eu.fraho.spring</groupId>
    <artifactId>security-jwt-internal</artifactId>
    <version>0.6.0</version>
</dependency>
```

# Usage
* Add the dependency to your build script
* Use ```eu.fraho.spring.securityJwt.service.InternalTokenStore``` as ```fraho.jwt.refresh.cache.impl``` configuration value

This module doesn't use any additional application properties.