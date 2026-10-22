package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

public class VideoAspectTest {
    @Test public void matching4kVideoFills1080pWithoutDistortion() {
        assertArrayEquals(new float[] {1f, 1f}, VideoAspect.centerCrop(1920, 1080, 3840, 2160), .001f);
    }
    @Test public void wideCinemaVideoCropsTheSides() {
        assertArrayEquals(new float[] {1.5f, 1f}, VideoAspect.centerCrop(1920, 1080, 2880, 1080), .001f);
    }
    @Test public void portraitVideoFillsRatherThanLetterboxes() {
        assertArrayEquals(new float[] {1f, 3.16049f}, VideoAspect.centerCrop(1920, 1080, 1080, 1920), .001f);
    }
    @Test public void missingMetadataDoesNotCreateAnInvalidMatrix() {
        assertArrayEquals(new float[] {1f, 1f}, VideoAspect.centerCrop(0, 1080, 0, 0), .001f);
    }
}
