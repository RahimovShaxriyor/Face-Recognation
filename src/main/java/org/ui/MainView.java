package org.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.model.UserProfile;

import java.util.List;

public class MainView {

    private final BorderPane root = new BorderPane();
    private final ObservableList<UserProfile> userItems = FXCollections.observableArrayList();

    private final ImageView cameraView = new ImageView();
    private final ListView<UserProfile> usersListView = new ListView<>(userItems);
    private final TextField nameField = new TextField();
    private final Button saveFaceButton = new Button("Сохранить лицо");
    private final Label statusLabel = new Label("Инициализация...");

    public MainView() {
        build();
    }

    private void build() {
        root.setStyle("-fx-background-color: #151515;");

        HBox topBar = new HBox();
        topBar.setPadding(new Insets(16));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setMinHeight(72);
        topBar.setStyle("-fx-background-color: #232323;");

        Label title = new Label("Face Commander Pro");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(26));
        topBar.getChildren().add(title);

        VBox leftPanel = new VBox(12);
        leftPanel.setPadding(new Insets(16));
        leftPanel.setMinWidth(260);
        leftPanel.setPrefWidth(320);
        leftPanel.setMaxWidth(420);
        leftPanel.setStyle("-fx-background-color: #1e1e1e;");

        Label usersTitle = new Label("Пользователи");
        usersTitle.setTextFill(Color.WHITE);
        usersTitle.setFont(Font.font(18));

        usersListView.setPlaceholder(new Label("Пока нет пользователей"));
        usersListView.setPrefHeight(500);
        usersListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(UserProfile item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label name = new Label(item.name());
                name.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

                Label meta = new Label("ID: " + item.label() + " • фото: " + item.samples());
                meta.setStyle("-fx-text-fill: #b8b8b8; -fx-font-size: 12px;");

                VBox box = new VBox(4, name, meta);
                box.setPadding(new Insets(8));
                box.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10;");

                setGraphic(box);
            }
        });
        VBox.setVgrow(usersListView, Priority.ALWAYS);

        Label nameTitle = new Label("Имя пользователя");
        nameTitle.setTextFill(Color.LIGHTGRAY);

        nameField.setPromptText("Например: Ali или Иван");
        nameField.setMaxWidth(Double.MAX_VALUE);
        nameField.setStyle("-fx-font-size: 14px; -fx-background-radius: 10;");

        saveFaceButton.setMaxWidth(Double.MAX_VALUE);
        saveFaceButton.setStyle("-fx-background-color: #4f7cff; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 10;");

        leftPanel.getChildren().addAll(
                usersTitle,
                usersListView,
                nameTitle,
                nameField,
                saveFaceButton
        );

        cameraView.setPreserveRatio(true);
        cameraView.setSmooth(true);
        cameraView.setCache(true);

        StackPane centerPane = new StackPane(cameraView);
        centerPane.setPadding(new Insets(20));
        centerPane.setStyle("-fx-background-color: #0f0f0f;");

        cameraView.fitWidthProperty().bind(centerPane.widthProperty().subtract(40));
        cameraView.fitHeightProperty().bind(centerPane.heightProperty().subtract(40));

        HBox bottomBar = new HBox();
        bottomBar.setPadding(new Insets(12, 16, 12, 16));
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setMinHeight(52);
        bottomBar.setStyle("-fx-background-color: #232323;");

        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setFont(Font.font(14));
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        bottomBar.getChildren().add(statusLabel);

        root.setTop(topBar);
        root.setLeft(leftPanel);
        root.setCenter(centerPane);
        root.setBottom(bottomBar);
    }

    public Scene createScene() {
        return new Scene(root, 1180, 760);
    }

    public TextField getNameField() {
        return nameField;
    }

    public Button getSaveFaceButton() {
        return saveFaceButton;
    }

    public ListView<UserProfile> getUsersListView() {
        return usersListView;
    }

    public void renderFrame(Image image) {
        Platform.runLater(() -> cameraView.setImage(image));
    }

    public void setStatusText(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    public void renderUsers(List<UserProfile> profiles) {
        Platform.runLater(() -> userItems.setAll(profiles));
    }

    public void setTrainingState(boolean training) {
        Platform.runLater(() -> {
            saveFaceButton.setDisable(training);
            nameField.setDisable(training);
        });
    }
}