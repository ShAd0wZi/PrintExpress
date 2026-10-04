package com.printxpress.app.viewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.printxpress.app.PrintXpressApp;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.User;
import com.printxpress.app.data.repository.UserRepository;
import com.printxpress.app.util.SecurityHelper;
import java.util.Date;
import java.util.concurrent.ExecutorService;

public class AuthViewModel extends AndroidViewModel {
    
    private final UserRepository repository;
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> loginMessage = new MutableLiveData<>();
    private final MutableLiveData<String> registerMessage = new MutableLiveData<>();
    private final MutableLiveData<User> loggedInUser = new MutableLiveData<>();
    private final ExecutorService executorService = PrintXpressDatabase.databaseWriteExecutor;
    
    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = PrintXpressApp.getAppContainer(application).getUserRepository();
    }
    
    public void register(String name, String email, String phone, String password) {
        executorService.execute(() -> {
            String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
            if (repository.isEmailRegistered(normalizedEmail)) {
                String err = "Email already registered";
                registerMessage.postValue(err);
                message.postValue(err);
                return;
            }
            
            User user = new User();
            user.setName(name != null ? name.trim() : "");
            user.setEmail(normalizedEmail);
            user.setPhone(phone != null ? phone.trim() : "");
            user.setPasswordHash(SecurityHelper.hashPassword(password));
            user.setRegistrationDate(new Date());
            user.setUserType("CUSTOMER");
            
            long result = repository.register(user);
            String msg = result > 0 ? "Registration successful" : "Registration failed";
            registerMessage.postValue(msg);
            message.postValue(msg);
        });
    }
    
    public void login(String email, String password) {
        executorService.execute(() -> {
            User user = repository.login(email, password);
            if (user != null) {
                loggedInUser.postValue(user);
                String msg = "Login successful";
                loginMessage.postValue(msg);
                message.postValue(msg);
            } else {
                String err = "Invalid email or password";
                loginMessage.postValue(err);
                message.postValue(err);
            }
        });
    }

    public void loginByUserId(int userId) {
        executorService.execute(() -> {
            User user = repository.getUserById(userId);
            if (user != null) {
                loggedInUser.postValue(user);
                String msg = "Authentication succeeded!";
                loginMessage.postValue(msg);
                message.postValue(msg);
            } else {
                String err = "Saved account not found. Please log in with password.";
                loginMessage.postValue(err);
                message.postValue(err);
            }
        });
    }
    
    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<String> getLoginMessage() {
        return loginMessage;
    }

    public LiveData<String> getRegisterMessage() {
        return registerMessage;
    }

    public void clearMessage() {
        message.setValue(null);
        loginMessage.setValue(null);
        registerMessage.setValue(null);
    }
    
    public LiveData<User> getLoggedInUser() {
        return loggedInUser;
    }
}