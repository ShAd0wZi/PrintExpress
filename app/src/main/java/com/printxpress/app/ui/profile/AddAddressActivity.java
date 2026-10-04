package com.printxpress.app.ui.profile;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.printxpress.app.data.local.entity.Address;
import com.printxpress.app.databinding.ActivityAddAddressBinding;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.util.ValidationHelper;
import com.printxpress.app.viewModel.ProfileViewModel;

public class AddAddressActivity extends AppCompatActivity {
    
    private ActivityAddAddressBinding binding;
    private ProfileViewModel profileViewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }

        binding = ActivityAddAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        binding.btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                int userId = SessionManager.getUserId(this);

                Address address = new Address();
                address.setUserId(userId);
                address.setAddressLine1(binding.etAddressLine1.getText().toString().trim());
                address.setAddressLine2(binding.etAddressLine2.getText().toString().trim());
                address.setCity(binding.etCity.getText().toString().trim());
                address.setPostalCode(binding.etPostalCode.getText().toString().trim());
                address.setPhone(binding.etPhone.getText().toString().trim());
                address.setDefault(true);

                profileViewModel.addAddress(address);
                com.printxpress.app.util.PrintXpressToast.success(this, "Address saved successfully");
                finish();
            }
        });
    }
    
    private boolean validateInputs() {
        String address1 = binding.etAddressLine1.getText().toString().trim();
        String city = binding.etCity.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        
        if (address1.isEmpty()) {
            binding.etAddressLine1.setError("Address is required");
            return false;
        }
        
        if (city.isEmpty()) {
            binding.etCity.setError("City is required");
            return false;
        }
        
        if (!ValidationHelper.isValidPhone(phone)) {
            binding.etPhone.setError("Enter valid phone");
            return false;
        }
        
        return true;
    }
}