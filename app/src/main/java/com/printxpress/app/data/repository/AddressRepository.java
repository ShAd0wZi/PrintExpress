package com.printxpress.app.data.repository;

import androidx.lifecycle.LiveData;
import com.printxpress.app.data.local.dao.AddressDao;
import com.printxpress.app.data.local.entity.Address;
import java.util.List;

public class AddressRepository {

    private final AddressDao addressDao;

    public AddressRepository(AddressDao addressDao) {
        this.addressDao = addressDao;
    }

    public LiveData<List<Address>> getAddressesByUserId(int userId) {
        return addressDao.getAddressesByUserId(userId);
    }

    public Address getAddressById(int addressId) {
        return addressDao.getAddressById(addressId);
    }

    public Address getDefaultAddress(int userId) {
        return addressDao.getDefaultAddress(userId);
    }

    public long insertAddress(Address address) {
        if (address == null) return -1L;
        return addressDao.insertWithDefaultEnforcement(address);
    }

    public void updateAddress(Address address) {
        if (address == null) return;
        if (address.isDefault()) {
            addressDao.clearDefaultAddresses(address.getUserId());
        }
        addressDao.update(address);
    }

    public void deleteAddress(Address address) {
        if (address == null) return;
        addressDao.delete(address);
    }
}
