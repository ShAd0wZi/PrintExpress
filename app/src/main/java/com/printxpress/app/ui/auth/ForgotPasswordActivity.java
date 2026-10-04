package com.printxpress.app.ui.auth;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.printxpress.app.databinding.ActivityForgotPasswordBinding;
import com.printxpress.app.util.ValidationHelper;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvBackToLogin.setOnClickListener(v -> finish());

        binding.btnSendResetLink.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                binding.etEmail.setError("Email is required");
                binding.etEmail.requestFocus();
                return;
            }
            if (!ValidationHelper.isValidEmail(email)) {
                binding.etEmail.setError("Enter a valid email address");
                binding.etEmail.requestFocus();
                return;
            }

            com.printxpress.app.util.PrintXpressToast.success(this, "Password reset instructions sent to " + email);
            finish();
        });
    }
}
