package tasks.control;

import com.google.gson.Gson;
import constants.WorkersConstants;
import dashboard.tableitems.SelectedTaskStatusTableItem;
import dashboard.tableitems.TargetsInfoTableItem;
import dtos.TaskDetailsDto;
import dtos.WorkerTaskDetailsDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import main.include.Constants;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import target.TargetForWorker;
import util.http.HttpClientUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class TaskController {
    private String userName;
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

    public TaskController() {
        isTaskSelected = new SimpleBooleanProperty(false);
        isTargetSelected = new SimpleBooleanProperty(false);

        currentSelectedTaskListListener = change -> {
            displaySelectedTaskInfo();
            isTaskSelected.setValue(change.getList().size() != 0);
        };

        currentSelectedTargetListListener = change -> {
          displaySelectedTargetInfo();
          isTargetSelected.setValue(change.getList().size() != 0);
        };
    }

    public void initialize() {
        currentSelectedTargetList = MyTargetsListView.getSelectionModel().getSelectedItems();
        currentSelectedTaskList = MyTasksListView.getSelectionModel().getSelectedItems();
        currentSelectedTargetList.addListener(currentSelectedTargetListListener);
        currentSelectedTaskList.addListener(currentSelectedTaskListListener);
        initializeTaskTable();
        MyTargetsListView.setItems(usersTargetList);
        MyTasksListView.setItems(registeredTasksList);
        refreshDataThread = new Thread(this::refreshData);
        refreshDataThread.setDaemon(true);
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
    void PauseTaskButtonClicked(ActionEvent event) {

    }

    @FXML
    void QuitTaskButtonClicked(ActionEvent event) {

    }

    public void initializeTaskTable() {
        this.TaskStatus.setCellValueFactory(new PropertyValueFactory<TaskTableItem, String>("status"));
        this.AmountOfWorkers.setCellValueFactory(new PropertyValueFactory<TaskTableItem, Integer>("amountOfWorkers"));
    }

    public void refreshData() {
        try{
            Thread.sleep(1000);
        }
        catch (Exception ignore) {}
        refreshMyTasksList();
        refreshMyTargetsList();
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
            if(!registeredTasksList.contains(target))
                registeredTasksList.add(target);
        }
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
                        Set<String> taskList = gson.fromJson(responseBody.string(), Set.class);
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
    }

    public void unregisterFromTask(String taskName) {
        RequestBody body = RequestBody.create("null", MediaType.parse("application/json"));
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
                response.close();
            }
        });
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
}
