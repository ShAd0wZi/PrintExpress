package com.printxpress.app.ui.product;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.printxpress.app.data.local.entity.Category;
import com.printxpress.app.databinding.ActivityCategoryListBinding;
import com.printxpress.app.ui.adapter.CategoryAdapter;
import com.printxpress.app.viewModel.ProductViewModel;
import java.util.ArrayList;
import java.util.List;

public class CategoryListActivity extends AppCompatActivity {
    
    private ActivityCategoryListBinding binding;
    private ProductViewModel viewModel;
    private CategoryAdapter adapter;
    private List<Category> allCategories = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        int spanCount = (getResources().getConfiguration().smallestScreenWidthDp >= 600
                || getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) ? 2 : 1;
        binding.rvCategories.setLayoutManager(new GridLayoutManager(this, spanCount));
        
        adapter = new CategoryAdapter(new ArrayList<>(), category -> {
            Intent intent = new Intent(CategoryListActivity.this, ProductListActivity.class);
            intent.putExtra("CATEGORY_ID", category.getCategoryId());
            intent.putExtra("CATEGORY_NAME", category.getName());
            startActivity(intent);
        });
        if (com.printxpress.app.util.SessionManager.isAdmin(this)) {
            adapter.setOnCategoryLongClickListener(this::showEditOrDeleteCategoryDialog);
        } else {
            adapter.setOnCategoryLongClickListener(null);
        }
        binding.rvCategories.setAdapter(adapter);
        
        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);
        
        viewModel.getAllCategories().observe(this, categories -> {
            binding.pbLoading.setVisibility(android.view.View.GONE);
            if (categories != null) {
                allCategories = categories;
                filterCategories(binding.etSearchCategory.getText().toString());
            }
        });

        binding.etSearchCategory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearSearchCategory.setOnClickListener(v -> {
            binding.etSearchCategory.setText("");
            binding.btnClearSearchCategory.setVisibility(android.view.View.GONE);
            hideSoftKeyboard(binding.etSearchCategory);
        });

        binding.etSearchCategory.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                    || actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                hideSoftKeyboard(binding.etSearchCategory);
                return true;
            }
            return false;
        });

        boolean isAdmin = com.printxpress.app.util.SessionManager.isAdmin(this);
        binding.btnAddCategory.setVisibility(isAdmin ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnAddCategory.setOnClickListener(v -> {
            if (com.printxpress.app.util.SessionManager.isAdmin(this)) {
                showCategoryDialog(null);
            } else {
                com.printxpress.app.util.PrintXpressToast.error(this, "Admin privileges required.");
            }
        });
    }

    private void filterCategories(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        binding.btnClearSearchCategory.setVisibility(q.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
        List<Category> filtered;
        if (q.isEmpty()) {
            filtered = allCategories;
        } else {
            filtered = new ArrayList<>();
            String[] tokens = q.split("\\s+");
            for (Category c : allCategories) {
                String name = c.getName() != null ? c.getName().toLowerCase(java.util.Locale.ROOT) : "";
                String desc = c.getDescription() != null ? c.getDescription().toLowerCase(java.util.Locale.ROOT) : "";
                String corpus = (name + " " + desc);
                String normalizedCorpus = corpus.replaceAll("[^a-z0-9\\s]", " ");

                boolean allMatch = true;
                for (String token : tokens) {
                    String tokenClean = token.replaceAll("[^a-z0-9]", "");
                    if (tokenClean.isEmpty()) continue;
                    String tokenSingular = tokenClean.endsWith("s") && tokenClean.length() > 3
                            ? tokenClean.substring(0, tokenClean.length() - 1)
                            : tokenClean;

                    boolean tokenMatched = corpus.contains(tokenClean)
                            || normalizedCorpus.contains(tokenClean)
                            || corpus.contains(tokenSingular)
                            || normalizedCorpus.contains(tokenSingular);

                    if (!tokenMatched) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    filtered.add(c);
                }
            }
        }
        if (adapter != null) {
            adapter.setCategories(filtered);
        }
        binding.tvEmptyCategories.setVisibility(filtered.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void hideSoftKeyboard(android.view.View view) {
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void showEditOrDeleteCategoryDialog(Category category) {
        if (!com.printxpress.app.util.SessionManager.isAdmin(this)) {
            return;
        }
        String[] options = {"Edit Category", "Delete Category"};
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(category.getName())
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        showCategoryDialog(category);
                    } else {
                        viewModel.deleteCategory(category);
                        com.printxpress.app.util.PrintXpressToast.success(this, "Category deleted successfully");
                    }
                })
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.65f);
        }
        dialog.show();
    }

    private void showCategoryDialog(Category existing) {
        com.printxpress.app.util.CategoryDialogHelper.showCategoryDialog(this, viewModel, existing, null);
    }
}