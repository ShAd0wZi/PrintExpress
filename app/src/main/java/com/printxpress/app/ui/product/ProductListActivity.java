package com.printxpress.app.ui.product;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Category;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.databinding.ActivityProductListBinding;
import com.printxpress.app.ui.adapter.ProductAdapter;
import com.printxpress.app.viewModel.ProductViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductListActivity extends AppCompatActivity {
    
    private static final int SORT_FEATURED = 0;
    private static final int SORT_PRICE_ASC = 1;
    private static final int SORT_PRICE_DESC = 2;
    private static final int SORT_NAME_ASC = 3;

    private ActivityProductListBinding binding;
    private ProductViewModel viewModel;
    private ProductAdapter adapter;
    private int categoryId;
    private String categoryName;
    private List<Category> availableCategories = new ArrayList<>();
    private List<Product> allLoadedProducts = new ArrayList<>();

    private int currentSortMode = SORT_FEATURED;
    private boolean filterUnder3k = false;
    private boolean filterInStockOnly = false;
    private String inlineQuery = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);
        
        categoryId = getIntent().getIntExtra("CATEGORY_ID", 0);
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        String searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
        
        boolean isTabletOrLandscape = getResources().getConfiguration().smallestScreenWidthDp >= 600
                || getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int spanCount = isTabletOrLandscape ? 3 : 2;
        binding.rvProducts.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, spanCount));
        
        adapter = new ProductAdapter(new ArrayList<>(), product -> {
            Intent intent = new Intent(ProductListActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getProductId());
            intent.putExtra("PRODUCT_NAME", product.getName());
            intent.putExtra("PRODUCT_PRICE", product.getBasePrice());
            intent.putExtra("PRODUCT_DESCRIPTION", product.getDescription());
            intent.putExtra("PRODUCT_IMAGE", product.getImageUrl());
            intent.putExtra("AVAILABLE_SIZES", product.getAvailableSizes());
            intent.putExtra("AVAILABLE_MATERIALS", product.getAvailableMaterials());
            startActivity(intent);
        });
        if (com.printxpress.app.util.SessionManager.isAdmin(this)) {
            adapter.setOnProductLongClickListener(this::showEditOrDeleteProductDialog);
        } else {
            adapter.setOnProductLongClickListener(null);
        }
        binding.rvProducts.setAdapter(adapter);

        setupFilterAndSortControls();
        updateChipStyles();

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            inlineQuery = searchQuery.trim();
            binding.etInlineSearch.setText(inlineQuery);
            binding.btnClearSearch.setVisibility(View.VISIBLE);
            binding.tvTitle.setText("Search: \"" + inlineQuery + "\"");
        } else if (categoryName != null && !categoryName.isEmpty()) {
            binding.tvTitle.setText(categoryName);
        } else {
            binding.tvTitle.setText("All Print Products");
        }

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        viewModel.getAllCategories().observe(this, cats -> {
            if (cats != null) {
                availableCategories = cats;
                applyFiltersAndSort();
            }
        });
        
        viewModel.getAllProducts().observe(this, products -> {
            binding.pbLoading.setVisibility(View.GONE);
            allLoadedProducts = products != null ? new ArrayList<>(products) : new ArrayList<>();
            applyFiltersAndSort();
        });

        boolean isAdmin = com.printxpress.app.util.SessionManager.isAdmin(this);
        binding.btnAddProduct.setVisibility(isAdmin ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnAddProduct.setOnClickListener(v -> {
            if (com.printxpress.app.util.SessionManager.isAdmin(this)) {
                showProductDialog(null);
            } else {
                com.printxpress.app.util.PrintXpressToast.error(this, "Admin privileges required.");
            }
        });
    }

    private void setupFilterAndSortControls() {
        binding.etInlineSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                inlineQuery = s != null ? s.toString().trim() : "";
                binding.btnClearSearch.setVisibility(!inlineQuery.isEmpty() ? View.VISIBLE : View.GONE);
                applyFiltersAndSort();
            }
        });

        binding.etInlineSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                    || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                hideSoftKeyboard(binding.etInlineSearch);
                return true;
            }
            return false;
        });

        binding.btnClearSearch.setOnClickListener(v -> {
            binding.etInlineSearch.setText("");
            binding.btnClearSearch.setVisibility(View.GONE);
            hideSoftKeyboard(binding.etInlineSearch);
        });

        binding.chipSortDefault.setOnClickListener(v -> {
            currentSortMode = SORT_FEATURED;
            updateChipStyles();
            applyFiltersAndSort();
        });

        binding.chipSortPriceAsc.setOnClickListener(v -> {
            currentSortMode = SORT_PRICE_ASC;
            updateChipStyles();
            applyFiltersAndSort();
        });

        binding.chipSortPriceDesc.setOnClickListener(v -> {
            currentSortMode = SORT_PRICE_DESC;
            updateChipStyles();
            applyFiltersAndSort();
        });

        binding.chipSortName.setOnClickListener(v -> {
            currentSortMode = SORT_NAME_ASC;
            updateChipStyles();
            applyFiltersAndSort();
        });

        binding.chipFilterUnder3k.setOnClickListener(v -> {
            filterUnder3k = !filterUnder3k;
            binding.chipFilterUnder3k.setText(filterUnder3k ? "✓ Under Rs. 3,000" : "Under Rs. 3,000");
            updateChipStyles();
            applyFiltersAndSort();
        });

        binding.chipFilterInStock.setOnClickListener(v -> {
            filterInStockOnly = !filterInStockOnly;
            binding.chipFilterInStock.setText(filterInStockOnly ? "✓ In Stock Only" : "⚡ In Stock Only");
            updateChipStyles();
            applyFiltersAndSort();
        });
    }

    private void updateChipStyles() {
        setChipAppearance(binding.chipSortDefault, currentSortMode == SORT_FEATURED);
        setChipAppearance(binding.chipSortPriceAsc, currentSortMode == SORT_PRICE_ASC);
        setChipAppearance(binding.chipSortPriceDesc, currentSortMode == SORT_PRICE_DESC);
        setChipAppearance(binding.chipSortName, currentSortMode == SORT_NAME_ASC);
        setChipAppearance(binding.chipFilterUnder3k, filterUnder3k);
        setChipAppearance(binding.chipFilterInStock, filterInStockOnly);
    }

    private void setChipAppearance(android.widget.TextView chip, boolean isActive) {
        if (chip == null) return;
        chip.setBackgroundResource(isActive ? R.drawable.bg_chip_active : R.drawable.bg_chip_neutral);
        int textColor = androidx.core.content.ContextCompat.getColor(
                this, isActive ? R.color.primary_container : R.color.on_surface_variant);
        chip.setTextColor(textColor);
    }

    private void updateProductListUI(List<Product> products) {
        binding.pbLoading.setVisibility(View.GONE);
        allLoadedProducts = products != null ? new ArrayList<>(products) : new ArrayList<>();
        applyFiltersAndSort();
    }

    private void applyFiltersAndSort() {
        List<Product> sourceList;
        boolean isSearching = !inlineQuery.isEmpty();

        if (isSearching) {
            // When user searches, search across the entire catalog!
            sourceList = allLoadedProducts;
        } else if (categoryId > 0) {
            sourceList = new ArrayList<>();
            for (Product p : allLoadedProducts) {
                if (p.getCategoryId() == categoryId) {
                    sourceList.add(p);
                }
            }
        } else {
            sourceList = allLoadedProducts;
        }

        Map<Integer, Category> catMap = new HashMap<>();
        if (availableCategories != null) {
            for (Category c : availableCategories) {
                catMap.put(c.getCategoryId(), c);
            }
        }

        List<Product> filtered = new ArrayList<>();
        for (Product p : sourceList) {
            if (isSearching && !matchesSearch(p, inlineQuery, catMap)) {
                continue;
            }
            if (filterUnder3k && p.getBasePrice() > 3000.0) {
                continue;
            }
            if (filterInStockOnly && p.getStockQuantity() <= 0) {
                continue;
            }
            filtered.add(p);
        }

        if (currentSortMode == SORT_PRICE_ASC) {
            Collections.sort(filtered, Comparator.comparingDouble(Product::getBasePrice));
        } else if (currentSortMode == SORT_PRICE_DESC) {
            Collections.sort(filtered, (a, b) -> Double.compare(b.getBasePrice(), a.getBasePrice()));
        } else if (currentSortMode == SORT_NAME_ASC) {
            Collections.sort(filtered, (a, b) -> {
                String na = a.getName() != null ? a.getName() : "";
                String nb = b.getName() != null ? b.getName() : "";
                return na.compareToIgnoreCase(nb);
            });
        }

        if (adapter != null) {
            adapter.setProducts(filtered);
        }
        binding.tvEmptyProducts.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);

        if (filtered.isEmpty() && isSearching) {
            binding.tvEmptyProducts.setText("No print products found matching \"" + inlineQuery + "\".\nTry searching for cards, flyers, banners, stickers, or finishes.");
        } else if (filtered.isEmpty()) {
            binding.tvEmptyProducts.setText("No products available in this category.");
        }

        String adminNote = com.printxpress.app.util.SessionManager.isAdmin(this) ? " • Long-press to edit/restock" : "";
        if (isSearching) {
            binding.tvTitle.setText("Search: \"" + inlineQuery + "\"");
            binding.tvProductCount.setText(filtered.size() + (filtered.size() == 1 ? " Result" : " Results") + " found across catalog" + adminNote);
        } else if (categoryName != null && !categoryName.isEmpty()) {
            binding.tvTitle.setText(categoryName);
            binding.tvProductCount.setText(filtered.size() + (filtered.size() == 1 ? " Product" : " Products") + " available" + adminNote);
        } else {
            binding.tvTitle.setText("All Print Products");
            binding.tvProductCount.setText(filtered.size() + (filtered.size() == 1 ? " Product" : " Products") + " available" + adminNote);
        }
    }

    private boolean matchesSearch(Product p, String query, Map<Integer, Category> catMap) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        String qClean = query.trim().toLowerCase(Locale.ROOT);
        String[] tokens = qClean.split("\\s+");

        StringBuilder corpusBuilder = new StringBuilder();
        if (p.getName() != null) corpusBuilder.append(p.getName()).append(" ");
        if (p.getDescription() != null) corpusBuilder.append(p.getDescription()).append(" ");
        if (p.getAvailableMaterials() != null) corpusBuilder.append(p.getAvailableMaterials()).append(" ");
        if (p.getAvailableSizes() != null) corpusBuilder.append(p.getAvailableSizes()).append(" ");

        if (catMap != null && catMap.containsKey(p.getCategoryId())) {
            Category cat = catMap.get(p.getCategoryId());
            if (cat != null) {
                if (cat.getName() != null) corpusBuilder.append(cat.getName()).append(" ");
                if (cat.getDescription() != null) corpusBuilder.append(cat.getDescription()).append(" ");
            }
        }

        String corpus = corpusBuilder.toString().toLowerCase(Locale.ROOT);
        String normalizedCorpus = corpus.replaceAll("[^a-z0-9\\s]", " ");
        String[] corpusWords = normalizedCorpus.split("\\s+");

        for (String rawToken : tokens) {
            String tokenClean = rawToken.replaceAll("[^a-z0-9]", "");
            if (tokenClean.isEmpty()) continue;

            String tokenSingular = tokenClean.endsWith("s") && tokenClean.length() > 3
                    ? tokenClean.substring(0, tokenClean.length() - 1)
                    : tokenClean;

            boolean tokenMatched = corpus.contains(tokenClean)
                    || normalizedCorpus.contains(tokenClean)
                    || corpus.contains(tokenSingular)
                    || normalizedCorpus.contains(tokenSingular);

            if (!tokenMatched) {
                tokenMatched = matchSynonyms(tokenClean, corpus);
            }

            if (!tokenMatched && tokenClean.length() >= 4) {
                for (String cWord : corpusWords) {
                    if (cWord.length() < 3) continue;
                    int maxDistance = tokenClean.length() >= 6 ? 2 : 1;
                    if (Math.abs(tokenClean.length() - cWord.length()) <= maxDistance) {
                        if (editDistance(tokenClean, cWord) <= maxDistance
                                || editDistance(tokenSingular, cWord) <= maxDistance) {
                            tokenMatched = true;
                            break;
                        }
                    }
                }
            }

            if (!tokenMatched) {
                return false;
            }
        }
        return true;
    }

    private boolean matchSynonyms(String token, String corpus) {
        switch (token) {
            case "card":
            case "cards":
            case "visiting":
            case "namecard":
                return corpus.contains("card") || corpus.contains("silk");
            case "tee":
            case "tshirt":
            case "shirt":
            case "apparel":
            case "cloth":
            case "clothing":
                return corpus.contains("t-shirt") || corpus.contains("shirt") || corpus.contains("cotton");
            case "mug":
            case "mugs":
            case "cup":
            case "cups":
            case "coffee":
            case "tea":
                return corpus.contains("mug") || corpus.contains("ceramic");
            case "flyer":
            case "flyers":
            case "flier":
            case "fliers":
            case "leaflet":
            case "leaflets":
            case "brochure":
            case "pamphlet":
                return corpus.contains("flyer") || corpus.contains("leaflet");
            case "sticker":
            case "stickers":
            case "stiker":
            case "label":
            case "labels":
            case "decal":
            case "decals":
            case "vinyl":
                return corpus.contains("sticker") || corpus.contains("vinyl");
            case "banner":
            case "banners":
            case "poster":
            case "posters":
            case "billboard":
            case "flex":
            case "hoarding":
                return corpus.contains("banner") || corpus.contains("outdoor");
            case "book":
            case "booklet":
            case "booklets":
            case "catalog":
            case "catalogs":
            case "catalogue":
            case "magazine":
                return corpus.contains("booklet") || corpus.contains("catalog");
            case "wedding":
            case "invite":
            case "invitation":
            case "envelope":
            case "letterhead":
                return corpus.contains("event") || corpus.contains("stationery");
            default:
                return false;
        }
    }

    private static int editDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[] dp = new int[len2 + 1];
        for (int j = 0; j <= len2; j++) dp[j] = j;

        for (int i = 1; i <= len1; i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= len2; j++) {
                int temp = dp[j];
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[j] = prev;
                } else {
                    dp[j] = 1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
                }
                prev = temp;
            }
        }
        return dp[len2];
    }

    private void hideSoftKeyboard(View view) {
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void showEditOrDeleteProductDialog(Product product) {
        if (!com.printxpress.app.util.SessionManager.isAdmin(this)) {
            return;
        }
        String[] options = {"Edit Product Specifications", "Quick Restock (+50 Units)", "Remove from Catalog"};
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(product.getName() + " (Stock: " + com.printxpress.app.util.SessionManager.formatNumber(product.getStockQuantity()) + " units)")
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        showProductDialog(product);
                    } else if (which == 1) {
                        viewModel.updateStock(product.getProductId(), product.getStockQuantity() + 50);
                        com.printxpress.app.util.PrintXpressToast.success(this, "Restocked +50 units successfully!");
                    } else {
                        viewModel.deleteProduct(product);
                        com.printxpress.app.util.PrintXpressToast.info(this, "Product removed from store catalog");
                    }
                })
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.65f);
        }
        dialog.show();
    }

    private void showProductDialog(Product existing) {
        com.printxpress.app.util.ProductDialogHelper.showAddEditDialog(
                this,
                viewModel,
                existing,
                availableCategories,
                categoryId,
                this::applyFiltersAndSort
        );
    }
}