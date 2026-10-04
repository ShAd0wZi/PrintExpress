package com.printxpress.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.printxpress.app.databinding.ActivitySavedAddressesBinding;
import com.printxpress.app.ui.adapter.AddressAdapter;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.viewModel.ProfileViewModel;
import java.util.ArrayList;

public class SavedAddressesActivity extends AppCompatActivity {
    
    private ActivitySavedAddressesBinding binding;
    private ProfileViewModel profileViewModel;
    private AddressAdapter adapter;
    private int userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }
        userId = SessionManager.getUserId(this);

        binding = ActivitySavedAddressesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        
        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        // Safeguarded deletion with confirmation dialog
        adapter = new AddressAdapter(new ArrayList<>(), address -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Delete Address")
                    .setMessage("Are you sure you want to delete this delivery address?")
                    .setPositiveButton("Delete", (d, which) -> {
                        profileViewModel.deleteAddress(address);
                        com.printxpress.app.util.PrintXpressToast.info(this, "Address deleted");
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                dialog.getWindow().setDimAmount(0.65f);
            }
            dialog.show();
        });
        binding.rvAddresses.setAdapter(adapter);
        
        profileViewModel.getAddressesByUserId(userId).observe(this, addresses -> {
            binding.pbLoading.setVisibility(android.view.View.GONE);
            if (addresses != null && !addresses.isEmpty()) {
                binding.tvEmptyAddresses.setVisibility(android.view.View.GONE);
                binding.rvAddresses.setVisibility(android.view.View.VISIBLE);
                adapter.setAddresses(addresses);
            } else {
                binding.tvEmptyAddresses.setVisibility(android.view.View.VISIBLE);
                binding.rvAddresses.setVisibility(android.view.View.GONE);
                adapter.setAddresses(new ArrayList<>());
            }
        });
        
        binding.btnAddAddress.setOnClickListener(v -> {
            startActivity(new Intent(SavedAddressesActivity.this, AddAddressActivity.class));
        });
    }
}