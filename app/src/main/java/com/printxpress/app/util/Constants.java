package com.printxpress.app.util;

public final class Constants {

    private Constants() {
        // Prevent instantiation
    }
    
    public static final String PREF_NAME = "PrintXpressPrefs";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_BIOMETRIC_USER_ID = "biometricUserId";
    public static final String KEY_USER_NAME = "userName";
    public static final String KEY_USER_EMAIL = "userEmail";
    public static final String KEY_USER_TYPE = "userType";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    public static final String KEY_THEME_MODE = "themeMode";
    public static final String KEY_CURRENCY = "preferredCurrency";
    public static final String KEY_LANGUAGE = "preferredLanguage";
    public static final String KEY_SESSION_TIMESTAMP = "sessionTimestamp";
    public static final String KEY_FAILED_LOGIN_COUNT = "failedLoginCount";
    public static final String KEY_LOGIN_LOCKOUT_UNTIL = "loginLockoutUntil";

    public static final String CURRENCY_LKR = "LKR";
    public static final String CURRENCY_USD = "USD";
    public static final String CURRENCY_EUR = "EUR";
    public static final String CURRENCY_GBP = "GBP";

    public static final String LANG_EN = "en";
    public static final String LANG_SI = "si";
    public static final String LANG_TA = "ta";

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;
    
    public static final int PICK_FILE_REQUEST = 1001;
    public static final int PICK_IMAGE_REQUEST = 1002;

    // Order Status Constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_PRINTING = "PRINTING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // User Type & Payment Status Constants
    public static final String USER_TYPE_CUSTOMER = "CUSTOMER";
    public static final String USER_TYPE_ADMIN = "ADMIN";
    public static final String PAYMENT_PAID = "PAID";
    public static final String PAYMENT_PENDING = "PENDING";

    // Delivery Method Constants
    public static final String DELIVERY_HOME = "Home Delivery";
    public static final String DELIVERY_PICKUP = "Store Pickup";

    // Intent Extra Keys
    public static final String EXTRA_PRODUCT_ID = "PRODUCT_ID";
    public static final String EXTRA_PRODUCT_NAME = "PRODUCT_NAME";
    public static final String EXTRA_PRODUCT_PRICE = "PRODUCT_PRICE";
    public static final String EXTRA_PRODUCT_DESCRIPTION = "PRODUCT_DESCRIPTION";
    public static final String EXTRA_PRODUCT_IMAGE = "PRODUCT_IMAGE";
    public static final String EXTRA_AVAILABLE_SIZES = "AVAILABLE_SIZES";
    public static final String EXTRA_AVAILABLE_MATERIALS = "AVAILABLE_MATERIALS";
    public static final String EXTRA_CATEGORY_ID = "CATEGORY_ID";
    public static final String EXTRA_CATEGORY_NAME = "CATEGORY_NAME";
    public static final String EXTRA_SEARCH_QUERY = "SEARCH_QUERY";
    public static final String EXTRA_ORDER_ID = "ORDER_ID";
    public static final String EXTRA_ORDER_STATUS = "ORDER_STATUS";
    public static final String EXTRA_DELIVERY_METHOD = "DELIVERY_METHOD";
    public static final String EXTRA_DELIVERY_ADDRESS_ID = "DELIVERY_ADDRESS_ID";
    public static final String EXTRA_GRAND_TOTAL = "GRAND_TOTAL";
}