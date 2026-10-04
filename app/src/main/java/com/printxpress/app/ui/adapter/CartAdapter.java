package com.printxpress.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.printxpress.app.R;
import com.printxpress.app.data.local.model.CartItemWithProduct;
import com.printxpress.app.databinding.ItemCartBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnCartItemRemoveListener {
        void onRemove(CartItemWithProduct item);
    }

    public interface OnQuantityChangeListener {
        void onQuantityChange(CartItemWithProduct item, int newQuantity);
    }

    public interface OnCheckoutSingleItemListener {
        void onCheckoutSingle(CartItemWithProduct item);
    }

    private List<CartItemWithProduct> items = new ArrayList<>();
    private final OnCartItemRemoveListener removeListener;
    private OnQuantityChangeListener quantityChangeListener;
    private OnCheckoutSingleItemListener checkoutSingleListener;

    public CartAdapter(List<CartItemWithProduct> items, OnCartItemRemoveListener removeListener) {
        if (items != null) this.items = items;
        this.removeListener = removeListener;
    }

    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.quantityChangeListener = listener;
    }

    public void setOnCheckoutSingleItemListener(OnCheckoutSingleItemListener listener) {
        this.checkoutSingleListener = listener;
    }

    public void setItems(List<CartItemWithProduct> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItemWithProduct item = items.get(position);
        
        if (item.product != null) {
            holder.binding.tvProductName.setText(item.product.getName());
            int resourceId = holder.itemView.getContext().getResources().getIdentifier(
                item.product.getImageUrl(), "drawable", holder.itemView.getContext().getPackageName());
            if (resourceId != 0) {
                Glide.with(holder.itemView.getContext()).load(resourceId).into(holder.binding.ivProductThumb);
            } else {
                Glide.with(holder.itemView.getContext()).load(R.color.primary_light).into(holder.binding.ivProductThumb);
            }
        }
        
        String specs = String.format("Size: %s | %s", item.cartItem.getSelectedSize(), item.cartItem.getSelectedMaterial());
        holder.binding.tvProductSpecs.setText(specs);
        holder.binding.tvPrice.setText(com.printxpress.app.util.SessionManager.formatPrice(
                holder.itemView.getContext(), item.cartItem.getSubtotal()));
        
        com.printxpress.app.util.MotionUtils.applySpringTouch(holder.itemView);
        com.printxpress.app.util.MotionUtils.applySpringTouch(holder.binding.btnRemove);
        com.printxpress.app.util.MotionUtils.applySpringTouch(holder.binding.btnQtyMinus);
        com.printxpress.app.util.MotionUtils.applySpringTouch(holder.binding.btnQtyPlus);

        int currentQty = item.cartItem != null ? item.cartItem.getQuantity() : 1;

        if (quantityChangeListener != null) {
            holder.binding.btnQtyMinus.setVisibility(android.view.View.VISIBLE);
            holder.binding.btnQtyPlus.setVisibility(android.view.View.VISIBLE);
            holder.binding.btnRemove.setVisibility(android.view.View.VISIBLE);
            holder.binding.tvQty.setText(String.valueOf(currentQty));

            holder.binding.btnQtyMinus.setOnClickListener(v -> {
                if (currentQty <= 1) {
                    androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(holder.itemView.getContext())
                            .setTitle("Remove Print Job")
                            .setMessage("Remove \"" + (item.product != null ? item.product.getName() : "this print item") + "\" from your cart?")
                            .setPositiveButton("Remove", (d, which) -> {
                                if (removeListener != null) removeListener.onRemove(item);
                            })
                            .setNegativeButton("Keep Item", null)
                            .create();
                    if (dialog.getWindow() != null) {
                        dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                        dialog.getWindow().setDimAmount(0.65f);
                    }
                    dialog.show();
                } else {
                    int step = currentQty > 50 ? 10 : 1;
                    int newQty = Math.max(1, currentQty - step);
                    quantityChangeListener.onQuantityChange(item, newQty);
                }
            });

            holder.binding.btnQtyPlus.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                int step = currentQty >= 50 ? 10 : 1;
                int newQty = Math.min(1000, currentQty + step);
                quantityChangeListener.onQuantityChange(item, newQty);
            });

            holder.binding.tvQty.setOnClickListener(v -> {
                android.content.Context ctx = holder.itemView.getContext();
                android.widget.FrameLayout container = new android.widget.FrameLayout(ctx);
                int padH = (int) (24 * ctx.getResources().getDisplayMetrics().density);
                int padV = (int) (12 * ctx.getResources().getDisplayMetrics().density);
                container.setPadding(padH, padV, padH, padV);

                android.widget.EditText input = new android.widget.EditText(ctx);
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                input.setText(String.valueOf(currentQty));
                input.setSelection(input.getText().length());
                input.setGravity(android.view.Gravity.CENTER);
                input.setBackgroundResource(com.printxpress.app.R.drawable.bg_search_box);
                input.setPadding(padH / 2, padV, padH / 2, padV);
                input.setTextSize(16);
                input.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, com.printxpress.app.R.color.on_surface));
                container.addView(input);

                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(ctx)
                        .setTitle("Update Batch Quantity")
                        .setMessage("Enter desired print run (1 - 1,000 units):")
                        .setView(container)
                        .setPositiveButton("Update", (d, which) -> {
                            try {
                                int val = Integer.parseInt(input.getText().toString().trim());
                                if (val > 0 && val <= 1000) {
                                    quantityChangeListener.onQuantityChange(item, val);
                                } else {
                                    com.printxpress.app.util.PrintXpressToast.error(ctx, "Quantity must be between 1 and 1,000 units");
                                }
                            } catch (Exception ignored) {}
                        })
                        .setNegativeButton("Cancel", null)
                        .create();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    dialog.getWindow().setDimAmount(0.65f);
                }
                dialog.show();
            });
        } else {
            // Read-only summary mode
            holder.binding.btnQtyMinus.setVisibility(android.view.View.GONE);
            holder.binding.btnQtyPlus.setVisibility(android.view.View.GONE);
            holder.binding.btnRemove.setVisibility(android.view.View.GONE);
            holder.binding.tvQty.setText("Qty: " + currentQty);
            holder.binding.tvQty.setOnClickListener(null);
        }

        if (checkoutSingleListener != null) {
            holder.binding.llCheckoutSingle.setVisibility(android.view.View.VISIBLE);
            holder.binding.btnCheckoutSingle.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                checkoutSingleListener.onCheckoutSingle(item);
            });
            com.printxpress.app.util.MotionUtils.applySpringTouch(holder.binding.btnCheckoutSingle);
        } else {
            holder.binding.llCheckoutSingle.setVisibility(android.view.View.GONE);
        }

        holder.binding.btnRemove.setOnClickListener(v -> {
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Remove Print Job")
                    .setMessage("Remove \"" + (item.product != null ? item.product.getName() : "this print item") + "\" from your cart?")
                    .setPositiveButton("Remove", (d, which) -> {
                        if (removeListener != null) removeListener.onRemove(item);
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                dialog.getWindow().setDimAmount(0.65f);
            }
            dialog.show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ItemCartBinding binding;

        CartViewHolder(@NonNull ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
