package com.printxpress.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.printxpress.app.R;
import java.lang.ref.WeakReference;

/**
 * High-end floating pill alert for PrintXpress. Replaces low-fidelity OS Toasts
 * with animated glassmorphic pill notifications.
 */
public final class PrintXpressToast {

    private static WeakReference<View> currentToastView = new WeakReference<>(null);

    private PrintXpressToast() {}

    public static void success(Context context, String message) {
        show(context, message, R.drawable.ic_check_circle, R.color.primary_container, Color.parseColor("#14532D"), null, null);
    }

    public static void info(Context context, String message) {
        show(context, message, R.drawable.ic_bell, R.color.info, Color.parseColor("#1E3A8A"), null, null);
    }

    public static void warning(Context context, String message) {
        show(context, message, R.drawable.ic_sparkle, R.color.warning, Color.parseColor("#78350F"), null, null);
    }

    public static void error(Context context, String message) {
        show(context, message, R.drawable.ic_shield_lock, R.color.error, Color.parseColor("#7F1D1D"), null, null);
    }

    public static void action(Context context, String message, String actionText, View.OnClickListener actionListener) {
        show(context, message, R.drawable.ic_check_circle, R.color.primary_container, Color.parseColor("#14532D"), actionText, actionListener);
    }

    public static void show(Context context, String message, int iconRes, int tintRes, int bgBadgeColor, String actionText, View.OnClickListener actionListener) {
        if (context == null || message == null || message.trim().isEmpty()) return;

        Activity activity = getActivity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            showFallbackCustomToast(context, message, iconRes, tintRes);
            return;
        }

        activity.runOnUiThread(() -> {
            ViewGroup root = activity.findViewById(android.R.id.content);
            if (root == null) {
                showFallbackCustomToast(context, message, iconRes, tintRes);
                return;
            }

            // Remove any existing active toast to prevent overlapping
            View previous = currentToastView.get();
            if (previous != null && previous.getParent() != null) {
                root.removeView(previous);
            }

            LayoutInflater inflater = LayoutInflater.from(activity);
            View toastView = inflater.inflate(R.layout.view_modern_toast, root, false);
            currentToastView = new WeakReference<>(toastView);

            ImageView ivIcon = toastView.findViewById(R.id.ivToastIcon);
            FrameLayout flIconContainer = toastView.findViewById(R.id.flToastIconContainer);
            TextView tvMsg = toastView.findViewById(R.id.tvToastMessage);
            TextView btnAction = toastView.findViewById(R.id.btnToastAction);

            ivIcon.setImageResource(iconRes);
            int tint = ContextCompat.getColor(activity, tintRes);
            ivIcon.setImageTintList(ColorStateList.valueOf(tint));

            if (flIconContainer != null) {
                GradientDrawable circle = new GradientDrawable();
                circle.setShape(GradientDrawable.OVAL);
                circle.setColor(bgBadgeColor);
                flIconContainer.setBackground(circle);
            }

            tvMsg.setText(message);

            if (actionText != null && actionListener != null) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText(actionText);
                btnAction.setOnClickListener(v -> {
                    actionListener.onClick(v);
                    dismissToast(root, toastView);
                });
            } else {
                btnAction.setVisibility(View.GONE);
            }

            root.addView(toastView);

            // Tactile haptic feedback
            MotionUtils.performHaptic(toastView);

            // Entry Animation: slide up + bounce overshoot
            toastView.setAlpha(0f);
            toastView.setTranslationY(100f);
            toastView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(280)
                    .setInterpolator(new OvershootInterpolator(0.9f))
                    .start();

            // Auto dismiss after 2.8 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                dismissToast(root, toastView);
            }, 2800);
        });
    }

    private static void dismissToast(ViewGroup root, View toastView) {
        if (toastView != null && toastView.getParent() != null) {
            toastView.animate()
                    .alpha(0f)
                    .translationY(60f)
                    .setDuration(220)
                    .withEndAction(() -> {
                        if (toastView.getParent() != null) {
                            root.removeView(toastView);
                        }
                    })
                    .start();
        }
    }

    private static void showFallbackCustomToast(Context context, String message, int iconRes, int tintRes) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.view_modern_toast, null);
            ImageView ivIcon = view.findViewById(R.id.ivToastIcon);
            TextView tvMsg = view.findViewById(R.id.tvToastMessage);
            ivIcon.setImageResource(iconRes);
            ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, tintRes)));
            tvMsg.setText(message);

            Toast t = new Toast(context.getApplicationContext());
            t.setView(view);
            t.setDuration(Toast.LENGTH_SHORT);
            t.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100);
            t.show();
        } catch (Exception e) {
            // Absolute fallback
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static Activity getActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
