package com.dict.cache.service;

import com.dict.config.CacheConfig;
import com.dict.dict.domain.entity.SysDictData;
import com.dict.dict.mapper.SysDictDataMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class DictCacheServiceImpl implements DictCacheService {  // 实现 DictCacheService 接口


    private static final Logger log = LoggerFactory.getLogger(DictCacheServiceImpl.class);

    // Redis缓存前缀
    private static final String REDIS_DICT_PREFIX = "sys_dict:";
    // 本地缓存前缀
    private static final String LOCAL_DICT_PREFIX = "local_dict:";
    // 空值占位符（防缓存穿透）
    private static final String NULL_PLACEHOLDER = "__NULL__";

    private final RedisTemplate<String, Object> redisTemplate;
    private final Cache<String, Object> dictCache;
    private final SysDictDataMapper dictDataMapper;  // 添加 Mapper 依赖

    // 使用构造函数注入
    public DictCacheServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            Cache<String, Object> dictCache,
            SysDictDataMapper dictDataMapper) {  // 添加 Mapper 参数

        this.redisTemplate = redisTemplate;
        this.dictCache = dictCache;
        this.dictDataMapper = dictDataMapper;
    }

    /**
     * 根据字典类型和字典键获取字典值
     * 首先尝试从本地缓存中获取，若未命中则尝试从Redis中获取，
     * 若仍未命中则从数据库中查询，并在查询到结果后更新两级缓存
     *
     * @param dictType 字典类型，用于区分不同类型的字典
     * @param dictKey  字典键，用于获取特定字典项的值
     * @return 对应的字典值，如果未找到则返回null
     */
    @Override
    public String getDictValue(String dictType, String dictKey) {
        // 构建缓存键值
        String cacheKey = buildValueKey(dictType, dictKey);

        // 1. 检查本地缓存
        String cachedValue = getCachedValue(cacheKey);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 2. 检查Redis缓存
        String redisKey = REDIS_DICT_PREFIX + cacheKey;
        String redisValue = getRedisValue(redisKey, redisKey);
        if (redisValue != null) {
            return redisValue;
        }

        // 3. 查询数据库
        String dbValue = dictDataMapper.selectDictLabel(dictType, dictKey);
        // 处理空值情况（防缓存穿透）
        if (StringUtils.isEmpty(dbValue)) {
            // 缓存空值（短时间）
            redisTemplate.opsForValue().set(redisKey, NULL_PLACEHOLDER, 5, TimeUnit.MINUTES);
            dictCache.put(cacheKey, NULL_PLACEHOLDER);
            return null;
        }
        // 4. 更新两级缓存
        redisTemplate.opsForValue().set(redisKey, dbValue,
                getRandomExpireTime(CacheConfig.REDIS_CACHE_EXPIRE_HOURS, 0.2),
                TimeUnit.HOURS);
        dictCache.put(cacheKey, dbValue);

        return dbValue;
    }

    /**
     * 根据字典类型和字典值获取对应的字典标签
     * 该方法首先尝试从本地缓存中获取数据，如果未命中，则尝试从Redis缓存中获取
     * 如果两者都未命中，则从数据库中查询，并将结果缓存到本地和Redis中
     *
     * @param dictType  字典类型，用于区分不同类型的字典数据
     * @param dictValue 字典值，用于获取对应的字典标签
     * @return 返回对应的字典标签，如果未找到则返回null
     */
    @Override
    public String getDictLabel(String dictType, String dictValue) {
        // 构建缓存键
        String cacheKey = buildLabelKey(dictType, dictValue);

        // 1. 检查本地缓存
        String cachedValue = getCachedValue(cacheKey);
        if (cachedValue != null) {
            return cachedValue;
        }

        // 2. 检查Redis缓存
        String redisKey = REDIS_DICT_PREFIX + cacheKey;
        String redisValue = getRedisValue(redisKey, redisKey);
        if (redisValue != null) {
            return redisValue;
        }

        // 3. 查询数据库
        String dbLabel = dictDataMapper.selectDictLabel(dictType, dictValue);

        // 处理空值情况
        if (!StringUtils.hasText(dbLabel)) {
            // 缓存空值（短时间）
            redisTemplate.opsForValue().set(redisKey, NULL_PLACEHOLDER, 5, TimeUnit.MINUTES);
            dictCache.put(cacheKey, NULL_PLACEHOLDER);
            return null;
        }

        // 4. 更新两级缓存
        redisTemplate.opsForValue().set(redisKey, dbLabel,
                getRandomExpireTime(CacheConfig.REDIS_CACHE_EXPIRE_HOURS, 0.2),
                TimeUnit.HOURS);
        dictCache.put(cacheKey, dbLabel);

        return dbLabel;
    }

    /**
     * 根据字典类型获取字典列表
     * 首先尝试从本地缓存中获取数据，如果未命中，则尝试从Redis缓存中获取
     * 如果两者都未命中，则从数据库中查询，并将结果缓存到本地和Redis中
     *
     * @param dictType 字典类型
     * @return 字典列表，每个字典项包含'value', 'label', 'remark'三个字段
     */
    @Override
    public List<SysDictData> getDictList(String dictType) {
        // 构建缓存键
        String cacheKey = buildListKey(dictType);

        // 1. 检查本地缓存
        Object cachedList = dictCache.getIfPresent(cacheKey);
        if (cachedList != null) {
            if (NULL_PLACEHOLDER.equals(cachedList)) {
                return Collections.emptyList();
            }
            return (List<SysDictData>) cachedList;
        }

        // 2. 检查Redis缓存
        String redisKey = REDIS_DICT_PREFIX + cacheKey;
        try {
            Object redisList = redisTemplate.opsForValue().get(redisKey);
            if (redisList != null) {
                // 更新本地缓存
                dictCache.put(cacheKey, redisList);

                if (NULL_PLACEHOLDER.equals(redisList)) {
                    return Collections.emptyList();
                }

                return (List<SysDictData>) redisList;
            }

        } catch (Exception e) {
            log.warn("Redis operation failed for key: {}", redisKey, e);
        }


        // 3. 查询数据库
        List<SysDictData> dictDataList = dictDataMapper.selectDictDataByType(dictType);

        // 处理空值情况
        if (dictDataList == null || dictDataList.isEmpty()) {
            // 缓存空值（短时间）
            redisTemplate.opsForValue().set(redisKey, NULL_PLACEHOLDER, 5, TimeUnit.MINUTES);
            dictCache.put(cacheKey, NULL_PLACEHOLDER);
            return Collections.emptyList();
        }

//        // 转换为前端需要的格式
//        List<Map<String, String>> result = dictDataList.stream()
//                .map(item -> {
//                    Map<String, String> map = new HashMap<>();
//                    map.put("value", item.getDictValue());
//                    map.put("label", item.getDictLabel());
//                    map.put("remark", item.getRemark());
//                    return map;
//                })
//                .collect(Collectors.toList());

        // 4. 更新两级缓存
        redisTemplate.opsForValue().set(redisKey, dictDataList,
                getRandomExpireTime(CacheConfig.REDIS_CACHE_EXPIRE_HOURS, 0.2),
                TimeUnit.HOURS);
        dictCache.put(cacheKey, dictDataList);

        return dictDataList;
    }

    /**
     * 重写刷新字典缓存方法
     * 当字典类型的数据发生变化时，调用此方法清除缓存，确保下次查询时能获取到最新的数据
     *
     * @param dictType 字典类型，用于指定需要刷新缓存的字典类别
     */
    @Override
    public void refreshDictCache(String dictType) {
        // 清除本地缓存
        dictCache.invalidate(buildValueKeyPrefix(dictType));
        dictCache.invalidate(buildLabelKeyPrefix(dictType));
        dictCache.invalidate(buildListKey(dictType));

        // 清除Redis缓存
        String pattern = REDIS_DICT_PREFIX + dictType + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // 记录日志信息
        log.info("字典缓存已刷新: {}", dictType);

    }

    /**
     * 获取缓存统计信息
     * <p>
     * 此方法收集缓存的统计信息，包括命中次数、未命中次数、加载成功次数、加载失败次数、
     * 总加载时间和驱逐次数，并计算命中率这些信息有助于评估缓存的性能和效率
     *
     * @return 包含缓存统计信息的映射，键为统计项名称，值为对应的统计值
     */
    @Override
    public Map<String, Object> getCacheStats() {
        // 获取当前缓存的统计信息
        CacheStats stats = dictCache.stats();

        // 创建一个映射来存储统计信息
        Map<String, Object> result = new HashMap<>();
        // 将各项统计信息放入结果映射中
        result.put("hitCount", stats.hitCount());
        result.put("missCount", stats.missCount());
        result.put("loadSuccessCount", stats.loadSuccessCount());
        result.put("loadFailureCount", stats.loadFailureCount());
        result.put("totalLoadTime", stats.totalLoadTime());
        result.put("evictionCount", stats.evictionCount());

        // 计算命中率
        double hitRate = (stats.hitCount() + stats.missCount()) == 0 ? 0 :
                (double) stats.hitCount() / (stats.hitCount() + stats.missCount());
        // 将命中率格式化为百分比字符串并存入结果映射
        result.put("hitRate", String.format("%.2f%%", hitRate * 100));

        // 返回包含所有统计信息的映射
        return result;
    }


    /**
     * 从缓存中获取指定键的值
     *
     * @param cacheKey 缓存键
     * @return 缓存中存储的字符串值，如果键不存在或值为NULL_PLACEHOLDER则返回null
     */
    private String getCachedValue(String cacheKey) {
        // 从缓存中获取值，如果存在则进行处理
        Object cachedValue = dictCache.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 检查是否为NULL占位符，如果是则返回null，否则返回实际值
            if (NULL_PLACEHOLDER.equals(cachedValue)) {
                return null;
            }
            return (String) cachedValue;
        }
        return null;
    }


    /**
     * 从Redis中获取指定键的值，并更新本地缓存
     *
     * @param cacheKey 本地缓存的键
     * @param redisKey Redis中的键
     * @return Redis中存储的字符串值，如果值为空或null则返回null
     */
    private String getRedisValue(String cacheKey, String redisKey) {
        try {
            Object redisValue = redisTemplate.opsForValue().get(redisKey);
            // 检查Redis中获取的值是否有效
            if (redisValue != null) {
                // 更新本地缓存
                dictCache.put(cacheKey, redisValue);

                // 处理空值占位符的情况
                if (NULL_PLACEHOLDER.equals(redisValue)) {
                    return null;
                }
                return (String) redisValue;
            }
        } catch (Exception e) {
            log.warn("Redis operation failed for key: {}", redisKey, e);
        }
        return null;
    }


    // ========== 辅助方法 ==========
    private String buildValueKey(String dictType, String dictKey) {
        return LOCAL_DICT_PREFIX + "value:" + dictType + ":" + dictKey;
    }

    private String buildValueKeyPrefix(String dictType) {
        return LOCAL_DICT_PREFIX + "value:" + dictType + ":";
    }

    private String buildLabelKey(String dictType, String dictValue) {
        return LOCAL_DICT_PREFIX + "label:" + dictType + ":" + dictValue;
    }

    private String buildLabelKeyPrefix(String dictType) {
        return LOCAL_DICT_PREFIX + "label:" + dictType + ":";
    }

    private String buildListKey(String dictType) {
        return LOCAL_DICT_PREFIX + "list:" + dictType;
    }

    /**
     * 获取随机过期时间（防止缓存雪崩）
     *
     * @param baseTime  基础时间
     * @param variation 变化幅度 (0-1)
     */
    private long getRandomExpireTime(long baseTime, double variation) {
        double factor = 1 + (Math.random() * 2 * variation - variation);
        return (long) (baseTime * factor);
    }
}
