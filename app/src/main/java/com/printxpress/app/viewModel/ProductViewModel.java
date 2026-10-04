package com.printxpress.app.viewModel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.printxpress.app.PrintXpressApp;
import com.printxpress.app.data.local.entity.Category;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.data.repository.ProductRepository;
import java.util.List;

public class ProductViewModel extends AndroidViewModel {
    
    private final ProductRepository repository;
    
    public ProductViewModel(@NonNull Application application) {
        super(application);
        repository = PrintXpressApp.getAppContainer(application).getProductRepository();
    }
    
    public LiveData<List<Category>> getAllCategories() {
        return repository.getAllCategories();
    }
    
    public LiveData<List<Product>> getProductsByCategory(int categoryId) {
        return repository.getProductsByCategory(categoryId);
    }

    public LiveData<Product> getProductLiveData(int productId) {
        return repository.getProductLiveData(productId);
    }

    public LiveData<List<Product>> getAllProducts() {
        return repository.getAllProducts();
    }

    public LiveData<List<Product>> searchProducts(String searchTerm) {
        return repository.searchProducts(searchTerm);
    }

    public void insertCategory(Category category) {
        repository.insertCategory(category);
    }

    public void updateCategory(Category category) {
        repository.updateCategory(category);
    }

    public void deleteCategory(Category category) {
        repository.deleteCategory(category);
    }

    public void insertProduct(Product product) {
        repository.insertProduct(product);
    }

    public void updateProduct(Product product) {
        repository.updateProduct(product);
    }

    public void deleteProduct(Product product) {
        repository.deleteProduct(product);
    }

    public LiveData<List<Product>> getLowStockProducts(int threshold) {
        return repository.getLowStockProducts(threshold);
    }

    public void updateStock(int productId, int stockQuantity) {
        repository.updateStock(productId, stockQuantity);
    }
}