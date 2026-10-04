package com.printxpress.app.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.printxpress.app.R;
import com.printxpress.app.data.local.entity.Address;
import com.printxpress.app.databinding.ActivityDeliverySelectionBinding;
import com.printxpress.app.util.Constants;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.viewModel.ProfileViewModel;
import java.util.ArrayList;
import java.util.List;

public class DeliverySelectionActivity extends AppCompatActivity {
    
    private ActivityDeliverySelectionBinding binding;
    private ProfileViewModel profileViewModel;
    private List<Address> savedAddresses = new ArrayList<>();
    private int singleCartItemId = -1;
    private int newlyAddedAddressId = -1;
    private boolean isHomeDeliverySelected = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }
        int userId = SessionManager.getUserId(this);
        singleCartItemId = getIntent().getIntExtra("SINGLE_CART_ITEM_ID", -1);

        binding = ActivityDeliverySelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnBack);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnNext);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.cardHomeDelivery);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.cardStorePickup);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnAddAddressDelivery);
        
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
                
        // Fetch saved addresses
        profileViewModel.getAddressesByUserId(userId).observe(this, addresses -> {
            if (addresses != null) {
                savedAddresses = addresses;
                List<String> addressStrings = new ArrayList<>();
                addressStrings.add("Select a delivery address...");
                int selectedPosition = 0;
                for (int i = 0; i < addresses.size(); i++) {
                    Address addr = addresses.get(i);
                    addressStrings.add(addr.getAddressLine1() + ", " + addr.getCity() + (addr.getPhone() != null ? " • " + addr.getPhone() : ""));
                    if (newlyAddedAddressId > 0 && addr.getAddressId() == newlyAddedAddressId) {
                        selectedPosition = i + 1;
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, addressStrings);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerAddress.setAdapter(adapter);
                if (selectedPosition > 0) {
                    binding.spinnerAddress.setSelection(selectedPosition);
                    newlyAddedAddressId = -1;
                } else if (!addresses.isEmpty()) {
                    binding.spinnerAddress.setSelection(1);
                }
            }
        });
        
        // Mutually exclusive delivery method selection
        selectDeliveryMethod(true);
        binding.cardHomeDelivery.setOnClickListener(v -> selectDeliveryMethod(true));
        binding.rbHomeDelivery.setOnClickListener(v -> selectDeliveryMethod(true));
        binding.cardStorePickup.setOnClickListener(v -> selectDeliveryMethod(false));
        binding.rbStorePickup.setOnClickListener(v -> selectDeliveryMethod(false));

        binding.btnAddAddressDelivery.setOnClickListener(v -> {
            startActivity(new Intent(this, com.printxpress.app.ui.profile.AddAddressActivity.class));
        });
        
        binding.btnNext.setOnClickListener(v -> {
            String deliveryMethod = isHomeDeliverySelected ? "Home Delivery" : "Store Pickup";
            int selectedAddressId = -1;
            
            if (isHomeDeliverySelected) {
                if (savedAddresses == null || savedAddresses.isEmpty()) {
                    com.printxpress.app.util.PrintXpressToast.info(this, "Please add a delivery address to continue.");
                    startActivity(new Intent(this, com.printxpress.app.ui.profile.AddAddressActivity.class));
                    return;
                }
                int spinnerPos = binding.spinnerAddress.getSelectedItemPosition();
                int addressIndex = spinnerPos - 1;
                if (spinnerPos <= 0 || addressIndex < 0 || addressIndex >= savedAddresses.size()) {
                    com.printxpress.app.util.PrintXpressToast.error(this, "Please select an address for home delivery");
                    return;
                }
                selectedAddressId = savedAddresses.get(addressIndex).getAddressId();
            }
            
            Intent intent = new Intent(DeliverySelectionActivity.this, OrderSummaryActivity.class);
            intent.putExtra("DELIVERY_METHOD", deliveryMethod);
            intent.putExtra("DELIVERY_ADDRESS_ID", selectedAddressId);
            intent.putExtra("SINGLE_CART_ITEM_ID", singleCartItemId);
            startActivity(intent);
        });
    }

    private void selectDeliveryMethod(boolean isHomeDelivery) {
        isHomeDeliverySelected = isHomeDelivery;
        binding.rbHomeDelivery.setChecked(isHomeDelivery);
        binding.rbStorePickup.setChecked(!isHomeDelivery);
        binding.llAddressSelection.setVisibility(isHomeDelivery ? View.VISIBLE : View.GONE);

        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary_container);
        int neutralColor = androidx.core.content.ContextCompat.getColor(this, R.color.outline_variant);
        int strokeWidthActive = (int) (2 * getResources().getDisplayMetrics().density);
        int strokeWidthNeutral = (int) (1 * getResources().getDisplayMetrics().density);

        binding.cardHomeDelivery.setStrokeColor(isHomeDelivery ? activeColor : neutralColor);
        binding.cardHomeDelivery.setStrokeWidth(isHomeDelivery ? strokeWidthActive : strokeWidthNeutral);
        binding.cardStorePickup.setStrokeColor(!isHomeDelivery ? activeColor : neutralColor);
        binding.cardStorePickup.setStrokeWidth(!isHomeDelivery ? strokeWidthActive : strokeWidthNeutral);
    }

    private void showAddAddressDialog(int userId) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        android.widget.EditText etLine1 = new android.widget.EditText(this);
        etLine1.setHint("Address Line 1 (Street / Building) *");
        layout.addView(etLine1);

        android.widget.EditText etLine2 = new android.widget.EditText(this);
        etLine2.setHint("Address Line 2 / Suite (Optional)");
        layout.addView(etLine2);

        android.widget.EditText etCity = new android.widget.EditText(this);
        etCity.setHint("City / District (e.g. Colombo, Kandy) *");
        layout.addView(etCity);

        android.widget.EditText etPostal = new android.widget.EditText(this);
        etPostal.setHint("Postal Code (Optional)");
        etPostal.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etPostal);

        android.widget.EditText etPhone = new android.widget.EditText(this);
        etPhone.setHint("Contact Phone (e.g. 0771234567) *");
        etPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add New Delivery Address")
                .setView(layout)
                .setPositiveButton("Save & Select", (dialog, which) -> {
                    String line1 = etLine1.getText().toString().trim();
                    String line2 = etLine2.getText().toString().trim();
                    String city = etCity.getText().toString().trim();
                    String postal = etPostal.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();

                    if (line1.isEmpty() || city.isEmpty()) {
                        com.printxpress.app.util.PrintXpressToast.error(this, "Street address and city are required.");
                        return;
                    }
                    if (phone.isEmpty()) {
                        phone = "0770000000";
                    }

                    Address address = new Address();
                    address.setUserId(userId);
                    address.setAddressLine1(line1);
                    address.setAddressLine2(line2);
                    address.setCity(city);
                    address.setPostalCode(postal);
                    address.setPhone(phone);
                    address.setDefault(true);

                    profileViewModel.addAddress(address);
                    com.printxpress.app.util.PrintXpressToast.success(this, "Delivery address saved and selected!");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}