package com.printxpress.app.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.printxpress.app.R;

/**
 * Motion and Tactile interaction utilities providing Spring physics,
 * tactile haptic feedback, and responsive micro-animations across PrintXpress.
 */
public final class MotionUtils {

    private MotionUtils() {
        // Utility class
    }

    /**
     * Applies subtle spring compression (scale 0.96) on touch down
     * and bounces back on release with haptic feedback, without consuming click events.
     */
    public static void applySpringTouch(View view) {
        if (view == null) return;

        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.96f)
                            .scaleY(0.96f)
                            .setDuration(110)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(180)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .start();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
            }
            return false; // Allows OnClickListener to fire normally
        });
    }

    /**
     * Performs subtle tactile haptic vibration.
     */
    public static void performHaptic(View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    /**
     * Dual-stage pop animation for wishlist favorite toggles.
     */
    public static void animateHeartPop(ImageView heartIcon, boolean isFilled) {
        if (heartIcon == null) return;

        performHaptic(heartIcon);

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(heartIcon, View.SCALE_X, 1.0f, 0.7f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(heartIcon, View.SCALE_Y, 1.0f, 0.7f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(heartIcon, View.SCALE_X, 0.7f, 1.35f, 1.0f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(heartIcon, View.SCALE_Y, 0.7f, 1.35f, 1.0f);
        scaleUpX.setDuration(240);
        scaleUpY.setDuration(240);
        scaleUpX.setInterpolator(new OvershootInterpolator(3.0f));
        scaleUpY.setInterpolator(new OvershootInterpolator(3.0f));

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(scaleDownX).with(scaleDownY);
        animatorSet.play(scaleUpX).with(scaleUpY).after(scaleDownX);

        scaleDownX.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                heartIcon.setImageResource(isFilled ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                heartIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        heartIcon.getContext().getColor(isFilled ? R.color.error : R.color.on_surface_variant)
                ));
            }
        });

        animatorSet.start();
    }
}
