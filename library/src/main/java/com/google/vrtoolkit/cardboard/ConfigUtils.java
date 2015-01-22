package com.google.vrtoolkit.cardboard;


import android.content.res.AssetManager;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class ConfigUtils {
    public static final String CARDBOARD_CONFIG_FOLDER = "Cardboard";
    public static final String CARDBOARD_DEVICE_PARAMS_FILE = "current_device_params";
    public static final String CARDBOARD_PHONE_PARAMS_FILE = "phone_params";
    
    public static File getConfigFile(final String filename) {
        final File configFolder = new File(
                Environment.getExternalStorageDirectory(), CARDBOARD_CONFIG_FOLDER);
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        else if (!configFolder.isDirectory()) {
            final String value = String.valueOf(String.valueOf(configFolder));
            throw new IllegalStateException(new StringBuilder()
                    .append("Folder ").append(value).append(" already exists").toString());
        }
        return new File(configFolder, filename);
    }
    
    public static InputStream openAssetConfigFile(final AssetManager assetManager,
                                                  final String filename) throws IOException {
        final String assetPath = new File(CARDBOARD_CONFIG_FOLDER, filename).getPath();
        return assetManager.open(assetPath);
    }
}
