package dashboard;

import com.google.gson.Gson;
import dashboard.tableitems.GraphInfoTableItem;
import dtos.GraphInfoDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import main.MainController;
import main.include.Constants;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import users.UsersLists;
import util.http.HttpClientUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class DashboardController {
    private ObservableList<String> onlineGraphsList = FXCollections.observableArrayList();
    private ObservableList<String> onlineTasksList = FXCollections.observableArrayList();
    private ObservableList<String> onlineAdminsList = FXCollections.observableArrayList();
    private ObservableList<String> onlineWorkersList = FXCollections.observableArrayList();
    private ObservableList<String> currentSelectedGraphList = FXCollections.observableArrayList();
    private ObservableList<String> currentSelectedMyTasksList = FXCollections.observableArrayList();
    private ObservableList<String> currentSelectedAllTasksList = FXCollections.observableArrayList();
    private ObservableList<GraphInfoTableItem> graphInfoTableList = FXCollections.observableArrayList();
    private ListChangeListener<String> currentSelectedGraphListListener;
    private ListChangeListener<String> currentSelectedMyTasksListListener;
    private ListChangeListener<String> currentSelectedAllTasksListListener;
    private SimpleBooleanProperty isGraphSelected;
    private SimpleBooleanProperty isMyTaskSelected;

    private Stage primaryStage;
    private String userName;
    private final FileChooser fileChooser = new FileChooser();
    private static String lastVisitedDirectory = System.getProperty("user.home");
    private Thread refreshDashboardDataThread;
    private MainController mainController;

    public DashboardController() {
        isGraphSelected = new SimpleBooleanProperty(false);
        isMyTaskSelected = new SimpleBooleanProperty(false);
        currentSelectedGraphListListener = change -> {
            displaySelectedGraphInfo();
            isGraphSelected.setValue(change.getList().size() != 0);
        };
        currentSelectedMyTasksListListener = change -> {
            isMyTaskSelected.setValue(change.getList().size() != 0);
        };
        currentSelectedAllTasksListListener = change -> {
            displaySelectedTaskInfo();
        };
    }

    public void initialize() {
        LoadGraphButton.disableProperty().bind(isGraphSelected.not());
        loadSelectedTaskButton.disableProperty().bind(isMyTaskSelected.not());
        currentSelectedGraphList = OnlineGraphsListView.getSelectionModel().getSelectedItems();
        currentSelectedMyTasksList = myTasksListView.getSelectionModel().getSelectedItems();
        currentSelectedAllTasksList = AllTasksListView.getSelectionModel().getSelectedItems();
        currentSelectedGraphList.addListener(currentSelectedGraphListListener);
        currentSelectedMyTasksList.addListener(currentSelectedMyTasksListListener);
        currentSelectedAllTasksList.addListener(currentSelectedAllTasksListListener);
        initializeTargetDetailsTable();
        refreshDashboardDataThread = new Thread(this::refreshDashboardData);
        Thread suddenExitHook = new Thread(this::logout);
        Runtime.getRuntime().addShutdownHook(suddenExitHook);
        onlineAdminsListView.setItems(onlineAdminsList);
        onlineWorkersListView.setItems(onlineWorkersList);
        OnlineGraphsListView.setItems(onlineGraphsList);
        refreshDashboardDataThread.setDaemon(true);
        refreshDashboardDataThread.start();
    }

    @FXML
    private TitledPane OnlineGraphsTiltedPane;
    @FXML
    private ListView<String> OnlineGraphsListView;
    @FXML
    private Button AddNewGraphButton;
    @FXML
    private Button LoadGraphButton;
    @FXML
    private TitledPane OnlineAdminsTiltedPane;
    @FXML
    private ListView<String> onlineAdminsListView;
    @FXML
    private TitledPane OnlineWorkersTiltedPane;
    @FXML
    private ListView<String> onlineWorkersListView;
    @FXML
    private TitledPane OnlineTasksTiltedPane;
    @FXML
    private ListView<String> myTasksListView;
    @FXML
    private Button loadSelectedTaskButton;
    @FXML
    private ListView<String> AllTasksListView;
    @FXML
    private Font x11;
    @FXML
    private Color x21;
    @FXML
    private TextField GraphNameTextField;
    @FXML
    private TextField uploadedByTextField;
    @FXML
    private TableView<GraphInfoTableItem> GraphTargetsTableView;
    @FXML
    private TableColumn<GraphInfoTableItem, Integer> GraphTargetsAmount;
    @FXML
    private TableColumn<GraphInfoTableItem, Integer> GraphIndependentAmount;
    @FXML
    private TableColumn<GraphInfoTableItem, Integer> GraphLeafAmount;
    @FXML
    private TableColumn<GraphInfoTableItem, Integer> GraphMiddleAmount;
    @FXML
    private TableColumn<GraphInfoTableItem, Integer> GraphRootAmount;
    @FXML
    private TextField SimulationPriceTextField;
    @FXML
    private TextField CompilationPriceTextField;
    @FXML
    private Font x1;
    @FXML
    private Color x2;
    @FXML
    private TextField TaskNameTextField;
    @FXML
    private TextField CreatedByTextField;
    @FXML
    private TextField TaskOnGraphTextField;
    @FXML
    private TableView<GraphInfoTableItem> TaskTypeTableView;
    @FXML
    private TableColumn<GraphInfoTableItem, Integer> TaskTargetsAmount;
    @FXML
    private TableColumn<GraphInfoTableItem, Integer> TaskIndependentAmount;
    @FXML
    private TableColumn<GraphInfoTableItem, Integer> TaskLeafAmount;
    @FXML
    private TableColumn<GraphInfoTableItem, Integer> TaskMiddleAmount;
    @FXML
    private TableColumn<GraphInfoTableItem, Integer> TaskRootAmount;
    @FXML
    private TableView<?> TaskInfoTableView;
    @FXML
    private TableColumn<?, ?> TaskStatus;
    @FXML
    private TableColumn<?, ?> currentWorkers;
    @FXML
    private TableColumn<?, ?> TaskWorkPayment;

    @FXML
    void AddNewGraphButtonClicked(ActionEvent event) throws IOException {
        addNewGraphToList();
    }

    public void addNewGraphToList() throws IOException {
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("TXT files (*.xml)", "*.xml");
        fileChooser.getExtensionFilters().add(extensionFilter);
        fileChooser.setInitialDirectory(new File(lastVisitedDirectory));
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null) {
            uploadFileToServer(Constants.GRAPHS_PATH, file);
            lastVisitedDirectory = file.getParent();
        }
    }

    public void uploadFileToServer(String url, File file) throws IOException {
        RequestBody body = new MultipartBody.Builder()
                .addFormDataPart("fileToUpload", file.getName(),
                        RequestBody.create(file, MediaType.parse("xml")))
                .build();

        Request request = new Request.Builder()
                .url(Constants.GRAPHS_PATH)
                .post(body).addHeader("username", this.userName)
                .build();

        HttpClientUtil.runAsyncWithRequest(request, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Platform.runLater(() -> errorPopup(e.getMessage()));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.code() < 200 || response.code() >= 300) {
                    Platform.runLater(() -> errorPopup(response.header("message")));
                }
            }
        });
    }

    @FXML
    void LoadGraphButtonClicked(ActionEvent event) {
        String selectedGraphName = this.OnlineGraphsListView.getSelectionModel().getSelectedItem();

        if(selectedGraphName == null)
            return;

        mainController.setSelectedGraphTextField(selectedGraphName);
        this.OnlineGraphsListView.getSelectionModel().clearSelection();
        String finalUrl = HttpUrl
                .parse(Constants.GRAPHS_PATH)
                .newBuilder()
                .addQueryParameter("graph", selectedGraphName)
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, "GET", null, new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Platform.runLater(() ->
                        errorPopup(e.getMessage()));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.code() >= 200 && response.code() < 300) //Success
                {
                    Gson gson = new Gson();
                    ResponseBody responseBody = response.body();
                    File graphXMLFile = gson.fromJson(responseBody.string(), File.class);
                    responseBody.close();
                    Platform.runLater(()-> mainController.LoadXMLFile(graphXMLFile));
                } else //Failed
                {
                    Platform.runLater(() -> errorPopup(response.message()));
                }
            }
        });
    }

    @FXML
    void loadSelectedTaskButtonClicked(ActionEvent event) {
        String selectedTaskName = this.myTasksListView.getSelectionModel().getSelectedItem();

        if(selectedTaskName == null)
            return;

        mainController.setSelectedTaskTextField(selectedTaskName);
        this.myTasksListView.getSelectionModel().clearSelection();


    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void errorPopup(String message) {
        Toolkit.getDefaultToolkit().beep();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Loading error");
        alert.setHeaderText(message);
        alert.initOwner(primaryStage);
        Optional<ButtonType> result = alert.showAndWait();
    }

    private void refreshDashboardData() {
        while (refreshDashboardDataThread.isAlive()) {
            getUsersLists();
            refreshGraphList();
        }
    }

    private void getUsersLists() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String finalUrl = HttpUrl
                .parse(Constants.USERS_LISTS)
                .newBuilder()
                .build()
                .toString();


        HttpClientUtil.runAsync(finalUrl, "GET", null, new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                Gson gson = new Gson();
                ResponseBody responseBody = response.body();
                UsersLists usersLists = gson.fromJson(responseBody.string(), UsersLists.class);
                responseBody.close();
                Platform.runLater(() -> {
                    updateUsersLists(usersLists);
                });
            }
        });
    }

    private void updateUsersLists(UsersLists usersLists) {
        onlineAdminsList.clear();
        onlineWorkersList.clear();
        onlineAdminsList.addAll(usersLists.getAdminsList());
        onlineWorkersList.addAll(usersLists.getWorkersList());
    }

    public void initializeTargetDetailsTable() {
        this.GraphTargetsAmount.setCellValueFactory(new PropertyValueFactory<GraphInfoTableItem, Integer>("targets"));
        this.GraphRootAmount.setCellValueFactory(new PropertyValueFactory<GraphInfoTableItem, Integer>("roots"));
        this.GraphMiddleAmount.setCellValueFactory(new PropertyValueFactory<GraphInfoTableItem, Integer>("middles"));
        this.GraphLeafAmount.setCellValueFactory(new PropertyValueFactory<GraphInfoTableItem, Integer>("leaves"));
        this.GraphIndependentAmount.setCellValueFactory(new PropertyValueFactory<GraphInfoTableItem, Integer>("independents"));
    }

    private void logout() {
        if (userName == null)
            return;

        String finalUrl = HttpUrl
                .parse(Constants.LOGOUT_PAGE)
                .newBuilder()
                .addQueryParameter("username", userName)
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

    private void refreshGraphList() {
        String finalUrl = HttpUrl
                .parse(Constants.GRAPHS_LISTS_PAGE)
                .newBuilder()
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, "GET", null, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.code() >= 200 && response.code() < 300) //Success
                {
                    Platform.runLater(() ->
                            {
                                Gson gson = new Gson();
                                ResponseBody responseBody = response.body();
                                try {
                                    if (responseBody != null) {
                                        Set graphsSet = gson.fromJson(responseBody.string(), Set.class);
                                        responseBody.close();
                                        updateGraphListView(graphsSet);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                    );
                }
            }
        });
    }

    private void updateGraphListView(Set<String> graphsSet) {
        if (graphsSet == null)
            return;

        for (String curr : graphsSet) {
            if (!onlineGraphsList.contains(curr))
                onlineGraphsList.add(curr);
        }
    }
    private void updateTasksListView(Set<String> taskSet) {
        if (taskSet == null)
            return;

        for (String curr : taskSet) {
            if (!onlineTasksList.contains(curr))
                onlineTasksList.add(curr);
        }
    }


    private void displaySelectedTaskInfo() {
        if (currentSelectedAllTasksList.isEmpty())
            return;

        String selectedTaskName = currentSelectedAllTasksList.get(0);

    }

    private void displaySelectedGraphInfo() {
        if (currentSelectedGraphList.isEmpty())
            return;

        String selectedGraphName = currentSelectedGraphList.get(0);

        String finalUrl = HttpUrl
                .parse(Constants.GRAPHS_PATH)
                .newBuilder()
                .addQueryParameter("selectedGraphName", selectedGraphName)
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, "GET", null, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.code() >= 200 && response.code() < 300) //Success
                {
                    Platform.runLater(() ->
                            {
                                Gson gson = new Gson();
                                ResponseBody responseBody = response.body();
                                try {
                                    if (responseBody != null) {
                                        GraphInfoDto graphInfoDto = gson.fromJson(responseBody.string(), GraphInfoDto.class);
                                        responseBody.close();
                                        refreshGraphDetailsDTO(graphInfoDto);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                    );
                }
            }
        });
    }

    private void refreshGraphDetailsDTO(GraphInfoDto graphInfoDto) {
        this.GraphNameTextField.setText(graphInfoDto.getGraphName());
        this.uploadedByTextField.setText(graphInfoDto.getUploader());
        this.SimulationPriceTextField.setText(graphInfoDto.getSimulationPrice().toString());
        this.CompilationPriceTextField.setText(graphInfoDto.getCompilationPrice().toString());

        updateTargetDetailsTable(graphInfoDto);
    }

    private void updateTargetDetailsTable(GraphInfoDto graphInfoDto) {

        GraphInfoTableItem graphInfoTableItem = new GraphInfoTableItem(graphInfoDto.getRoots(),
                graphInfoDto.getMiddles(), graphInfoDto.getLeaves(), graphInfoDto.getIndependents(), graphInfoDto.getTargets());

        this.graphInfoTableList.clear();
        this.graphInfoTableList.add(graphInfoTableItem);

        DashboardController.this.GraphTargetsTableView.setItems(this.graphInfoTableList);
    }

    public MainController getMainController() {
        return mainController;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}
