package com.badlogic.gdx.backends.gwt.preloader;

public class IgnoreDirectoriesAssetFilter extends DefaultAssetFilter {
    @Override
    public boolean accept(String file, boolean isDirectory) {
        return !isDirectory;
    }
}
