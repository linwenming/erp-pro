package com.skyeye.dao;

import com.github.miemiedev.mybatis.paginator.domain.PageBounds;

import java.util.List;
import java.util.Map;

/**
 * @Author: 奈何繁华如云烟
 * @Description: TODO
 * @Date: 2019/9/16 21:23
 */
public interface SupplierDao {
    public List<Map<String, Object>> querySupplierByList(Map<String, Object> params, PageBounds pageBounds) throws Exception;

    public Map<String, Object> querySupplierByUserIdAndSupplier(Map<String, Object> params) throws Exception;

    public void insertSupplier(Map<String, Object> params) throws Exception;

    public Map<String, Object> querySupplierById(Map<String, Object> params) throws Exception;

    public void editSupplierByDeleteFlag(Map<String, Object> params) throws Exception;

    public void editSupplierById(Map<String, Object> params) throws Exception;

    public void editSupplierByEnabled(Map<String, Object> params) throws Exception;

    public void editSupplierByNotEnabled(Map<String, Object> params) throws Exception;
}
