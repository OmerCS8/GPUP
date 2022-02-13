package tasks.control;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import constants.WorkersConstants;
import dashboard.tableitems.SelectedTaskStatusTableItem;
import dashboard.tableitems.TargetsInfoTableItem;
import dtos.TaskDetailsDto;
import dtos.WorkerTaskDetailsDto;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import main.WorkerMainController;
import main.include.Constants;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import target.TargetForWorker;
import util.http.HttpClientUtil;

import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class TaskController {
    private String userName;
    private SimpleBooleanProperty isPaused;
    private final ChangeListener<Boolean> isPausedListener;
    private ObservableList<String> registeredTasksList = FXCollections.observableArrayList();
    private ObservableList<String> usersTargetList = FXCollections.observableArrayList();
    private ObservableList<String> currentSelectedTaskList = FXCollections.observableArrayList();
    private ObservableList<String> currentSelectedTargetList = FXCollections.observableArrayList();
    private ObservableList<TaskTableItem> TaskInfoTableList = FXCollections.observableArrayList();
    private ListChangeListener<String> currentSelectedTaskListListener;
    private ListChangeListener<String> currentSelectedTargetListListener;
    private SimpleBooleanProperty isTaskSelected;
    private SimpleBooleanProperty isTargetSelected;
    private Thread refreshDataThread;
    private Integer currTaskAmountOfChosenTargets = 1;
    private Integer currTaskAmountOfFinishedTargets = 0;
    private Thread progressBarThread = null;
    private Stage primaryStage;
    private WorkerMainController workerMainController;

    public TaskController() {
        isTaskSelected = new SimpleBooleanProperty(false);
        isTargetSelected = new SimpleBooleanProperty(false);
        isPaused = new SimpleBooleanProperty(false);

        currentSelectedTaskListListener = change -> {
            displaySelectedTaskInfo();
            isTaskSelected.setValue(change.getList().size() != 0);
            resetProgressBarValues();
            if(!currentSelectedTaskList.isEmpty())
                createNewProgressBar();
        };

        currentSelectedTargetListListener = change -> {
            isTargetSelected.setValue(change.getList().size() != 0);
            displaySelectedTargetInfo();
            if (!currentSelectedTaskList.isEmpty())
                isPaused.setValue(this.workerMainController.getTaskExecutor().getPausedTasksSet()
                        .contains(MyTasksListView.getSelectionModel().getSelectedItem()));
        };

        isPausedListener = (observable, oldValue, newValue) -> {
            if (newValue)
                PauseTaskButton.setText("Resume");
            else
                PauseTaskButton.setText("Pause");
        };
        isPaused.addListener(isPausedListener);
    }

    public void initialize() {
        currentSelectedTargetList = MyTargetsListView.getSelectionModel().getSelectedItems();
        currentSelectedTaskList = MyTasksListView.getSelectionModel().getSelectedItems();
        currentSelectedTargetList.addListener(currentSelectedTargetListListener);
        currentSelectedTaskList.addListener(currentSelectedTaskListListener);
        QuitTaskButton.disableProperty().bind(isTaskSelected.not());
        PauseTaskButton.disableProperty().bind(isTaskSelected.not());
        initializeTaskTable();
        MyTargetsListView.setItems(usersTargetList);
        MyTasksListView.setItems(registeredTasksList);
        refreshDataThread = new Thread(this::refreshData);
        refreshDataThread.start();

    }

    @FXML
    private TitledPane OnlineTasksTiltedPane1;

    @FXML
    private ListView<String> MyTasksListView;

    @FXML
    private Font x111;

    @FXML
    private Color x211;

    @FXML
    private TextField TaskNameTextField;

    @FXML
    private TableView<TaskTableItem> TaskInfoTableView;

    @FXML
    private TableColumn<TaskTableItem, String> TaskStatus;

    @FXML
    private TableColumn<TaskTableItem, Integer> AmountOfWorkers;

    @FXML
    private TextField TargetsDoneTextField;

    @FXML
    private TextField TaskCreditsEarnedTextField;

    @FXML
    private Button PauseTaskButton;

    @FXML
    private Button QuitTaskButton;

    @FXML
    private Font x1111;

    @FXML
    private Color x2111;

    @FXML
    private ProgressBar TaskProgressBar;

    @FXML
    private TitledPane OnlineTasksTiltedPane;

    @FXML
    private ListView<String> MyTargetsListView;

    @FXML
    private Font x11;

    @FXML
    private Color x21;

    @FXML
    private TextField TargetNameTextField;

    @FXML
    private TextField TargetTaskNameTextField;

    @FXML
    private TextField TargetTaskTypeTextField;

    @FXML
    private TextField TargetStatusTextField;

    @FXML
    private TextField TargetCreditsEarnedTextField;

    @FXML
    private Font x112;

    @FXML
    private Color x212;

    @FXML
    private TextArea TargetsRunLogTextArea;

    @FXML
    private Label ProgressBarLabel;

    @FXML
    void PauseTaskButtonClicked(ActionEvent event) {
        isPaused.setValue(!isPaused.getValue());
        if (isPaused.getValue())
            this.workerMainController.getTaskExecutor().pauseRegisteredTask(MyTargetsListView.getSelectionModel().getSelectedItem());
        else
            this.workerMainController.getTaskExecutor().resumeRegisteredTask(MyTargetsListView.getSelectionModel().getSelectedItem());

    }

    @FXML
    void QuitTaskButtonClicked(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quit task confirmation");
        alert.setHeaderText("Are you sure you want to quit the task?");

        alert.initOwner(primaryStage);
        Toolkit.getDefaultToolkit().beep();
        Optional<ButtonType> result = alert.showAndWait();
        if(result.get() == ButtonType.OK) {
            String selectedItem = MyTasksListView.getSelectionModel().getSelectedItem();
            if(selectedItem != null) {
                unregisterFromTask(selectedItem);
                clearTaskDetails();
            }
        }
        event.consume();
    }

    public void initializeTaskTable() {
        this.TaskStatus.setCellValueFactory(new PropertyValueFactory<TaskTableItem, String>("status"));
        this.AmountOfWorkers.setCellValueFactory(new PropertyValueFactory<TaskTableItem, Integer>("amountOfWorkers"));
    }

    public void refreshData() {
        while (refreshDataThread.isAlive()) {
            try {
                Thread.sleep(1000);
            } catch (Exception ignore) {
            }
            refreshMyTasksList();
            refreshMyTargetsList();
        }
    }

    public void refreshMyTargetsList() {
        String finalUrl = HttpUrl
                .parse(WorkersConstants.WORKER_TASK_PAGE)
                .newBuilder()
                .addQueryParameter("workerName", userName)
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, "GET", null, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody responseBody = response.body();
                Gson gson = new Gson();
                try {
                    if (responseBody != null) {
                        Set<String> targetList = gson.fromJson(responseBody.string(), Set.class);
                        Platform.runLater(()->updateTargetList(targetList));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Objects.requireNonNull(response.body()).close();;
                response.close();
            }
        });
    }

    private void updateTargetList(Set<String> targetList) {
        for (String target: targetList) {
            if(!usersTargetList.contains(target))
                usersTargetList.add(target);
        }
        displaySelectedTargetInfo();
    }

    public void refreshMyTasksList() {
        String finalUrl = HttpUrl
                .parse(WorkersConstants.GET_WORKER_PAGE)
                .newBuilder()
                .addQueryParameter("getWorkerTasks", "getWorkerTasks")
                .addQueryParameter("workerName", userName)
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, "GET", null, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody responseBody = response.body();
                Gson gson = new Gson();
                try {
                    if (responseBody != null) {
                        Set<String> taskList = gson.fromJson(responseBody.string(), new TypeToken<Set<String>>(){}.getType());
                        Platform.runLater(()->updateTasksList(taskList));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Objects.requireNonNull(response.body()).close();;
                response.close();
            }
        });
    }

    private void updateTasksList(Set<String> taskList) {
        registeredTasksList.removeIf(task -> !taskList.contains(task));
        for (String task: taskList) {
            if(!registeredTasksList.contains(task))
                registeredTasksList.add(task);
        }
        displaySelectedTaskInfo();
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    private void displaySelectedTaskInfo() {
        if (currentSelectedTaskList.isEmpty())
            return;

        String selectedTaskName = currentSelectedTaskList.get(0);

        String finalUrl = HttpUrl
                .parse(WorkersConstants.GET_WORKER_PAGE)
                .newBuilder()
                .addQueryParameter("getWorkerTask", "getWorkerTask")
                .addQueryParameter("workerName", userName)
                .addQueryParameter("taskName", selectedTaskName)
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
                    ResponseBody responseBody = response.body();
                    Gson gson = new Gson();
                    try {
                        if (responseBody != null) {
                            WorkerTaskDetailsDto workerTaskDetailsDto = gson.fromJson(responseBody.string(), WorkerTaskDetailsDto.class);
                            responseBody.close();
                            Platform.runLater(() -> displaySelectedTaskInfoFromDto(workerTaskDetailsDto));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Objects.requireNonNull(response.body()).close();
                response.close();
            }
        });
    }

    private void displaySelectedTaskInfoFromDto(WorkerTaskDetailsDto workerTaskDetailsDto) {
        TaskNameTextField.setText(workerTaskDetailsDto.getTaskName());
        TargetsDoneTextField.setText(workerTaskDetailsDto.getTargetsDone().toString());
        TaskCreditsEarnedTextField.setText(workerTaskDetailsDto.getCreditsEarned().toString());
        TaskTableItem taskTableItem =
                new TaskTableItem(workerTaskDetailsDto.getTaskStatus(),workerTaskDetailsDto.getAmountOfWorkers());
        TaskInfoTableList.clear();
        TaskInfoTableList.add(taskTableItem);
        TaskInfoTableView.setItems(TaskInfoTableList);
    }

    private void displaySelectedTargetInfo() {
        if (currentSelectedTaskList.isEmpty())
            return;

        String selectedTargetName = currentSelectedTargetList.get(0);

        String finalUrl = HttpUrl
                .parse(WorkersConstants.WORKER_TASK_PAGE)
                .newBuilder()
                .addQueryParameter("targetId", selectedTargetName)
                .addQueryParameter("workerName", userName)
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
                    ResponseBody responseBody = response.body();
                    Gson gson = new Gson();
                    try {
                        if (responseBody != null) {
                            TargetForWorker targetForWorker = gson.fromJson(responseBody.string(), TargetForWorker.class);
                            responseBody.close();
                            if(targetForWorker != null)
                                Platform.runLater(() -> displaySelectedTargetInfoFromDto(targetForWorker));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Objects.requireNonNull(response.body()).close();
                response.close();
            }
        });
    }

    private void displaySelectedTargetInfoFromDto(TargetForWorker targetForWorker) {
        TargetNameTextField.setText(targetForWorker.getName());
        TargetTaskNameTextField.setText(targetForWorker.getTaskName());
        TargetTaskTypeTextField.setText(targetForWorker.getTaskType());
        TargetStatusTextField.setText(targetForWorker.getStatus());
        if(targetForWorker.getStatus().equalsIgnoreCase("finished"))
            TargetCreditsEarnedTextField.setText(targetForWorker.getPricing().toString());
        TargetsRunLogTextArea.setText(targetForWorker.getRunLog());

    }

    private void createNewProgressBar()
    {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    updateProgressFromServer();
                    while (currTaskAmountOfFinishedTargets < currTaskAmountOfChosenTargets) {
                        Thread.sleep(300);
                        updateProgressFromServer();
                        updateProgress(currTaskAmountOfFinishedTargets, currTaskAmountOfChosenTargets);
                    }
                    updateProgress(currTaskAmountOfFinishedTargets, currTaskAmountOfChosenTargets);
                } catch (InterruptedException e) {
                    updateProgress(0,1);
                }
                return null;
            }
        };
        this.TaskProgressBar.progressProperty().bind(task.progressProperty());
        this.ProgressBarLabel.textProperty().bind
                (Bindings.concat(Bindings.format("%.0f", Bindings.multiply(task.progressProperty(), 100)), " %"));

        this.progressBarThread = new Thread(task);
        progressBarThread.setDaemon(true);
        progressBarThread.start();
    }

    private void updateProgressFromServer() {
        String finalUrl = HttpUrl
                .parse(Constants.TASKS_PATH)
                .newBuilder()
                .addQueryParameter("getProgress", MyTasksListView.getSelectionModel().getSelectedItem())
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
                    currTaskAmountOfChosenTargets = (Integer.parseInt(Objects.requireNonNull(response.header("amountOfChosenTargets"))));
                    currTaskAmountOfFinishedTargets = (Integer.parseInt(Objects.requireNonNull(response.header("amountOfFinishedOrSkipped"))));
                }
                Objects.requireNonNull(response.body()).close();
                response.close();
            }
        });
    }

    public void unregisterFromTask(String taskName) {
        RequestBody body = RequestBody.create("null",MediaType.parse("application/json"));
        String finalUrl = HttpUrl
                .parse(Constants.WORKER_TASK_PAGE)
                .newBuilder()
                .addQueryParameter("unregisterFromTask", "unregisterFromTask")
                .addQueryParameter("taskName",taskName)
                .addQueryParameter("workerName", userName)
                .build()
                .toString();

        HttpClientUtil.runAsync(finalUrl, "POST", body, new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code() > 300 || response.code() < 200) {
                    String message = response.header("message");
                    Platform.runLater(() -> errorPopup(message));
                }
                else
                    workerMainController.getTaskExecutor().removeRegisteredTask(taskName);
                response.close();
            }
        });
    }

    public void errorPopup(String message) {
        Toolkit.getDefaultToolkit().beep();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.initOwner(primaryStage);
        Optional<ButtonType> result = alert.showAndWait();
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setWorkerMainController(WorkerMainController workerMainController) {
        this.workerMainController = workerMainController;
    }

    public void resetProgressBarValues() {
        if(progressBarThread != null)
            progressBarThread.interrupt();
    }

    public void clearTaskDetails() {
        TaskNameTextField.clear();
        TaskInfoTableList.clear();
        TargetsDoneTextField.clear();
        TaskCreditsEarnedTextField.clear();
        resetProgressBarValues();
    }
}
