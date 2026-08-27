package com.wk.ti.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FileValidationUtil {
    public static final String PDF_EXTENSION = "pdf";

    public static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "bmp", "webp");

    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList("pdf", "png", "jpg", "jpeg", "gif", "bmp", "webp");

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/bmp",
            "image/webp"
    );

    public static boolean isPdf(File file,String filename) {
        if (!isValid(file)) {
            return false;
        }
        return PDF_EXTENSION.equals(getFileExtension(filename));
    }

    public static boolean isKnownImage(File file, String filename) {
        if (!isValid(file)) {
            return false;
        }
        return IMAGE_EXTENSIONS.contains(getFileExtension(filename));
    }

    private static boolean isValid(File file) {
        return file != null;
    }

    public static boolean isValidFile(File file, String filename) {
        if (!isValid(file)) {
            return false;
        }

        String extension = getFileExtension(filename);

        return ALLOWED_EXTENSIONS.contains(extension);
    }

    private static String getFileExtension(String filename) {

        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public static boolean isValidMimeType(String contentType) {
        return ALLOWED_MIME_TYPES.contains(contentType);
    }
}

