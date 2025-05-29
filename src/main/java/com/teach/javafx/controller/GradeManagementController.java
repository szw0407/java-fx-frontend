package com.teach.javafx.controller;

import com.teach.javafx.models.GradeEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GradeManagementController {

    @FXML
    private TextField studentSearchField;

    @FXML
    private TextField courseSearchField;

    @FXML
    private Button searchButton;

    @FXML
    private TableView<GradeEntry> gradesTableView;

    @FXML
    private TableColumn<GradeEntry, String> studentNameColumn;

    @FXML
    private TableColumn<GradeEntry, String> studentIdColumn;

    @FXML
    private TableColumn<GradeEntry, String> classIdColumn;

    @FXML
    private TableColumn<GradeEntry, String> courseNameColumn;

    @FXML
    private TableColumn<GradeEntry, Integer> scoreColumn;

    @FXML
    private Button saveButton;

    private ObservableList<GradeEntry> gradeEntries = FXCollections.observableArrayList();

    // 用于模拟的后端数据存储
    private List<GradeEntry> mockDatabase = new ArrayList<>();

    @FXML
    public void initialize() {
        // 初始化模拟数据
        mockDatabase.add(new GradeEntry("张三", "S001", "CS101-A", "计算机科学导论", 85));
        mockDatabase.add(new GradeEntry("李四", "S002", "CS101-A", "计算机科学导论", 90));
        mockDatabase.add(new GradeEntry("李四", "S002", "MA201-B", "高等数学", null)); // 未打分
        mockDatabase.add(new GradeEntry("王五", "S003", "PH303-C", "大学物理", 77));
        mockDatabase.add(new GradeEntry("张三", "S001", "CS101-A", "计算机科学导论", 92)); // 张三的重修记录


        // 设置TableView的列和数据绑定
        studentNameColumn.setCellValueFactory(cellData -> cellData.getValue().studentNameProperty());
        studentIdColumn.setCellValueFactory(cellData -> cellData.getValue().studentIdProperty());
        classIdColumn.setCellValueFactory(cellData -> cellData.getValue().classIdProperty());
        courseNameColumn.setCellValueFactory(cellData -> cellData.getValue().courseNameProperty());
        scoreColumn.setCellValueFactory(cellData -> cellData.getValue().scoreProperty());

        // 使成绩列可编辑
        scoreColumn.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Integer>() {
            @Override
            public String toString(Integer object) {
                return object == null ? "" : object.toString(); // null显示为空字符串
            }

            @Override
            public Integer fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null; // 空字符串转为null
                }
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    //  实际应用中应提示用户格式错误
                    showAlert("输入错误", "成绩必须是有效的数字或为空。");
                    //  返回一个特殊值或抛出异常，阻止提交
                    //  这里简单返回null，或者可以考虑返回旧值（如果可以获取）
                    return null; // 或者可以考虑不修改，但这需要更复杂的逻辑
                }
            }
        }));

        scoreColumn.setOnEditCommit(event -> {
            GradeEntry entry = event.getRowValue();
            Integer newValue = event.getNewValue();
            // 可以在这里添加校验逻辑，例如分数范围
            if (newValue != null && (newValue < 0 || newValue > 100)) {
                showAlert("输入错误", "分数必须在0-100之间。");
                // 恢复旧值，需要刷新单元格
                entry.setScore(event.getOldValue()); // 设置回旧值
                gradesTableView.refresh(); // 刷新表格以显示旧值
            } else {
                entry.setScore(newValue);
            }
        });

        gradesTableView.setItems(gradeEntries);
        // 初始加载所有数据或提示搜索
        // gradeEntries.addAll(mockDatabase); // 可以选择初始加载所有
    }

    @FXML
    private void handleSearchAction() {
        String studentQuery = studentSearchField.getText().trim().toLowerCase();
        String courseQuery = courseSearchField.getText().trim().toLowerCase();

        // ---- 模拟HTTP请求和后端筛选 ----
        System.out.println("模拟搜索: 学生='" + studentQuery + "', 课程='" + courseQuery + "'");
        // 实际应用中，这里会构建HTTP请求发送到后端API
        // 后端API会根据 studentQuery 和 courseQuery 进行数据库查询

        List<GradeEntry> results = mockDatabase.stream()
                .filter(entry ->
                        (studentQuery.isEmpty() ||
                                entry.getStudentName().toLowerCase().contains(studentQuery) ||
                                entry.getStudentId().toLowerCase().contains(studentQuery)) &&
                                (courseQuery.isEmpty() ||
                                        entry.getCourseName().toLowerCase().contains(courseQuery))
                )
                .collect(Collectors.toList());
        // ---- 模拟结束 ----

        gradeEntries.setAll(results); // 更新TableView显示的数据
        if (results.isEmpty()) {
            showAlert("搜索结果", "没有找到匹配的成绩记录。");
        }
    }

    @FXML
    private void handleSaveAction() {
        // ---- 模拟HTTP请求保存修改 ----
        // 实际应用中，这里会遍历 gradeEntries (或者只遍历已修改的条目)
        // 对每一条修改过的记录，构建HTTP请求发送到后端API进行更新

        if (gradeEntries.isEmpty()) {
            showAlert("保存操作", "没有可保存的数据。");
            return;
        }

        System.out.println("模拟保存以下修改:");
        for (GradeEntry entry : gradesTableView.getItems()) { // 获取当前表格中显示的数据
            // 在实际应用中，您可能需要一个标志来判断记录是否真的被修改过
            // 这里简单地“保存”所有当前显示的条目
            System.out.println("保存: " + entry.getStudentId() + " - " + entry.getCourseName() + " - 成绩: " + entry.getScore());

            // 模拟更新到 "mockDatabase"
            // 这部分逻辑会比较复杂，需要匹配原始记录并更新
            // 为简化，这里仅打印，实际中会是API调用
        }
        // ---- 模拟结束 ----

        showAlert("保存成功", "所有显示的成绩修改已（模拟）保存。");
        // 实际应用中，保存后可能需要重新获取数据或确认保存状态
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}