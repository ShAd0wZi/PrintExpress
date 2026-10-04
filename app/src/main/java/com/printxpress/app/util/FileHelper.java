package com.printxpress.app.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileHelper {

    private static final String TAG = "FileHelper";
    
    public static File saveFileToInternalStorage(Context context, Uri uri, String fileName) {
        if (context == null || uri == null || fileName == null || fileName.trim().isEmpty()) {
            return null;
        }
        String finalName = ensureExtension(context, uri, fileName.trim());
        File file = new File(context.getFilesDir(), finalName);
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(file)) {
            
            if (inputStream == null) return null;
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Error saving file to internal storage: " + finalName, e);
            deleteFileIfExists(file);
            return null;
        }
    }

    public static boolean deleteFileIfExists(File file) {
        if (file != null && file.exists()) {
            return file.delete();
        }
        return false;
    }

    private static String ensureExtension(Context context, Uri uri, String baseName) {
        String lower = baseName.toLowerCase();
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".pdf")) {
            return baseName;
        }
        try {
            ContentResolver resolver = context.getContentResolver();
            String mimeType = resolver.getType(uri);
            if (mimeType != null) {
                String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if (ext != null && !ext.isEmpty()) {
                    return baseName + "." + ext;
                }
            }
        } catch (Exception ignored) {
        }
        return baseName + ".png";
    }
}