package org.xwiki.contrib.xwikifs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * MapWithReferencesTest.
 *
 * @version $Id$
 */
public class MapWithReferencesTest
{
    private File tempDir;

    @Before
    public void before() throws Exception
    {

        File f = new File(new File(System.getProperty("java.io.tmpdir")), String.format("temp-%s",
                Long.toString(System.nanoTime())));
        f.mkdirs();

        tempDir = f;
    }

    @After
    public void after() throws IOException
    {
        FileUtils.deleteDirectory(tempDir);
    }

    @Test
    public void testSimpleYaml() throws Exception
    {
        File yamlFile = new File(getClass().getResource("/simpleYaml/test.yaml").getFile());

        MapWithReferences mapWithReferences = MapWithReferences.fromYaml(yamlFile);

        assertEquals("foo", mapWithReferences.get("foo"));
        assertEquals("bar", mapWithReferences.get("bar"));
        assertEquals(1, mapWithReferences.getDeclaredReferences().size());
    }

    @Test
    public void testNestedYaml() throws Exception
    {
        File yamlFile = new File(getClass().getResource("/nestedYaml/test.yaml").getFile());

        MapWithReferences mapWithReferences = MapWithReferences.fromYaml(yamlFile);

        assertEquals("a", mapWithReferences.get("a"));
        assertEquals("b", mapWithReferences.get("b"));
        assertEquals(1, mapWithReferences.getDeclaredReferences().size());

        MapWithReferences nestedMapWithReferences = (MapWithReferences) mapWithReferences.get("c");
        assertEquals("ca", nestedMapWithReferences.get("ca"));
        assertEquals("cb", nestedMapWithReferences.get("cb"));
        assertEquals(1, nestedMapWithReferences.getDeclaredReferences().size());

        nestedMapWithReferences = (MapWithReferences) nestedMapWithReferences.get("cc");
        assertEquals("cca", nestedMapWithReferences.get("cca"));
        assertEquals("ccb", nestedMapWithReferences.get("ccb"));
        assertEquals(1, nestedMapWithReferences.getDeclaredReferences().size());
    }

    @Test(expected = IOException.class)
    public void testMissingReferences() throws Exception
    {
        File yamlFile = new File(getClass().getResource("/missingReferencesYaml/test.yaml").getFile());

        MapWithReferences mapWithReferences = MapWithReferences.fromYaml(yamlFile);
    }

    @Test
    public void testCollectReferences() throws Exception
    {
        File yamlFile = new File(getClass().getResource("/nestedYaml/test.yaml").getFile());

        MapWithReferences mapWithReferences = MapWithReferences.fromYaml(yamlFile);
        Map<String, String> references = mapWithReferences.collectReferences(mapWithReferences);

        assertEquals(3, references.size());
        assertTrue(references.keySet().contains("b"));
        assertTrue(references.keySet().contains("cb"));
        assertTrue(references.keySet().contains("ccb"));
    }

    @Test
    public void testWriteSimpleYaml() throws Exception
    {
        File yamlFile = new File(getClass().getResource("/simpleYaml/test.yaml").getFile());

        MapWithReferences mapWithReferences = MapWithReferences.fromYaml(yamlFile);

        File outputFile = new File(tempDir, "test.yaml");
        mapWithReferences.toYaml(outputFile);

        assertEquals(IOUtils.toString(new FileInputStream(yamlFile)).trim(),
                IOUtils.toString(new FileInputStream(outputFile)).trim());

        Map<String, String> references = mapWithReferences.collectReferences(mapWithReferences);

        for (String key : references.keySet()) {
            assertEquals(
                    IOUtils.toString(new FileInputStream(
                            new File(getClass().getResource(String.format("/simpleYaml/%s", key)).getFile())))
                            .trim(),
                    IOUtils.toString(new FileInputStream(new File(tempDir, key))).trim());
        }
    }

    @Test
    public void testWriteNestedYaml() throws Exception
    {
        File yamlFile = new File(getClass().getResource("/nestedYaml/test.yaml").getFile());

        MapWithReferences mapWithReferences = MapWithReferences.fromYaml(yamlFile);

        File outputFile = new File(tempDir, "test.yaml");
        mapWithReferences.toYaml(outputFile);

        assertEquals(IOUtils.toString(new FileInputStream(yamlFile)).trim(),
                IOUtils.toString(new FileInputStream(outputFile)).trim());

        Map<String, String> references = mapWithReferences.collectReferences(mapWithReferences);

        for (String key : references.keySet()) {
            assertEquals(
                    IOUtils.toString(new FileInputStream(
                            new File(getClass().getResource(String.format("/nestedYaml/%s", key)).getFile())))
                            .trim(),
                    IOUtils.toString(new FileInputStream(new File(tempDir, key))).trim());
        }
    }

    @Test
    public void testWriteReferenceInSubdirectory() throws Exception
    {
        MapWithReferences mapWithReferences = new MapWithReferences();
        mapWithReferences.put("a", "a");
        mapWithReferences.putReference("b", "data/b", "b");

        File outputFile = new File(tempDir, "test.yaml");
        mapWithReferences.toYaml(outputFile);

        assertEquals("b", IOUtils.toString(new FileInputStream(new File(tempDir, "data/b"))).trim());
    }
}
