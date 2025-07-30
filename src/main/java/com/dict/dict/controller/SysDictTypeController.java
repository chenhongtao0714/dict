package com.dict.dict.controller;

import com.dict.dict.domain.entity.SysDictType;
import com.dict.dict.service.ISysDictTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据字典信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/dict/type")
public class SysDictTypeController {

    @Autowired
    private ISysDictTypeService dictTypeService;

    @GetMapping("/list")
    public List<SysDictType> list(SysDictType dictType) {

        return dictTypeService.selectDictTypeList(dictType);
    }


    /**
     * 查询字典类型详细
     */
    @GetMapping(value = "/{dictId}")
    public SysDictType getInfo(@PathVariable Long dictId) {
        return dictTypeService.selectDictTypeById(dictId);
    }

    /**
     * 查询字典类型详细
     */
    @GetMapping(value = "/getLabel/{dictLabel}")
    public String getLabel(@PathVariable String dictLabel) {
        return dictTypeService.selectDictDataByValue(dictLabel);
    }

    /**
     * 新增字典类型
     */

    @PostMapping
    public int add(@Validated @RequestBody SysDictType dict) {
        if (!dictTypeService.checkDictTypeUnique(dict)) {
            throw new RuntimeException("新增字典'" + dict.getDictName() + "'失败，字典类型已存在");
        }

        return dictTypeService.insertDictType(dict);
    }

    /**
     * 修改字典类型
     */

    @PutMapping
    public int edit(@Validated @RequestBody SysDictType dict) {
        if (!dictTypeService.checkDictTypeUnique(dict)) {
            throw new RuntimeException("修改字典'" + dict.getDictName() + "'失败，字典类型已存在");
        }

        return dictTypeService.updateDictType(dict);
    }

    /**
     * 删除字典类型
     */

    @DeleteMapping("/{dictIds}")
    public void remove(@PathVariable Long[] dictIds) {
        dictTypeService.deleteDictTypeByIds(dictIds);
    }

    /**
     * 刷新字典缓存
     */

    @DeleteMapping("/refreshCache")
    public void refreshCache() {
        dictTypeService.resetDictCache();
    }

    /**
     * 获取字典选择框列表
     */
    @GetMapping("/optionselect")
    public List<SysDictType> optionselect() {
        return dictTypeService.selectDictTypeAll();
    }
}
