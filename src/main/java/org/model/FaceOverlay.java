package org.model;

import org.bytedeco.opencv.opencv_core.Rect;

public record FaceOverlay(
        Rect rect,
        String title,
        String status,
        double confidence,
        OverlayMode mode
) {
}