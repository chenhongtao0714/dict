package com.dict.cache.service;

import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class DictCacheServiceImplTest {

    @Autowired
    private DictCacheService dictCacheService;
    @Test
    void getDictValue() {

    }
}