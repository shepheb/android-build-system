/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.builder.png;

import static org.junit.Assert.assertTrue;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.builder.internal.aapt.v1.AaptV1;
import com.android.ide.common.internal.ResourceCompilationException;
import com.android.ide.common.internal.ResourceProcessor;
import com.android.ide.common.resources.CompileResourceRequest;
import com.android.repository.Revision;
import com.android.repository.testframework.FakeProgressIndicator;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.testutils.TestResources;
import com.android.testutils.TestUtils;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.io.Files;
import com.google.common.truth.TestVerb;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.DataFormatException;
import javax.imageio.ImageIO;
import org.junit.Assert;

/**
 * Utilities common to tests for both the synchronous and the asynchronous Aapt processor.
 */
public class NinePatchAaptProcessorTestUtils {

    /** Signature of a PNG file. */
    public static final byte[] SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    /** Returns the aapt binary to use. */
    static File getAapt() {
        FakeProgressIndicator progress = new FakeProgressIndicator();
        Range<Revision> versionRange = Range.atLeast(AaptV1.VERSION_FOR_SERVER_AAPT);

        BuildToolInfo buildToolInfo =
                BuildToolInfo.fromLocalPackage(
                        Verify.verifyNotNull(
                                AndroidSdkHandler.getInstance(TestUtils.getSdk())
                                        .getPackageInRange(
                                                SdkConstants.FD_BUILD_TOOLS,
                                                versionRange,
                                                progress),
                                "Build tools in %s required.",
                                versionRange));

        return new File(buildToolInfo.getPath(BuildToolInfo.PathId.AAPT));
    }

    public static void tearDownAndCheck(
            int cruncherKey,
            @NonNull Map<File, File> sourceAndCrunchedFiles,
            @NonNull ResourceProcessor cruncher,
            @NonNull AtomicLong classStartTime,
            @NonNull TestVerb expect)
            throws IOException, DataFormatException, InterruptedException {
        long startTime = System.currentTimeMillis();
        try {
            cruncher.end(cruncherKey);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(
                "waiting for requests completion : " + (System.currentTimeMillis() - startTime));
        System.out.println("total time : " + (System.currentTimeMillis() - classStartTime.get()));
        TestUtils.waitForFileSystemTick();
        long comparisonStartTime = System.currentTimeMillis();
        System.out.print("Comparing crunched files");
        for (Map.Entry<File, File> sourceAndCrunched : sourceAndCrunchedFiles.entrySet()) {
            System.out.print('.');
            File crunched = new File(sourceAndCrunched.getKey().getParent(),
                    sourceAndCrunched.getKey().getName() + getControlFileSuffix());

            Map<String, Chunk> testedChunks =
                    compareChunks(expect, crunched, sourceAndCrunched.getValue());

            try {
                compareImageContent(expect, crunched, sourceAndCrunched.getValue());
            } catch (Throwable e) {
                throw new RuntimeException("Failed with " + testedChunks.get("IHDR"), e);
            }
        }
        System.out.println();
        System.out.format(
                "Done comparing %1$d crunched files in %2$dms%n",
                sourceAndCrunchedFiles.size(), (System.currentTimeMillis() - comparisonStartTime));
    }

    /**
     * Suffix used by "golden" files, generated with aapt.
     *
     * <p>To regenerate the files using a new version of aapt, run the following:
     * <pre>
     * $ cd src/test/resources/testData/png/ninepatch
     * $ rm *.crunched.aapt
     * $ for f in *png; do aapt s -i $f -o $f.crunched.aapt; done
     * </pre>
     */
    protected static String getControlFileSuffix() {
        return ".crunched.aapt";
    }


    @NonNull
    static File crunchFile(int crunchKey, @NonNull File file, ResourceProcessor aaptCruncher)
            throws ResourceCompilationException, IOException {
        File outFile = File.createTempFile("pngWriterTest", ".png");
        outFile.deleteOnExit();
        try {
            CompileResourceRequest request =
                    new CompileResourceRequest(file, outFile, "test", false, true);
            aaptCruncher.compile(crunchKey, request, null);
        } catch (ResourceCompilationException e) {
            e.printStackTrace();
            throw e;
        }
        //System.out.println("crunch " + file.getPath());
        return outFile;
    }


    private static Map<String, Chunk> compareChunks(
            @NonNull TestVerb expect,
            @NonNull File original,
            @NonNull File tested)
            throws IOException, DataFormatException {
        Map<String, Chunk> originalChunks = readChunks(original);
        Map<String, Chunk> testedChunks = readChunks(tested);

        compareChunk(expect, originalChunks, testedChunks, "IHDR");
        compareChunk(expect, originalChunks, testedChunks, "npLb");
        compareChunk(expect, originalChunks, testedChunks, "npTc");

        return testedChunks;
    }

    private static void compareChunk(
            @NonNull TestVerb expect,
            @NonNull Map<String, Chunk> originalChunks,
            @NonNull Map<String, Chunk> testedChunks,
            @NonNull String chunkType) {
        expect.that(testedChunks.get(chunkType)).isEqualTo(originalChunks.get(chunkType));
    }

    public static Collection<Object[]> getNinePatches() {
        File pngFolder = getPngFolder();
        File ninePatchFolder = new File(pngFolder, "ninepatch");

        File[] files =
                ninePatchFolder.listFiles(file -> file.getPath().endsWith(SdkConstants.DOT_9PNG));

        if (files != null) {
            ImmutableList.Builder<Object[]> params = ImmutableList.builder();
            for (File file : files) {
                params.add(new Object[]{file, file.getName()});
            }
            return params.build();
        }

        return ImmutableList.of();
    }

    protected static void compareImageContent(
            @NonNull TestVerb expect, @NonNull File originalFile, @NonNull File createdFile)
            throws IOException {
        BufferedImage originalImage = ImageIO.read(originalFile);
        BufferedImage createdImage = ImageIO.read(createdFile);

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int createdWidth = createdImage.getWidth();
        int createdHeight = createdImage.getHeight();

        // compare sizes taking into account if the image is a 9-patch
        // in which case the original is bigger by 2 since it has the patch area still.
        Assert.assertEquals(originalWidth, createdWidth);
        Assert.assertEquals(originalHeight, createdHeight);

        // get the file content
        // always use the created Size. And for the original image, if 9-patch, just take
        // the image minus the 1-pixel border all around.
        int[] originalContent = new int[createdWidth * createdHeight];
        originalImage.getRGB(0, 0, createdWidth, createdHeight, originalContent, 0, createdWidth);

        int[] createdContent = new int[createdWidth * createdHeight];
        createdImage.getRGB(0, 0, createdWidth, createdHeight, createdContent, 0, createdWidth);

        List<String> errors = Lists.newArrayList();

        for (int y = 0; y < createdHeight; y++) {
            for (int x = 0; x < createdWidth; x++) {
                int originalRGBA = originalContent[y * createdWidth + x];
                int createdRGBA = createdContent[y * createdWidth + x];
                if (originalRGBA != createdRGBA) {
                    errors.add(String.format(
                            "%dx%d: 0x%08x : 0x%08x", x, y, originalRGBA, createdRGBA));
                }
            }
        }

        expect.that(errors).isEmpty();
    }

    @NonNull
    protected static Map<String, Chunk> readChunks(@NonNull File file) throws IOException {
        Map<String, Chunk> chunks = Maps.newHashMap();

        byte[] fileBuffer = Files.toByteArray(file);
        ByteBuffer buffer = ByteBuffer.wrap(fileBuffer);

        byte[] sig = new byte[8];
        buffer.get(sig);

        assertTrue(Arrays.equals(sig, SIGNATURE));

        byte[] data, type;
        int len;
        int crc32;

        while (buffer.hasRemaining()) {
            len = buffer.getInt();

            type = new byte[4];
            buffer.get(type);

            data = new byte[len];
            buffer.get(data);

            // crc
            crc32 = buffer.getInt();

            Chunk chunk = new Chunk(type, data, crc32);
            chunks.put(chunk.getTypeAsString(), chunk);
        }

        return chunks;
    }

    @NonNull
    protected static File getFile(@NonNull String name) {
        return new File(getPngFolder(), name);
    }

    @NonNull
    protected static File getPngFolder() {
        File folder = TestResources.getDirectory("/testData/png");
        assertTrue(folder.isDirectory());
        return folder;
    }
}
