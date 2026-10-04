package com.printxpress.app.data;

import com.printxpress.app.data.local.entity.Address;
import com.printxpress.app.data.local.entity.CartItem;
import com.printxpress.app.data.local.entity.Category;
import com.printxpress.app.data.local.entity.Notification;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.data.local.entity.OrderItem;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.data.local.entity.Promotion;
import com.printxpress.app.data.local.entity.User;
import com.printxpress.app.data.local.model.CartItemWithProduct;
import com.printxpress.app.util.Constants;
import org.junit.Test;
import java.util.Date;
import static org.junit.Assert.*;

public class EntityAndRepositoryLogicTest {

    @Test
    public void testUserEntityFields() {
        User user = new User();
        Date now = new Date();
        user.setUserId(42);
        user.setName("Kasun Perera");
        user.setEmail("kasun@printxpress.lk");
        user.setPhone("0771234567");
        user.setPasswordHash("salt:hash");
        user.setRegistrationDate(now);
        user.setUserType(Constants.USER_TYPE_CUSTOMER);

        assertEquals(42, user.getUserId());
        assertEquals("Kasun Perera", user.getName());
        assertEquals("kasun@printxpress.lk", user.getEmail());
        assertEquals("0771234567", user.getPhone());
        assertEquals("salt:hash", user.getPasswordHash());
        assertEquals(now, user.getRegistrationDate());
        assertEquals("CUSTOMER", user.getUserType());
    }

    @Test
    public void testCategoryAndProductEntities() {
        Category category = new Category();
        category.setCategoryId(1);
        category.setName("Business Cards");
        category.setDescription("Offset & Digital Cards");
        category.setImageUrl("ic_cat_business_cards");

        Product product = new Product();
        product.setProductId(10);
        product.setCategoryId(category.getCategoryId());
        product.setName("400gsm Velvet Card");
        product.setDescription("Soft-touch laminated business card");
        product.setBasePrice(2750.0);
        product.setAvailableSizes("Standard, Square");
        product.setAvailableMaterials("400gsm Velvet, 350gsm Matte");
        product.setImageUrl("premium_card");

        assertEquals(1, product.getCategoryId());
        assertEquals("400gsm Velvet Card", product.getName());
        assertEquals(2750.0, product.getBasePrice(), 0.001);
        assertEquals("Standard, Square", product.getAvailableSizes());
    }

    @Test
    public void testCartItemAndCartItemWithProductModel() {
        Product product = new Product();
        product.setProductId(5);
        product.setName("Roll-up Banner");
        product.setBasePrice(8250.0);

        CartItem item = new CartItem();
        item.setCartItemId(99);
        item.setUserId(1);
        item.setProductId(5);
        item.setQuantity(2);
        item.setSelectedSize("Standard");
        item.setSelectedMaterial("440gsm Matte Vinyl");
        item.setCustomText("Expo Booth #14");
        item.setArtworkFilePath("/data/user/0/com.printxpress.app/files/artwork_1.pdf");
        item.setUnitPrice(8250.0);
        item.setSubtotal(16500.0);

        CartItemWithProduct combined = new CartItemWithProduct();
        combined.cartItem = item;
        combined.product = product;

        assertNotNull(combined.cartItem);
        assertNotNull(combined.product);
        assertEquals(16500.0, combined.cartItem.getSubtotal(), 0.001);
        assertEquals("Roll-up Banner", combined.product.getName());
    }

    @Test
    public void testOrderAndOrderItemEntities() {
        Order order = new Order();
        Date orderDate = new Date();
        order.setOrderId(101);
        order.setCustomerId(1);
        order.setOrderDate(orderDate);
        order.setDeliveryMethod(Constants.DELIVERY_HOME);
        order.setDeliveryAddressId(7);
        order.setPaymentMethod("Credit / Debit Card");
        order.setPaymentStatus(Constants.PAYMENT_PAID);
        order.setOrderStatus(Constants.STATUS_PRINTING);
        order.setTotalAmount(12450.0);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(501);
        orderItem.setOrderId(order.getOrderId());
        orderItem.setProductId(10);
        orderItem.setQuantity(3);
        orderItem.setUnitPrice(4150.0);
        orderItem.setSubtotal(12450.0);
        orderItem.setCustomisationDetails("Size: A5 | Material: Silk Matte | Text: Promo");

        assertEquals(101, orderItem.getOrderId());
        assertEquals(Constants.STATUS_PRINTING, order.getOrderStatus());
        assertEquals(Constants.PAYMENT_PAID, order.getPaymentStatus());
        assertEquals(Integer.valueOf(7), order.getDeliveryAddressId());
        assertEquals(12450.0, orderItem.getSubtotal(), 0.001);
    }

    @Test
    public void testAddressPromotionAndNotificationEntities() {
        Address address = new Address();
        address.setAddressId(3);
        address.setUserId(1);
        address.setAddressLine1("No. 42, Galle Road");
        address.setAddressLine2("Kollupitiya");
        address.setCity("Colombo 03");
        address.setPostalCode("00300");
        address.setPhone("0771234567");
        address.setDefault(true);

        assertTrue(address.isDefault());
        assertEquals("Colombo 03", address.getCity());

        Date expiry = new Date(System.currentTimeMillis() + 86400000L);
        Promotion promo = new Promotion("SAVE20", "20% Off", "Bulk discount", 20.0, 2500.0, expiry, true);
        assertEquals("SAVE20", promo.getCode());
        assertEquals(20.0, promo.getDiscountPercentage(), 0.001);
        assertTrue(promo.isActive());

        Notification notif = new Notification();
        notif.setNotificationId(8);
        notif.setUserId(1);
        notif.setTitle("Order Dispatched");
        notif.setMessage("Your order is on the way.");
        notif.setRead(false);
        notif.setType("ORDER");
        assertFalse(notif.isRead());
        assertEquals("ORDER", notif.getType());
    }
}
