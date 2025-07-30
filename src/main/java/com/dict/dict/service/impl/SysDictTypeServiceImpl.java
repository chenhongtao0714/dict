package com.dict.dict.service.impl;


import com.dict.cache.service.DictCacheService;
import com.dict.dict.domain.entity.SysDictData;
import com.dict.dict.domain.entity.SysDictType;
import com.dict.dict.mapper.SysDictDataMapper;
import com.dict.dict.mapper.SysDictTypeMapper;
import com.dict.dict.service.ISysDictTypeService;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;


/**
 * 字典 业务层处理
 *
 * @author ruoyi
 */
@Service
public class SysDictTypeServiceImpl implements ISysDictTypeService {

    @Autowired
    private SysDictTypeMapper dictTypeMapper;

    @Autowired
    private SysDictDataMapper dictDataMapper;

    @Autowired
    private DictCacheService dictCacheService;

    /**
     * 项目启动时，初始化字典到缓存
     */
    @PostConstruct
    public void init() {
        loadingDictCache();
    }

    /**
     * 根据条件分页查询字典类型
     *
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeList(SysDictType dictType) {
        return dictTypeMapper.selectDictTypeList(dictType);
    }

    /**
     * 根据所有字典类型
     *
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeAll() {
        return dictTypeMapper.selectDictTypeAll();

    }

    /**
     * 根据字典类型查询字典数据
     *
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataByType(String dictType) {
        return dictCacheService.getDictList(dictType);
    }

    @Override
    public String selectDictDataByValue(String dictLabel) {
        String dictType = "sys_user_sex";

        return dictCacheService.getDictLabel(dictType,dictLabel);
    }

    /**
     * 根据字典类型ID查询信息
     *
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeById(Long dictId) {
        return dictTypeMapper.selectDictTypeById(dictId);
    }

    /**
     * 根据字典类型查询信息
     *
     * @param dictType 字典类型
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeByType(String dictType) {
        return dictTypeMapper.selectDictTypeByType(dictType);
    }

    /**
     * 批量删除字典类型信息
     *
     * @param dictIds 需要删除的字典ID
     */
    @Override
    public void deleteDictTypeByIds(Long[] dictIds) {
        for (Long dictId : dictIds) {
            SysDictType dictType = selectDictTypeById(dictId);
            if (dictDataMapper.countDictDataByType(dictType.getDictType()) > 0) {
//                throw new ServiceException(String.format("%1$s已分配,不能删除", dictType.getDictName()));
                throw new RuntimeException(dictType.getDictName() + "已分配,不能删除");
            }
            dictTypeMapper.deleteDictTypeById(dictId);
            dictCacheService.refreshDictCache(dictType.getDictType());
        }
    }

    /**
     * 加载字典缓存数据
     */
    @Override
    public void loadingDictCache() {
        SysDictData dictData = new SysDictData();
        dictData.setStatus("0");
        List<SysDictType> dictTypeList = dictTypeMapper.selectDictTypeAll();
        for (SysDictType dictType : dictTypeList) {
            dictCacheService.getDictList(dictType.getDictType());
        }
    }

    /**
     * 清空字典缓存数据
     */
    @Override
    public void clearDictCache() {
        List<SysDictType> dictTypeList = dictTypeMapper.selectDictTypeAll();
        for (SysDictType dictType : dictTypeList) {
            // 逐个刷新字典类型缓存
            dictCacheService.refreshDictCache(dictType.getDictType());
        }
    }

    /**
     * 重置字典缓存数据
     */
    @Override
    public void resetDictCache() {
        clearDictCache();
        loadingDictCache();
    }

    /**
     * 新增保存字典类型信息
     *
     * @param dictType 字典类型信息
     * @return 结果
     */
    @Override
    public int insertDictType(SysDictType dictType) {
        int row = dictTypeMapper.insertDictType(dictType);
        if (row > 0) {
            dictCacheService.refreshDictCache(dictType.getDictType());
        }
        return row;
    }

    /**
     * 修改保存字典类型信息
     *
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateDictType(SysDictType dict) {
        SysDictType oldDict = dictTypeMapper.selectDictTypeById(dict.getDictId());
        // 如果字典类型发生变化，更新关联的字典数据
        if (!oldDict.getDictType().equals(dict.getDictType())) {
            dictDataMapper.updateDictDataType(oldDict.getDictType(), dict.getDictType());
        }

        int row = dictTypeMapper.updateDictType(dict);
        if (row > 0) {
            // 刷新旧字典类型缓存
            dictCacheService.refreshDictCache(oldDict.getDictType());

            // 如果字典类型发生变化，刷新新字典类型缓存
            if (!oldDict.getDictType().equals(dict.getDictType())) {
                dictCacheService.refreshDictCache(dict.getDictType());
            }
        }
        return row;
    }

    /**
     * 校验字典类型称是否唯一
     *
     * @param dict 字典类型
     * @return 结果
     */
    @Override
    public boolean checkDictTypeUnique(SysDictType dict) {
        Long dictId = dict.getDictId() == null ? -1L : dict.getDictId();
        SysDictType dictType = dictTypeMapper.checkDictTypeUnique(dict.getDictType());
        return dictType == null || dictType.getDictId().equals(dictId);
    }
}
