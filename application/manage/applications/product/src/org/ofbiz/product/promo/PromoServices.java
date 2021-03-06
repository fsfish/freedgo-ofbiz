/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.product.promo;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.lang.RandomStringUtils;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.datasource.GenericHelperInfo;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.entity.jdbc.SQLProcessor;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Promotions Services
 */
public class PromoServices {

    public final static String module = org.ofbiz.product.promo.PromoServices.class.getName();
    public static final String resource = "ProductUiLabels";
    protected final static char[] smartChars = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
            'Z', '2', '3', '4', '5', '6', '7', '8', '9'};

    public static Map<String, Object> createProductPromoCodeSet(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericEntityException {

        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map success = ServiceUtil.returnSuccess();
        Long quantity = (Long) context.get("quantity");
        int codeLength = (Integer) context.get("codeLength");
        String promoCodeLayout = (String) context.get("promoCodeLayout");

        // For PromoCodes we give the option not to use chars that are easy to mix up like 0<>O, 1<>I, ...
        boolean useSmartLayout = false;
        boolean useNormalLayout = false;
        if ("smart".equals(promoCodeLayout)) {
            useSmartLayout = true;
        } else if ("normal".equals(promoCodeLayout)) {
            useNormalLayout = true;
        }


        String couponCode = (String) context.get("couponCode");
        //查询出该代金券的开始时间和结束时间
        Timestamp fromDate = null;
        Timestamp thruDate = null;
        GenericValue productPromoCoupon = delegator.findByPrimaryKey("ProductPromoCoupon", UtilMisc.toMap("couponCode", couponCode));
        String validitType = productPromoCoupon.getString("validitType");
        if ("ROLL".equals(validitType)) {
            fromDate = new Timestamp(System.currentTimeMillis());
            int validitDays = productPromoCoupon.getLong("validitDays").intValue();
            thruDate = new Timestamp(System.currentTimeMillis() + validitDays * 24 * 60 * 60 * 1000);
        } else {
            fromDate = productPromoCoupon.getTimestamp("useBeginDate");
            thruDate = productPromoCoupon.getTimestamp("useEndDate");
        }
        GenericValue userLogin = (GenericValue) context.get("userLogin");


        String newPromoCodeId = "";
        StringBuilder bankOfNumbers = new StringBuilder();
        StringBuilder productPromoCodeIdSB = new StringBuilder();
        bankOfNumbers.append("Following PromoCodes have been created: ");
        for (long i = 0; i < quantity; i++) {
            Map<String, Object> createProductPromoCodeMap = null;
            boolean foundUniqueNewCode = false;
            long count = 0;

            while (!foundUniqueNewCode) {
                if (useSmartLayout) {
                    newPromoCodeId = RandomStringUtils.random(codeLength, smartChars);
                } else if (useNormalLayout) {
                    newPromoCodeId = RandomStringUtils.randomAlphanumeric(codeLength);
                }
                GenericValue existingPromoCode = null;
                try {
                    existingPromoCode = delegator.findByPrimaryKeyCache("ProductPromoCode", "productPromoCodeId", newPromoCodeId);
                } catch (GenericEntityException e) {
                    Debug.logWarning("Could not find ProductPromoCode for just generated ID: " + newPromoCodeId, module);
                }
                if (existingPromoCode == null) {
                    foundUniqueNewCode = true;
                }

                count++;
                if (count > 999999) {
                    return ServiceUtil.returnError("Unable to locate unique PromoCode! Length [" + codeLength + "]");
                }
            }
            try {
//                Map<String, Object> newContext = dctx.makeValidContext("createProductPromoCode", "IN", context);
//                newContext.put("productPromoCodeId", newPromoCodeId);
//                createProductPromoCodeMap = dispatcher.runSync("createProductPromoCode", newContext);
                Map<String, Object> newContext = FastMap.newInstance();
                GenericValue productPromoCode = delegator.makeValue("ProductPromoCode");
                productPromoCode.setNonPKFields(context);
                productPromoCode.put("productPromoCodeId", newPromoCodeId);
                productPromoCode.put("promoCodeStatus", "C");
                productPromoCode.put("fromDate", fromDate);
                productPromoCode.put("thruDate", thruDate);
                productPromoCode.put("createdDate", new Timestamp(System.currentTimeMillis()));
                productPromoCode.put("lastModifiedDate", new Timestamp(System.currentTimeMillis()));

                productPromoCode.create();
                createProductPromoCodeMap = productPromoCode.toMap();

            } catch (Exception err) {
                return ServiceUtil.returnError("Could not create a bank of promo codes", null, null, createProductPromoCodeMap);
            }
            if (ServiceUtil.isError(createProductPromoCodeMap)) {
                return ServiceUtil.returnError("Could not create a bank of promo codes", null, null, createProductPromoCodeMap);
            }
            bankOfNumbers.append((String) createProductPromoCodeMap.get("productPromoCodeId"));
            bankOfNumbers.append(",");

            productPromoCodeIdSB.append((String) createProductPromoCodeMap.get("productPromoCodeId"));
            productPromoCodeIdSB.append(",");
        }

        success.put(ModelService.SUCCESS_MESSAGE, bankOfNumbers.toString());
        String productPromoCodeIds = productPromoCodeIdSB.toString();
        success.put("productPromoCodeIds", productPromoCodeIds.substring(0, productPromoCodeIds.length() - 1));

        return success;
    }

    public static synchronized Map<String, Object> createOneProductPromoCode(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericEntityException, SQLException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map success = ServiceUtil.returnSuccess();
        String couponCode = (String) context.get("couponCode");
        //查询出该代金券的开始时间和结束时间
        Timestamp fromDate = null;
        Timestamp thruDate = null;
        GenericValue productPromoCoupon = delegator.findByPrimaryKey("ProductPromoCoupon", UtilMisc.toMap("couponCode", couponCode));
        String validitType = productPromoCoupon.getString("validitType");
        if ("ROLL".equals(validitType)) {
            fromDate = new Timestamp(System.currentTimeMillis());
            int validitDays = productPromoCoupon.getLong("validitDays").intValue();
            thruDate = new Timestamp(System.currentTimeMillis() + (long)validitDays * 24 * 60 * 60 * 1000);
        } else {
            fromDate = productPromoCoupon.getTimestamp("useBeginDate");
            thruDate = productPromoCoupon.getTimestamp("useEndDate");
        }
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        boolean foundUniqueNewCode = false;
        String productPromoCodeId = "";

        while (!foundUniqueNewCode) {
            productPromoCodeId = RandomStringUtils.random(8+new Random().nextInt(3), smartChars);

            GenericValue existingPromoCode = null;
            try {
                existingPromoCode = delegator.findByPrimaryKeyCache("ProductPromoCode", "productPromoCodeId", productPromoCodeId);
            } catch (GenericEntityException e) {
                Debug.logWarning("生成的productPromoCodeId已存在 " + productPromoCodeId, module);
            }
            if (existingPromoCode == null) {
                foundUniqueNewCode = true;
            }
            GenericValue productPromoCode = delegator.makeValue("ProductPromoCode");
            productPromoCode.setNonPKFields(context);
            productPromoCode.put("productPromoCodeId", productPromoCodeId);
            productPromoCode.put("promoCodeStatus", "C");
            productPromoCode.put("userEntered", "N");
            productPromoCode.put("requireEmailOrParty", "Y");
            productPromoCode.put("useLimitPerCode", 1L);
            productPromoCode.put("useLimitPerCustomer", 1L);
            productPromoCode.put("codeStatus", "COUPON_CODE_ENABLE");
            productPromoCode.put("fromDate", fromDate);
            productPromoCode.put("thruDate", thruDate);
            productPromoCode.put("createdByUserLogin", userLogin.getString("userLoginId"));
            productPromoCode.put("lastModifiedByUserLogin", userLogin.getString("userLoginId"));
            productPromoCode.put("createdDate", new Timestamp(System.currentTimeMillis()));
            productPromoCode.put("lastModifiedDate", new Timestamp(System.currentTimeMillis()));
            productPromoCode.create();
        }
        success.put("productPromoCodeId", productPromoCodeId);
        //代金券使用数量加1
        String sql="UPDATE PRODUCT_PROMO_COUPON SET USER_COUNT=USER_COUNT+1 where COUPON_CODE='"+couponCode+"'";
        SQLProcessor sqlP =null;
        try {
            GenericHelperInfo helperInfo = delegator.getGroupHelperInfo("org.ofbiz");
            sqlP = new SQLProcessor(helperInfo);
            sqlP.prepareStatement(sql.toString());
            sqlP.executeUpdate();
        }finally {
            sqlP.close();
        }

//        String groupHelperName = delegator.getGroupHelperName("org.ofbiz");
//        Connection conn = ConnectionFactory.getConnection(groupHelperName);
//        Statement stmt = conn.createStatement();
//        stmt.executeUpdate(sql);
//        stmt.close();
//        conn.close();

        return success;

    }

    public static Map<String, Object> purgeOldStoreAutoPromos(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String productStoreId = (String) context.get("productStoreId");
        Locale locale = (Locale) context.get("locale");
        Timestamp nowTimestamp = UtilDateTime.nowTimestamp();

        List<EntityCondition> condList = FastList.newInstance();
        if (UtilValidate.isEmpty(productStoreId)) {
            condList.add(EntityCondition.makeCondition("productStoreId", EntityOperator.EQUALS, productStoreId));
        }
        condList.add(EntityCondition.makeCondition("userEntered", EntityOperator.EQUALS, "Y"));
        condList.add(EntityCondition.makeCondition("thruDate", EntityOperator.NOT_EQUAL, null));
        condList.add(EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN, nowTimestamp));
        EntityCondition cond = EntityCondition.makeCondition(condList, EntityOperator.AND);

        try {
            EntityListIterator eli = delegator.find("ProductStorePromoAndAppl", cond, null, null, null, null);
            GenericValue productStorePromoAndAppl = null;
            while ((productStorePromoAndAppl = eli.next()) != null) {
                GenericValue productStorePromo = delegator.makeValue("ProductStorePromoAppl");
                productStorePromo.setAllFields(productStorePromoAndAppl, true, null, null);
                productStorePromo.remove();
            }
            eli.close();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error removing expired ProductStorePromo records: " + e.toString(), module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ProductPromoCodeCannotBeRemoved", UtilMisc.toMap("errorString", e.toString()), locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> importPromoCodesFromFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");

        // check the uploaded file
        ByteBuffer fileBytes = (ByteBuffer) context.get("uploadedFile");
        if (fileBytes == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ProductPromoCodeImportUploadedFileNotValid", locale));
        }

        String encoding = System.getProperty("file.encoding");
        String file = Charset.forName(encoding).decode(fileBytes).toString();
        // get the createProductPromoCode Model
        ModelService promoModel;
        try {
            promoModel = dispatcher.getDispatchContext().getModelService("createProductPromoCode");
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }

        // make a temp context for invocations
        Map<String, Object> invokeCtx = promoModel.makeValid(context, ModelService.IN_PARAM);

        // read the bytes into a reader
        BufferedReader reader = new BufferedReader(new StringReader(file));
        List<Object> errors = FastList.newInstance();
        int lines = 0;
        String line;

        // read the uploaded file and process each line
        try {
            while ((line = reader.readLine()) != null) {
                // check to see if we should ignore this line
                if (line.length() > 0 && !line.startsWith("#")) {
                    if (line.length() > 0 && line.length() <= 20) {
                        // valid promo code
                        Map<String, Object> inContext = FastMap.newInstance();
                        inContext.putAll(invokeCtx);
                        inContext.put("productPromoCodeId", line);
                        Map<String, Object> result = dispatcher.runSync("createProductPromoCode", inContext);
                        if (result != null && ServiceUtil.isError(result)) {
                            errors.add(line + ": " + ServiceUtil.getErrorMessage(result));
                        }
                    } else {
                        // not valid ignore and notify
                        errors.add(line + UtilProperties.getMessage(resource, "ProductPromoCodeInvalidCode", locale));
                    }
                    ++lines;
                }
            }
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }

        // return errors or success
        if (errors.size() > 0) {
            return ServiceUtil.returnError(errors);
        } else if (lines == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ProductPromoCodeImportEmptyFile", locale));
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> importPromoCodeEmailsFromFile(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String productPromoCodeId = (String) context.get("productPromoCodeId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        ByteBuffer bytebufferwrapper = (ByteBuffer) context.get("uploadedFile");

        if (bytebufferwrapper == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ProductPromoCodeImportUploadedFileNotValid", locale));
        }

        byte[] wrapper = bytebufferwrapper.array();

        // read the bytes into a reader
        BufferedReader reader = new BufferedReader(new StringReader(new String(wrapper)));
        List<Object> errors = FastList.newInstance();
        int lines = 0;
        String line;

        // read the uploaded file and process each line
        try {
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0 && !line.startsWith("#")) {
                    if (UtilValidate.isEmail(line)) {
                        // valid email address
                        Map<String, Object> result = dispatcher.runSync("createProductPromoCodeEmail", UtilMisc.<String, Object>toMap("productPromoCodeId",
                                productPromoCodeId, "emailAddress", line, "userLogin", userLogin));
                        if (result != null && ServiceUtil.isError(result)) {
                            errors.add(line + ": " + ServiceUtil.getErrorMessage(result));
                        }
                    } else {
                        // not valid ignore and notify
                        errors.add(line + ": is not a valid email address");
                    }
                    ++lines;
                }
            }
        } catch (IOException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }

        // return errors or success
        if (errors.size() > 0) {
            return ServiceUtil.returnError(errors);
        } else if (lines == 0) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ProductPromoCodeImportEmptyFile", locale));
        }

        return ServiceUtil.returnSuccess();
    }
}
