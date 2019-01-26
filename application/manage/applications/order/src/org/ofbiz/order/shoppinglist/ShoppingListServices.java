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
package org.ofbiz.order.shoppinglist;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityTypeUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.order.shoppingcart.CartItemModifyException;
import org.ofbiz.order.shoppingcart.CheckOutHelper;
import org.ofbiz.order.shoppingcart.ItemNotFoundException;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.ofbiz.product.config.ProductConfigWorker;
import org.ofbiz.product.config.ProductConfigWrapper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.product.store.ProductStoreWorker;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.calendar.RecurrenceInfo;
import org.ofbiz.service.calendar.RecurrenceInfoException;

import com.ibm.icu.util.Calendar;

/**
 * Shopping List Services
 */
public class ShoppingListServices {

    public static final String module = ShoppingListServices.class.getName();
    public static final String resource_error = "OrderErrorUiLabels";

    public static Map<String, Object> setShoppingListRecurrence(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Timestamp startDate = (Timestamp) context.get("startDateTime");
        Timestamp endDate = (Timestamp) context.get("endDateTime");
        Integer frequency = (Integer) context.get("frequency");
        Integer interval = (Integer) context.get("intervalNumber");
        Locale locale = (Locale) context.get("locale");

        if (frequency == null || interval == null) {
            Debug.logWarning(UtilProperties.getMessage(resource_error,"OrderFrequencyOrIntervalWasNotSpecified", locale), module);
            return ServiceUtil.returnSuccess();
        }

        if (startDate == null) {
            switch (frequency.intValue()) {
                case 5:
                    startDate = UtilDateTime.getWeekStart(UtilDateTime.nowTimestamp(), 0, interval.intValue());
                    break;
                case 6:
                    startDate = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp(), 0, interval.intValue());
                    break;
                case 7:
                    startDate = UtilDateTime.getYearStart(UtilDateTime.nowTimestamp(), 0, interval.intValue());
                    break;
                default:
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderInvalidFrequencyForShoppingListRecurrence",locale));
            }
        }

        long startTime = startDate.getTime();
        long endTime = 0;
        if (endDate != null) {
            endTime = endDate.getTime();
        }

        RecurrenceInfo recInfo = null;
        try {
            recInfo = RecurrenceInfo.makeInfo(delegator, startTime, frequency.intValue(), interval.intValue(), -1, endTime);
        } catch (RecurrenceInfoException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToCreateShoppingListRecurrenceInformation",locale));
        }

        Debug.logInfo("Next Recurrence - " + UtilDateTime.getTimestamp(recInfo.next()), module);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("recurrenceInfoId", recInfo.getID());

        return result;
    }
 

    public static Map<String, Object> splitShipmentMethodString(DispatchContext dctx, Map<String, ? extends Object> context) {
        String shipmentMethodString = (String) context.get("shippingMethodString");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        if (UtilValidate.isNotEmpty(shipmentMethodString)) {
            int delimiterPos = shipmentMethodString.indexOf('@');
            String shipmentMethodTypeId = null;
            String carrierPartyId = null;

            if (delimiterPos > 0) {
                shipmentMethodTypeId = shipmentMethodString.substring(0, delimiterPos);
                carrierPartyId = shipmentMethodString.substring(delimiterPos + 1);
                result.put("shipmentMethodTypeId", shipmentMethodTypeId);
                result.put("carrierPartyId", carrierPartyId);
            }
        }
        return result;
    }

    public static Map<String, Object> makeListFromOrder(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        String shoppingListTypeId = (String) context.get("shoppingListTypeId");
        String shoppingListId = (String) context.get("shoppingListId");
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");

        Timestamp startDate = (Timestamp) context.get("startDateTime");
        Timestamp endDate = (Timestamp) context.get("endDateTime");
        Integer frequency = (Integer) context.get("frequency");
        Integer interval = (Integer) context.get("intervalNumber");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            GenericValue orderHeader = null;
            orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));

            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToLocateOrder", UtilMisc.toMap("orderId",orderId), locale));
            }
            String productStoreId = orderHeader.getString("productStoreId");

            if (UtilValidate.isEmpty(shoppingListId)) {
                // create a new shopping list
                if (partyId == null) {
                    partyId = userLogin.getString("partyId");
                }

                Map<String, Object> serviceCtx = UtilMisc.<String, Object>toMap("userLogin", userLogin, "partyId", partyId,
                        "productStoreId", productStoreId, "listName", "List Created From Order #" + orderId);

                if (UtilValidate.isNotEmpty(shoppingListTypeId)) {
                    serviceCtx.put("shoppingListTypeId", shoppingListTypeId);
                }

                Map<String, Object> newListResult = null;
                try {

                    newListResult = dispatcher.runSync("createShoppingList", serviceCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problems creating new ShoppingList", module);
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToCreateNewShoppingList",locale));
                }

                // check for errors
                if (ServiceUtil.isError(newListResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(newListResult));
                }

                // get the new list id
                if (newListResult != null) {
                    shoppingListId = (String) newListResult.get("shoppingListId");
                }
            }

            GenericValue shoppingList = null;
            shoppingList = delegator.findByPrimaryKey("ShoppingList", UtilMisc.toMap("shoppingListId", shoppingListId));

            if (shoppingList == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderNoShoppingListAvailable",locale));
            }
            shoppingListTypeId = shoppingList.getString("shoppingListTypeId");

            OrderReadHelper orh = new OrderReadHelper(orderHeader);
            if (orh == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToLoadOrderReadHelper", UtilMisc.toMap("orderId",orderId), locale));
            }

            List<GenericValue> orderItems = orh.getOrderItems();
            for(GenericValue orderItem : orderItems) {
                String productId = orderItem.getString("productId");
                if (UtilValidate.isNotEmpty(productId)) {
                    Map<String, Object> ctx = UtilMisc.<String, Object>toMap("userLogin", userLogin, "shoppingListId", shoppingListId, "productId",
                            orderItem.get("productId"), "quantity", orderItem.get("quantity"));
                    if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", ProductWorker.getProductTypeId(delegator, productId), "parentTypeId", "AGGREGATED")) {
                        try {
                            GenericValue instanceProduct = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
                            String configId = instanceProduct.getString("configId");
                            ctx.put("configId", configId);
                            String aggregatedProductId = ProductWorker.getInstanceAggregatedId(delegator, productId);
                            //override the instance productId with aggregated productId
                            ctx.put("productId", aggregatedProductId);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, module);
                        }
                    }
                    Map<String, Object> serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync("createShoppingListItem", ctx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, module);
                    }
                    if (serviceResult == null || ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToAddItemToShoppingList",UtilMisc.toMap("shoppingListId",shoppingListId), locale));
                    }
                }
            }

            if ("SLT_AUTO_REODR".equals(shoppingListTypeId)) {
                GenericValue paymentPref = EntityUtil.getFirst(orh.getPaymentPreferences());
                GenericValue shipGroup = EntityUtil.getFirst(orh.getOrderItemShipGroups());

                Map<String, Object> slCtx = FastMap.newInstance();
                slCtx.put("shipmentMethodTypeId", shipGroup.get("shipmentMethodTypeId"));
                slCtx.put("carrierRoleTypeId", shipGroup.get("carrierRoleTypeId"));
                slCtx.put("carrierPartyId", shipGroup.get("carrierPartyId"));
                slCtx.put("contactMechId", shipGroup.get("contactMechId"));
                slCtx.put("paymentMethodId", paymentPref.get("paymentMethodId"));
                slCtx.put("currencyUom", orh.getCurrency());
                slCtx.put("startDateTime", startDate);
                slCtx.put("endDateTime", endDate);
                slCtx.put("frequency", frequency);
                slCtx.put("intervalNumber", interval);
                slCtx.put("isActive", "Y");
                slCtx.put("shoppingListId", shoppingListId);
                slCtx.put("userLogin", userLogin);

                Map<String, Object> slUpResp = null;
                try {
                    slUpResp = dispatcher.runSync("updateShoppingList", slCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }

                if (slUpResp == null || ServiceUtil.isError(slUpResp)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource_error,"OrderUnableToUpdateShoppingListInformation",UtilMisc.toMap("shoppingListId",shoppingListId), locale));
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("shoppingListId", shoppingListId);
            return result;

        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error making shopping list from order", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[Delegator] Could not rollback transaction: " + e2.toString(), module);
            }

            String errMsg = "Error while creating new shopping list based on order" + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(errMsg);
        } finally {
            try {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not commit transaction for creating new shopping list based on order", module);
            }
        }
    }
    /**
     * Create a new shoppingCart form a shoppingList
     * @param dispatcher the local dispatcher
     * @param shoppingList a GenericValue object of the shopping list
     * @param locale the locale in use
     * @return returns a new shopping cart form a shopping list
     */
    public static ShoppingCart makeShoppingListCart(LocalDispatcher dispatcher, GenericValue shoppingList, Locale locale) {
        return makeShoppingListCart(null, dispatcher, shoppingList, locale); }

    /**
     * Add a shoppinglist to an existing shoppingcart
     *
     * @param listCart the shopping cart list
     * @param dispatcher the local dispatcher
     * @param shoppingList a GenericValue object of the shopping list
     * @param locale the locale in use
     * @return the modified shopping cart adding the shopping list elements
     */
    public static ShoppingCart makeShoppingListCart(ShoppingCart listCart, LocalDispatcher dispatcher, GenericValue shoppingList, Locale locale) {
        Delegator delegator = dispatcher.getDelegator();
        if (shoppingList != null && shoppingList.get("productStoreId") != null) {
            String productStoreId = shoppingList.getString("productStoreId");
            String currencyUom = shoppingList.getString("currencyUom");
            if (currencyUom == null) {
                GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
                if (productStore == null) {
                    return null;
                }
                currencyUom = productStore.getString("defaultCurrencyUomId");
            }
            if (locale == null) {
                locale = Locale.getDefault();
            }

            List<GenericValue> items = null;
            try {
                items = shoppingList.getRelated("ShoppingListItem", UtilMisc.toList("shoppingListItemSeqId"));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }

            if (UtilValidate.isNotEmpty(items)) {
                if (listCart == null) {
                    listCart = new ShoppingCart(delegator, productStoreId, locale, currencyUom);
                    listCart.setOrderPartyId(shoppingList.getString("partyId"));
                    listCart.setAutoOrderShoppingListId(shoppingList.getString("shoppingListId"));
                } else {
                    if (!listCart.getPartyId().equals(shoppingList.getString("partyId"))) {
                        Debug.logError("CANNOT add shoppingList: " + shoppingList.getString("shoppingListId")
                                + " of partyId: " + shoppingList.getString("partyId")
                                + " to a shoppingcart with a different orderPartyId: "
                                + listCart.getPartyId(), module);
                        return listCart;
                    }
                }


                ProductConfigWrapper configWrapper = null;
                for(GenericValue shoppingListItem : items) {
                    String productId = shoppingListItem.getString("productId");
                    BigDecimal quantity = shoppingListItem.getBigDecimal("quantity");
                    Timestamp reservStart = shoppingListItem.getTimestamp("reservStart");
                    BigDecimal reservLength = null;
                    String configId = shoppingListItem.getString("configId");
              //    String accommodationMapId = shoppingListItem.getString("accommodationMapId");
              //    String accommodationSpotId = shoppingListItem.getString("accommodationSpotId");

                    if (shoppingListItem.get("reservLength") != null) {
                        reservLength = shoppingListItem.getBigDecimal("reservLength");
                    }
                    BigDecimal reservPersons = null;
                    if (shoppingListItem.get("reservPersons") != null) {
                        reservPersons = shoppingListItem.getBigDecimal("reservPersons");
                    }
               /*   if (shoppingListItem.get("accommodationMapId") != null) {
                       accommodationMapId = shoppingListItem.getString("accommodationMapId");
                    }
                    if (shoppingListItem.get("accommodationSpotId") != null) {
                       accommodationSpotId = shoppingListItem.getString("accommodationSpotId");
                    }   */
                    if (UtilValidate.isNotEmpty(productId) && quantity != null) {

                    if (UtilValidate.isNotEmpty(configId)) {
                        configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, configId, productId, null, null, listCart.getWebSiteId(), listCart.getCurrency(), listCart.getLocale(), listCart.getAutoUserLogin());
                    }
                        // list items are noted in the shopping cart
                        String listId = shoppingListItem.getString("shoppingListId");
                        String itemId = shoppingListItem.getString("shoppingListItemSeqId");
                        Map<String, Object> attributes = UtilMisc.<String, Object>toMap("shoppingListId", listId, "shoppingListItemSeqId", itemId);
                        BigDecimal price = shoppingListItem.getBigDecimal("modifiedPrice");
                        try {
                            listCart.addOrIncreaseItem(productId, null, quantity, reservStart, reservLength, reservPersons, null, null, null, null, null, attributes, null,null, configWrapper, null, null, null, dispatcher,"Y",price);
                        } catch (CartItemModifyException e) {
                            Debug.logError(e, "Unable to add product to List Cart - " + productId, module);
                        } catch (ItemNotFoundException e) {
                            Debug.logError(e, "Product not found - " + productId, module);
                        }
                    }
                }

                if (listCart.size() > 0) {
                    if (UtilValidate.isNotEmpty(shoppingList.get("paymentMethodId"))) {
                        listCart.addPayment(shoppingList.getString("paymentMethodId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get("contactMechId"))) {
                        listCart.setShippingContactMechId(0, shoppingList.getString("contactMechId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get("shipmentMethodTypeId"))) {
                        listCart.setShipmentMethodTypeId(0, shoppingList.getString("shipmentMethodTypeId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get("carrierPartyId"))) {
                        listCart.setCarrierPartyId(0, shoppingList.getString("carrierPartyId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.getString("productPromoCodeId"))) {
                        listCart.addProductPromoCode(shoppingList.getString("productPromoCodeId"), dispatcher);
                    }
                }
            }
        }
        return listCart;
    }

    public static ShoppingCart makeShoppingListCart(LocalDispatcher dispatcher, String shoppingListId, Locale locale) {
        Delegator delegator = dispatcher.getDelegator();
        GenericValue shoppingList = null;
        try {
            shoppingList = delegator.findByPrimaryKey("ShoppingList", UtilMisc.toMap("shoppingListId", shoppingListId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }
        return makeShoppingListCart(dispatcher, shoppingList, locale);
    }

    /**
     *
     * Given an orderId, this service will look through all its OrderItems and for each shoppingListItemId
     * and shoppingListItemSeqId, update the quantity purchased in the ShoppingListItem entity.  Used for
     * tracking how many of shopping list items are purchased.  This service is mounted as a seca on storeOrder.
     *
     * @param ctx - The DispatchContext that this service is operating in
     * @param context - Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateShoppingListQuantitiesFromOrder(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = FastMap.newInstance();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        try {
            List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId));
            for(GenericValue orderItem : orderItems) {
                String shoppingListId = orderItem.getString("shoppingListId");
                String shoppingListItemSeqId = orderItem.getString("shoppingListItemSeqId");
                if (UtilValidate.isNotEmpty(shoppingListId)) {
                    GenericValue shoppingListItem=delegator.findByPrimaryKey("ShoppingListItem", UtilMisc.toMap("shoppingListId",
                                shoppingListId, "shoppingListItemSeqId", shoppingListItemSeqId));
                    if (shoppingListItem != null) {
                        BigDecimal quantityPurchased = shoppingListItem.getBigDecimal("quantityPurchased");
                        BigDecimal orderQuantity = orderItem.getBigDecimal("quantity");
                        if (quantityPurchased != null) {
                            shoppingListItem.set("quantityPurchased", orderQuantity.add(quantityPurchased));
                        } else {
                            shoppingListItem.set("quantityPurchased", orderQuantity);
                        }
                        shoppingListItem.store();
                    }
                }
            }
        } catch (Exception e) {
            Debug.logInfo("updateShoppingListQuantitiesFromOrder error:"+e.getMessage(), module);
        }
        return result;
    }

    public static Map<String,Object> autoDeleteAutoSaveShoppingList(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        List<GenericValue> shoppingList = null;
        try {
            EntityCondition cond = EntityCondition.makeCondition(UtilMisc.toList(
                                        EntityCondition.makeCondition("partyId", null),
                                        EntityCondition.makeCondition("shoppingListTypeId", "SLT_SPEC_PURP")), EntityOperator.AND);
            shoppingList = delegator.findList("ShoppingList", cond, null, null, null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), module);
        }
        String maxDaysStr = UtilProperties.getPropertyValue("order.properties", "autosave.max.age", "30");
        int maxDays = 0;
        try {
            maxDays = Integer.parseInt(maxDaysStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to get maxDays", module);
        }
        for (GenericValue sl : shoppingList) {
            if (maxDays > 0) {
                Timestamp lastModified = sl.getTimestamp("lastAdminModified");
                if (lastModified == null) {
                    lastModified = sl.getTimestamp("lastUpdatedStamp");
                }
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(lastModified.getTime());
                cal.add(Calendar.DAY_OF_YEAR, maxDays);
                Date expireDate = cal.getTime();
                Date nowDate = new Date();
                if (expireDate.equals(nowDate) || nowDate.after(expireDate)) {
                    List<GenericValue> shoppingListItems = null;
                    try {
                        shoppingListItems = sl.getRelated("ShoppingListItem");
                    } catch (GenericEntityException e) {
                        Debug.logError(e.getMessage(), module);
                    }
                    for (GenericValue sli : shoppingListItems) {
                        try {
                            dispatcher.runSync("removeShoppingListItem", UtilMisc.toMap("shoppingListId", sl.getString("shoppingListId"),
                                    "shoppingListItemSeqId", sli.getString("shoppingListItemSeqId"),
                                    "userLogin", userLogin));
                        } catch (GenericServiceException e) {
                            Debug.logError(e.getMessage(), module);
                        }
                    }
                    try {
                        dispatcher.runSync("removeShoppingList", UtilMisc.toMap("shoppingListId", sl.getString("shoppingListId"), "userLogin", userLogin));
                    } catch (GenericServiceException e) {
                        Debug.logError(e.getMessage(), module);
                    }
                }
            }
        }
        return ServiceUtil.returnSuccess();
    }
}