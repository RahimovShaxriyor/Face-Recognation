package org.controller;

import javafx.stage.Stage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.model.FaceOverlay;
import org.service.CameraService;
import org.service.RecognitionService;
import org.service.StorageService;
import org.ui.HudRenderer;
import org.ui.MainView;
import org.util.FxImageUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainController {

    private static final int TRAINING_SAMPLES = 30;
    private static final double CONFIDENCE_THRESHOLD = 80.0;
    private static final long TRAINING_CAPTURE_INTERVAL_MS = 180L;
    private static final long TRAINING_HINT_INTERVAL_MS = 1000L;

    private final MainView view = new MainView();
    private final StorageService storageService = new StorageService();
    private final RecognitionService recognitionService = new RecognitionService();
    private final CameraService cameraService = new CameraService();
    private final HudRenderer hudRenderer = new HudRenderer();

    private final Map<Integer, String> users = new LinkedHashMap<>();
    private final AtomicBoolean trainingMode = new AtomicBoolean(false);

    private String trainingUserName;
    private int trainingUserLabel = -1;
    private int collectedSamples = 0;
    private long lastCaptureAt = 0L;
    private long lastHintAt = 0L;
    private long hudTick = 0L;
    private String lastStatus = "";

    public void start(Stage stage) throws Exception {
        stage.setTitle("Face Commander Pro");
        stage.setScene(view.createScene());

        bindEvents();

        storageService.initialize();
        users.putAll(storageService.loadUsers());
        view.renderUsers(storageService.buildProfiles(users));

        recognitionService.initialize();
        if (!users.isEmpty()) {
            recognitionService.rebuildRecognizer(users, storageService);
        }

        cameraService.start(this::handleFrame, this::handleCameraError);

        if (users.isEmpty()) {
            setStatus("Введите имя и нажмите «Сохранить лицо»");
        } else {
            setStatus("Камера запущена. Система готова.");
        }

        stage.setOnCloseRequest(e -> shutdown());
        stage.show();
    }

    private void bindEvents() {
        view.getSaveFaceButton().setOnAction(e -> beginTraining());

        view.getUsersListView().getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                view.getNameField().setText(newValue.name());
            }
        });
    }

    private void beginTraining() {
        if (trainingMode.get()) {
            setStatus("Обучение уже идёт");
            return;
        }

        String name = normalizeName(view.getNameField().getText());
        if (name.isBlank()) {
            setStatus("Введите имя пользователя");
            return;
        }

        int label = findUserLabelByName(name);
        if (label == -1) {
            label = nextUserLabel();
            users.put(label, name);

            try {
                storageService.saveUsers(users);
            } catch (Exception e) {
                users.remove(label);
                setStatus("Не удалось сохранить пользователя: " + e.getMessage());
                return;
            }
        }

        trainingMode.set(true);
        trainingUserName = name;
        trainingUserLabel = label;
        collectedSamples = 0;
        lastCaptureAt = 0L;
        lastHintAt = 0L;

        view.setTrainingState(true);
        view.renderUsers(storageService.buildProfiles(users));
        setStatus("Режим обучения: " + name + ". Смотрите прямо в камеру.");
    }

    private void handleFrame(Mat image) {
        try {
            hudTick++;

            Mat gray = recognitionService.toGray(image);
            RectVector faces = recognitionService.detectFaces(gray);

            if (trainingMode.get()) {
                processTraining(gray, faces);
            }

            List<FaceOverlay> overlays = recognitionService.analyze(
                    gray,
                    faces,
                    users,
                    trainingMode.get(),
                    trainingUserName,
                    CONFIDENCE_THRESHOLD
            );

            hudRenderer.render(image, overlays, hudTick);
            view.renderFrame(FxImageUtils.matToFxImage(image));

            if (!trainingMode.get()) {
                updateRecognitionStatus(overlays);
            }
        } catch (Exception e) {
            setStatus("Ошибка обработки кадра: " + e.getMessage());
        }
    }

    private void processTraining(Mat gray, RectVector faces) {
        long now = System.currentTimeMillis();

        if (faces.size() != 1) {
            if (now - lastHintAt > TRAINING_HINT_INTERVAL_MS) {
                if (faces.size() == 0) {
                    setStatus("Не вижу лица. Подойдите ближе к камере.");
                } else {
                    setStatus("В кадре должно быть только одно лицо.");
                }
                lastHintAt = now;
            }
            return;
        }

        if (now - lastCaptureAt < TRAINING_CAPTURE_INTERVAL_MS) {
            return;
        }

        try {
            Rect rect = faces.get(0);
            Mat face = recognitionService.extractFace(gray, rect);

            storageService.saveTrainingSample(trainingUserLabel, collectedSamples, face);
            collectedSamples++;
            lastCaptureAt = now;

            setStatus("Сохраняю " + trainingUserName + ": " + collectedSamples + "/" + TRAINING_SAMPLES);

            if (collectedSamples >= TRAINING_SAMPLES) {
                finishTraining();
            }
        } catch (Exception e) {
            stopTrainingWithError("Ошибка сохранения лица: " + e.getMessage());
        }
    }

    private void finishTraining() {
        try {
            recognitionService.rebuildRecognizer(users, storageService);

            String finishedName = trainingUserName;

            trainingMode.set(false);
            trainingUserName = null;
            trainingUserLabel = -1;
            collectedSamples = 0;

            view.setTrainingState(false);
            view.renderUsers(storageService.buildProfiles(users));
            setStatus("Обучение завершено. Пользователь сохранён: " + finishedName);
        } catch (Exception e) {
            stopTrainingWithError("Ошибка сборки модели: " + e.getMessage());
        }
    }

    private void stopTrainingWithError(String message) {
        trainingMode.set(false);
        trainingUserName = null;
        trainingUserLabel = -1;
        collectedSamples = 0;

        view.setTrainingState(false);
        setStatus(message);
    }

    private void updateRecognitionStatus(List<FaceOverlay> overlays) {
        if (overlays.isEmpty()) {
            setStatus("Лицо не найдено");
            return;
        }

        FaceOverlay first = overlays.get(0);

        switch (first.mode()) {
            case RECOGNIZED -> setStatus("Распознан: " + first.title() + " | confidence=" + formatDouble(first.confidence()));
            case UNKNOWN -> setStatus("Пользователь не распознан | confidence=" + formatDouble(first.confidence()));
            case DETECTED -> setStatus("Лицо найдено, но модель пока не собрана");
            default -> {
            }
        }
    }

    private void handleCameraError(String error) {
        setStatus("Ошибка камеры: " + error);
    }

    private void setStatus(String text) {
        if (text == null || text.equals(lastStatus)) {
            return;
        }

        lastStatus = text;
        view.setStatusText(text);
    }

    private int findUserLabelByName(String name) {
        for (Map.Entry<Integer, String> entry : users.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private int nextUserLabel() {
        return users.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String formatDouble(double value) {
        return String.format("%.2f", value);
    }

    public void shutdown() {
        cameraService.stop();
    }
}