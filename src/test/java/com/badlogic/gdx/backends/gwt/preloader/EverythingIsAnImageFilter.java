package com.badlogic.gdx.backends.gwt.preloader;

public class EverythingIsAnImageFilter implements AssetFilter {
    @Override
    public boolean accept(String file, boolean isDirectory) {
        return true;
    }

    @Override
    public AssetType getType(String file) {
        return AssetType.Image;
    }
}
