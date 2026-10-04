package com.printxpress.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;
import com.printxpress.app.data.local.entity.User;
import com.printxpress.app.ui.auth.LoginActivity;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "PrintXpressSessionKey";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    public static final int NO_USER = -1;

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SecretKey getOrCreateSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            if (entry != null) {
                return entry.getSecretKey();
            }
        }
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return keyGenerator.generateKey();
    }

    private static String encryptValue(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey());
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.w(TAG, "Keystore encryption fallback", e);
            return plainText;
        }
    }

    private static String decryptValue(String storedValue, String fallback) {
        if (storedValue == null || storedValue.isEmpty()) return fallback;
        if (!storedValue.contains(":")) return storedValue;
        try {
            String[] parts = storedValue.split(":", 2);
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] cipherText = Base64.decode(parts[1], Base64.NO_WRAP);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec);
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return storedValue;
        }
    }

    public static final long SESSION_TTL_MS = 7L * 24 * 60 * 60 * 1000L; // 7 days
    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    public static final long LOGIN_LOCKOUT_DURATION_MS = 30_000L; // 30 seconds cooldown

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = getPrefs(context);
        boolean loggedIn = prefs.getBoolean(Constants.KEY_IS_LOGGED_IN, false);
        if (!loggedIn) {
            return false;
        }
        long sessionTimestamp = prefs.getLong(Constants.KEY_SESSION_TIMESTAMP, 0L);
        if (sessionTimestamp > 0L && (System.currentTimeMillis() - sessionTimestamp) > SESSION_TTL_MS) {
            clearSession(context);
            return false;
        }
        return true;
    }

    public static boolean hasLoggedInBefore(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.contains(Constants.KEY_BIOMETRIC_USER_ID) || prefs.contains(Constants.KEY_USER_ID);
    }

    public static int getBiometricUserId(Context context) {
        SharedPreferences prefs = getPrefs(context);
        int bioId = prefs.getInt(Constants.KEY_BIOMETRIC_USER_ID, NO_USER);
        return bioId != NO_USER ? bioId : prefs.getInt(Constants.KEY_USER_ID, NO_USER);
    }

    public static int getUserId(Context context) {
        if (!isLoggedIn(context)) {
            return NO_USER;
        }
        return getPrefs(context).getInt(Constants.KEY_USER_ID, NO_USER);
    }

    public static String getUserName(Context context) {
        if (!isLoggedIn(context)) {
            return "Guest User";
        }
        String raw = getPrefs(context).getString(Constants.KEY_USER_NAME, "");
        return raw.isEmpty() ? "Customer" : decryptValue(raw, "Customer");
    }

    public static String getUserEmail(Context context) {
        if (!isLoggedIn(context)) {
            return "";
        }
        String raw = getPrefs(context).getString(Constants.KEY_USER_EMAIL, "");
        return decryptValue(raw, "");
    }

    public static String getUserType(Context context) {
        if (!isLoggedIn(context)) {
            return Constants.USER_TYPE_CUSTOMER;
        }
        return getPrefs(context).getString(Constants.KEY_USER_TYPE, Constants.USER_TYPE_CUSTOMER);
    }

    public static boolean isAdmin(Context context) {
        return Constants.USER_TYPE_ADMIN.equalsIgnoreCase(getUserType(context))
                || "admin@printxpress.lk".equalsIgnoreCase(getUserEmail(context));
    }

    public static void saveUserSession(Context context, User user) {
        if (user == null) return;
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.putInt(Constants.KEY_USER_ID, user.getUserId());
        editor.putInt(Constants.KEY_BIOMETRIC_USER_ID, user.getUserId());
        editor.putString(Constants.KEY_USER_NAME, encryptValue(user.getName()));
        editor.putString(Constants.KEY_USER_EMAIL, encryptValue(user.getEmail()));
        editor.putString(Constants.KEY_USER_TYPE, user.getUserType() != null ? user.getUserType() : Constants.USER_TYPE_CUSTOMER);
        editor.putLong(Constants.KEY_SESSION_TIMESTAMP, System.currentTimeMillis());
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true);
        editor.remove(Constants.KEY_FAILED_LOGIN_COUNT);
        editor.remove(Constants.KEY_LOGIN_LOCKOUT_UNTIL);
        editor.apply();
    }

    public static void clearSession(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.remove(Constants.KEY_USER_ID);
        editor.remove(Constants.KEY_USER_NAME);
        editor.remove(Constants.KEY_USER_EMAIL);
        editor.remove(Constants.KEY_USER_TYPE);
        editor.remove(Constants.KEY_SESSION_TIMESTAMP);
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, false);
        editor.apply();
    }

    // Login Rate Limiting & Cooldown
    public static int recordFailedLoginAttempt(Context context) {
        SharedPreferences prefs = getPrefs(context);
        int count = prefs.getInt(Constants.KEY_FAILED_LOGIN_COUNT, 0) + 1;
        SharedPreferences.Editor editor = prefs.edit().putInt(Constants.KEY_FAILED_LOGIN_COUNT, count);
        if (count >= MAX_FAILED_LOGIN_ATTEMPTS) {
            editor.putLong(Constants.KEY_LOGIN_LOCKOUT_UNTIL, System.currentTimeMillis() + LOGIN_LOCKOUT_DURATION_MS);
        }
        editor.apply();
        return count;
    }

    public static void resetFailedLoginAttempts(Context context) {
        getPrefs(context).edit()
                .remove(Constants.KEY_FAILED_LOGIN_COUNT)
                .remove(Constants.KEY_LOGIN_LOCKOUT_UNTIL)
                .apply();
    }

    public static long getRemainingLockoutSeconds(Context context) {
        SharedPreferences prefs = getPrefs(context);
        long lockoutUntil = prefs.getLong(Constants.KEY_LOGIN_LOCKOUT_UNTIL, 0L);
        long remainingMs = lockoutUntil - System.currentTimeMillis();
        if (remainingMs <= 0L) {
            if (lockoutUntil > 0L) {
                resetFailedLoginAttempts(context);
            }
            return 0L;
        }
        return (remainingMs + 999L) / 1000L;
    }

    // Multi-Currency Support
    public static String getCurrency(Context context) {
        return getPrefs(context).getString(Constants.KEY_CURRENCY, Constants.CURRENCY_LKR);
    }

    public static void setCurrency(Context context, String currencyCode) {
        getPrefs(context).edit().putString(Constants.KEY_CURRENCY, currencyCode).apply();
    }

    public static String formatPrice(Context context, double amountInLkr) {
        return PricingCalculator.formatCurrency(amountInLkr, getCurrency(context));
    }

    public static String formatNumber(long number) {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(number);
    }

    // Multi-Language Localization Support
    public static String getLanguage(Context context) {
        return getPrefs(context).getString(Constants.KEY_LANGUAGE, Constants.LANG_EN);
    }

    public static void setLanguage(Context context, String langCode) {
        getPrefs(context).edit().putString(Constants.KEY_LANGUAGE, langCode).apply();
        AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(langCode));
    }

    public static int getThemeMode(Context context) {
        return getPrefs(context).getInt(Constants.KEY_THEME_MODE, Constants.THEME_SYSTEM);
    }

    public static void setThemeMode(Context context, int themeMode) {
        getPrefs(context).edit().putInt(Constants.KEY_THEME_MODE, themeMode).apply();
        applyTheme(themeMode);
    }

    public static void applySavedTheme(Context context) {
        applyTheme(getThemeMode(context));
    }

    public static void applyTheme(int themeMode) {
        if (themeMode == Constants.THEME_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (themeMode == Constants.THEME_DARK) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    /**
     * Checks if the user is authenticated. If not, alerts the user and redirects to LoginActivity.
     * @return true if user is logged in, false if redirected to login.
     */
    public static boolean requireLogin(Activity activity) {
        if (!isLoggedIn(activity)) {
            PrintXpressToast.info(activity, "Please log in to continue.");
            Intent intent = new Intent(activity, LoginActivity.class);
            activity.startActivity(intent);
            return false;
        }
        return true;
    }

    public static boolean isProductWishlisted(Context context, int productId) {
        return getPrefs(context).getBoolean("wishlist_prod_" + productId, false);
    }

    public static boolean toggleProductWishlist(Context context, int productId) {
        SharedPreferences prefs = getPrefs(context);
        boolean current = prefs.getBoolean("wishlist_prod_" + productId, false);
        boolean newState = !current;
        prefs.edit().putBoolean("wishlist_prod_" + productId, newState).apply();
        return newState;
    }
}
