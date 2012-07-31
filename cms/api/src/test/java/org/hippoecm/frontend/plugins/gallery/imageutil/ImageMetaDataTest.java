/*
 * Copyright 2012 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.gallery.imageutil;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ImageMetaDataTest {

    private static final String TEST_RGB_JPG = "test-RGB.jpg";
    private static final String TEST_CMYK_JPG = "test-CMYK.jpg";
    private static final String TEST_YCCK_JPG = "test-YCCK.jpg";
    private static final String TEST_RGB_PNG = "test-5000x1.png";
    private static final String TEST_RGB_GIF = "test-380x428.gif";

    private static final String TEST_BROKEN_THUMBS_JPG = "test-broken-thumbnails.jpg";

    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String PNG_MIME_TYPE = "image/png";
    private static final String GIF_MIME_TYPE = "image/gif";

    private InputStream readImage(String fileName) {
        return getClass().getResourceAsStream("/" + fileName);
    }

    @Test
    public void testMetadataForRGBJpeg() throws Exception {

        ImageMetaData meta = new ImageMetaData(JPEG_MIME_TYPE, TEST_RGB_JPG);
        assertEquals(TEST_RGB_JPG, meta.getFilename());
        assertEquals(JPEG_MIME_TYPE, meta.getMimeType());
        assertTrue(meta.isJpeg());

        InputStream is = meta.parse(readImage(TEST_RGB_JPG));
        assertEquals(TEST_RGB_JPG, meta.getFilename());
        assertEquals(JPEG_MIME_TYPE, meta.getMimeType());
        assertEquals(ImageMetaData.ColorModel.RGB, meta.getColorModel());

        IOUtils.closeQuietly(is);
    }

    @Test
    public void testMetadataForYCCKJpeg() throws Exception {

        ImageMetaData meta = new ImageMetaData(JPEG_MIME_TYPE, TEST_YCCK_JPG);
        assertEquals(TEST_YCCK_JPG, meta.getFilename());
        assertEquals(JPEG_MIME_TYPE, meta.getMimeType());
        assertTrue(meta.isJpeg());

        InputStream is = meta.parse(readImage(TEST_YCCK_JPG));
        assertEquals(TEST_YCCK_JPG, meta.getFilename());
        assertEquals(JPEG_MIME_TYPE, meta.getMimeType());
        assertTrue(meta.isJpeg());
        assertEquals(ImageMetaData.ColorModel.YCCK, meta.getColorModel());

        IOUtils.closeQuietly(is);
    }

    @Test
    public void testMetadataForCMYKJpeg() throws Exception {

        ImageMetaData meta = new ImageMetaData(JPEG_MIME_TYPE, TEST_CMYK_JPG);
        assertEquals(TEST_CMYK_JPG, meta.getFilename());
        assertEquals(JPEG_MIME_TYPE, meta.getMimeType());
        assertTrue(meta.isJpeg());

        InputStream is = meta.parse(readImage(TEST_CMYK_JPG));
        assertEquals(TEST_CMYK_JPG, meta.getFilename());
        assertEquals(JPEG_MIME_TYPE, meta.getMimeType());
        assertTrue(meta.isJpeg());
        assertEquals(ImageMetaData.ColorModel.CMYK, meta.getColorModel());

        IOUtils.closeQuietly(is);
    }

    @Test
    public void testMetadataForRGBPng() throws Exception {

        ImageMetaData meta = new ImageMetaData(PNG_MIME_TYPE, TEST_RGB_PNG);
        assertEquals(TEST_RGB_PNG, meta.getFilename());
        assertEquals(PNG_MIME_TYPE, meta.getMimeType());
        assertFalse(meta.isJpeg());

        InputStream is = meta.parse(readImage(TEST_RGB_PNG));
        assertEquals(TEST_RGB_PNG, meta.getFilename());
        assertEquals(PNG_MIME_TYPE, meta.getMimeType());
        assertFalse(meta.isJpeg());
        assertEquals(ImageMetaData.ColorModel.RGB, meta.getColorModel());

        IOUtils.closeQuietly(is);
    }

    @Test
    public void testMetadataForRGBGif() throws Exception {

        ImageMetaData meta = new ImageMetaData(GIF_MIME_TYPE, TEST_RGB_GIF);
        assertEquals(TEST_RGB_GIF, meta.getFilename());
        assertEquals(GIF_MIME_TYPE, meta.getMimeType());
        assertFalse(meta.isJpeg());

        InputStream is = meta.parse(readImage(TEST_RGB_GIF));
        assertEquals(TEST_RGB_GIF, meta.getFilename());
        assertEquals(GIF_MIME_TYPE, meta.getMimeType());
        assertFalse(meta.isJpeg());
        assertEquals(ImageMetaData.ColorModel.RGB, meta.getColorModel());

        IOUtils.closeQuietly(is);
    }

    /**
     * Sometimes images contain broken thumbnails which generates an error when the metadata is parsed. This test
     * verifies that this error is not thrown.
     *
     * @throws Exception
     */
    @Test
    public void testMetadataForBrokenThumbnailsJpg() throws Exception {

        ImageMetaData meta = new ImageMetaData(JPEG_MIME_TYPE, TEST_BROKEN_THUMBS_JPG);

        InputStream is = null;
        try {
            is = meta.parse(readImage(TEST_BROKEN_THUMBS_JPG));
        } catch(ImageMetadataException e) {
            fail("ImageMetadataException should not have been thrown");
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}
