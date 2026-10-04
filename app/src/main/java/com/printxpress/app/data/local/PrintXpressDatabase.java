package com.printxpress.app.data.local;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.printxpress.app.data.local.dao.*;
import com.printxpress.app.data.local.entity.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
    entities = {
        User.class,
        Address.class,
        Category.class,
        Product.class,
        CartItem.class,
        Order.class,
        OrderItem.class,
        Notification.class,
        Promotion.class
    },
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters.class)
public abstract class PrintXpressDatabase extends RoomDatabase {
    
    private static volatile PrintXpressDatabase INSTANCE;

    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "PrintXpress-DB-Worker");
        t.setDaemon(true);
        return t;
    });
    
    public abstract UserDao userDao();
    public abstract AddressDao addressDao();
    public abstract CategoryDao categoryDao();
    public abstract ProductDao productDao();
    public abstract CartItemDao cartItemDao();
    public abstract OrderDao orderDao();
    public abstract OrderItemDao orderItemDao();
    public abstract NotificationDao notificationDao();
    public abstract PromotionDao promotionDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `promotion` (" +
                    "`promotionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`code` TEXT, " +
                    "`title` TEXT, " +
                    "`description` TEXT, " +
                    "`discountPercentage` REAL NOT NULL, " +
                    "`minOrderAmount` REAL NOT NULL, " +
                    "`validUntil` INTEGER, " +
                    "`isActive` INTEGER NOT NULL)");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_email` ON `user` (`email`)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_promotion_code` ON `promotion` (`code`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_order_table_orderDate` ON `order_table` (`orderDate`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_order_table_orderStatus` ON `order_table` (`orderStatus`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_isRead` ON `notification` (`isRead`)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `cart_items_new` (" +
                    "`cartItemId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`userId` INTEGER NOT NULL, " +
                    "`productId` INTEGER NOT NULL, " +
                    "`quantity` INTEGER NOT NULL, " +
                    "`selectedSize` TEXT, " +
                    "`selectedMaterial` TEXT, " +
                    "`artworkFilePath` TEXT, " +
                    "`customText` TEXT, " +
                    "`unitPrice` REAL NOT NULL, " +
                    "`subtotal` REAL NOT NULL, " +
                    "FOREIGN KEY(`userId`) REFERENCES `user`(`userId`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                    "FOREIGN KEY(`productId`) REFERENCES `product`(`productId`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("INSERT INTO `cart_items_new` (`cartItemId`, `userId`, `productId`, `quantity`, `selectedSize`, `selectedMaterial`, `artworkFilePath`, `customText`, `unitPrice`, `subtotal`) " +
                    "SELECT `cartItemId`, `userId`, `productId`, `quantity`, `selectedSize`, `selectedMaterial`, `artworkFilePath`, `customText`, `unitPrice`, `subtotal` FROM `cart_items` " +
                    "WHERE `userId` IN (SELECT `userId` FROM `user`) AND `productId` IN (SELECT `productId` FROM `product`)");
            database.execSQL("DROP TABLE `cart_items`");
            database.execSQL("ALTER TABLE `cart_items_new` RENAME TO `cart_items`");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_cart_items_userId` ON `cart_items` (`userId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_cart_items_productId` ON `cart_items` (`productId`)");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `product` ADD COLUMN `stockQuantity` INTEGER NOT NULL DEFAULT 100");
        }
    };
    
    public static synchronized PrintXpressDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                context.getApplicationContext(),
                PrintXpressDatabase.class,
                "printxpress_database"
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build();
        }
        return INSTANCE;
    }
}