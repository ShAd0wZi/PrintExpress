package com.printxpress.app.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Order;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    private List<Order> orders = new ArrayList<>();
    private final OnOrderClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public OrderHistoryAdapter(List<Order> orders, OnOrderClickListener listener) {
        if (orders != null) {
            this.orders = orders;
        }
        this.listener = listener;
    }

    public void setOrders(List<Order> newOrders) {
        this.orders = newOrders != null ? newOrders : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.tvOrderId.setText("Order #" + order.getOrderId());
        
        String status = order.getOrderStatus() != null ? order.getOrderStatus() : "PENDING";
        holder.tvOrderStatus.setText(status);
        
        // Status color coding preserving rounded badge background and dark mode contrast
        ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending_bg);
        int bgColorRes;
        int textColorRes;
        if ("COMPLETED".equalsIgnoreCase(status) || "READY".equalsIgnoreCase(status)) {
            bgColorRes = R.color.status_ready_bg;
            textColorRes = R.color.status_ready_text;
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            bgColorRes = R.color.status_cancelled_bg;
            textColorRes = R.color.status_cancelled_text;
        } else if ("PRINTING".equalsIgnoreCase(status)) {
            bgColorRes = R.color.status_printing_bg;
            textColorRes = R.color.status_printing_text;
        } else if ("PROCESSING".equalsIgnoreCase(status)) {
            bgColorRes = R.color.status_processing_bg;
            textColorRes = R.color.status_processing_text;
        } else {
            bgColorRes = R.color.status_pending_bg;
            textColorRes = R.color.status_pending_text;
        }
        holder.tvOrderStatus.setBackgroundResource(R.drawable.bg_badge_primary);
        ViewCompat.setBackgroundTintList(
                holder.tvOrderStatus,
                ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), bgColorRes))
        );
        holder.tvOrderStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), textColorRes));

        if (order.getOrderDate() != null) {
            holder.tvOrderDate.setText("Placed on: " + dateFormat.format(order.getOrderDate()));
        } else {
            holder.tvOrderDate.setText("Placed on: Recent");
        }

        String delivery = order.getDeliveryMethod() != null ? order.getDeliveryMethod() : "Standard";
        holder.tvDeliveryMethod.setText("Method: " + delivery);
        holder.tvTotalAmount.setText(com.printxpress.app.util.SessionManager.formatPrice(
                holder.itemView.getContext(), order.getTotalAmount()));

        com.printxpress.app.util.MotionUtils.applySpringTouch(holder.itemView);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOrderClick(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId;
        TextView tvOrderStatus;
        TextView tvOrderDate;
        TextView tvDeliveryMethod;
        TextView tvTotalAmount;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvDeliveryMethod = itemView.findViewById(R.id.tvDeliveryMethod);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
        }
    }
}
