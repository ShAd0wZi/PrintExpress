package com.printxpress.app.di;

import android.content.Context;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.repository.AddressRepository;
import com.printxpress.app.data.repository.CartRepository;
import com.printxpress.app.data.repository.NotificationRepository;
import com.printxpress.app.data.repository.OrderRepository;
import com.printxpress.app.data.repository.ProductRepository;
import com.printxpress.app.data.repository.PromotionRepository;
import com.printxpress.app.data.repository.UserRepository;

/**
 * Application-scoped Dependency Injection container providing shared singletons
 * for the database and all repositories across ViewModels and Activities.
 */
public class AppContainer {

    private final PrintXpressDatabase database;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final PromotionRepository promotionRepository;
    private final NotificationRepository notificationRepository;

    public AppContainer(Context context) {
        this.database = PrintXpressDatabase.getDatabase(context.getApplicationContext());
        this.userRepository = new UserRepository(database.userDao());
        this.productRepository = new ProductRepository(database.productDao(), database.categoryDao());
        this.cartRepository = new CartRepository(database.cartItemDao());
        this.orderRepository = new OrderRepository(database);
        this.addressRepository = new AddressRepository(database.addressDao());
        this.promotionRepository = new PromotionRepository(database.promotionDao());
        this.notificationRepository = new NotificationRepository(database.notificationDao());
    }

    public PrintXpressDatabase getDatabase() {
        return database;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public ProductRepository getProductRepository() {
        return productRepository;
    }

    public CartRepository getCartRepository() {
        return cartRepository;
    }

    public OrderRepository getOrderRepository() {
        return orderRepository;
    }

    public AddressRepository getAddressRepository() {
        return addressRepository;
    }

    public PromotionRepository getPromotionRepository() {
        return promotionRepository;
    }

    public NotificationRepository getNotificationRepository() {
        return notificationRepository;
    }
}
