package com.printxpress.app.viewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.printxpress.app.PrintXpressApp;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Address;
import com.printxpress.app.data.local.entity.User;
import com.printxpress.app.data.repository.AddressRepository;
import com.printxpress.app.data.repository.UserRepository;
import com.printxpress.app.di.AppContainer;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final ExecutorService executorService = PrintXpressDatabase.databaseWriteExecutor;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        AppContainer container = PrintXpressApp.getAppContainer(application);
        this.userRepository = container.getUserRepository();
        this.addressRepository = container.getAddressRepository();
    }

    public void loadUser(int userId) {
        executorService.execute(() -> {
            User user = userRepository.getUserById(userId);
            currentUser.postValue(user);
        });
    }

    public void updateUser(User user) {
        if (user == null || user.getUserId() <= 0) {
            message.postValue("Cannot update invalid user profile");
            return;
        }
        executorService.execute(() -> {
            User existing = userRepository.getUserById(user.getUserId());
            if (existing == null) {
                message.postValue("User account not found");
                return;
            }
            existing.setName(user.getName());
            existing.setEmail(user.getEmail());
            existing.setPhone(user.getPhone());
            userRepository.updateUser(existing);
            currentUser.postValue(existing);
            message.postValue("Profile updated successfully");
        });
    }

    public void updateUserProfile(int userId, String name, String email, String phone) {
        updateUserProfile(userId, name, email, phone, null);
    }

    public void updateUserProfile(int userId, String name, String email, String phone, String newPassword) {
        if (userId <= 0) {
            message.postValue("Invalid user session");
            return;
        }
        executorService.execute(() -> {
            User existing = userRepository.getUserById(userId);
            if (existing == null) {
                message.postValue("User account not found");
                return;
            }
            existing.setName(name != null ? name.trim() : existing.getName());
            existing.setEmail(email != null ? email.trim().toLowerCase() : existing.getEmail());
            existing.setPhone(phone != null ? phone.trim() : existing.getPhone());
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                existing.setPasswordHash(com.printxpress.app.util.SecurityHelper.hashPassword(newPassword.trim()));
            }
            userRepository.updateUser(existing);
            currentUser.postValue(existing);
            message.postValue("Profile updated successfully");
        });
    }

    public LiveData<List<Address>> getAddressesByUserId(int userId) {
        return addressRepository.getAddressesByUserId(userId);
    }

    public void addAddress(Address address) {
        executorService.execute(() -> {
            long id = addressRepository.insertAddress(address);
            if (id > 0) {
                message.postValue("Address added successfully");
            } else {
                message.postValue("Failed to add address");
            }
        });
    }

    public void deleteAddress(Address address) {
        executorService.execute(() -> {
            addressRepository.deleteAddress(address);
            message.postValue("Address removed successfully");
        });
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<String> getMessage() {
        return message;
    }
}
