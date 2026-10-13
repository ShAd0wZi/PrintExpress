package com.printxpress.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import com.printxpress.app.databinding.ActivityLoginBinding;
import com.printxpress.app.ui.home.HomeActivity;
import com.printxpress.app.util.SessionManager;
import com.printxpress.app.viewModel.AuthViewModel;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {
    
    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        setupBiometricAuth();
        
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnLogin);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnSkip);

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        binding.tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        binding.btnSkip.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        
        viewModel.getLoginMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                if (message.toLowerCase().contains("invalid")) {
                    SessionManager.recordFailedLoginAttempt(this);
                    long lockoutSecs = SessionManager.getRemainingLockoutSeconds(this);
                    String detailMsg = lockoutSecs > 0
                            ? "Too many failed attempts! Account temporarily locked for " + lockoutSecs + "s."
                            : "Invalid email or password. Please try again.";
                    binding.etPassword.setError(detailMsg);
                    binding.etPassword.requestFocus();
                    com.printxpress.app.util.PrintXpressToast.error(this, detailMsg);
                } else {
                    com.printxpress.app.util.PrintXpressToast.info(this, message);
                }
                viewModel.clearMessage();
            }
        });
        
        viewModel.getLoggedInUser().observe(this, user -> {
            if (user != null) {
                SessionManager.resetFailedLoginAttempts(this);
                SessionManager.saveUserSession(this, user);
                if ("ADMIN".equalsIgnoreCase(user.getUserType())) {
                    com.printxpress.app.util.PrintXpressToast.success(this, "Welcome Admin! Opening Admin Dashboard...");
                    Intent intent = new Intent(this, com.printxpress.app.ui.admin.AdminDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                finish();
            }
        });
    }
    
    private void setupBiometricAuth() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            
            boolean hasLoggedInBefore = SessionManager.isLoggedIn(this) || SessionManager.hasLoggedInBefore(this);
            
            if (hasLoggedInBefore) {
                binding.llBiometric.setVisibility(View.VISIBLE);
                
                Executor executor = ContextCompat.getMainExecutor(this);
                BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        int bioUserId = SessionManager.getBiometricUserId(LoginActivity.this);
                        if (bioUserId != SessionManager.NO_USER) {
                            viewModel.loginByUserId(bioUserId);
                        } else {
                            com.printxpress.app.util.PrintXpressToast.info(LoginActivity.this, "Please log in with your password first.");
                        }
                    }
                    
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // Do not show errors if user intentionally cancelled or chose password
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON 
                                && errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errorCode != BiometricPrompt.ERROR_CANCELED) {
                            com.printxpress.app.util.PrintXpressToast.error(LoginActivity.this, "Biometric: " + errString);
                        }
                    }
                });
                
                BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Biometric Login")
                        .setSubtitle("Log in using your fingerprint or face")
                        .setNegativeButtonText("Use account password")
                        .build();
                        
                binding.llBiometric.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));
            }
        }
    }
    
    private void attemptLogin() {
        long lockoutSeconds = SessionManager.getRemainingLockoutSeconds(this);
        if (lockoutSeconds > 0) {
            String lockMsg = "Account temporarily locked. Try again in " + lockoutSeconds + "s.";
            binding.etPassword.setError(lockMsg);
            com.printxpress.app.util.PrintXpressToast.error(this, lockMsg);
            return;
        }

        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        
        if (email.isEmpty()) {
            binding.etEmail.setError("Email is required");
            binding.etEmail.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            binding.etPassword.setError("Password is required");
            binding.etPassword.requestFocus();
            return;
        }
        
        viewModel.login(email, password);
    }
}