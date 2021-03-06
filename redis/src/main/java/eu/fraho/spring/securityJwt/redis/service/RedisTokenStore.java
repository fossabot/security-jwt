/*
 * MIT Licence
 * Copyright (c) 2017 Simon Frankenberger
 *
 * Please see LICENCE.md for complete licence text.
 */
package eu.fraho.spring.securityJwt.redis.service;

import eu.fraho.spring.securityJwt.config.RefreshProperties;
import eu.fraho.spring.securityJwt.dto.JwtUser;
import eu.fraho.spring.securityJwt.dto.RefreshToken;
import eu.fraho.spring.securityJwt.redis.config.RedisProperties;
import eu.fraho.spring.securityJwt.redis.dto.RedisEntry;
import eu.fraho.spring.securityJwt.service.RefreshTokenStore;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import redis.clients.jedis.*;

import java.util.*;

@Slf4j
@NoArgsConstructor
public class RedisTokenStore implements RefreshTokenStore {
    @Setter(onMethod = @__({@Autowired, @NonNull}))
    private RefreshProperties refreshProperties;

    @Setter(onMethod = @__({@Autowired, @NonNull}))
    private RedisProperties redisProperties;

    @Setter(onMethod = @__({@Autowired, @NonNull}))
    private UserDetailsService userDetailsService;

    private JedisPool redisPool;

    @Override
    public void saveToken(@NotNull JwtUser user, @NotNull String token) {
        String key = redisProperties.getPrefix() + token;
        String entry = RedisEntry.from(user).toString();
        try (Jedis jedis = redisPool.getResource()) {
            Transaction t = jedis.multi();
            t.set(key, entry);
            t.pexpire(key, refreshProperties.getExpiration().toMillis());
            t.exec();
        }
    }

    @Override
    public <T extends JwtUser> Optional<T> useToken(@NotNull String token) {
        String key = redisProperties.getPrefix() + token;
        Optional<T> result = Optional.empty();
        try (Jedis jedis = redisPool.getResource()) {
            Transaction transaction = jedis.multi();
            Response<String> tmp = transaction.get(key);
            Response<Long> del = transaction.del(key);
            transaction.exec();
            String found = tmp.get();
            if (found != null && del.get() == 1) {
                String username = RedisEntry.from(found).getUsername();
                result = Optional.ofNullable((T) userDetailsService.loadUserByUsername(username));
            }
        }
        return result;
    }

    @NotNull
    @Override
    public List<RefreshToken> listTokens(@NotNull JwtUser user) {
        return listTokens().getOrDefault(user.getId(), Collections.emptyList());
    }

    private <K, V> Map<K, V> zipToMap(List<K> keys, List<V> values) {
        Iterator<K> keyIter = keys.iterator();
        Iterator<V> valIter = values.iterator();
        Map<K, V> result = new HashMap<>();
        while (keyIter.hasNext() && valIter.hasNext()) {
            K key = keyIter.next();
            V val = valIter.next();

            if (key != null && val != null) {
                result.put(key, val);
            }
        }
        return result;
    }

    private Map<String, String> listKeysWithValues(Jedis jedis) {
        List<String> keys = new ArrayList<>(jedis.keys(redisProperties.getPrefix() + "*"));
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> values = jedis.mget(keys.toArray(new String[0]));
        return zipToMap(keys, values);
    }

    @NotNull
    @Override
    public Map<Long, List<RefreshToken>> listTokens() {
        final Map<Long, List<RefreshToken>> result = new HashMap<>();
        final int prefixLen = redisProperties.getPrefix().length();
        try (Jedis jedis = redisPool.getResource()) {
            Map<String, String> entries = listKeysWithValues(jedis);
            List<Runnable> resultBuilder = new ArrayList<>();
            Pipeline p = jedis.pipelined();
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                Long id = RedisEntry.from(entry.getValue()).getId();
                String token = entry.getKey().substring(prefixLen);
                List<RefreshToken> tokenList = result.computeIfAbsent(id, s -> new ArrayList<>());
                Response<Long> expiresIn = p.ttl(entry.getKey());
                resultBuilder.add(() -> tokenList.add(
                        RefreshToken.builder()
                                .token(token)
                                .expiresIn(expiresIn.get())
                                .build()
                        )
                );
            }
            p.sync();
            resultBuilder.forEach(Runnable::run);
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean revokeToken(@NotNull String token) {
        String key = redisProperties.getPrefix() + token;
        try (Jedis jedis = redisPool.getResource()) {
            return jedis.del(key) == 1;
        }
    }

    @Override
    public int revokeTokens(@NotNull JwtUser user) {
        try (Jedis jedis = redisPool.getResource()) {
            List<String> keys = new ArrayList<>(jedis.keys(redisProperties.getPrefix() + "*"));
            List<String> values = jedis.mget(keys.toArray(new String[0]));

            int counter = 0;
            for (int i = 0; i < keys.size(); i++) {
                if (values.get(i) == null) continue;
                RedisEntry dto = RedisEntry.from(values.get(i));
                if (Objects.equals(dto.getId(), user.getId())) {
                    counter += jedis.del(keys.get(i));
                }
            }
            return counter;
        }
    }

    @Override
    public int revokeTokens() {
        try (Jedis jedis = redisPool.getResource()) {
            List<String> keys = new ArrayList<>(jedis.keys(redisProperties.getPrefix() + "*"));
            Transaction transaction = jedis.multi();
            keys.forEach(transaction::del);
            return transaction.exec().size();
        }
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Using redis implementation to handle refresh tokens");
        log.info("Startingredis connection pool to {}:{}", redisProperties.getHost(), redisProperties.getPort());
        redisPool = new JedisPool(redisProperties.getPoolConfig(), redisProperties.getHost(), redisProperties.getPort());
    }
}
