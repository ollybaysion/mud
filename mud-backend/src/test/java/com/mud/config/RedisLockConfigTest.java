package com.mud.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RedisLockConfigTest {

    @Mock private RedisConnectionFactory connectionFactory;

    @Test
    @DisplayName("RedisLockRegistry 빈 생성")
    void createsRedisLockRegistry() {
        RedisLockConfig config = new RedisLockConfig();
        RedisLockRegistry registry = config.redisLockRegistry(connectionFactory);
        assertThat(registry).isNotNull();
    }
}
