package com.printxpress.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Promotion;
import com.printxpress.app.databinding.ItemCouponSelectableBinding;
import com.printxpress.app.util.SessionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SelectCouponAdapter extends RecyclerView.Adapter<SelectCouponAdapter.CouponViewHolder> {

    public interface OnCouponSelectedListener {
        void onCouponSelected(Promotion promotion);
    }

    private List<Promotion> promotions = new ArrayList<>();
    private final double cartSubtotal;
    private final OnCouponSelectedListener listener;

    public SelectCouponAdapter(List<Promotion> promotions, double cartSubtotal, OnCouponSelectedListener listener) {
        if (promotions != null) {
            this.promotions = promotions;
        }
        this.cartSubtotal = cartSubtotal;
        this.listener = listener;
    }

    public void setPromotions(List<Promotion> newPromotions) {
        this.promotions = newPromotions != null ? newPromotions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CouponViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCouponSelectableBinding binding = ItemCouponSelectableBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CouponViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CouponViewHolder holder, int position) {
        Promotion promo = promotions.get(position);
        Context context = holder.itemView.getContext();

        holder.binding.tvCouponCode.setText(promo.getCode());
        holder.binding.tvCouponDiscount.setText(
                String.format(Locale.getDefault(), "%.0f%% OFF", promo.getDiscountPercentage()));
        holder.binding.tvCouponTitle.setText(promo.getTitle());

        if (promo.getDescription() != null && !promo.getDescription().trim().isEmpty()) {
            holder.binding.tvCouponDescription.setText(promo.getDescription());
            holder.binding.tvCouponDescription.setVisibility(View.VISIBLE);
        } else {
            holder.binding.tvCouponDescription.setVisibility(View.GONE);
        }

        double minAmount = promo.getMinOrderAmount();
        if (minAmount > 0) {
            holder.binding.tvCouponMinOrder.setText("Min. spend: " + SessionManager.formatPrice(context, minAmount));
            if (cartSubtotal >= minAmount) {
                holder.binding.tvCouponEligibility.setText("✓ Eligible for this order");
                holder.binding.tvCouponEligibility.setTextColor(ContextCompat.getColor(context, R.color.primary_container));
                holder.binding.btnApplyCoupon.setEnabled(true);
                holder.binding.btnApplyCoupon.setAlpha(1.0f);
            } else {
                double diff = minAmount - cartSubtotal;
                holder.binding.tvCouponEligibility.setText("Add " + SessionManager.formatPrice(context, diff) + " more");
                holder.binding.tvCouponEligibility.setTextColor(ContextCompat.getColor(context, R.color.secondary));
                holder.binding.btnApplyCoupon.setEnabled(false);
                holder.binding.btnApplyCoupon.setAlpha(0.6f);
            }
        } else {
            holder.binding.tvCouponMinOrder.setText("No minimum spend");
            holder.binding.tvCouponEligibility.setText("✓ Eligible for this order");
            holder.binding.tvCouponEligibility.setTextColor(ContextCompat.getColor(context, R.color.primary_container));
            holder.binding.btnApplyCoupon.setEnabled(true);
            holder.binding.btnApplyCoupon.setAlpha(1.0f);
        }

        View.OnClickListener clickListener = v -> {
            if (listener != null) {
                listener.onCouponSelected(promo);
            }
        };

        holder.binding.btnApplyCoupon.setOnClickListener(clickListener);
        holder.binding.cardCoupon.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return promotions.size();
    }

    static class CouponViewHolder extends RecyclerView.ViewHolder {
        final ItemCouponSelectableBinding binding;

        CouponViewHolder(ItemCouponSelectableBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
