package com.printxpress.app.data.repository;

import android.database.sqlite.SQLiteConstraintException;
import com.printxpress.app.data.local.dao.UserDao;
import com.printxpress.app.data.local.entity.User;
import com.printxpress.app.util.SecurityHelper;

public class UserRepository {
    
    private final UserDao userDao;
    
    public UserRepository(UserDao userDao) {
        this.userDao = userDao;
    }
    
    public long register(User user) {
        if (user == null || user.getEmail() == null) {
            return -1L;
        }
        user.setEmail(user.getEmail().trim().toLowerCase());
        try {
            return userDao.insert(user);
        } catch (SQLiteConstraintException e) {
            return -1L;
        }
    }
    
    public User login(String email, String rawPassword) {
        if (email == null || rawPassword == null) {
            return null;
        }
        User user = userDao.getUserByEmail(email.trim().toLowerCase());
        if (user == null) {
            user = userDao.getUserByEmail(email.trim());
        }
        if (user != null && SecurityHelper.verifyPassword(rawPassword, user.getPasswordHash())) {
            return user;
        }
        return null;
    }
    
    public User getUserById(int userId) {
        return userDao.getUserById(userId);
    }
    
    public void updateUser(User user) {
        if (user != null) {
            userDao.update(user);
        }
    }
    
    public boolean isEmailRegistered(String email) {
        if (email == null) return false;
        return userDao.isEmailRegistered(email.trim().toLowerCase()) || userDao.isEmailRegistered(email.trim());
    }
}