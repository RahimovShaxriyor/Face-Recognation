package org.service;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.model.FaceOverlay;
import org.model.OverlayMode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecognitionService {

    private static final String CASCADE_RESOURCE = "/data/haarcascade_frontalface_default.xml";
    private static final int FACE_SIZE = 200;

    private CascadeClassifier faceDetector;
    private FaceRecognizer recognizer;

    public void initialize() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(CASCADE_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Не найден ресурс " + CASCADE_RESOURCE);
            }

            Path tempCascade = Files.createTempFile("haarcascade-", ".xml");
            Files.copy(input, tempCascade, StandardCopyOption.REPLACE_EXISTING);
            tempCascade.toFile().deleteOnExit();

            faceDetector = new CascadeClassifier(tempCascade.toString());

            if (faceDetector.empty()) {
                throw new IllegalStateException("CascadeClassifier не загрузился");
            }
        }
    }

    public RectVector detectFaces(Mat gray) {
        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(gray, faces);
        return faces;
    }

    public Mat toGray(Mat image) {
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);
        return gray;
    }

    public Mat extractFace(Mat gray, Rect rect) {
        Mat faceRoi = new Mat(gray, rect);
        Mat resized = new Mat();
        opencv_imgproc.resize(faceRoi, resized, new Size(FACE_SIZE, FACE_SIZE));
        return resized;
    }

    public boolean hasRecognizer() {
        return recognizer != null;
    }

    public List<FaceOverlay> analyze(
            Mat gray,
            RectVector faces,
            Map<Integer, String> users,
            boolean trainingMode,
            String trainingUserName,
            double confidenceThreshold
    ) {
        List<FaceOverlay> overlays = new ArrayList<>();

        for (long i = 0; i < faces.size(); i++) {
            Rect rect = faces.get(i);

            if (trainingMode) {
                String title = (trainingUserName == null || trainingUserName.isBlank())
                        ? "TRAINING"
                        : trainingUserName;

                overlays.add(new FaceOverlay(
                        rect,
                        title,
                        "CAPTURE MODE",
                        0.0,
                        OverlayMode.TRAINING
                ));
                continue;
            }

            if (recognizer == null) {
                overlays.add(new FaceOverlay(
                        rect,
                        "FACE DETECTED",
                        "MODEL OFFLINE",
                        0.0,
                        OverlayMode.DETECTED
                ));
                continue;
            }

            Mat sample = extractFace(gray, rect);

            int[] label = new int[1];
            double[] confidence = new double[1];
            recognizer.predict(sample, label, confidence);

            String name = users.get(label[0]);

            if (name != null && confidence[0] < confidenceThreshold) {
                overlays.add(new FaceOverlay(
                        rect,
                        name,
                        "IDENTITY VERIFIED",
                        confidence[0],
                        OverlayMode.RECOGNIZED
                ));
            } else {
                overlays.add(new FaceOverlay(
                        rect,
                        "UNKNOWN",
                        "NO MATCH FOUND",
                        confidence[0],
                        OverlayMode.UNKNOWN
                ));
            }
        }

        return overlays;
    }

    public void rebuildRecognizer(Map<Integer, String> users, StorageService storage) throws Exception {
        List<Mat> images = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : users.entrySet()) {
            int label = entry.getKey();

            for (Path file : storage.userImageFiles(label)) {
                Mat image = opencv_imgcodecs.imread(file.toString(), opencv_imgcodecs.IMREAD_GRAYSCALE);
                if (image == null || image.empty()) {
                    continue;
                }

                Mat resized = new Mat();
                opencv_imgproc.resize(image, resized, new Size(FACE_SIZE, FACE_SIZE));

                images.add(resized);
                labels.add(label);
            }
        }

        if (images.isEmpty()) {
            recognizer = null;
            return;
        }

        MatVector imagesMat = new MatVector(images.size());
        for (int i = 0; i < images.size(); i++) {
            imagesMat.put(i, images.get(i));
        }

        Mat labelsMat = new Mat(labels.size(), 1, opencv_core.CV_32SC1);
        for (int i = 0; i < labels.size(); i++) {
            labelsMat.ptr(i, 0).putInt(labels.get(i));
        }

        FaceRecognizer newRecognizer = LBPHFaceRecognizer.create();
        newRecognizer.train(imagesMat, labelsMat);
        newRecognizer.save(storage.modelFile().toString());

        recognizer = newRecognizer;
    }
}