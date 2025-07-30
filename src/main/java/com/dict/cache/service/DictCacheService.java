package com.dict.cache.service;

import com.dict.dict.domain.entity.SysDictData;

import java.util.List;
import java.util.Map;

public interface DictCacheService {
    /**
     * 根据字典类型和字典键获取字典值
     *
     * @param dictType 字典类型
     * @param dictKey  字典键
     * @return 字典值
     */
    String getDictValue(String dictType, String dictKey);

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    String getDictLabel(String dictType, String dictValue);


    /**
     * 根据字典类型获取字典列表
     *
     * @param dictType 字典类型，用于指定要查询的字典数据类别
     * @return 返回指定类型的字典数据列表，每个元素是一个包含字典项信息的Map，
     * Map中的key为字典项的标识，value为字典项的值
     */
    List<SysDictData> getDictList(String dictType);


    /**
     * 刷新指定字典类型的缓存
     *
     * @param dictType 字典类型
     */
    void refreshDictCache(String dictType);

    /**
     * 获取缓存统计信息
     *
     * @return 包含缓存统计信息的Map
     */
    Map<String, Object> getCacheStats();

}
