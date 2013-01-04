package com.badlogic.gdx.backends.gwt.preloader;

import com.badlogic.gdx.backends.gwt.preloader.AssetFilter.AssetType;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.gwt.core.ext.*;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * This class for the most part is a copy-n-paste of the class from the libgdx-gwt-backend.
 * <p/>
 * The commented out lines are the scenarios to still be covered by the test.
 */
public class PreloaderBundlerGeneratorTestCoverageProgress extends Generator {
    private class Asset {
        FileWrapper file;
        AssetType type;

        public Asset(FileWrapper file, AssetType type) {
            this.file = file;
            this.type = type;
        }
    }

    @Override
    public String generate(TreeLogger logger, GeneratorContext context, String typeName) throws UnableToCompleteException {
        String assetPath = getAssetPath(context);
        String assetOutputPath = getAssetOutputPath(context);

        if (assetOutputPath == null) {
            assetOutputPath = "war/";
        }
        AssetFilter assetFilter = getAssetFilter(context);

        FileWrapper source = new FileWrapper(assetPath);
        if (!source.exists()) {
//            source = new FileWrapper("../" + assetPath);
//            if (!source.exists())
            throw new RuntimeException("assets path '" + assetPath
                    + "' does not exist. Check your gdx.assetpath property in your GWT project's module gwt.xml file");
        }
        if (!source.isDirectory())
            throw new RuntimeException("assets path '" + assetPath
                    + "' is not a directory. Check your gdx.assetpath property in your GWT project's module gwt.xml file");

        FileWrapper target = new FileWrapper("assets/"); // this should always be the war/ directory of the GWT project.
        if (!target.file.getAbsolutePath().replace("\\", "/").endsWith(assetOutputPath + "assets")) {
            target = new FileWrapper(assetOutputPath + "assets/");
        }
        if (target.exists()) {
            if (!target.deleteDirectory()) throw new RuntimeException("Couldn't clean target path '" + target + "'");
        }
        ArrayList<Asset> assets = new ArrayList<Asset>();
        copyDirectory(source, target, assetFilter, assets);

        writeAssetsFile(assetOutputPath, target, assets);
        return createDummyClass(logger, context);
    }

    private void writeAssetsFile(String assetOutputPath, FileWrapper target, ArrayList<Asset> assets) {
        StringBuffer buffer = new StringBuffer();
        for (Asset asset : assets) {
            String path = asset.file.path().replace('\\', '/').replace(assetOutputPath + "assets/", "").replaceFirst("assets", "");
//            if (path.startsWith("/")) path = path.substring(1);
            buffer.append(asset.type.code);
            buffer.append(":");
            buffer.append(path);
            buffer.append("\n");
        }
        target.child("assets.txt").writeString(buffer.toString(), false);
        System.out.println(buffer.toString());
    }

    private void copyFile(FileWrapper source, FileWrapper dest, AssetFilter filter, ArrayList<Asset> assets) {
        if (filter.accept(dest.path(), false))
            ;
        try {
            assets.add(new Asset(dest, filter.getType(dest.path())));
            dest.write(source.read(), false);
        } catch (Exception ex) {
            throw new GdxRuntimeException("Error copying source file: " + source + "\n" //
                    + "To destination: " + dest, ex);
        }
    }

    private void copyDirectory(FileWrapper sourceDir, FileWrapper destDir, AssetFilter filter, ArrayList<Asset> assets) {
        System.out.println("Copying resources from " + sourceDir + " to " + destDir);
        if (!filter.accept(destDir.path(), true)) return;
        assets.add(new Asset(destDir, AssetType.Directory));
        destDir.mkdirs();
        FileWrapper[] files = sourceDir.list();
        for (int i = 0, n = files.length; i < n; i++) {
            FileWrapper srcFile = files[i];
            FileWrapper destFile = destDir.child(srcFile.name());
            if (srcFile.isDirectory())
                copyDirectory(srcFile, destFile, filter, assets);
            else
                copyFile(srcFile, destFile, filter, assets);
        }
    }

    private AssetFilter getAssetFilter(GeneratorContext context) {
        ConfigurationProperty assetFilterClassProperty = null;
        try {
            assetFilterClassProperty = context.getPropertyOracle().getConfigurationProperty("gdx.assetfilterclass");
        } catch (BadPropertyValueException e) {
            return new DefaultAssetFilter();
        }
        if (assetFilterClassProperty.getValues().size() == 0) {
            return new DefaultAssetFilter();
        }
        String assetFilterClass = assetFilterClassProperty.getValues().get(0);
        if (assetFilterClass == null) return new DefaultAssetFilter();
        try {
            return (AssetFilter) Class.forName(assetFilterClass).newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't instantiate custom AssetFilter '" + assetFilterClass
                    + "', make sure the class is public and has a public default constructor", e);
        }
    }

    private String getAssetPath(GeneratorContext context) {
        ConfigurationProperty assetPathProperty = null;
        try {
            assetPathProperty = context.getPropertyOracle().getConfigurationProperty("gdx.assetpath");
        } catch (BadPropertyValueException e) {
            throw new BadAssetSourceValueException();
        }
        if (assetPathProperty.getValues().size() == 0) {
            throw new BadAssetSourceValueException();
        }
        String paths = assetPathProperty.getValues().get(0);
        if (paths == null) {
            throw new BadAssetSourceValueException();
        } else {
            String[] tokens = paths.split(",");
            for (String token : tokens) {
//                if(new FileWrapper(token).exists() || new FileWrapper("../" + token).exists()) {
//                    return token;
//                }
                if (new FileWrapper(token).exists()) {
                    return token;
                }
            }
//            return null;
            return "";
        }
    }

    private String getAssetOutputPath(GeneratorContext context) {
        ConfigurationProperty assetPathProperty = null;
        try {
            assetPathProperty = context.getPropertyOracle().getConfigurationProperty("gdx.assetoutputpath");
        } catch (BadPropertyValueException e) {
            return null;
        }
        if (assetPathProperty.getValues().size() == 0) {
            return null;
        }
        String paths = assetPathProperty.getValues().get(0);
        if (paths == null) {
            return null;
        } else {
            String[] tokens = paths.split(",");
            String path = null;
            for (String token : tokens) {
                if (new FileWrapper(token).exists()) {
                    path = token;
                }
            }
            if (path != null && !path.endsWith("/")) {
                path += "/";
            }
            return path.replace("\\", "/");
        }
    }

    private String createDummyClass(TreeLogger logger, GeneratorContext context) {
        String packageName = "com.badlogic.gdx.backends.gwt.preloader";
        String className = "PreloaderBundleImpl";
        ClassSourceFileComposerFactory composer = buildComposerFactory(packageName, className);
        composer.addImplementedInterface(packageName + ".PreloaderBundle");
        PrintWriter printWriter = context.tryCreate(logger, packageName, className);
        if (printWriter == null) {
            return packageName + "." + className;
        }
        SourceWriter sourceWriter = composer.createSourceWriter(context, printWriter);
        sourceWriter.commit(logger);
        return packageName + "." + className;
    }

    protected ClassSourceFileComposerFactory buildComposerFactory(String packageName, String className) {
        return new ClassSourceFileComposerFactory(packageName, className);
    }

    public static class BadAssetSourceValueException extends RuntimeException {
        public BadAssetSourceValueException() {
            super("No gdx.assetpath defined. Add <set-configuration-property name=\"gdx.assetpath\" value=\"relative/path/to/assets/\"/> to your GWT projects gwt.xml file");
        }
    }

}