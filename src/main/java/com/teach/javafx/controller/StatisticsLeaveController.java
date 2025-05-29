package com.teach.javafx.controller;

import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.Map;

public class StatisticsLeaveController extends ToolController {

    @FXML private TableView<Map<String, Object>> dataTableView;
    @FXML private TableColumn<Map<String, Object>, String> studentIdColumn;
    @FXML private TableColumn<Map<String, Object>, String> studentNameColumn;
    @FXML private TableColumn<Map<String, Object>, String> totalLeaveColumn;
    @FXML private TableColumn<Map<String, Object>, String> approvedLeaveColumn;

    @FXML private PieChart leavePieChart;
    @FXML private Label chartInfoLabel;
    @FXML private Label summaryLabel;

    private final ObservableList<Map<String, Object>> observableList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        studentIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().get("学生ID")))
        );
        studentNameColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().get("学生姓名")))
        );
        totalLeaveColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().get("总请假次数")))
        );
        approvedLeaveColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().get("批准请假次数")))
        );

        dataTableView.setItems(observableList);

        new Thread(() -> {
            loadLeaveData();
            loadLeaveDistribution();  // 仅一次饼图
        }).start();
    }

    private void loadLeaveData() {
        DataResponse res = HttpRequestUtil.request("/api/statistics/leave-count", new DataRequest());
        if (res != null && res.getCode() == 0) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) res.getData();
            Platform.runLater(() -> {
                observableList.setAll(list);
                summaryLabel.setText("请假统计概览：共 " + list.size() + " 名学生");
            });
        }
    }

    private void loadLeaveDistribution() {
        DataResponse res = HttpRequestUtil.request("/api/statistics/leave-distribution", new DataRequest());
        if (res != null && res.getCode() == 0) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) res.getData();
            Platform.runLater(() -> {
                leavePieChart.getData().clear();
                for (Map<String, Object> row : list) {
                    String label = String.valueOf(row.get("状态"));
                    Number count = (Number) row.get("数量");
                    PieChart.Data slice = new PieChart.Data(label, count.doubleValue());
                    leavePieChart.getData().add(slice);
                    slice.getNode().setOnMouseClicked(event ->
                            chartInfoLabel.setText(label + "：人数 = " + count)
                    );
                }
            });
        }
    }

    @Override public void doNew() {}
    @Override public void doSave() {}
    @Override public void doDelete() {}
    @Override public void doPrint() {}
    @Override public void doImport() {}
    @Override public void doExport() {}
    @Override public void doTest() {}
    @Override public void doRefresh() {
        loadLeaveData();
        loadLeaveDistribution();
    }
}
