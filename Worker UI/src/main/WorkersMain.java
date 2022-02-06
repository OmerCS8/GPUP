package main;

import constants.WorkersConstants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import login.WorkerLoginController;
import main.include.Constants;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import util.http.HttpClientUtil;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import static constants.WorkersConstants.WORKERS_LOGIN_FXML_RESOURCE;
import static main.include.Constants.LOGIN_FXML_RESOURCE;


public class WorkersMain extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.getIcons().add(new Image(WorkersConstants.ICON_IMAGE));
        FXMLLoader fxmlLoader = new FXMLLoader();
        URL url = getClass().getResource(WORKERS_LOGIN_FXML_RESOURCE);
        fxmlLoader.setLocation(url);
        VBox loginComponent = fxmlLoader.load(url.openStream());
        WorkerLoginController workerLoginController = fxmlLoader.getController();

        Scene scene = new Scene(loginComponent,400, 280);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(WorkersConstants.CLASSIC_CSS)).toExternalForm());
        primaryStage.setScene(scene);

        workerLoginController.initialize(primaryStage);

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Close confirmation");
                alert.setHeaderText("Are you sure you want to exit?");

                alert.initOwner(primaryStage);
                Toolkit.getDefaultToolkit().beep();
                Optional<ButtonType> result = alert.showAndWait();
                if(result.get() == ButtonType.OK) {
                    logout(workerLoginController.getCurrentUser());
                    Platform.exit();
                }
                event.consume();
            }
        });
        primaryStage.show();
    }


    private void logout(String userName) {
        if(userName == null)
            return;

        String finalUrl = HttpUrl
                .parse(Constants.LOGOUT_PAGE)
                .newBuilder()
                .addQueryParameter("username",userName)
                .build()
                .toString();


        HttpClientUtil.runAsync(finalUrl, "DELETE", null, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
            }
        });
    }
}
