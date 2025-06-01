package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Map;

public class CommentStatisticsController extends ToolController {
    @FXML
    private LineChart<String, Number> commentLineChart;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private TextField searchDateField;

    @FXML
    public void initialize() {
        xAxis.setLabel("日期");
        yAxis.setLabel("评论数量");
        loadCommentStatistics();
    }

    private void loadCommentStatistics() {
        DataRequest req = new DataRequest();
        req.add("date", ""); // 默认加载所有数据
        DataResponse res = HttpRequestUtil.request("/api/post/commentStatistics/daily", req);
        if (res != null && res.getCode() == 0) {
            updateLineChart((List<Map<String, Object>>) res.getData());
        } else {
            MessageDialog.showDialog("加载数据失败：" + (res != null ? res.getMsg() : ""));
        }
    }

    private void updateLineChart(List<Map<String, Object>> dataList) {
        commentLineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("每日新增评论数");

        for (Map<String, Object> data : dataList) {
            String date = (String) data.get("date");
            Number count = (Number) data.get("count");
            series.getData().add(new XYChart.Data<>(date, count));
        }

        commentLineChart.getData().add(series);
    }

    @FXML
    protected void onQueryButtonClick() {
        loadCommentStatistics();
    }

    @FXML
    protected void onSearchByDateButtonClick() {
        String date = searchDateField.getText().trim();
        DataRequest req = new DataRequest();
        req.add("date", date);
        DataResponse res = HttpRequestUtil.request("/api/commentStatistics/daily", req);
        if (res != null && res.getCode() == 0) {
            updateLineChart((List<Map<String, Object>>) res.getData());
        } else {
            MessageDialog.showDialog("查询失败：" + (res != null ? res.getMsg() : ""));
        }
    }
}