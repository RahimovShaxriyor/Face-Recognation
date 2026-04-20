package org.service;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CameraService {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private OpenCVFrameGrabber grabber;
    private Thread worker;

    public void start(Consumer<Mat> frameConsumer, Consumer<String> errorConsumer) {
        if (running.get()) {
            return;
        }

        running.set(true);

        worker = new Thread(() -> {
            OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

            try {
                grabber = new OpenCVFrameGrabber(0);
                grabber.start();

                while (running.get()) {
                    Frame frame = grabber.grab();
                    if (frame == null) {
                        continue;
                    }

                    Mat mat = converter.convert(frame);
                    if (mat == null) {
                        continue;
                    }

                    frameConsumer.accept(mat.clone());
                    Thread.sleep(33);
                }
            } catch (Exception e) {
                errorConsumer.accept(e.getMessage());
            } finally {
                stop();
            }
        });

        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running.set(false);

        if (grabber != null) {
            try {
                grabber.stop();
            } catch (Exception ignored) {
            }
        }
    }
}