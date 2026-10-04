package com.printxpress.app.ui.profile;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.printxpress.app.data.local.entity.User;
import com.printxpress.app.databinding.ActivityEditProfileBinding;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.util.ValidationHelper;
import com.printxpress.app.viewModel.ProfileViewModel;

public class EditProfileActivity extends AppCompatActivity {
    
    private ActivityEditProfileBinding binding;
    private ProfileViewModel profileViewModel;
    private int userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.requireLogin(this)) {
            finish();
            return;
        }

        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> finish());
        
        userId = SessionManager.getUserId(this);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        
        String currentName = SessionManager.getUserName(this);
        binding.etName.setText(currentName);
        binding.etEmail.setText(SessionManager.getUserEmail(this));
        if (currentName != null && !currentName.isEmpty()) {
            binding.tvAvatarInitials.setText(currentName.substring(0, 1).toUpperCase());
        }
        
        profileViewModel.loadUser(userId);
        profileViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                if (binding.etName.getText().toString().isEmpty()) binding.etName.setText(user.getName());
                if (binding.etEmail.getText().toString().isEmpty()) binding.etEmail.setText(user.getEmail());
                if (binding.etPhone.getText().toString().isEmpty() && user.getPhone() != null) {
                    binding.etPhone.setText(user.getPhone());
                }
                if (user.getName() != null && !user.getName().isEmpty()) {
                    binding.tvAvatarInitials.setText(user.getName().substring(0, 1).toUpperCase());
                }
            }
        });
        
        binding.btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                String newName = binding.etName.getText().toString().trim();
                String newEmail = binding.etEmail.getText().toString().trim();
                String newPhone = binding.etPhone.getText().toString().trim();
                String newPassword = binding.etNewPassword != null ? binding.etNewPassword.getText().toString().trim() : "";
                
                profileViewModel.updateUserProfile(userId, newName, newEmail, newPhone, newPassword.isEmpty() ? null : newPassword);
                
                User sessionUser = new User();
                sessionUser.setUserId(userId);
                sessionUser.setName(newName);
                sessionUser.setEmail(newEmail);
                sessionUser.setPhone(newPhone);
                sessionUser.setUserType(SessionManager.getUserType(this));
                SessionManager.saveUserSession(this, sessionUser);
                
                if (binding.etNewPassword != null) {
                    binding.etNewPassword.setText("");
                }
                
                com.printxpress.app.util.PrintXpressToast.success(this, "Profile updated successfully");
                finish();
            }
        });
    }
    
    private boolean validateInputs() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String newPassword = binding.etNewPassword != null ? binding.etNewPassword.getText().toString().trim() : "";
        
        if (name.isEmpty()) {
            binding.etName.setError("Name is required");
            return false;
        }
        
        if (!ValidationHelper.isValidEmail(email)) {
            binding.etEmail.setError("Enter valid email");
            return false;
        }
        
        if (!ValidationHelper.isValidPhone(phone)) {
            binding.etPhone.setError("Enter valid phone");
            return false;
        }
        
        if (!newPassword.isEmpty() && !ValidationHelper.isValidPassword(newPassword)) {
            binding.etNewPassword.setError("Password must be at least 6 characters");
            return false;
        }

        return true;
    }
}