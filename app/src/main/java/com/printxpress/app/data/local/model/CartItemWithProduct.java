package com.printxpress.app.data.local.model;

import androidx.room.Embedded;
import androidx.room.Relation;
import com.printxpress.app.data.local.entity.CartItem;
import com.printxpress.app.data.local.entity.Product;

public class CartItemWithProduct {
    @Embedded
    public CartItem cartItem;
    
    @Relation(
         parentColumn = "productId",
         entityColumn = "productId"
    )
    public Product product;
}
