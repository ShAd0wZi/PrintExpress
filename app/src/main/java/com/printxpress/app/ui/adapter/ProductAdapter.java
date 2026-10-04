package com.printxpress.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.databinding.ItemProductBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnProductLongClickListener {
        void onProductLongClick(Product product);
    }

    private List<Product> products = new ArrayList<>();
    private final OnProductClickListener listener;
    private OnProductLongClickListener longClickListener;

    public ProductAdapter(List<Product> products, OnProductClickListener listener) {
        if (products != null) {
            this.products = products;
        }
        this.listener = listener;
    }

    public void setOnProductLongClickListener(OnProductLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setProducts(List<Product> newProducts) {
        this.products = newProducts != null ? newProducts : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.binding.tvProductName.setText(product.getName());
        holder.binding.tvProductDescription.setText(product.getDescription());
        String formattedPrice = com.printxpress.app.util.SessionManager.formatPrice(
                holder.itemView.getContext(), product.getBasePrice());
        holder.binding.tvProductPrice.setText("From " + formattedPrice);

        int stock = product.getStockQuantity();
        if (stock <= 0) {
            holder.binding.tvStockBadge.setText("Out of Stock (0)");
        } else if (stock <= 15) {
            holder.binding.tvStockBadge.setText("Low Stock (" + stock + " left)");
        } else {
            holder.binding.tvStockBadge.setText("In Stock (" + stock + ") • 24h");
        }
        
        String sizes = product.getAvailableSizes();
        if (sizes != null && !sizes.isEmpty()) {
            holder.binding.tvAvailableSizes.setText(sizes);
            holder.binding.tvAvailableSizes.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.tvAvailableSizes.setVisibility(android.view.View.GONE);
        }

        String img = product.getImageUrl();
        if (img != null && (img.startsWith("http://") || img.startsWith("https://"))) {
            Glide.with(holder.itemView.getContext())
                .load(img)
                .placeholder(R.drawable.premium_card)
                .into(holder.binding.ivProductImage);
        } else {
            int resourceId = img != null ? holder.itemView.getContext().getResources().getIdentifier(
                img, "drawable", holder.itemView.getContext().getPackageName()) : 0;
            if (resourceId != 0) {
                Glide.with(holder.itemView.getContext())
                    .load(resourceId)
                    .into(holder.binding.ivProductImage);
            } else {
                Glide.with(holder.itemView.getContext())
                    .load(R.drawable.premium_card)
                    .into(holder.binding.ivProductImage);
            }
        }

        // Spring tactile touch physics on product card
        com.printxpress.app.util.MotionUtils.applySpringTouch(holder.itemView);

        boolean isWishlisted = com.printxpress.app.util.SessionManager.isProductWishlisted(
                holder.itemView.getContext(), product.getProductId());
        holder.binding.ivWishlist.setImageResource(isWishlisted ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        holder.binding.ivWishlist.setColorFilter(isWishlisted
                ? androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.error)
                : androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.on_surface_variant));

        holder.binding.btnWishlist.setOnClickListener(v -> {
            boolean newState = com.printxpress.app.util.SessionManager.toggleProductWishlist(
                    holder.itemView.getContext(), product.getProductId());
            holder.binding.ivWishlist.setImageResource(newState ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            holder.binding.ivWishlist.setColorFilter(newState
                    ? androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.error)
                    : androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.on_surface_variant));
            com.printxpress.app.util.MotionUtils.animateHeartPop(holder.binding.ivWishlist, newState);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onProductLongClick(product);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ItemProductBinding binding;

        ProductViewHolder(@NonNull ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
