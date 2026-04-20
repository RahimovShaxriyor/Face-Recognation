package org.service;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.model.UserProfile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class StorageService {

    private final Path rootDir = Paths.get("face_data");
    private final Path usersFile = rootDir.resolve("users.xml");
    private final Path imagesDir = rootDir.resolve("images");
    private final Path modelFile = rootDir.resolve("users_model.yml");

    public void initialize() throws Exception {
        Files.createDirectories(rootDir);
        Files.createDirectories(imagesDir);
    }

    public Path modelFile() {
        return modelFile;
    }

    public Path userDir(int label) {
        return imagesDir.resolve(String.valueOf(label));
    }

    public Map<Integer, String> loadUsers() throws Exception {
        Map<Integer, String> users = new LinkedHashMap<>();

        if (!Files.exists(usersFile)) {
            return users;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(usersFile)) {
            properties.loadFromXML(input);
        }

        properties.stringPropertyNames()
                .stream()
                .map(Integer::parseInt)
                .sorted()
                .forEach(id -> users.put(id, properties.getProperty(String.valueOf(id))));

        return users;
    }

    public void saveUsers(Map<Integer, String> users) throws Exception {
        Properties properties = new Properties();

        for (Map.Entry<Integer, String> entry : users.entrySet()) {
            properties.setProperty(String.valueOf(entry.getKey()), entry.getValue());
        }

        try (OutputStream output = Files.newOutputStream(usersFile)) {
            properties.storeToXML(output, "SmartFace users", "UTF-8");
        }
    }

    public void saveTrainingSample(int label, int sampleIndex, Mat image) throws Exception {
        Path dir = userDir(label);
        Files.createDirectories(dir);

        String fileName = "sample_" + System.currentTimeMillis() + "_" + sampleIndex + ".png";
        Path filePath = dir.resolve(fileName);

        boolean written = opencv_imgcodecs.imwrite(filePath.toString(), image);
        if (!written) {
            throw new IllegalStateException("Не удалось сохранить изображение");
        }
    }

    public int countSamples(int label) {
        Path dir = userDir(label);
        if (!Files.exists(dir)) {
            return 0;
        }

        try (var stream = Files.list(dir)) {
            return (int) stream
                    .filter(Files::isRegularFile)
                    .filter(this::isImageFile)
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    public List<Path> userImageFiles(int label) throws Exception {
        Path dir = userDir(label);
        if (!Files.exists(dir)) {
            return List.of();
        }

        try (var stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isImageFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }
    }

    public List<UserProfile> buildProfiles(Map<Integer, String> users) {
        List<UserProfile> profiles = new ArrayList<>();

        users.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> profiles.add(new UserProfile(
                        entry.getKey(),
                        entry.getValue(),
                        countSamples(entry.getKey())
                )));

        return profiles;
    }

    private boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }
}