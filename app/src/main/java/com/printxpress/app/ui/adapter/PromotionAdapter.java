package com.printxpress.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Promotion;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.PromotionViewHolder> {

    public interface OnPromotionClickListener {
        void onPromotionClick(Promotion promotion);
        void onPromotionLongClick(Promotion promotion);
    }

    private List<Promotion> promotions = new ArrayList<>();
    private final OnPromotionClickListener listener;

    public PromotionAdapter(List<Promotion> promotions, OnPromotionClickListener listener) {
        if (promotions != null) {
            this.promotions = promotions;
        }
        this.listener = listener;
    }

    public void setPromotions(List<Promotion> newPromotions) {
        this.promotions = newPromotions != null ? newPromotions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promotion, parent, false);
        return new PromotionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
        Promotion promo = promotions.get(position);
        holder.tvPromoDiscount.setText(String.format(Locale.getDefault(), "%.0f%% OFF", promo.getDiscountPercentage()));
        holder.tvPromoTitle.setText(promo.getTitle());
        holder.tvPromoCode.setText("CODE: " + promo.getCode());

        String desc = promo.getDescription();
        if (promo.getMinOrderAmount() > 0) {
            desc += String.format(Locale.getDefault(), "\nMin. Order: Rs. %,.2f • Tap to copy code", promo.getMinOrderAmount());
        }
        holder.tvPromoDescription.setText(desc);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPromotionClick(promo);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onPromotionLongClick(promo);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return promotions.size();
    }

    static class PromotionViewHolder extends RecyclerView.ViewHolder {
        TextView tvPromoDiscount;
        TextView tvPromoTitle;
        TextView tvPromoCode;
        TextView tvPromoDescription;

        PromotionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPromoDiscount = itemView.findViewById(R.id.tvPromoDiscount);
            tvPromoTitle = itemView.findViewById(R.id.tvPromoTitle);
            tvPromoCode = itemView.findViewById(R.id.tvPromoCode);
            tvPromoDescription = itemView.findViewById(R.id.tvPromoDescription);
        }
    }
}
