package com.printxpress.app.data.repository;

import androidx.lifecycle.LiveData;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.dao.CategoryDao;
import com.printxpress.app.data.local.dao.ProductDao;
import com.printxpress.app.data.local.entity.Category;
import com.printxpress.app.data.local.entity.Product;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ProductRepository {
    
    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final ExecutorService executorService = PrintXpressDatabase.databaseWriteExecutor;
    
    public ProductRepository(ProductDao productDao, CategoryDao categoryDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
    }
    
    public LiveData<List<Category>> getAllCategories() {
        return categoryDao.getAllCategories();
    }
    
    public LiveData<List<Product>> getProductsByCategory(int categoryId) {
        return productDao.getProductsByCategory(categoryId);
    }
    
    public Product getProductById(int productId) {
        return productDao.getProductById(productId);
    }

    public LiveData<Product> getProductLiveData(int productId) {
        return productDao.getProductLiveData(productId);
    }
    
    public LiveData<List<Product>> getAllProducts() {
        return productDao.getAllProducts();
    }

    public LiveData<List<Product>> searchProducts(String searchTerm) {
        return productDao.searchProducts(searchTerm);
    }

    public void insertCategory(Category category) {
        executorService.execute(() -> categoryDao.insert(category));
    }

    public void updateCategory(Category category) {
        executorService.execute(() -> categoryDao.update(category));
    }

    public void deleteCategory(Category category) {
        executorService.execute(() -> categoryDao.delete(category));
    }

    public void insertProduct(Product product) {
        executorService.execute(() -> productDao.insert(product));
    }

    public void updateProduct(Product product) {
        executorService.execute(() -> productDao.update(product));
    }

    public void deleteProduct(Product product) {
        executorService.execute(() -> productDao.delete(product));
    }

    public LiveData<List<Product>> getLowStockProducts(int threshold) {
        return productDao.getLowStockProducts(threshold);
    }

    public void updateStock(int productId, int stockQuantity) {
        executorService.execute(() -> productDao.updateStock(productId, Math.max(0, stockQuantity)));
    }
}