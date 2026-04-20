package org.ui;

import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.model.FaceOverlay;
import org.model.OverlayMode;

import java.util.List;

public class HudRenderer {

    public void render(Mat image, List<FaceOverlay> overlays, long tick) {
        for (FaceOverlay overlay : overlays) {
            Scalar accent = colorForMode(overlay.mode());
            Rect rect = overlay.rect();

            drawCornerFrame(image, rect, accent);
            drawCenterMarker(image, rect, accent);
            drawScanLine(image, rect, accent, tick);
            drawHeaderPanel(image, rect, overlay, accent);
            drawFooterPanel(image, rect, overlay.status(), accent);
        }
    }

    private void drawCornerFrame(Mat image, Rect rect, Scalar color) {
        int x = rect.x();
        int y = rect.y();
        int w = rect.width();
        int h = rect.height();

        int cornerX = Math.max(18, w / 5);
        int cornerY = Math.max(18, h / 5);

        drawLine(image, x, y, x + cornerX, y, color, 2);
        drawLine(image, x, y, x, y + cornerY, color, 2);

        drawLine(image, x + w, y, x + w - cornerX, y, color, 2);
        drawLine(image, x + w, y, x + w, y + cornerY, color, 2);

        drawLine(image, x, y + h, x + cornerX, y + h, color, 2);
        drawLine(image, x, y + h, x, y + h - cornerY, color, 2);

        drawLine(image, x + w, y + h, x + w - cornerX, y + h, color, 2);
        drawLine(image, x + w, y + h, x + w, y + h - cornerY, color, 2);
    }

    private void drawCenterMarker(Mat image, Rect rect, Scalar color) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;

        opencv_imgproc.circle(image, new Point(cx, cy), 10, color, 1, 8, 0);
        opencv_imgproc.circle(image, new Point(cx, cy), 2, color, -1, 8, 0);

        drawLine(image, cx - 16, cy, cx - 5, cy, color, 1);
        drawLine(image, cx + 5, cy, cx + 16, cy, color, 1);
        drawLine(image, cx, cy - 16, cx, cy - 5, color, 1);
        drawLine(image, cx, cy + 5, cx, cy + 16, color, 1);
    }

    private void drawScanLine(Mat image, Rect rect, Scalar color, long tick) {
        int availableHeight = Math.max(1, rect.height() - 8);
        int offset = (int) (tick % availableHeight);
        int y = rect.y() + 4 + offset;

        drawLine(
                image,
                rect.x() + 6,
                y,
                rect.x() + rect.width() - 6,
                y,
                color,
                1
        );
    }

    private void drawHeaderPanel(Mat image, Rect rect, FaceOverlay overlay, Scalar accent) {
        String confidenceText = overlay.mode() == OverlayMode.TRAINING || overlay.mode() == OverlayMode.DETECTED
                ? "LIVE"
                : "CONF " + formatDouble(overlay.confidence());

        String text = overlay.title() + " | " + confidenceText;

        int boxWidth = clamp(text.length() * 9 + 20, 150, 360);
        int boxHeight = 26;

        int x = clamp(rect.x(), 6, Math.max(6, image.cols() - boxWidth - 6));
        int y = rect.y() - boxHeight - 8;

        if (y < 6) {
            y = rect.y() + 8;
        }

        drawPanel(image, x, y, boxWidth, boxHeight, accent);
        putText(image, text, x + 10, y + 17);
    }

    private void drawFooterPanel(Mat image, Rect rect, String text, Scalar accent) {
        int boxWidth = clamp(text.length() * 9 + 20, 140, 300);
        int boxHeight = 24;

        int x = clamp(rect.x(), 6, Math.max(6, image.cols() - boxWidth - 6));
        int y = rect.y() + rect.height() + 8;

        if (y + boxHeight > image.rows() - 6) {
            y = rect.y() + rect.height() - boxHeight - 8;
        }

        drawPanel(image, x, y, boxWidth, boxHeight, accent);
        putText(image, text, x + 10, y + 16);
    }

    private void drawPanel(Mat image, int x, int y, int width, int height, Scalar accent) {
        Point p1 = new Point(x, y);
        Point p2 = new Point(x + width, y + height);

        opencv_imgproc.rectangle(image, p1, p2, new Scalar(18, 18, 18, 0), -1, 8, 0);
        opencv_imgproc.rectangle(image, p1, p2, accent, 1, 8, 0);
    }

    private void putText(Mat image, String text, int x, int y) {
        opencv_imgproc.putText(
                image,
                text,
                new Point(x, y),
                opencv_imgproc.FONT_HERSHEY_SIMPLEX,
                0.45,
                new Scalar(255, 255, 255, 0),
                1,
                8,
                false
        );
    }

    private void drawLine(Mat image, int x1, int y1, int x2, int y2, Scalar color, int thickness) {
        opencv_imgproc.line(
                image,
                new Point(x1, y1),
                new Point(x2, y2),
                color,
                thickness,
                8,
                0
        );
    }

    private Scalar colorForMode(OverlayMode mode) {
        return switch (mode) {
            case TRAINING -> new Scalar(0, 200, 255, 0);
            case RECOGNIZED -> new Scalar(0, 255, 120, 0);
            case UNKNOWN -> new Scalar(0, 90, 255, 0);
            case DETECTED -> new Scalar(255, 180, 0, 0);
        };
    }

    private String formatDouble(double value) {
        return String.format("%.2f", value);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}