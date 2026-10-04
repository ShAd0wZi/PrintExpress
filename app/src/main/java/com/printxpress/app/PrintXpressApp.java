package com.printxpress.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.dao.CategoryDao;
import com.printxpress.app.data.local.dao.ProductDao;
import com.printxpress.app.data.local.dao.PromotionDao;
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
import com.printxpress.app.di.AppContainer;
import com.printxpress.app.util.NotificationHelper;
import com.printxpress.app.util.SecurityHelper;
import com.printxpress.app.util.SessionManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PrintXpressApp extends Application {

    private static final String PREF_DB_STATE = "PrintXpressDbState";
    private static final String KEY_CATALOG_SEEDED = "catalog_v6_stuffed_launch";
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
        SessionManager.applySavedTheme(this);
        NotificationHelper.createNotificationChannels(this);
        seedDatabaseIfNeeded();
    }

    public static synchronized AppContainer getAppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext instanceof PrintXpressApp) {
            PrintXpressApp app = (PrintXpressApp) appContext;
            if (app.appContainer == null) {
                app.appContainer = new AppContainer(app);
            }
            return app.appContainer;
        }
        return new AppContainer(appContext);
    }

    private void ensureAdminAccounts(PrintXpressDatabase db) {
        String[][] admins = new String[][]{
            {"PrintXpress Press Admin", "admin@printxpress.lk", "0112345000", "Admin123!"},
            {"Dilshan Silva (Operations Lead)", "ops.admin@printxpress.lk", "0112345001", "Admin123!"},
            {"Nadeeka Jayasuriya (Prepress Lead)", "prepress.admin@printxpress.lk", "0112345002", "Admin123!"},
            {"Kamal Wickramasinghe (Billing Admin)", "billing.admin@printxpress.lk", "0112345003", "Admin123!"},
            {"Rohan Perera (Logistics Lead)", "dispatch.admin@printxpress.lk", "0112345004", "Admin123!"}
        };

        for (String[] adminInfo : admins) {
            String name = adminInfo[0];
            String email = adminInfo[1];
            String phone = adminInfo[2];
            String password = adminInfo[3];

            User existing = db.userDao().getUserByEmail(email);
            if (existing == null) {
                User user = new User();
                user.setName(name);
                user.setEmail(email);
                user.setPhone(phone);
                user.setPasswordHash(SecurityHelper.hashPassword(password));
                user.setRegistrationDate(new Date(System.currentTimeMillis() - 60L * 86400000L));
                user.setUserType("ADMIN");
                db.userDao().insert(user);
            }
        }
    }

    private List<Integer> ensureCustomerAccounts(PrintXpressDatabase db) {
        String[][] customers = new String[][]{
            {"Kasun Perera (Apex Tech)", "customer@printxpress.lk", "0771234567", "Password123!"},
            {"Kasun Perera (Apex Tech)", "demo@printxpress.lk", "0771234567", "Password123!"},
            {"Dinesh Fernando (Ceylon Crafts)", "dinesh.fernando@ceyloncrafts.lk", "0772345678", "Password123!"},
            {"Anusha Senaratne (Lanka Retail)", "anusha.senaratne@lankaretail.lk", "0773456789", "Password123!"},
            {"Ravi Wickramasinghe (Colombo Tech)", "ravi.wickramasinghe@colombobiz.lk", "0774567890", "Password123!"},
            {"Nimali Jayawardena (BrandPulse Studio)", "nimali.jayawardena@brandpulse.lk", "0775678901", "Password123!"},
            {"Suren Silva (Island Media Network)", "suren.silva@islandmedia.lk", "0776789012", "Password123!"}
        };

        List<Integer> userIds = new ArrayList<>();
        for (String[] cust : customers) {
            String name = cust[0];
            String email = cust[1];
            String phone = cust[2];
            String password = cust[3];

            User existing = db.userDao().getUserByEmail(email);
            int uid;
            if (existing == null) {
                User user = new User();
                user.setName(name);
                user.setEmail(email);
                user.setPhone(phone);
                user.setPasswordHash(SecurityHelper.hashPassword(password));
                user.setRegistrationDate(new Date(System.currentTimeMillis() - 30L * 86400000L));
                user.setUserType("CUSTOMER");
                uid = (int) db.userDao().insert(user);
            } else {
                uid = existing.getUserId();
            }
            userIds.add(uid);
        }
        return userIds;
    }

    private void seedDatabaseIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(PREF_DB_STATE, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_CATALOG_SEEDED, false)) {
            // Guarantee Admin and Customer accounts exist
            PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    PrintXpressDatabase db = PrintXpressDatabase.getDatabase(this);
                    ensureAdminAccounts(db);
                    ensureCustomerAccounts(db);
                } catch (Exception ignored) {
                }
            });
            return;
        }

        PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
            try {
                PrintXpressDatabase db = PrintXpressDatabase.getDatabase(this);
                CategoryDao categoryDao = db.categoryDao();
                ProductDao productDao = db.productDao();
                PromotionDao promotionDao = db.promotionDao();

                String jsonStr = readAssetJson("initial_catalog.json");
                JSONObject root = new JSONObject(jsonStr);
                List<Integer> allProductIds = new ArrayList<>();

                db.runInTransaction(() -> {
                    try {
                        JSONArray categoriesArr = root.getJSONArray("categories");
                        for (int i = 0; i < categoriesArr.length(); i++) {
                            JSONObject catObj = categoriesArr.getJSONObject(i);
                            String catName = catObj.getString("name");
                            String catDesc = catObj.getString("description");
                            String catIcon = catObj.getString("imageUrl");

                            int categoryId = ensureCategory(categoryDao, catName, catDesc, catIcon);

                            JSONArray productsArr = catObj.getJSONArray("products");
                            for (int j = 0; j < productsArr.length(); j++) {
                                JSONObject prodObj = productsArr.getJSONObject(j);
                                int prodId = ensureProduct(
                                        productDao,
                                        categoryId,
                                        prodObj.getString("name"),
                                        prodObj.getString("description"),
                                        prodObj.getDouble("basePrice"),
                                        prodObj.getString("imageUrl"),
                                        prodObj.getString("availableSizes"),
                                        prodObj.getString("availableMaterials")
                                );
                                allProductIds.add(prodId);
                            }
                        }

                        Date thirtyDaysFromNow = new Date(System.currentTimeMillis() + 30L * 86400000L);
                        JSONArray promosArr = root.getJSONArray("promotions");
                        for (int i = 0; i < promosArr.length(); i++) {
                            JSONObject promoObj = promosArr.getJSONObject(i);
                            ensurePromotion(
                                    promotionDao,
                                    promoObj.getString("code"),
                                    promoObj.getString("title"),
                                    promoObj.getString("description"),
                                    promoObj.getDouble("discountPercentage"),
                                    promoObj.getDouble("minOrderAmount"),
                                    thirtyDaysFromNow
                            );
                        }

                        ensureAdminAccounts(db);
                        List<Integer> customerIds = ensureCustomerAccounts(db);

                        if (!allProductIds.isEmpty() && !customerIds.isEmpty()) {
                            // Seed comprehensive data for primary customer and demo customer
                            seedCustomerData(db, customerIds.get(0), allProductIds);
                            if (customerIds.size() > 1) {
                                seedCustomerData(db, customerIds.get(1), allProductIds);
                            }
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed seeding catalog transaction", ex);
                    }
                });

                prefs.edit().putBoolean(KEY_CATALOG_SEEDED, true).apply();
            } catch (Exception e) {
                android.util.Log.e("PrintXpressApp", "Failed to seed initial catalog", e);
            }
        });
    }

    private String readAssetJson(String fileName) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private int ensureCategory(CategoryDao dao, String name, String description, String imageUrl) {
        Category existing = dao.getCategoryByName(name);
        if (existing != null) {
            existing.setDescription(description);
            existing.setImageUrl(imageUrl);
            dao.update(existing);
            return existing.getCategoryId();
        }
        Category cat = new Category();
        cat.setName(name);
        cat.setDescription(description);
        cat.setImageUrl(imageUrl);
        return (int) dao.insert(cat);
    }

    private int ensureProduct(ProductDao dao, int categoryId, String name, String description,
                              double basePrice, String imageUrl, String sizes, String materials) {
        int defaultStock = name != null && (name.toLowerCase().contains("foil") || name.toLowerCase().contains("banner")) ? 12 : 120;
        Product existing = dao.getProductByName(name);
        if (existing != null) {
            existing.setCategoryId(categoryId);
            existing.setDescription(description);
            existing.setBasePrice(basePrice);
            existing.setImageUrl(imageUrl);
            existing.setAvailableSizes(sizes);
            existing.setAvailableMaterials(materials);
            if (existing.getStockQuantity() <= 0) {
                existing.setStockQuantity(defaultStock);
            }
            dao.update(existing);
            return existing.getProductId();
        }
        Product product = new Product();
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setDescription(description);
        product.setBasePrice(basePrice);
        product.setImageUrl(imageUrl);
        product.setAvailableSizes(sizes);
        product.setAvailableMaterials(materials);
        product.setStockQuantity(defaultStock);
        return (int) dao.insert(product);
    }

    private void ensurePromotion(PromotionDao dao, String code, String title, String description,
                                 double discountPct, double minOrder, Date validUntil) {
        Promotion existing = dao.getPromotionByCode(code);
        if (existing == null) {
            Promotion promo = new Promotion(code, title, description, discountPct, minOrder, validUntil, true);
            dao.insert(promo);
        }
    }

    private void seedCustomerData(PrintXpressDatabase db, int userId, List<Integer> productIds) {
        // 1. Seed 5 Saved Delivery Addresses
        List<Address> existingAddrs = db.addressDao().getAddressesByUserIdSync(userId);
        int defaultAddrId = -1;
        if (existingAddrs == null || existingAddrs.size() < 5) {
            String[][] addrDefs = new String[][]{
                {"No. 42, Galle Road, Level 3 Apex Tower", "Kollupitiya", "Colombo 03", "00300", "0771234567", "1"},
                {"Block B, Orion City IT Park, 752 Baseline Road", "Dematagoda", "Colombo 09", "00900", "0112345678", "0"},
                {"No. 18/4, Alfred Place, Duplication Road", "Bambalapitiya", "Colombo 04", "00400", "0771234567", "0"},
                {"Level 5, West Tower, World Trade Center", "Echelon Square", "Colombo 01", "00100", "0112445566", "0"},
                {"No. 120, Kandy Road, Bulugaha Junction", "Kelaniya Industrial Zone", "Kelaniya", "11600", "0112911222", "0"}
            };
            for (String[] def : addrDefs) {
                Address a = new Address();
                a.setUserId(userId);
                a.setAddressLine1(def[0]);
                a.setAddressLine2(def[1]);
                a.setCity(def[2]);
                a.setPostalCode(def[3]);
                a.setPhone(def[4]);
                a.setDefault("1".equals(def[5]));
                int aid = (int) db.addressDao().insert(a);
                if ("1".equals(def[5])) {
                    defaultAddrId = aid;
                }
            }
        } else {
            defaultAddrId = existingAddrs.get(0).getAddressId();
        }

        // 2. Seed 6 Customer Orders spanning various production statuses
        List<Order> existingOrders = db.orderDao().getOrdersByCustomerIdSync(userId);
        if (existingOrders == null || existingOrders.size() < 5) {
            int prodBizCard = productIds.size() > 0 ? productIds.get(0) : 1;
            int prodSpotUv = productIds.size() > 1 ? productIds.get(1) : prodBizCard;
            int prodLetterhead = productIds.size() > 2 ? productIds.get(2) : prodBizCard;
            int prodFlyer = productIds.size() > 6 ? productIds.get(6) : prodBizCard;
            int prodBanner = productIds.size() > 12 ? productIds.get(12) : prodBizCard;
            int prodTshirt = productIds.size() > 18 ? productIds.get(18) : prodBizCard;
            int prodMug = productIds.size() > 24 ? productIds.get(24) : prodBizCard;
            int prodSticker = productIds.size() > 30 ? productIds.get(30) : prodBizCard;
            int prodCatalog = productIds.size() > 36 ? productIds.get(36) : prodBizCard;
            int prodIdCard = productIds.size() > 42 ? productIds.get(42) : prodBizCard;
            int prodLanyard = productIds.size() > 43 ? productIds.get(43) : prodBizCard;

            // Order 1: PRINTING
            Order o1 = new Order();
            o1.setCustomerId(userId);
            o1.setOrderDate(new Date(System.currentTimeMillis() - 86400000L));
            o1.setDeliveryMethod("Home Delivery");
            o1.setDeliveryAddressId(defaultAddrId > 0 ? defaultAddrId : null);
            o1.setPaymentMethod("Credit / Debit Card");
            o1.setPaymentStatus("PAID");
            o1.setOrderStatus("PRINTING");
            o1.setTotalAmount(11840.00);
            int o1Id = (int) db.orderDao().insert(o1);

            OrderItem i1_1 = new OrderItem();
            i1_1.setOrderId(o1Id);
            i1_1.setProductId(prodBizCard);
            i1_1.setQuantity(2);
            i1_1.setUnitPrice(2750.00);
            i1_1.setSubtotal(5500.00);
            i1_1.setCustomisationDetails("Size: 90x54mm Standard (100 pcs) | Material: 400gsm Velvet Soft-Touch | Text: Kasun Perera - Managing Director");
            db.orderItemDao().insert(i1_1);

            OrderItem i1_2 = new OrderItem();
            i1_2.setOrderId(o1Id);
            i1_2.setProductId(prodFlyer);
            i1_2.setQuantity(3);
            i1_2.setUnitPrice(1650.00);
            i1_2.setSubtotal(4950.00);
            i1_2.setCustomisationDetails("Size: A5 Double-Sided (100 pcs) | Material: 150gsm Silk Matte | Text: Colombo Tech Expo 2026 Handout");
            db.orderItemDao().insert(i1_2);

            // Order 2: READY
            Order o2 = new Order();
            o2.setCustomerId(userId);
            o2.setOrderDate(new Date(System.currentTimeMillis() - 3L * 86400000L));
            o2.setDeliveryMethod("Store Pickup");
            o2.setDeliveryAddressId(null);
            o2.setPaymentMethod("Cash on Delivery");
            o2.setPaymentStatus("PENDING");
            o2.setOrderStatus("READY");
            o2.setTotalAmount(8910.00);
            int o2Id = (int) db.orderDao().insert(o2);

            OrderItem i2_1 = new OrderItem();
            i2_1.setOrderId(o2Id);
            i2_1.setProductId(prodBanner);
            i2_1.setQuantity(1);
            i2_1.setUnitPrice(8250.00);
            i2_1.setSubtotal(8250.00);
            i2_1.setCustomisationDetails("Size: Standard (2.5x6.5 ft) | Material: 440gsm Matte Blockout Vinyl | Text: BMICH Booth #B14 Standee");
            db.orderItemDao().insert(i2_1);

            // Order 3: PROCESSING
            Order o3 = new Order();
            o3.setCustomerId(userId);
            o3.setOrderDate(new Date(System.currentTimeMillis() - 5L * 86400000L));
            o3.setDeliveryMethod("Home Delivery");
            o3.setDeliveryAddressId(defaultAddrId > 0 ? defaultAddrId : null);
            o3.setPaymentMethod("Credit / Debit Card");
            o3.setPaymentStatus("PAID");
            o3.setOrderStatus("PROCESSING");
            o3.setTotalAmount(14400.00);
            int o3Id = (int) db.orderDao().insert(o3);

            OrderItem i3_1 = new OrderItem();
            i3_1.setOrderId(o3Id);
            i3_1.setProductId(prodTshirt);
            i3_1.setQuantity(3);
            i3_1.setUnitPrice(3200.00);
            i3_1.setSubtotal(9600.00);
            i3_1.setCustomisationDetails("Size: L | Material: 190gsm 100% Combed Cotton | Text: Apex Engineering Core Team");
            db.orderItemDao().insert(i3_1);

            OrderItem i3_2 = new OrderItem();
            i3_2.setOrderId(o3Id);
            i3_2.setProductId(prodSticker);
            i3_2.setQuantity(3);
            i3_2.setUnitPrice(1600.00);
            i3_2.setSubtotal(4800.00);
            i3_2.setCustomisationDetails("Size: 3x3 inch (50 pcs) | Material: White Waterproof PVC Vinyl | Text: Apex High-Tech Logo Pack");
            db.orderItemDao().insert(i3_2);

            // Order 4: COMPLETED
            Order o4 = new Order();
            o4.setCustomerId(userId);
            o4.setOrderDate(new Date(System.currentTimeMillis() - 9L * 86400000L));
            o4.setDeliveryMethod("Home Delivery");
            o4.setDeliveryAddressId(defaultAddrId > 0 ? defaultAddrId : null);
            o4.setPaymentMethod("Credit / Debit Card");
            o4.setPaymentStatus("PAID");
            o4.setOrderStatus("COMPLETED");
            o4.setTotalAmount(23800.00);
            int o4Id = (int) db.orderDao().insert(o4);

            OrderItem i4_1 = new OrderItem();
            i4_1.setOrderId(o4Id);
            i4_1.setProductId(prodCatalog);
            i4_1.setQuantity(2);
            i4_1.setUnitPrice(6200.00);
            i4_1.setSubtotal(12400.00);
            i4_1.setCustomisationDetails("Size: A4 Portrait (16 Pages - 25 Copies) | Material: 300gsm Matte Laminated Cover | Text: Annual Operations Profile 2026");
            db.orderItemDao().insert(i4_1);

            OrderItem i4_2 = new OrderItem();
            i4_2.setOrderId(o4Id);
            i4_2.setProductId(prodLetterhead);
            i4_2.setQuantity(3);
            i4_2.setUnitPrice(3800.00);
            i4_2.setSubtotal(11400.00);
            i4_2.setCustomisationDetails("Size: A4 (100 Sheets) | Material: 120gsm Conqueror Wove | Text: Official Apex Legal Letterhead");
            db.orderItemDao().insert(i4_2);

            // Order 5: PENDING
            Order o5 = new Order();
            o5.setCustomerId(userId);
            o5.setOrderDate(new Date(System.currentTimeMillis() - 12L * 86400000L));
            o5.setDeliveryMethod("Store Pickup");
            o5.setDeliveryAddressId(null);
            o5.setPaymentMethod("Cash on Delivery");
            o5.setPaymentStatus("PENDING");
            o5.setOrderStatus("PENDING");
            o5.setTotalAmount(6000.00);
            int o5Id = (int) db.orderDao().insert(o5);

            OrderItem i5_1 = new OrderItem();
            i5_1.setOrderId(o5Id);
            i5_1.setProductId(prodMug);
            i5_1.setQuantity(5);
            i5_1.setUnitPrice(1200.00);
            i5_1.setSubtotal(6000.00);
            i5_1.setCustomisationDetails("Size: 11 oz Standard (325ml) | Material: AAA-Grade Glossy White Ceramic | Text: Apex Team Morning Brew");
            db.orderItemDao().insert(i5_1);

            // Order 6: COMPLETED
            Order o6 = new Order();
            o6.setCustomerId(userId);
            o6.setOrderDate(new Date(System.currentTimeMillis() - 16L * 86400000L));
            o6.setDeliveryMethod("Home Delivery");
            o6.setDeliveryAddressId(defaultAddrId > 0 ? defaultAddrId : null);
            o6.setPaymentMethod("Credit / Debit Card");
            o6.setPaymentStatus("PAID");
            o6.setOrderStatus("COMPLETED");
            o6.setTotalAmount(18200.00);
            int o6Id = (int) db.orderDao().insert(o6);

            OrderItem i6_1 = new OrderItem();
            i6_1.setOrderId(o6Id);
            i6_1.setProductId(prodIdCard);
            i6_1.setQuantity(4);
            i6_1.setUnitPrice(950.00);
            i6_1.setSubtotal(3800.00);
            i6_1.setCustomisationDetails("Size: CR80 Credit Card Size | Material: 760 Micron Glossy Thermal PVC | Text: Executive Staff Access Passes");
            db.orderItemDao().insert(i6_1);

            OrderItem i6_2 = new OrderItem();
            i6_2.setOrderId(o6Id);
            i6_2.setProductId(prodLanyard);
            i6_2.setQuantity(6);
            i6_2.setUnitPrice(2400.00);
            i6_2.setSubtotal(14400.00);
            i6_2.setCustomisationDetails("Size: 20mm Standard Width | Material: Double-Sided Polyester Satin | Text: Apex Tech Corporate Lanyards");
            db.orderItemDao().insert(i6_2);
        }

        // 3. Seed 6 Customer Notifications
        if (db.notificationDao().getUnreadCount(userId) < 3) {
            String[][] notifDefs = new String[][]{
                {"Order is now Printing 🖨️", "Your 400gsm Velvet Business Cards and A5 Silk Flyers are running on our Heidelberg press.", "ORDER", "0"},
                {"Order Ready for Pickup ✅", "Your Roll-up Pull Banner is packed and waiting at our Colombo 03 Production Hub.", "ORDER", "0"},
                {"Artwork Preflight Approved 📄", "Vector curves and 300 DPI high-res artwork checks passed successfully.", "ORDER", "0"},
                {"Package Delivered Successfully 🚚", "Your Annual Operations Catalogs were signed for at Level 3, Apex Tower.", "ORDER", "1"},
                {"Exclusive Promo: 20% OFF Bulk Cards 🎉", "Upgrade your team's visiting cards with spot UV or gold foil. Use promo code PXBIZ20 at checkout.", "PROMOTION", "0"},
                {"Packaging Deal: 25% OFF Boxes & Mailers 📦", "Take 25% off custom e-commerce mailer boxes and roll labels with code PACK25.", "PROMOTION", "1"}
            };
            long now = System.currentTimeMillis();
            for (int i = 0; i < notifDefs.length; i++) {
                String[] def = notifDefs[i];
                Notification n = new Notification();
                n.setUserId(userId);
                n.setTitle(def[0]);
                n.setMessage(def[1]);
                n.setType(def[2]);
                n.setRead("1".equals(def[3]));
                n.setTimestamp(new Date(now - (i + 1) * 3600000L * 4));
                db.notificationDao().insert(n);
            }
        }

        // 4. Seed 5 Cart Items
        List<CartItemWithProduct> existingCart = db.cartItemDao().getCartItemsWithProductSync(userId);
        if (existingCart == null || existingCart.size() < 5) {
            int prodSpotUv = productIds.size() > 1 ? productIds.get(1) : 1;
            int prodMug = productIds.size() > 24 ? productIds.get(24) : 1;
            int prodSticker = productIds.size() > 30 ? productIds.get(30) : 1;
            int prodPolo = productIds.size() > 19 ? productIds.get(19) : 1;
            int prodCatalog = productIds.size() > 36 ? productIds.get(36) : 1;

            CartItem c1 = new CartItem();
            c1.setUserId(userId);
            c1.setProductId(prodSpotUv);
            c1.setQuantity(2);
            c1.setSelectedSize("90x54mm Standard (100 pcs)");
            c1.setSelectedMaterial("400gsm Velvet Matte + Gold Foil");
            c1.setCustomText("Apex Tech - Executive Suite");
            c1.setUnitPrice(4800.00);
            c1.setSubtotal(9600.00);
            db.cartItemDao().insert(c1);

            CartItem c2 = new CartItem();
            c2.setUserId(userId);
            c2.setProductId(prodMug);
            c2.setQuantity(5);
            c2.setSelectedSize("11 oz Standard (325ml)");
            c2.setSelectedMaterial("AAA-Grade Glossy White Ceramic");
            c2.setCustomText("Apex Morning Brew");
            c2.setUnitPrice(1200.00);
            c2.setSubtotal(6000.00);
            db.cartItemDao().insert(c2);

            CartItem c3 = new CartItem();
            c3.setUserId(userId);
            c3.setProductId(prodSticker);
            c3.setQuantity(3);
            c3.setSelectedSize("3x3 inch (50 pcs)");
            c3.setSelectedMaterial("White Waterproof PVC Vinyl + Matte Lamination");
            c3.setCustomText("Laptop Stickers Pack");
            c3.setUnitPrice(1600.00);
            c3.setSubtotal(4800.00);
            db.cartItemDao().insert(c3);

            CartItem c4 = new CartItem();
            c4.setUserId(userId);
            c4.setProductId(prodPolo);
            c4.setQuantity(4);
            c4.setSelectedSize("L");
            c4.setSelectedMaterial("220gsm Honeycomb Pique Cotton");
            c4.setCustomText("Apex Staff Polo");
            c4.setUnitPrice(3950.00);
            c4.setSubtotal(15800.00);
            db.cartItemDao().insert(c4);

            CartItem c5 = new CartItem();
            c5.setUserId(userId);
            c5.setProductId(prodCatalog);
            c5.setQuantity(1);
            c5.setSelectedSize("A4 Portrait (16 Pages - 25 Copies)");
            c5.setSelectedMaterial("300gsm Matte Laminated Cover + 150gsm Silk Inner");
            c5.setCustomText("Q4 Print Portfolio");
            c5.setUnitPrice(6200.00);
            c5.setSubtotal(6200.00);
            db.cartItemDao().insert(c5);
        }
    }
}