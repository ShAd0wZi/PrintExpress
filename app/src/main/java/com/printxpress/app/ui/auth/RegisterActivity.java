package com.printxpress.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.printxpress.app.databinding.ActivityRegisterBinding;
import com.printxpress.app.util.ValidationHelper;
import com.printxpress.app.viewModel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {
    
    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnRegister);
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvLogin.setOnClickListener(v -> finish());
        
        viewModel.getRegisterMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                if (message.toLowerCase().contains("email")) {
                    binding.etEmail.setError(message);
                    binding.etEmail.requestFocus();
                }
                if (message.contains("successful")) {
                    com.printxpress.app.util.PrintXpressToast.success(this, message);
                } else {
                    com.printxpress.app.util.PrintXpressToast.info(this, message);
                }
                viewModel.clearMessage();
                if (message.contains("successful")) {
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                }
            }
        });
    }
    
    private void attemptRegister() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        
        if (name.isEmpty()) {
            binding.etName.setError("Name is required");
            binding.etName.requestFocus();
            return;
        }
        
        if (!ValidationHelper.isValidEmail(email)) {
            binding.etEmail.setError("Enter a valid email");
            binding.etEmail.requestFocus();
            return;
        }
        
        if (!ValidationHelper.isValidPhone(phone)) {
            binding.etPhone.setError("Enter a valid phone number");
            binding.etPhone.requestFocus();
            return;
        }
        
        if (!ValidationHelper.isValidPassword(password)) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return;
        }
        
        viewModel.register(name, email, phone, password);
    }
}