# Memcache refresh token support for [security-jwt](../)

This module adds support for storing refresh tokens at an external memcache server.

Please note that the memcache-plugin needs an external memcached server.

# Dependencies
```xml
<dependency>
    <groupId>eu.fraho.spring</groupId>
    <artifactId>security-jwt-memcache</artifactId>
    <version>0.6.0</version>
</dependency>
```

# Usage
* Add the dependency to your build script
* Use ```eu.fraho.spring.securityJwt.service.MemcacheTokenStore``` as ```fraho.jwt.refresh.cache.impl``` configuration value

This module also uses some additional application properties:

| Property                                 | Default   | Description   |
|------------------------------------------|-----------|---------------|
| fraho.jwt.refresh.cache.memcache.host    | 127.0.0.1 | Hostname or IP Adress of memcache server|
| fraho.jwt.refresh.cache.memcache.port    | 11211     | Port of memcache server|
| fraho.jwt.refresh.cache.memcache.timeout | 5         | Timeout (in seconds) when talking to memcache server|