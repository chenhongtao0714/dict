package com.dict.config;

import com.dict.dict.mapper.SysDictDataMapper;
import com.dict.cache.service.DictCacheService;
import com.dict.cache.service.DictCacheServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    // 本地缓存过期时间（分钟）
    public static final long LOCAL_CACHE_EXPIRE_MINUTES = 30;
    // Redis缓存过期时间（小时）
    public static final long REDIS_CACHE_EXPIRE_HOURS = 12;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SysDictDataMapper dictDataMapper;

    /**
     * 创建Caffeine本地缓存实例
     */
    @Bean("dictCache")
    public Cache<String, Object> dictCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(LOCAL_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats() // 启用统计
                .build();
    }


    /**
     * 缓存服务Bean
     */
    @Bean
    public DictCacheService dictCacheService(
            @Qualifier("dictCache") Cache<String, Object> caffeineCache) {
        return new DictCacheServiceImpl(redisTemplate, caffeineCache, dictDataMapper);
    }

}
