package com.printxpress.app.util;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Category;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.databinding.DialogAddEditProductBinding;
import com.printxpress.app.viewModel.ProductViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductDialogHelper {

    private static final String[] PRESET_KEYS = {
            "premium_card", "flyer_a5", "ic_cat_banners", "ic_cat_mugs", "ic_cat_tshirts", "ic_cat_stickers", "ic_cat_booklets", "ic_cat_events"
    };

    private static final String[] PRESET_LABELS = {
            "Business Cards Preset", "Flyers & Leaflets", "Outdoor Banners", "Ceramic Mugs", "Custom T-Shirts", "Die-Cut Stickers", "Catalogs & Booklets", "Event Stationery"
    };

    public static void showAddEditDialog(
            Activity activity,
            ProductViewModel viewModel,
            Product existing,
            List<Category> categories,
            int defaultCategoryId,
            Runnable onSaved
    ) {
        DialogAddEditProductBinding binding = DialogAddEditProductBinding.inflate(LayoutInflater.from(activity));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(binding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.70f);
        }

        binding.tvDialogTitle.setText(existing == null ? "Add New Print Product" : "Edit Product Specifications");

        // Categories Spinner
        List<String> categoryNames = new ArrayList<>();
        List<Integer> categoryIds = new ArrayList<>();
        int selectedCategoryIndex = 0;

        if (categories != null && !categories.isEmpty()) {
            for (int i = 0; i < categories.size(); i++) {
                Category c = categories.get(i);
                categoryNames.add(c.getName());
                categoryIds.add(c.getCategoryId());
                if (existing != null && c.getCategoryId() == existing.getCategoryId()) {
                    selectedCategoryIndex = i;
                } else if (existing == null && c.getCategoryId() == defaultCategoryId) {
                    selectedCategoryIndex = i;
                }
            }
        } else {
            categoryNames.add("Standard Printing (Default)");
            categoryIds.add(1);
        }

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, categoryNames);
        binding.spinnerCategory.setAdapter(catAdapter);
        binding.spinnerCategory.setSelection(selectedCategoryIndex);

        // Image Presets Spinner
        ArrayAdapter<String> presetAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, PRESET_LABELS);
        binding.spinnerPresetImage.setAdapter(presetAdapter);
        int presetIndex = 0;
        if (existing != null && existing.getImageUrl() != null) {
            for (int i = 0; i < PRESET_KEYS.length; i++) {
                if (PRESET_KEYS[i].equalsIgnoreCase(existing.getImageUrl())) {
                    presetIndex = i;
                    break;
                }
            }
        }
        binding.spinnerPresetImage.setSelection(presetIndex);

        // Pre-fill existing data
        if (existing != null) {
            binding.etProductName.setText(existing.getName());
            binding.etBasePrice.setText(String.format(Locale.US, "%.2f", existing.getBasePrice()));
            binding.etStockQty.setText(String.valueOf(existing.getStockQuantity()));
            binding.etDescription.setText(existing.getDescription());
            if (existing.getAvailableSizes() != null) {
                binding.etAvailableSizes.setText(existing.getAvailableSizes());
            }
            if (existing.getAvailableMaterials() != null) {
                binding.etAvailableMaterials.setText(existing.getAvailableMaterials());
            }
        }

        setupFinishChips(activity, binding);

        binding.btnCancelDialog.setOnClickListener(v -> dialog.dismiss());

        binding.btnSaveProductDialog.setOnClickListener(v -> {
            String name = binding.etProductName.getText() != null ? binding.etProductName.getText().toString().trim() : "";
            String priceStr = binding.etBasePrice.getText() != null ? binding.etBasePrice.getText().toString().trim() : "";
            String stockStr = binding.etStockQty.getText() != null ? binding.etStockQty.getText().toString().trim() : "";
            String desc = binding.etDescription.getText() != null ? binding.etDescription.getText().toString().trim() : "";
            String sizes = binding.etAvailableSizes.getText() != null ? binding.etAvailableSizes.getText().toString().trim() : "";
            String materials = binding.etAvailableMaterials.getText() != null ? binding.etAvailableMaterials.getText().toString().trim() : "";

            if (name.isEmpty() || priceStr.isEmpty()) {
                PrintXpressToast.error(activity, "Product Name and Base Price are required.");
                return;
            }

            double price;
            int stockQty = 100;
            try {
                price = Double.parseDouble(priceStr);
                if (price < 0) {
                    PrintXpressToast.error(activity, "Price cannot be negative.");
                    return;
                }
                if (!stockStr.isEmpty()) {
                    stockQty = Math.max(0, Integer.parseInt(stockStr));
                }
            } catch (NumberFormatException e) {
                PrintXpressToast.error(activity, "Please enter valid numeric values for price & stock.");
                return;
            }

            int selectedCatPos = binding.spinnerCategory.getSelectedItemPosition();
            int finalCatId = (selectedCatPos >= 0 && selectedCatPos < categoryIds.size()) ? categoryIds.get(selectedCatPos) : 1;

            int selectedPresetPos = binding.spinnerPresetImage.getSelectedItemPosition();
            String finalImageKey = (selectedPresetPos >= 0 && selectedPresetPos < PRESET_KEYS.length) ? PRESET_KEYS[selectedPresetPos] : "premium_card";

            if (existing == null) {
                Product newProduct = new Product();
                newProduct.setCategoryId(finalCatId);
                newProduct.setName(name);
                newProduct.setBasePrice(price);
                newProduct.setStockQuantity(stockQty);
                newProduct.setDescription(desc.isEmpty() ? name : desc);
                newProduct.setAvailableSizes(sizes.isEmpty() ? "Standard" : sizes);
                newProduct.setAvailableMaterials(materials.isEmpty() ? "Standard Stock" : materials);
                newProduct.setImageUrl(finalImageKey);
                viewModel.insertProduct(newProduct);
                PrintXpressToast.success(activity, "Product added to catalog successfully!");
            } else {
                existing.setCategoryId(finalCatId);
                existing.setName(name);
                existing.setBasePrice(price);
                existing.setStockQuantity(stockQty);
                existing.setDescription(desc.isEmpty() ? name : desc);
                existing.setAvailableSizes(sizes.isEmpty() ? "Standard" : sizes);
                existing.setAvailableMaterials(materials.isEmpty() ? "Standard Stock" : materials);
                existing.setImageUrl(finalImageKey);
                viewModel.updateProduct(existing);
                PrintXpressToast.success(activity, "Product specifications updated!");
            }

            dialog.dismiss();
            if (onSaved != null) {
                onSaved.run();
            }
        });

        dialog.show();
    }

    private static void setupFinishChips(Activity activity, DialogAddEditProductBinding binding) {
        TextView[] chips = {
                binding.chipFinishSpotUv,
                binding.chipFinishGoldFoil,
                binding.chipFinishSilverFoil,
                binding.chipFinishMatte,
                binding.chipFinishGloss,
                binding.chipFinishVelvet,
                binding.chipFinishEmbossed,
                binding.chipFinishSilk
        };
        String[] finishNames = {
                "Spot UV",
                "Gold Foil",
                "Silver Foil",
                "Matte Lamination",
                "Gloss Lamination",
                "Velvet Soft-Touch",
                "Embossed",
                "350gsm Silk Art"
        };

        Runnable updateChipsHighlight = () -> {
            String current = binding.etAvailableMaterials.getText() != null
                    ? binding.etAvailableMaterials.getText().toString().toLowerCase(Locale.ROOT)
                    : "";
            for (int i = 0; i < chips.length; i++) {
                boolean active = current.contains(finishNames[i].toLowerCase(Locale.ROOT));
                chips[i].setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip_neutral);
                chips[i].setTextColor(ContextCompat.getColor(activity, active ? R.color.primary_container : R.color.on_surface_variant));
            }
        };

        for (int i = 0; i < chips.length; i++) {
            final String finishName = finishNames[i];
            chips[i].setOnClickListener(v -> {
                String current = binding.etAvailableMaterials.getText() != null
                        ? binding.etAvailableMaterials.getText().toString().trim()
                        : "";
                List<String> list = new ArrayList<>();
                if (!current.isEmpty()) {
                    for (String item : current.split(",")) {
                        String trimmed = item.trim();
                        if (!trimmed.isEmpty()) {
                            list.add(trimmed);
                        }
                    }
                }
                boolean found = false;
                for (int j = 0; j < list.size(); j++) {
                    if (list.get(j).equalsIgnoreCase(finishName)) {
                        list.remove(j);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    list.add(finishName);
                }
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < list.size(); j++) {
                    if (j > 0) sb.append(", ");
                    sb.append(list.get(j));
                }
                binding.etAvailableMaterials.setText(sb.toString());
                updateChipsHighlight.run();
            });
        }

        binding.etAvailableMaterials.addTextChangedListener(new android.text.TextWatcher() {
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
    }
}
