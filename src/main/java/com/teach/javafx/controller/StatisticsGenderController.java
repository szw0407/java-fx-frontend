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

public class StatisticsGenderController extends ToolController {

    @FXML private Label summaryLabel;
    @FXML private Label chartInfoLabel;
    @FXML private PieChart genderPieChart;

    @FXML private TableView<Map<String, Object>> dataTableView;
    @FXML private TableColumn<Map<String, Object>, String> genderColumn;
    @FXML private TableColumn<Map<String, Object>, String> countColumn;

    private final ObservableList<Map<String, Object>> observableList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        genderColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().get("gender");
            return new SimpleStringProperty(value == null ? "" : value.toString());
        });

        countColumn.setCellValueFactory(cellData -> {
            Object value = cellData.getValue().get("count");
            return new SimpleStringProperty(value == null ? "" : value.toString());
        });

        dataTableView.setItems(observableList);
        dataTableView.setEditable(false);

        new Thread(this::loadData).start();
    }

    private void loadData() {
        DataRequest req = new DataRequest();
        DataResponse res = HttpRequestUtil.request("/api/statistics/gender-distribution", req);
        if (res != null && res.getCode() == 0) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) res.getData();
            Platform.runLater(() -> {
                observableList.setAll(list);
                genderPieChart.getData().clear();

                for (Map<String, Object> item : list) {
                    String gender = item.get("gender").toString();
                    Number count = (Number) item.get("count");

                    PieChart.Data slice = new PieChart.Data(gender, count.doubleValue());
                    genderPieChart.getData().add(slice);

                    slice.getNode().setOnMouseClicked(event -> {
                        chartInfoLabel.setText(gender + " 人数: " + count);
                    });
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
    @Override public void doRefresh() { loadData(); }
}
