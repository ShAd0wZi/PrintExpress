package com.printxpress.app.ui.support;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.printxpress.app.R;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Promotion;
import com.printxpress.app.ui.adapter.PromotionAdapter;
import java.util.ArrayList;
import java.util.Date;

public class PromotionsActivity extends AppCompatActivity {

    private PrintXpressDatabase db;
    private PromotionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotions);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvActiveVoucherCount = findViewById(R.id.tvActiveVoucherCount);
        TextView tvEmptyPromotions = findViewById(R.id.tvEmptyPromotions);
        TextView btnAddPromo = findViewById(R.id.btnAddPromo);
        View pbLoading = findViewById(R.id.pbLoading);
        RecyclerView rvPromotions = findViewById(R.id.rvPromotions);

        rvPromotions.setLayoutManager(new LinearLayoutManager(this));

        db = PrintXpressDatabase.getDatabase(this);

        adapter = new PromotionAdapter(new ArrayList<>(), new PromotionAdapter.OnPromotionClickListener() {
            @Override
            public void onPromotionClick(Promotion promotion) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Promo Code", promotion.getCode()));
                }
                com.printxpress.app.util.PrintXpressToast.success(PromotionsActivity.this,
                        "Copied code " + promotion.getCode() + " to clipboard!");
            }

            @Override
            public void onPromotionLongClick(Promotion promotion) {
                if (!com.printxpress.app.util.SessionManager.isAdmin(PromotionsActivity.this)) {
                    return;
                }
                AlertDialog dialog = new AlertDialog.Builder(PromotionsActivity.this)
                        .setTitle("Delete Promotion")
                        .setMessage("Remove promo code '" + promotion.getCode() + "' from active vouchers?")
                        .setPositiveButton("Delete", (d, which) -> {
                            PrintXpressDatabase.databaseWriteExecutor.execute(() -> db.promotionDao().delete(promotion));
                            com.printxpress.app.util.PrintXpressToast.info(PromotionsActivity.this, "Promotion removed successfully");
                        })
                        .setNegativeButton("Cancel", null)
                        .create();
                if (dialog.getWindow() != null) {
                    dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    dialog.getWindow().setDimAmount(0.65f);
                }
                dialog.show();
            }
        });
        rvPromotions.setAdapter(adapter);

        // Observe live promotions directly from the SQLite database
        db.promotionDao().getActivePromotions().observe(this, promotions -> {
            if (pbLoading != null) {
                pbLoading.setVisibility(View.GONE);
            }
            if (promotions != null && !promotions.isEmpty()) {
                tvActiveVoucherCount.setText(promotions.size() + " Active Commercial Voucher" + (promotions.size() == 1 ? "" : "s"));
                tvEmptyPromotions.setVisibility(View.GONE);
                rvPromotions.setVisibility(View.VISIBLE);
                adapter.setPromotions(promotions);
            } else {
                tvActiveVoucherCount.setText("0 Active Commercial Vouchers");
                tvEmptyPromotions.setVisibility(View.VISIBLE);
                rvPromotions.setVisibility(View.GONE);
                adapter.setPromotions(new ArrayList<>());
            }
        });

        boolean isAdmin = com.printxpress.app.util.SessionManager.isAdmin(this);
        TextView tvPromoSubtitle = findViewById(R.id.tvPromoSubtitle);
        if (tvPromoSubtitle != null) {
            if (isAdmin) {
                tvPromoSubtitle.setText("Tap a promo to copy code • Long-press to delete");
            } else {
                tvPromoSubtitle.setText("Tap any promotion code to copy to clipboard");
            }
        }

        if (btnAddPromo != null) {
            btnAddPromo.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            btnAddPromo.setOnClickListener(v -> {
                if (com.printxpress.app.util.SessionManager.isAdmin(this)) {
                    showAddPromoDialog();
                } else {
                    com.printxpress.app.util.PrintXpressToast.error(this, "Admin privileges required.");
                }
            });
        }
    }

    private void showAddPromoDialog() {
        com.printxpress.app.util.PromoDialogHelper.showAddPromoDialog(this, db, null);
    }
}