package com.badlogic.gdx.backends.gwt.preloader;

import com.google.gwt.core.ext.*;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PreloaderBundleGeneratorTest {
    private final String packageName = "com.badlogic.gdx.backends.gwt.preloader";
    private final String className = "PreloaderBundleImpl";
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private PrintWriterTreeLogger logger;
    @Mock
    private GeneratorContext context;
    @Mock
    private ClassSourceFileComposerFactory composerFactory;
    @Mock
    private SourceWriter sourceWriter;
    @Mock
    private PropertyOracle propertyOracle;
    @Spy
    private PreloaderBundleGenerator generator;
    private File sourceDirectory;
    private File targetDirectory;
    private File rootDirectory;
    private File warDirectory;

    @Before
    public void setUp() throws Exception {
        rootDirectory = new File(System.getProperty("java.io.tmpdir"), "libgdx/" + String.valueOf(System.nanoTime()));
        sourceDirectory = new File(rootDirectory, "source");
        sourceDirectory.mkdirs();
        targetDirectory = new File(rootDirectory, "target");
        targetDirectory.mkdirs();

        logger = new PrintWriterTreeLogger();

//        when(generator.buildComposerFactory(packageName, className))
//                .thenReturn(composerFactory);

        expectSourceValue(sourceDirectory.getAbsolutePath());
        expectOutputValue(targetDirectory.getAbsolutePath());
        expectEmptyProperty("gdx.assetfilterclass");
        warDirectory = new File("war");
    }

    @After
    public void tearDown() {
        new FileWrapper(rootDirectory).deleteDirectory();
        new FileWrapper(warDirectory).deleteDirectory();
    }


    @Ignore("not sure how to test this functionally without being on a UNIX box, or mocking out the file stuff")
    @Test
    public void generate_shouldRemoveLeadingSlashesFromTheAssetFile() {
        expectOutputValue("/" + targetDirectory.getAbsolutePath().replace("\\", "/"));
        expectAsset("test.txt");

        runGenerator();

        assertAssetCopiedIn("test.txt", targetDirectory);
        assertAssetFile(targetDirectory, "");
    }

    @Ignore("currently failing for a NPE, should throw a more informative error")
    @Test
    public void generate_shouldBlowUpWhenANullValueIsGivenFromTheInputProperty() {
        expectErrorAboutSourceDirectory();

        expectSourceValue(null);

        runGenerator();
    }

    @Test
    public void generate_shouldDefaultTo_war_directoryWhenTheOutputDirectoryPropertyIsNull() throws BadPropertyValueException {
        expectOutputValue(null);
        expectAsset("test.txt");

        runGenerator();

        assertAssetCopiedIn("test.txt", warDirectory);
    }

    @Test
    public void generate_shouldDefaultTo_war_directoryWhenTheOutputDirectoryPropertyIsEmpty() throws BadPropertyValueException {
        expectEmptyProperty("gdx.assetoutputpath");
        expectAsset("test.txt");

        runGenerator();

        assertAssetCopiedIn("test.txt", warDirectory);
    }

    @Test
    public void generate_shouldDefaultTo_war_directoryWhenTheOutputDirectoryPropertyExplodes() throws BadPropertyValueException {
        expectPropertyFailure("gdx.assetoutputpath");
        expectAsset("test.txt");

        runGenerator();

        assertAssetCopiedIn("test.txt", warDirectory);
        assertAssetFile(warDirectory, "d:war/", "t:test.txt");
    }

    @Ignore("exposing bug on windows of not properly replacing the directory path in the asset lines")
    @Test
    public void generate_shouldCreateAnAssetFileListingAllTheAssetsCopied() {
        expectAsset("test.jpg");
        expectAsset("test.txt");
        expectAsset("test.mp3");
        expectAsset("test.xxx");
        expectAsset("sub/test.xxx");

        runGenerator();

        assertAssetFile(targetDirectory,
                "d:" + targetDirectory.getAbsolutePath().replace("\\", "/") + "/",
                "d:sub",
                "b:sub/test.xxx",
                "i:test.jpg",
                "a:test.mp3",
                "t:test.txt",
                "b:test.xxx"
        );
    }

    @Test
    public void generate_shouldCleanTheTargetDirectoryBeforeCopyingTheAssets() {
        expectAsset("test.jpg");
        expectAssetInDestination("test.fnt");

        runGenerator();

        assertAssetCopiedIn("test.jpg", targetDirectory);
        assertAssetNotCopied("test.fnt");
    }

    @Test
    public void generate_shouldAllowIgnoringDirectories() {
        expectAssetFilter(IgnoreDirectoriesAssetFilter.class.getName());
        expectAsset("test.xxx");
        expectAsset("sub/test.txt");

        runGenerator();

        assertAssetFile(targetDirectory, "");
        assertAssetNotCopied("test.xxx");
        assertAssetNotCopied("sub/test.txt");
    }

    @Test
    public void generate_shouldAllowCustomAssetFilters() {
        expectAssetFilter(EverythingIsAnImageFilter.class.getName());
        expectAsset("test.fnt");
        expectAsset("test.xxx");

        runGenerator();

        assertAssetCopiedIn("test.fnt", targetDirectory);
        assertAssetCopiedIn("test.xxx", targetDirectory);
    }

    @Test
    public void generate_shouldThrowAnErrorIfWeFailToLoadTheCustomAssetFilter() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Couldn't instantiate custom AssetFilter 'ClassDoesNotExist', make sure the class is public and has a public default constructor");

        expectAssetFilter("ClassDoesNotExist");

        runGenerator();
    }

    @Test
    public void generate_shouldUseTheDefaultAssetFilterWhenTheFilterClassValueIsNull() throws BadPropertyValueException {
        expectAssetFilter(null);
        expectAsset("test.fnt");

        runGenerator();

        assertAssetCopiedIn("test.fnt", targetDirectory);
    }

    @Test
    public void generate_shouldUseTheDefaultAssetFilterWhenWeEncounterAnErrorAccessingTheFilterClassProperty() throws BadPropertyValueException {
        expectPropertyFailure("gdx.assetfilterclass");
        expectAsset("test.fnt");

        runGenerator();

        assertAssetCopiedIn("test.fnt", targetDirectory);
    }

    @Test
    public void generate_shouldStopOnTheFirstOutputDirectoryThatExists() {
        expectOutputValue("doesNotExist," + targetDirectory.getAbsolutePath() + ",anotherDirectory");
        expectAsset("test.jpg");

        runGenerator();

        assertAssetCopiedIn("test.jpg", targetDirectory);
    }

    @Test
    public void generate_shouldStopOnTheFirstInputDirectoryThatExists() {
        expectSourceValue("doesNotExist," + sourceDirectory.getAbsolutePath() + ",anotherDirectory");
        expectAsset("test.jpg");

        runGenerator();

        assertAssetCopiedIn("test.jpg", targetDirectory);
    }

    @Test
    public void generate_shouldCopyAllTheAssetsToTheTargetDirectory() {
        expectAsset("test.jpg");
        expectAsset("test.fnt");

        runGenerator();

        assertAssetCopiedIn("test.jpg", targetDirectory);
        assertAssetCopiedIn("test.fnt", targetDirectory);
    }

    @Test
    public void generate_shouldThrowAnErrorWhenTheAssetPathDirectoryIsNotADirectory() {
        String assetPath = expectAsset("test.jpg");
        expectSourceValue(assetPath);
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("assets path '" + assetPath + "' is not a directory. Check your gdx.assetpath property in your GWT project's module gwt.xml file");

        runGenerator();
    }

    @Ignore("currently failing for a NPE, should throw a more informative error")
    @Test
    public void generate_shouldThrowAnErrorWhenTheAssetPathDirectoryDoesNotExist() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("assets path '' does not exist. Check your gdx.assetpath property in your GWT project's module gwt.xml file");
        expectSourceValue("doesNotExist");

        runGenerator();
    }

    @Test
    public void generate_shouldThrownAnErrorWhenTheAssetPathHasNoValues() throws BadPropertyValueException {
        expectErrorAboutSourceDirectory();
        expectEmptyProperty("gdx.assetpath");

        runGenerator();
    }

    @Test
    public void generate_shouldThrownAnErrorWhenFailToLookupTheAssetPath() throws BadPropertyValueException {
        expectErrorAboutSourceDirectory();
        expectPropertyFailure("gdx.assetpath");

        runGenerator();
    }

    @Ignore("can only be ran once I make the code changes to mock out the composer factory")
    @Test
    public void generate_shouldNotAttemptToSetupTheLoggerOnTheSourceWriter() throws UnableToCompleteException {
        when(context.tryCreate(logger, packageName, className)).thenReturn(null);

        assertBundleClass(runGenerator());

        verifyBundleInterfaceWasSetOnTheComposer();
        verifyZeroInteractions(sourceWriter);
    }

    @Ignore("can only be ran once I make the code changes to mock out the composer factory")
    @Test
    public void generate_shouldSetTheLoggerOnTheSourceWriter() throws Exception {
        StubPrintWriter contextWriter = new StubPrintWriter();

        when(context.tryCreate(logger, packageName, className)).thenReturn(contextWriter);
        when(composerFactory.createSourceWriter(context, contextWriter)).thenReturn(sourceWriter);

        assertBundleClass(runGenerator());

        verifyBundleInterfaceWasSetOnTheComposer();
        verify(sourceWriter).commit(logger);
    }

    private void assertAssetCopiedIn(String expectedAsset, File directory) {
        assertTrue("Did NOT find asset [" + expectedAsset + "] in target directory",
                copiedAssetFileIn(expectedAsset, directory).exists());
    }

    private void assertAssetNotCopied(String expectedAssetToNotExist) {
        assertFalse("Assert exists [" + expectedAssetToNotExist + "] in target directory",
                copiedAssetFileIn(expectedAssetToNotExist, targetDirectory).exists());
    }

    private void assertAssetFile(File directory, String... expectedLines) {
        String[] actualLines = new FileWrapper(copiedAssetFileIn("assets.txt", directory)).readString().split("\n");
        assertEquals("The 'assets.txt' is not as expected",
                Arrays.asList(expectedLines),
                Arrays.asList(actualLines));
    }

    private File copiedAssetFileIn(String expectedAsset, File directory) {
        return new File(directory, "assets/" + expectedAsset);
    }

    private void expectAssetInDestination(String fileName) {
        new FileWrapper(copiedAssetFileIn(fileName, targetDirectory)).writeString(fileName, false);
    }

    private String expectAsset(String assetFilename) {
        return expectAssetIn(assetFilename, sourceDirectory);
    }

    private String expectAssetIn(String assetFilename, File directory) {
        String[] dirs = assetFilename.split("/");
        File currentDir = directory;
        for (int i = 0; i < dirs.length - 1; i++) {
            currentDir = new File(currentDir, dirs[i]);
        }
        currentDir.mkdirs();
        File file = new File(currentDir, dirs[dirs.length - 1]);
        FileWrapper fileWrapper = new FileWrapper(file);
        fileWrapper.writeString(assetFilename, false);
        return file.getAbsolutePath();
    }

    private void expectErrorAboutSourceDirectory() {
        thrown.expectMessage("No gdx.assetpath defined. Add <set-configuration-property name=\"gdx.assetpath\" value=\"relative/path/to/assets/\"/> to your GWT projects gwt.xml file");
        thrown.expect(RuntimeException.class);
    }

    private String runGenerator() {
        try {
            return generator.generate(logger, context, "");
        } catch (UnableToCompleteException e) {
            throw new RuntimeException(e);
        }
    }

    private void expectProperty(String propertyName, String propertyValue) {
        try {
            ConfigurationProperty configurationProperty = mock(ConfigurationProperty.class, propertyName + "config-property");
            when(mockPropertyOracle().getConfigurationProperty(propertyName)).thenReturn(configurationProperty);
            when(configurationProperty.getValues()).thenReturn(Arrays.asList(propertyValue));
        } catch (BadPropertyValueException e) {
        }
    }

    private void expectEmptyProperty(String propertyName) throws BadPropertyValueException {
        ConfigurationProperty configurationProperty = mock(ConfigurationProperty.class);
        when(mockPropertyOracle().getConfigurationProperty(propertyName)).thenReturn(configurationProperty);
    }

    private void expectPropertyFailure(String propertyName) throws BadPropertyValueException {
        BadPropertyValueException expectedError = new BadPropertyValueException("property.name");
        when(mockPropertyOracle().getConfigurationProperty(propertyName)).thenThrow(expectedError);
    }

    private PropertyOracle mockPropertyOracle() {
        when(context.getPropertyOracle()).thenReturn(propertyOracle);
        return propertyOracle;
    }

    private void assertBundleClass(String actualClassName) throws UnableToCompleteException {
        assertEquals(packageName + "." + className, actualClassName);
    }

    private void verifyBundleInterfaceWasSetOnTheComposer() {
        verify(composerFactory).addImplementedInterface("com.badlogic.gdx.backends.gwt.preloader.PreloaderBundle");
    }

    private void expectOutputValue(String expectedValue) {
        expectProperty("gdx.assetoutputpath", expectedValue);
    }

    private void expectSourceValue(String expectedValue) {
        expectProperty("gdx.assetpath", expectedValue);
    }

    private void expectAssetFilter(String expectedFilterClass) {
        expectProperty("gdx.assetfilterclass", expectedFilterClass);
    }

    private class StubPrintWriter extends PrintWriter {
        public StubPrintWriter() {
            super(new OutputStreamWriter(new ByteArrayOutputStream()));
        }
    }
}
