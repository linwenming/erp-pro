/**
 * Copyright 卫志强 QQ：598748873@qq.com Inc. All rights reserved.
 */
package com.skyeye.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.miemiedev.mybatis.paginator.domain.PageBounds;
import com.github.miemiedev.mybatis.paginator.domain.PageList;
import com.skyeye.common.object.InputObject;
import com.skyeye.common.object.OutputObject;
import com.skyeye.common.util.ExcelUtil;
import com.skyeye.common.util.ToolUtil;
import com.skyeye.dao.ReceivablesDao;
import com.skyeye.erp.util.ErpConstants;
import com.skyeye.erp.util.ErpOrderNum;
import com.skyeye.service.ReceivablesService;

import net.sf.json.JSONArray;

/**
 * @Author 卫志强
 * @Description TODO
 * @Date 2019/10/20 10:23
 */
@Service
public class ReceivablesServiceImpl implements ReceivablesService {

    @Autowired
    private ReceivablesDao receivablesDao;

    /**
     * 查询收款单列表信息
     * @param inputObject
     * @param outputObject
     * @throws Exception
     */
    @Override
    public void queryReceivablesByList(InputObject inputObject, OutputObject outputObject) throws Exception {
        Map<String, Object> params = inputObject.getParams();
        params.put("tenantId", inputObject.getLogParams().get("tenantId"));
        List<Map<String, Object>> beans = receivablesDao.queryReceivablesByList(params,
                new PageBounds(Integer.parseInt(params.get("page").toString()), Integer.parseInt(params.get("limit").toString())));
        PageList<Map<String, Object>> beansPageList = (PageList<Map<String, Object>>) beans;
        int total = beansPageList.getPaginator().getTotalCount();
        outputObject.setBeans(beans);
        outputObject.settotal(total);
    }

    /**
     * 添加收款单
     * @param inputObject
     * @param outputObject
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
	@Override
    @Transactional(value="transactionManager")
    public void insertReceivables(InputObject inputObject, OutputObject outputObject) throws Exception {
        Map<String, Object> params = inputObject.getParams();
        String initemStr = params.get("initemStr").toString();
        if(ToolUtil.isJson(initemStr)) {
            //财务主表ID
            String useId = ToolUtil.getSurFaceId();
            String tenantId = inputObject.getLogParams().get("tenantId").toString();
            //处理数据
            JSONArray jArray = JSONArray.fromObject(initemStr);
            //收款单中间转换对象，财务子表存储对象
            Map<String, Object> bean;
            List<Map<String, Object>> entitys = new ArrayList<>();//财务子表实体集合信息
            BigDecimal allPrice = new BigDecimal("0");//主单总价
            BigDecimal itemAllPrice = null;//子单对象
            for(int i = 0; i < jArray.size(); i++){
                bean = jArray.getJSONObject(i);
                Map<String, Object> entity = new HashMap<>();
                //获取子项金额
                itemAllPrice = new BigDecimal(bean.get("initemMoney").toString());
                entity.put("id", ToolUtil.getSurFaceId());
                entity.put("headerId", useId);
                entity.put("accountId", bean.get("accountId"));
                entity.put("eachAmount", bean.get("initemMoney"));
                entity.put("remark", bean.get("remark"));
                entity.put("tenantId", tenantId);
                entity.put("deleteFlag", 0);
                entitys.add(entity);
                //计算总金额
                allPrice = allPrice.add(itemAllPrice);
            }
            if(entitys.size() == 0){
                outputObject.setreturnMessage("请选择账户");
                return;
            }
            Map<String, Object> accountHead = new HashMap<>();
            ErpOrderNum erpOrderNum = new ErpOrderNum();
            String orderNum = erpOrderNum.getAccountOrderNumBySubType(tenantId, ErpConstants.AccountTheadSubType.RECEIVABLES_ORDER.getNum());
            accountHead.put("id", useId);
            accountHead.put("type", ErpConstants.AccountTheadSubType.RECEIVABLES_ORDER.getNum());//收款单
            accountHead.put("billNo", orderNum);
            accountHead.put("totalPrice", allPrice);
            accountHead.put("tenantId", tenantId);
            accountHead.put("organId", params.get("organId"));
            accountHead.put("operTime", params.get("operTime"));
            accountHead.put("handsPersonId", params.get("handsPersonId"));
            accountHead.put("remark", params.get("remark"));
            accountHead.put("changeAmount", params.get("changeAmount"));
            accountHead.put("deleteFlag", 0);
            receivablesDao.insertReceivables(accountHead);
            receivablesDao.insertReceivablesItem(entitys);
        }else{
            outputObject.setreturnMessage("数据格式错误");
        }
    }

    /**
     * 查询收款单用于数据回显
     * @param inputObject
     * @param outputObject
     * @throws Exception
     */
    @Override
    public void queryReceivablesToEditById(InputObject inputObject, OutputObject outputObject) throws Exception {
        Map<String, Object> params = inputObject.getParams();
        params.put("tenantId", inputObject.getLogParams().get("tenantId"));
        Map<String, Object> bean = receivablesDao.queryReceivablesToEditById(params);
        if(bean != null && !bean.isEmpty()){
        	List<Map<String, Object>> beans = receivablesDao.queryReceivablesItemsToEditById(params);
        	bean.put("items", beans);
        	//获取经手人员
        	List<Map<String, Object>> userInfo = receivablesDao.queryUserInfoById(bean);
        	bean.put("userInfo", userInfo);
        	outputObject.setBean(bean);
        	outputObject.settotal(1);
        }else{
        	outputObject.setreturnMessage("未查询到信息！");
        }
    }

    /**
     * 编辑收款单信息
     * @param inputObject
     * @param outputObject
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
	@Override
    @Transactional(value="transactionManager")
    public void editReceivablesById(InputObject inputObject, OutputObject outputObject) throws Exception {
        Map<String, Object> params = inputObject.getParams();
        String initemStr = params.get("initemStr").toString();
        if(ToolUtil.isJson(initemStr)) {
        	String useId = params.get("id").toString();
        	String tenantId = inputObject.getLogParams().get("tenantId").toString();
            //处理数据
            JSONArray jArray = JSONArray.fromObject(initemStr);
            //收款单中间转换对象，财务子表存储对象
            Map<String, Object> bean;
            List<Map<String, Object>> entitys = new ArrayList<>();//财务子表实体集合信息
            BigDecimal allPrice = new BigDecimal("0");//主单总价
            BigDecimal itemAllPrice = null;//子单对象
            for(int i = 0; i < jArray.size(); i++){
                bean = jArray.getJSONObject(i);
                Map<String, Object> entity = new HashMap<>();
                //获取子项金额
                itemAllPrice = new BigDecimal(bean.get("initemMoney").toString());
                entity.put("id", ToolUtil.getSurFaceId());
                entity.put("headerId", useId);
                entity.put("accountId", bean.get("accountId"));
                entity.put("eachAmount", bean.get("initemMoney"));
                entity.put("remark", bean.get("remark"));
                entity.put("tenantId", tenantId);
                entity.put("deleteFlag", "0");
                entitys.add(entity);
                //计算总金额
                allPrice = allPrice.add(itemAllPrice);
            }
            if(entitys.size() == 0){
                outputObject.setreturnMessage("请选择账户");
                return;
            }
            Map<String, Object> accountHead = new HashMap<>();
            accountHead.put("id", useId);
            accountHead.put("tenantId", tenantId);
            accountHead.put("totalPrice", allPrice);
            accountHead.put("organId", params.get("organId"));
            accountHead.put("operTime", params.get("operTime"));
            accountHead.put("handsPersonId", params.get("handsPersonId"));
            accountHead.put("remark", params.get("remark"));
            accountHead.put("changeAmount", params.get("changeAmount"));
            receivablesDao.editReceivablesById(accountHead);
            //删除之前的绑定信息
            receivablesDao.deleteReceivablesItemById(params);
            receivablesDao.insertReceivablesItem(entitys);
        }else{
            outputObject.setreturnMessage("数据格式错误");
        }
    }

    /**
     * 删除收款单信息
     * @param inputObject
     * @param outputObject
     * @throws Exception
     */
    @Override
    @Transactional(value="transactionManager")
    public void deleteReceivablesById(InputObject inputObject, OutputObject outputObject) throws Exception {
        Map<String, Object> params = inputObject.getParams();
        params.put("tenantId", inputObject.getLogParams().get("tenantId"));
        params.put("deleteFlag", 1);
        receivablesDao.editReceivablesByDeleteFlag(params);
        receivablesDao.editReceivablesItemsByDeleteFlag(params);
    }

    /**
     * 查看收款单详情
     * @param inputObject
     * @param outputObject
     * @throws Exception
     */
    @Override
    public void queryReceivablesByDetail(InputObject inputObject, OutputObject outputObject) throws Exception {
        Map<String, Object> params = inputObject.getParams();
        params.put("tenantId", inputObject.getLogParams().get("tenantId"));
        //获取财务主表信息
        Map<String, Object> bean = receivablesDao.queryReceivablesDetailById(params);
        if(bean != null && !bean.isEmpty()){
            //获取子表信息
            List<Map<String, Object>> beans = receivablesDao.queryReceivablesItemsDetailById(bean);
            bean.put("items", beans);
            outputObject.setBean(bean);
            outputObject.settotal(1);
        }else{
            outputObject.setreturnMessage("该数据已不存在.");
        }
    }

    /**
     * 导出Excel
     * @param inputObject
     * @param outputObject
     * @throws Exception
     */
	@SuppressWarnings("static-access")
	@Override
	public void queryMationToExcel(InputObject inputObject, OutputObject outputObject) throws Exception {
		Map<String, Object> params = inputObject.getParams();
        params.put("tenantId", inputObject.getLogParams().get("tenantId"));
        List<Map<String, Object>> beans = receivablesDao.queryMationToExcel(params);
        String[] key = new String[]{"billNo", "supplierName", "totalPrice", "hansPersonName", "billTime"};
        String[] column = new String[]{"单据编号", "付款单位", "合计金额", "经手人", "单据日期"};
        String[] dataType = new String[]{"", "data", "data", "data", "data"};
        //收款单信息导出
        ExcelUtil.createWorkBook("收款单", "收款单详细", beans, key, column, dataType, inputObject.getResponse());
	}
}
