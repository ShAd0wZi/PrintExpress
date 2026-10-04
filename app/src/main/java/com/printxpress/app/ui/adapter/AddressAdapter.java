package com.printxpress.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Address;
import java.util.ArrayList;
import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    public interface OnAddressDeleteListener {
        void onDeleteClick(Address address);
    }

    private List<Address> addresses = new ArrayList<>();
    private final OnAddressDeleteListener deleteListener;

    public AddressAdapter(List<Address> addresses, OnAddressDeleteListener deleteListener) {
        if (addresses != null) {
            this.addresses = addresses;
        }
        this.deleteListener = deleteListener;
    }

    public void setAddresses(List<Address> newAddresses) {
        this.addresses = newAddresses != null ? newAddresses : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        Address address = addresses.get(position);
        holder.tvAddressLine1.setText(address.getAddressLine1());
        
        String line2 = address.getAddressLine2();
        if (line2 != null && !line2.isEmpty()) {
            holder.tvAddressLine2.setText(line2);
            holder.tvAddressLine2.setVisibility(View.VISIBLE);
        } else {
            holder.tvAddressLine2.setVisibility(View.GONE);
        }

        String cityPostal = (address.getCity() != null ? address.getCity() : "") + 
                (address.getPostalCode() != null && !address.getPostalCode().isEmpty() ? " - " + address.getPostalCode() : "");
        holder.tvCityPostal.setText(cityPostal);

        String phone = address.getPhone();
        if (phone != null && !phone.isEmpty()) {
            holder.tvPhone.setText("Phone: " + phone);
            holder.tvPhone.setVisibility(View.VISIBLE);
        } else {
            holder.tvPhone.setVisibility(View.GONE);
        }

        holder.btnDeleteAddress.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(address);
            }
        });
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView tvAddressLine1;
        TextView tvAddressLine2;
        TextView tvCityPostal;
        TextView tvPhone;
        Button btnDeleteAddress;

        AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAddressLine1 = itemView.findViewById(R.id.tvAddressLine1);
            tvAddressLine2 = itemView.findViewById(R.id.tvAddressLine2);
            tvCityPostal = itemView.findViewById(R.id.tvCityPostal);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            btnDeleteAddress = itemView.findViewById(R.id.btnDeleteAddress);
        }
    }
}
