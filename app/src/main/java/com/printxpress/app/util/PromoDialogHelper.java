package com.printxpress.app.util;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.printxpress.app.R;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Promotion;
import com.printxpress.app.databinding.DialogAddPromoBinding;
import java.util.Date;
import java.util.Locale;

public class PromoDialogHelper {

    public static void showAddPromoDialog(Activity activity, PrintXpressDatabase db, Runnable onSaved) {
        DialogAddPromoBinding binding = DialogAddPromoBinding.inflate(LayoutInflater.from(activity));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(binding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.70f);
        }

        // Setup preset chips
        TextView[] chips = {
                binding.chipDisc10,
                binding.chipDisc15,
                binding.chipDisc20,
                binding.chipDisc25,
                binding.chipDisc50
        };
        double[] values = {10, 15, 20, 25, 50};

        Runnable updateChipsHighlight = () -> {
            String currentStr = binding.etDiscountPercentage.getText() != null
                    ? binding.etDiscountPercentage.getText().toString().trim()
                    : "";
            double currentVal = -1;
            try {
                currentVal = Double.parseDouble(currentStr);
            } catch (Exception ignored) {}

            for (int i = 0; i < chips.length; i++) {
                boolean active = Math.abs(values[i] - currentVal) < 0.01;
                chips[i].setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_neutral);
                chips[i].setTextColor(ContextCompat.getColor(activity, active ? R.color.primary_container : R.color.on_surface_variant));
            }
        };

        for (int i = 0; i < chips.length; i++) {
            final double val = values[i];
            chips[i].setOnClickListener(v -> {
                binding.etDiscountPercentage.setText(String.format(Locale.US, "%.0f", val));
                updateChipsHighlight.run();
            });
        }

        binding.etDiscountPercentage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateChipsHighlight.run();
            }
        });

        updateChipsHighlight.run();

        binding.btnCancelPromo.setOnClickListener(v -> dialog.dismiss());

        binding.btnSavePromo.setOnClickListener(v -> {
            String code = binding.etPromoCode.getText() != null
                    ? binding.etPromoCode.getText().toString().trim().toUpperCase(Locale.ROOT)
                    : "";
            String title = binding.etPromoTitle.getText() != null
                    ? binding.etPromoTitle.getText().toString().trim()
                    : "";
            String desc = binding.etPromoDescription.getText() != null
                    ? binding.etPromoDescription.getText().toString().trim()
                    : "";
            String discStr = binding.etDiscountPercentage.getText() != null
                    ? binding.etDiscountPercentage.getText().toString().trim()
                    : "";
            String minStr = binding.etMinOrderAmount.getText() != null
                    ? binding.etMinOrderAmount.getText().toString().trim()
                    : "";

            if (code.isEmpty() || title.isEmpty() || discStr.isEmpty()) {
                PrintXpressToast.error(activity, "Code, Title, and Discount % are required.");
                return;
            }

            double disc;
            double minOrder;
            try {
                disc = Double.parseDouble(discStr);
                minOrder = minStr.isEmpty() ? 0.0 : Double.parseDouble(minStr);
                if (disc <= 0 || disc > 100) {
                    PrintXpressToast.error(activity, "Discount must be between 1% and 100%.");
                    return;
                }
            } catch (NumberFormatException e) {
                PrintXpressToast.error(activity, "Please enter valid numeric values.");
                return;
            }

            Date validUntil = new Date(System.currentTimeMillis() + 30L * 86400000L); // 30 days
            Promotion promo = new Promotion(code, title, desc, disc, minOrder, validUntil, true);

            PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
                db.promotionDao().insert(promo);
                activity.runOnUiThread(() -> {
                    PrintXpressToast.success(activity, "Promotion " + code + " activated successfully!");
                    dialog.dismiss();
                    if (onSaved != null) {
                        onSaved.run();
                    }
                });
            });
        });

        dialog.show();
    }
}
