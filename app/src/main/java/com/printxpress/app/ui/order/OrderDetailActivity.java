package com.printxpress.app.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.printxpress.app.data.local.PrintXpressDatabase;
import com.printxpress.app.data.local.entity.Address;
import com.printxpress.app.data.local.entity.Notification;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.data.local.entity.OrderItem;
import com.printxpress.app.data.local.entity.Product;
import com.printxpress.app.databinding.ActivityOrderDetailBinding;
import com.printxpress.app.viewModel.OrderViewModel;
import android.view.View;
import com.printxpress.app.databinding.ItemOrderDetailSpecBinding;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity {
    
    private ActivityOrderDetailBinding binding;
    private OrderViewModel orderViewModel;
    private PrintXpressDatabase db;
    private int orderId;
    private String status;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        db = PrintXpressDatabase.getDatabase(this);
        binding.btnBack.setOnClickListener(v -> finish());
        
        orderId = getIntent().getIntExtra("ORDER_ID", 0);
        status = getIntent().getStringExtra("ORDER_STATUS");
        if (status == null) status = "PENDING";
        
        binding.tvOrderId.setText("Order #" + orderId);
        boolean isAdminInitial = com.printxpress.app.util.SessionManager.isAdmin(this);
        binding.tvStatus.setText("Status: " + status + (isAdminInitial ? " ✎" : ""));
        
        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);
        
        // Observe live Order entity from SQLite database
        db.orderDao().getOrderLiveData(orderId).observe(this, order -> {
            if (order != null) {
                int currentUserId = com.printxpress.app.util.SessionManager.getUserId(this);
                if (!com.printxpress.app.util.SessionManager.isAdmin(this) && order.getCustomerId() != currentUserId) {
                    com.printxpress.app.util.PrintXpressToast.error(this, "Unauthorized access to order details.");
                    finish();
                    return;
                }
                status = order.getOrderStatus();
                boolean isAdmin = com.printxpress.app.util.SessionManager.isAdmin(this);
                binding.tvStatus.setText("Status: " + status + (isAdmin ? " ✎" : ""));
                loadOrderDetails(order);
            }
        });

        // Allow updating production status in the database ONLY for Admins
        binding.tvStatus.setOnClickListener(v -> {
            if (com.printxpress.app.util.SessionManager.isAdmin(this)) {
                showUpdateStatusDialog();
            }
        });
        
        binding.btnTrackOrder.setOnClickListener(v -> {
            Intent intent = new Intent(OrderDetailActivity.this, OrderTrackingActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            intent.putExtra("ORDER_STATUS", status);
            startActivity(intent);
        });

        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnTrackOrder);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnReorder);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnDownloadReceipt);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnShareReceipt);
        com.printxpress.app.util.MotionUtils.applySpringTouch(binding.btnCancelOrder);

        binding.btnReorder.setOnClickListener(v -> reorderItems());
        binding.btnDownloadReceipt.setOnClickListener(v -> generateAndOpenPdfReceipt());
        binding.btnShareReceipt.setOnClickListener(v -> sharePdfReceipt());
        
        binding.btnCancelOrder.setOnClickListener(v -> {
            if ("COMPLETED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                com.printxpress.app.util.PrintXpressToast.error(this, "Order cannot be cancelled in its current state.");
                return;
            }
            
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Cancel Order")
                    .setMessage("Are you sure you want to cancel this order?")
                    .setPositiveButton("Yes, Cancel", (d, which) -> {
                        orderViewModel.cancelOrder(orderId);
                        com.printxpress.app.util.PrintXpressToast.success(this, "Order cancelled successfully.");
                    })
                    .setNegativeButton("No", null)
                    .create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                dialog.getWindow().setDimAmount(0.65f);
            }
            dialog.show();
        });
    }

    private void reorderItems() {
        binding.btnReorder.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        com.printxpress.app.util.PrintXpressToast.info(this, "Loading items into cart...");
        PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
            List<OrderItem> items = db.orderItemDao().getOrderItemsByOrderId(orderId);
            if (items != null && !items.isEmpty()) {
                int currentUserId = com.printxpress.app.util.SessionManager.getUserId(this);
                for (OrderItem item : items) {
                    com.printxpress.app.data.local.entity.CartItem cartItem = new com.printxpress.app.data.local.entity.CartItem();
                    cartItem.setUserId(currentUserId);
                    cartItem.setProductId(item.getProductId());
                    cartItem.setQuantity(item.getQuantity());
                    cartItem.setCustomText(item.getCustomisationDetails());
                    cartItem.setArtworkFilePath(item.getArtworkFilePath());
                    cartItem.setUnitPrice(item.getUnitPrice());
                    cartItem.setSubtotal(item.getSubtotal());
                    db.cartItemDao().insert(cartItem);
                }
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    com.printxpress.app.util.PrintXpressToast.success(this, "Print items reordered and added to Studio Cart!");
                    Intent intent = new Intent(OrderDetailActivity.this, CartActivity.class);
                    startActivity(intent);
                });
            } else {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    com.printxpress.app.util.PrintXpressToast.error(this, "No reorderable items found in this order.");
                });
            }
        });
    }

    private void sharePdfReceipt() {
        binding.btnShareReceipt.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        com.printxpress.app.util.PrintXpressToast.info(this, "Preparing invoice to share...");
        PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
            Order order = db.orderDao().getOrderById(orderId);
            if (order != null) {
                List<OrderItem> items = db.orderItemDao().getOrderItemsByOrderId(orderId);
                java.util.Map<Integer, Product> productMap = new java.util.HashMap<>();
                if (items != null) {
                    for (OrderItem item : items) {
                        if (!productMap.containsKey(item.getProductId())) {
                            Product p = db.productDao().getProductById(item.getProductId());
                            if (p != null) {
                                productMap.put(p.getProductId(), p);
                            }
                        }
                    }
                }
                String customerName = com.printxpress.app.util.SessionManager.getUserName(this);
                java.io.File pdfFile = com.printxpress.app.util.PdfReceiptGenerator.generateDetailedReceipt(this, order, items, productMap, customerName);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pdfFile != null && pdfFile.exists()) {
                        try {
                            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("application/pdf");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "PrintXpress Commercial Invoice - Order #" + orderId);
                            shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is the signed commercial tax invoice and pre-press receipt for PrintXpress Order #" + orderId + ".");
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(shareIntent, "Share Commercial Invoice"));
                        } catch (Exception e) {
                            com.printxpress.app.util.PrintXpressToast.error(this, "Could not share PDF: " + e.getMessage());
                        }
                    } else {
                        com.printxpress.app.util.PrintXpressToast.error(this, "Failed to generate receipt PDF.");
                    }
                });
            }
        });
    }

    private void generateAndOpenPdfReceipt() {
        com.printxpress.app.util.PrintXpressToast.info(this, "Generating signed PDF invoice...");
        PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
            Order order = db.orderDao().getOrderById(orderId);
            if (order != null) {
                List<OrderItem> items = db.orderItemDao().getOrderItemsByOrderId(orderId);
                java.util.Map<Integer, Product> productMap = new java.util.HashMap<>();
                if (items != null) {
                    for (OrderItem item : items) {
                        if (!productMap.containsKey(item.getProductId())) {
                            Product p = db.productDao().getProductById(item.getProductId());
                            if (p != null) {
                                productMap.put(p.getProductId(), p);
                            }
                        }
                    }
                }
                String customerName = com.printxpress.app.util.SessionManager.getUserName(this);
                java.io.File pdfFile = com.printxpress.app.util.PdfReceiptGenerator.generateDetailedReceipt(this, order, items, productMap, customerName);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (pdfFile != null && pdfFile.exists()) {
                        try {
                            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(uri, "application/pdf");
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } catch (Exception e) {
                            com.printxpress.app.util.PrintXpressToast.info(this, "No PDF viewer installed on this device.");
                        }
                    } else {
                        com.printxpress.app.util.PrintXpressToast.error(this, "Failed to generate receipt PDF.");
                    }
                });
            }
        });
    }

    private void showUpdateStatusDialog() {
        com.printxpress.app.databinding.DialogManageOrderBinding dialogBinding =
                com.printxpress.app.databinding.DialogManageOrderBinding.inflate(getLayoutInflater());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.70f);
        }

        dialogBinding.tvOrderNumberTitle.setText("Manage Order #" + orderId);
        dialogBinding.tvOrderDetailsSub.setText("Update pre-press or press production status");
        dialogBinding.tvCurrentStatusBadge.setText(status != null ? status : "PENDING");
        dialogBinding.btnViewOrderSpecs.setVisibility(View.GONE);

        String currentStatus = status != null ? status.toUpperCase() : "PROCESSING";
        switch (currentStatus) {
            case "PRINTING":
                dialogBinding.rbStatusPrinting.setChecked(true);
                break;
            case "READY":
                dialogBinding.rbStatusReady.setChecked(true);
                break;
            case "COMPLETED":
                dialogBinding.rbStatusCompleted.setChecked(true);
                break;
            case "CANCELLED":
                dialogBinding.rbStatusCancelled.setChecked(true);
                break;
            default:
                dialogBinding.rbStatusProcessing.setChecked(true);
                break;
        }

        dialogBinding.btnCloseManageOrder.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnUpdateStatus.setOnClickListener(v -> {
            String targetStatus = "PROCESSING";
            if (dialogBinding.rbStatusPrinting.isChecked()) {
                targetStatus = "PRINTING";
            } else if (dialogBinding.rbStatusReady.isChecked()) {
                targetStatus = "READY";
            } else if (dialogBinding.rbStatusCompleted.isChecked()) {
                targetStatus = "COMPLETED";
            } else if (dialogBinding.rbStatusCancelled.isChecked()) {
                targetStatus = "CANCELLED";
            }

            final String newStatus = targetStatus;
            orderViewModel.updateOrderStatus(orderId, newStatus);
            status = newStatus;
            binding.tvStatus.setText("Status: " + newStatus + (com.printxpress.app.util.SessionManager.isAdmin(this) ? " ✎" : ""));
            PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
                Order order = db.orderDao().getOrderById(orderId);
                if (order != null) {
                    Notification notif = new Notification();
                    notif.setUserId(order.getCustomerId());
                    notif.setTitle("Order #" + orderId + " Status: " + newStatus);
                    notif.setMessage("Your print order #" + orderId + " has been updated to " + newStatus + " in our production queue.");
                    notif.setTimestamp(new Date());
                    notif.setRead(false);
                    notif.setType("ORDER");
                    db.notificationDao().insert(notif);
                }
            });
            com.printxpress.app.util.PrintXpressToast.success(this, "Order #" + orderId + " status updated to " + newStatus + "!");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadOrderDetails(Order order) {
        PrintXpressDatabase.databaseWriteExecutor.execute(() -> {
            List<OrderItem> items = db.orderItemDao().getOrderItemsByOrderId(orderId);
            Map<Integer, Product> productMap = new HashMap<>();
            double calculatedSubtotal = 0;
            if (items != null) {
                for (OrderItem item : items) {
                    calculatedSubtotal += item.getSubtotal();
                    if (!productMap.containsKey(item.getProductId())) {
                        Product p = db.productDao().getProductById(item.getProductId());
                        if (p != null) {
                            productMap.put(item.getProductId(), p);
                        }
                    }
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String formattedDate = order.getOrderDate() != null ? sdf.format(order.getOrderDate()) : "Date unavailable";
            String delMethod = order.getDeliveryMethod() != null ? order.getDeliveryMethod().toUpperCase() : "STANDARD";
            
            Address addr = (order.getDeliveryAddressId() != null && order.getDeliveryAddressId() > 0)
                    ? db.addressDao().getAddressById(order.getDeliveryAddressId())
                    : null;
            String shippingAddressText;
            if (addr != null) {
                shippingAddressText = addr.getAddressLine1() + ", " + addr.getCity() + (addr.getPostalCode() != null ? " (" + addr.getPostalCode() + ")" : "");
            } else if ("PICKUP".equalsIgnoreCase(delMethod)) {
                shippingAddressText = "Direct Collection at PrintXpress Main Print Studio, Colombo 03";
            } else {
                shippingAddressText = "Standard Courier Delivery (To registered customer address)";
            }

            String payMethod = order.getPaymentMethod() != null ? order.getPaymentMethod().toUpperCase() : "PENDING";
            String payStatus = order.getPaymentStatus() != null ? order.getPaymentStatus().toUpperCase() : "PENDING";
            double totalAmount = order.getTotalAmount();
            double subtotalToDisplay = calculatedSubtotal > 0 ? calculatedSubtotal : totalAmount;
            double deliveryFee = Math.max(0, totalAmount - subtotalToDisplay);

            StringBuilder legacySb = new StringBuilder();
            legacySb.append("Order Date: ").append(formattedDate).append("\n");
            legacySb.append("Delivery Method: ").append(delMethod).append("\n");
            legacySb.append("Shipping Address: ").append(shippingAddressText).append("\n");
            legacySb.append("Payment: ").append(payMethod).append(" (").append(payStatus).append(")\n");
            legacySb.append("Total Amount: ").append(com.printxpress.app.util.SessionManager.formatPrice(this, totalAmount)).append("\n\n");

            final double finalSubtotal = subtotalToDisplay;
            final double finalDeliveryFee = deliveryFee;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                // 1. Fulfillment & Delivery
                binding.tvOrderDate.setText(formattedDate);
                binding.tvDeliveryMethodBadge.setText(delMethod);
                binding.tvShippingAddress.setText(shippingAddressText);

                // 2. Ordered Print Items
                binding.llOrderItemsContainer.removeAllViews();
                if (items != null && !items.isEmpty()) {
                    binding.tvNoItems.setVisibility(View.GONE);
                    binding.tvItemsCount.setText(com.printxpress.app.util.SessionManager.formatNumber(items.size()) + (items.size() == 1 ? " Print Job" : " Print Jobs"));

                    for (int i = 0; i < items.size(); i++) {
                        OrderItem item = items.get(i);
                        ItemOrderDetailSpecBinding itemBinding = ItemOrderDetailSpecBinding.inflate(
                                getLayoutInflater(), binding.llOrderItemsContainer, false);

                        Product product = productMap.get(item.getProductId());
                        itemBinding.tvItemName.setText(product != null ? product.getName() : "Custom Print Item #" + item.getProductId());
                        itemBinding.tvItemSubtotal.setText(com.printxpress.app.util.SessionManager.formatPrice(this, item.getSubtotal()));
                        itemBinding.tvItemQty.setText("Qty: " + com.printxpress.app.util.SessionManager.formatNumber(item.getQuantity()));
                        itemBinding.tvItemUnitPrice.setText("Unit: " + com.printxpress.app.util.SessionManager.formatPrice(this, item.getUnitPrice()));

                        if (item.getCustomisationDetails() != null && !item.getCustomisationDetails().trim().isEmpty()) {
                            itemBinding.tvItemSpecs.setVisibility(View.VISIBLE);
                            itemBinding.tvItemSpecs.setText(item.getCustomisationDetails().trim());
                        } else {
                            itemBinding.tvItemSpecs.setVisibility(View.GONE);
                        }

                        if (i == items.size() - 1) {
                            itemBinding.itemDivider.setVisibility(View.GONE);
                        }

                        binding.llOrderItemsContainer.addView(itemBinding.getRoot());
                    }
                } else {
                    binding.tvNoItems.setVisibility(View.VISIBLE);
                    binding.tvItemsCount.setText("0 Jobs");
                }

                // 3. Payment & Billing
                binding.tvPaymentMethod.setText(payMethod);
                binding.tvPaymentStatusBadge.setText(payStatus);
                binding.tvSubtotalAmount.setText(com.printxpress.app.util.SessionManager.formatPrice(this, finalSubtotal));
                binding.tvDeliveryFee.setText(finalDeliveryFee > 0 ? com.printxpress.app.util.SessionManager.formatPrice(this, finalDeliveryFee) : "FREE");
                binding.tvTotalAmount.setText(com.printxpress.app.util.SessionManager.formatPrice(this, totalAmount));

                // Backwards compatibility
                binding.tvDetails.setText(legacySb.toString());
            });
        });
    }
}