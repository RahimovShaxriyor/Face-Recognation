package org.util;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;

public final class FxImageUtils {

    private FxImageUtils() {
    }

    public static Image matToFxImage(Mat mat) {
        Mat bgra = new Mat();
        opencv_imgproc.cvtColor(mat, bgra, opencv_imgproc.COLOR_BGR2BGRA);

        byte[] data = new byte[(int) (bgra.total() * bgra.channels())];
        bgra.data().get(data);

        WritableImage image = new WritableImage(bgra.cols(), bgra.rows());
        image.getPixelWriter().setPixels(
                0,
                0,
                bgra.cols(),
                bgra.rows(),
                javafx.scene.image.PixelFormat.getByteBgraInstance(),
                data,
                0,
                bgra.cols() * 4
        );
        return image;
    }
}