package com.printxpress.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Category;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public interface OnCategoryLongClickListener {
        void onCategoryLongClick(Category category);
    }

    private List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private OnCategoryLongClickListener longClickListener;

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        if (categories != null) {
            this.categories = categories;
        }
        this.listener = listener;
    }

    public void setOnCategoryLongClickListener(OnCategoryLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setCategories(List<Category> newCategories) {
        this.categories = newCategories != null ? newCategories : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvCategoryName.setText(category.getName());
        holder.tvCategoryDescription.setText(category.getDescription());

        if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
            int resId = holder.itemView.getContext().getResources().getIdentifier(
                    category.getImageUrl(), "drawable", holder.itemView.getContext().getPackageName());
            if (resId != 0) {
                holder.ivCategoryIcon.setImageResource(resId);
            } else {
                holder.ivCategoryIcon.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            holder.ivCategoryIcon.setImageResource(R.drawable.ic_launcher_foreground);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onCategoryLongClick(category);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategoryName;
        TextView tvCategoryDescription;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCategoryDescription = itemView.findViewById(R.id.tvCategoryDescription);
        }
    }
}
