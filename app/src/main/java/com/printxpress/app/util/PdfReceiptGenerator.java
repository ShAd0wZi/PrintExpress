package com.printxpress.app.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import com.printxpress.app.data.local.entity.Order;
import com.printxpress.app.data.local.entity.OrderItem;
import com.printxpress.app.data.local.entity.Product;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PdfReceiptGenerator {

    private static final int PAGE_WIDTH = 595; // A4 point width
    private static final int PAGE_HEIGHT = 842; // A4 point height

    public static File generateDetailedReceipt(Context context,
                                               Order order,
                                               List<OrderItem> items,
                                               Map<Integer, Product> productMap,
                                               String customerName) {
        if (order == null) return null;

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // 1. Background
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, paint);

        // 2. Header Banner
        paint.setColor(Color.parseColor("#1A237E")); // Primary brand color
        canvas.drawRect(0, 0, PAGE_WIDTH, 100, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(32);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("PrintXpress", 40, 60, paint);

        paint.setTextSize(14);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("TAX INVOICE & RECEIPT", 400, 60, paint);

        // 3. Order Information Block
        paint.setColor(Color.DKGRAY);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        int y = 140;
        paint.setColor(Color.BLACK);
        paint.setTextSize(14);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Order Details", 40, y, paint);
        
        y += 24;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(12);
        canvas.drawText("Order ID: #" + order.getOrderId(), 40, y, paint);

        String dateStr = order.getOrderDate() != null
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(order.getOrderDate())
                : new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Date: " + dateStr, 340, y, paint);

        y += 20;
        if (customerName != null && !customerName.isEmpty()) {
            canvas.drawText("Customer: " + customerName, 40, y, paint);
        }
        canvas.drawText("Delivery: " + (order.getDeliveryMethod() != null ? order.getDeliveryMethod() : "Standard"), 340, y, paint);

        y += 20;
        canvas.drawText("Payment Method: " + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "Cash"), 40, y, paint);
        canvas.drawText("Payment Status: " + (order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID"), 340, y, paint);

        // 4. Line Items Table Header
        y += 40;
        paint.setColor(Color.parseColor("#E8EAF6")); // Soft primary tint
        canvas.drawRect(40, y - 18, 555, y + 10, paint);

        paint.setColor(Color.parseColor("#1A237E"));
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Item / Description", 50, y, paint);
        canvas.drawText("Qty", 320, y, paint);
        canvas.drawText("Unit Price", 380, y, paint);
        canvas.drawText("Subtotal", 480, y, paint);

        // 5. Line Items Rows
        paint.setColor(Color.BLACK);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(11);

        double itemsTotal = 0.0;
        int pageNumber = 1;
        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                if (y > 680) {
                    // Draw footer on current page and start a new page
                    paint.setColor(Color.GRAY);
                    paint.setTextSize(10);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    canvas.drawText("Page " + pageNumber + " (Continued on next page...)", 40, 810, paint);
                    document.finishPage(page);

                    pageNumber++;
                    PdfDocument.PageInfo nextPageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                    page = document.startPage(nextPageInfo);
                    canvas = page.getCanvas();

                    paint.setColor(Color.WHITE);
                    canvas.drawRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, paint);

                    y = 60;
                    paint.setColor(Color.parseColor("#E8EAF6"));
                    canvas.drawRect(40, y - 18, 555, y + 10, paint);
                    paint.setColor(Color.parseColor("#1A237E"));
                    paint.setTextSize(12);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("Item / Description (Cont.)", 50, y, paint);
                    canvas.drawText("Qty", 320, y, paint);
                    canvas.drawText("Unit Price", 380, y, paint);
                    canvas.drawText("Subtotal", 480, y, paint);
                }

                y += 28;
                Product product = productMap != null ? productMap.get(item.getProductId()) : null;
                String productName = product != null ? product.getName() : "Custom Print Item";

                paint.setColor(Color.BLACK);
                paint.setTextSize(11);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText(productName, 50, y, paint);

                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                canvas.drawText(SessionManager.formatNumber(item.getQuantity()), 325, y, paint);
                canvas.drawText(String.format(Locale.getDefault(), "Rs. %,.2f", item.getUnitPrice()), 380, y, paint);
                canvas.drawText(String.format(Locale.getDefault(), "Rs. %,.2f", item.getSubtotal()), 480, y, paint);

                itemsTotal += item.getSubtotal();

                // Specs subtitle if present
                if (item.getCustomisationDetails() != null && !item.getCustomisationDetails().isEmpty()) {
                    y += 14;
                    paint.setColor(Color.GRAY);
                    paint.setTextSize(9);
                    String specs = item.getCustomisationDetails();
                    if (specs.length() > 60) specs = specs.substring(0, 57) + "...";
                    canvas.drawText("Specs: " + specs, 55, y, paint);
                    paint.setColor(Color.BLACK);
                    paint.setTextSize(11);
                }

                // Divider line
                y += 8;
                paint.setColor(Color.parseColor("#EEEEEE"));
                canvas.drawLine(40, y, 555, y, paint);
                paint.setColor(Color.BLACK);
            }
        } else {
            y += 28;
            canvas.drawText("Custom Print Services", 50, y, paint);
            canvas.drawText("1", 325, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "Rs. %,.2f", order.getTotalAmount()), 380, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "Rs. %,.2f", order.getTotalAmount()), 480, y, paint);
            itemsTotal = order.getTotalAmount();
        }

        // 6. Totals Section
        if (y > 670) {
            document.finishPage(page);
            pageNumber++;
            PdfDocument.PageInfo nextPageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
            page = document.startPage(nextPageInfo);
            canvas = page.getCanvas();
            paint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, paint);
            y = 60;
        }

        y += 35;
        paint.setColor(Color.parseColor("#CCCCCC"));
        canvas.drawLine(300, y, 555, y, paint);

        y += 25;
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Items Subtotal:", 320, y, paint);
        canvas.drawText(String.format(Locale.getDefault(), "Rs. %,.2f", itemsTotal), 460, y, paint);

        double diff = order.getTotalAmount() - itemsTotal;
        if (diff > 0.01) {
            y += 20;
            canvas.drawText("Delivery & Fees:", 320, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "Rs. %,.2f", diff), 460, y, paint);
        }

        y += 30;
        paint.setColor(Color.parseColor("#1A237E"));
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Grand Total:", 320, y, paint);
        canvas.drawText(String.format(Locale.getDefault(), "Rs. %,.2f", order.getTotalAmount()), 450, y, paint);

        // 7. Footer
        paint.setColor(Color.GRAY);
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Thank you for your business! Need assistance? Contact support@printxpress.com", 80, 800, paint);

        document.finishPage(page);

        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Receipts");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, "Receipt_Order_" + order.getOrderId() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
        } catch (IOException e) {
            android.util.Log.e("PdfReceiptGenerator", "Failed to write PDF receipt", e);
            file = null;
        } finally {
            document.close();
        }

        return file;
    }

    /**
     * Backward-compatible simple receipt generator.
     */
    public static File generateReceipt(Context context, int orderId, double totalAmount, String dateStr) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setTotalAmount(totalAmount);
        order.setDeliveryMethod("Standard");
        order.setPaymentMethod("Card");
        order.setPaymentStatus("PAID");
        try {
            if (dateStr != null) {
                order.setOrderDate(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(dateStr));
            } else {
                order.setOrderDate(new Date());
            }
        } catch (Exception e) {
            order.setOrderDate(new Date());
        }
        return generateDetailedReceipt(context, order, null, null, "Customer");
    }
}
