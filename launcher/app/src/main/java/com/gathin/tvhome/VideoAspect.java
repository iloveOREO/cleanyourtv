package com.gathin.tvhome;

/** TextureView scales the source to its bounds first; restore its aspect ratio by cropping. */
final class VideoAspect {
    private VideoAspect() {}

    static float[] centerCrop(int viewWidth, int viewHeight, int videoWidth, int videoHeight) {
        if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) {
            return new float[] {1f, 1f};
        }
        float ratio = ((float) videoWidth / videoHeight) / ((float) viewWidth / viewHeight);
        return ratio >= 1f ? new float[] {ratio, 1f} : new float[] {1f, 1f / ratio};
    }
}
