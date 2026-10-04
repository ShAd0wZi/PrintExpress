package com.printxpress.app.data.repository;

import androidx.lifecycle.LiveData;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.dao.PromotionDao;
import com.printxpress.app.data.local.entity.Promotion;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class PromotionRepository {

    private final PromotionDao promotionDao;
    private final ExecutorService executorService = PrintXpressDatabase.databaseWriteExecutor;

    public PromotionRepository(PromotionDao promotionDao) {
        this.promotionDao = promotionDao;
    }

    public LiveData<List<Promotion>> getActivePromotions() {
        return promotionDao.getActivePromotions();
    }

    public LiveData<List<Promotion>> getAllPromotions() {
        return promotionDao.getAllPromotions();
    }

    public Promotion getPromotionByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        return promotionDao.getPromotionByCode(code.trim());
    }

    public void insertPromotion(Promotion promotion) {
        if (promotion == null) return;
        executorService.execute(() -> promotionDao.insert(promotion));
    }

    public void deletePromotion(Promotion promotion) {
        if (promotion == null) return;
        executorService.execute(() -> promotionDao.delete(promotion));
    }
}
