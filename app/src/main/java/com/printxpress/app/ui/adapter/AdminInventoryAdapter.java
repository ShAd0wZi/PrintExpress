package com.printxpress.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Category;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.databinding.ItemInventoryAdminBinding;
import com.printxpress.app.util.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class AdminInventoryAdapter extends RecyclerView.Adapter<AdminInventoryAdapter.InventoryViewHolder> {

    public interface OnRestockClickListener {
        void onRestock(Product product, int addUnits);
    }

    public interface OnEditClickListener {
        void onEdit(Product product);
    }

    private List<Product> products = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();
    private final OnRestockClickListener restockListener;
    private final OnEditClickListener editListener;

    public AdminInventoryAdapter(List<Product> products, OnRestockClickListener restockListener, OnEditClickListener editListener) {
        if (products != null) this.products = products;
        this.restockListener = restockListener;
        this.editListener = editListener;
    }

    public void setProducts(List<Product> products) {
        this.products = products != null ? products : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public InventoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemInventoryAdminBinding binding = ItemInventoryAdminBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new InventoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryViewHolder holder, int position) {
        Product p = products.get(position);
        Context ctx = holder.itemView.getContext();

        holder.binding.tvInventoryProductName.setText(p.getName());

        String catName = "General Catalog";
        for (Category c : categories) {
            if (c.getCategoryId() == p.getCategoryId()) {
                catName = c.getName();
                break;
            }
        }
        holder.binding.tvInventoryProductCategory.setText(catName + " • Base " + SessionManager.formatPrice(ctx, p.getBasePrice()));

        int stock = p.getStockQuantity();
        if (stock <= 0) {
            holder.binding.tvInventoryStockBadge.setText("⚠️ Out of Stock (0 units)");
            holder.binding.tvInventoryStockBadge.setBackgroundResource(R.drawable.bg_badge_cancelled);
            holder.binding.tvInventoryStockBadge.setTextColor(ContextCompat.getColor(ctx, R.color.status_cancelled_text));
        } else if (stock <= 15) {
            holder.binding.tvInventoryStockBadge.setText("⚠️ Low Stock: " + SessionManager.formatNumber(stock) + " units left");
            holder.binding.tvInventoryStockBadge.setBackgroundResource(R.drawable.bg_badge_printing);
            holder.binding.tvInventoryStockBadge.setTextColor(ContextCompat.getColor(ctx, R.color.status_printing_text));
        } else {
            holder.binding.tvInventoryStockBadge.setText("📦 In Stock: " + SessionManager.formatNumber(stock) + " units");
            holder.binding.tvInventoryStockBadge.setBackgroundResource(R.drawable.bg_badge_ready);
            holder.binding.tvInventoryStockBadge.setTextColor(ContextCompat.getColor(ctx, R.color.status_ready_text));
        }

        // Image
        String img = p.getImageUrl();
        if (img != null && (img.startsWith("http://") || img.startsWith("https://"))) {
            Glide.with(ctx).load(img).placeholder(R.drawable.premium_card).into(holder.binding.ivInventoryProductImage);
        } else {
            int resourceId = img != null ? ctx.getResources().getIdentifier(img, "drawable", ctx.getPackageName()) : 0;
            Glide.with(ctx).load(resourceId != 0 ? resourceId : R.drawable.premium_card).into(holder.binding.ivInventoryProductImage);
        }

        holder.binding.btnQuickRestock25.setOnClickListener(v -> {
            if (restockListener != null) restockListener.onRestock(p, 25);
        });

        holder.binding.btnQuickRestock50.setOnClickListener(v -> {
            if (restockListener != null) restockListener.onRestock(p, 50);
        });

        holder.binding.btnEditInventoryProduct.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(p);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class InventoryViewHolder extends RecyclerView.ViewHolder {
        ItemInventoryAdminBinding binding;
        InventoryViewHolder(ItemInventoryAdminBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
