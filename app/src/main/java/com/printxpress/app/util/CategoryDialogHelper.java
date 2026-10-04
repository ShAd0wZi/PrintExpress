package com.printxpress.app.util;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;
import com.printxpress.app.data.local.entity.Category;
import com.printxpress.app.databinding.DialogAddEditCategoryBinding;
import com.printxpress.app.viewModel.ProductViewModel;

public class CategoryDialogHelper {

    private static final String[] ICON_KEYS = {
            "ic_cat_business_cards",
            "flyer_a5",
            "ic_cat_banners",
            "ic_cat_tshirts",
            "ic_cat_stickers",
            "ic_cat_mugs",
            "ic_cat_booklets",
            "ic_cat_events"
    };

    private static final String[] ICON_LABELS = {
            "Business Cards & Stationery",
            "Flyers & Leaflets",
            "Outdoor Signs & Banners",
            "Custom Apparel & T-Shirts",
            "Labels & Stickers",
            "Promotional Mugs & Drinkware",
            "Catalogs & Booklets",
            "Wedding & Event Stationery"
    };

    public static void showCategoryDialog(
            Activity activity,
            ProductViewModel viewModel,
            Category existing,
            Runnable onSaved
    ) {
        DialogAddEditCategoryBinding binding = DialogAddEditCategoryBinding.inflate(LayoutInflater.from(activity));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(binding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.70f);
        }

        binding.tvCategoryDialogTitle.setText(existing == null ? "Add New Print Category" : "Edit Category Details");

        // Icon preset dropdown
        ArrayAdapter<String> iconAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, ICON_LABELS);
        binding.spinnerCategoryIcon.setAdapter(iconAdapter);

        int iconPos = 0;
        if (existing != null && existing.getImageUrl() != null) {
            for (int i = 0; i < ICON_KEYS.length; i++) {
                if (ICON_KEYS[i].equalsIgnoreCase(existing.getImageUrl())) {
                    iconPos = i;
                    break;
                }
            }
        }
        binding.spinnerCategoryIcon.setSelection(iconPos);

        if (existing != null) {
            binding.etCategoryName.setText(existing.getName());
            binding.etCategoryDescription.setText(existing.getDescription());
        }

        binding.btnCancelCategory.setOnClickListener(v -> dialog.dismiss());

        binding.btnSaveCategory.setOnClickListener(v -> {
            String name = binding.etCategoryName.getText() != null ? binding.etCategoryName.getText().toString().trim() : "";
            String desc = binding.etCategoryDescription.getText() != null ? binding.etCategoryDescription.getText().toString().trim() : "";

            if (name.isEmpty()) {
                PrintXpressToast.error(activity, "Category name is required.");
                return;
            }

            int selectedPos = binding.spinnerCategoryIcon.getSelectedItemPosition();
            String chosenIconKey = (selectedPos >= 0 && selectedPos < ICON_KEYS.length) ? ICON_KEYS[selectedPos] : "ic_cat_business_cards";

            if (existing == null) {
                Category c = new Category();
                c.setName(name);
                c.setDescription(desc.isEmpty() ? "Commercial print catalog division" : desc);
                c.setImageUrl(chosenIconKey);
                viewModel.insertCategory(c);
                PrintXpressToast.success(activity, "Category created successfully!");
            } else {
                existing.setName(name);
                existing.setDescription(desc);
                existing.setImageUrl(chosenIconKey);
                viewModel.updateCategory(existing);
                PrintXpressToast.success(activity, "Category updated successfully!");
            }

            dialog.dismiss();
            if (onSaved != null) {
                onSaved.run();
            }
        });

        dialog.show();
    }
}
