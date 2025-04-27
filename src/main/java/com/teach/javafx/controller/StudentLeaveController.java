package com.teach.javafx.controller;

import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.DatePicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StudentLeaveController extends ToolController {
    @FXML
    private TableView<Map> leaveTableView;

    @FXML
    private TableColumn<Map, String> idColumn;
    @FXML
    private TableColumn<Map, String> studentIdColumn;
    @FXML
    private TableColumn<Map, String> studentNameColumn;
    @FXML
    private TableColumn<Map, String> collegeColumn;
    @FXML
    private TableColumn<Map, String> startDateColumn;
    @FXML
    private TableColumn<Map, String> endDateColumn;
    @FXML
    private TableColumn<Map, String> reasonColumn;
    @FXML
    private TableColumn<Map, String> approverIdColumn;
    @FXML
    private TableColumn<Map, String> isApprovedColumn;

    @FXML
    private TextField studentIdField;
    @FXML
    private TextField studentNameField;
    @FXML
    private TextField collegeField;
    @FXML
    private TextField reasonField;
    @FXML
    private TextField approverIdField;
    @FXML
    private CheckBox approvedCheckBox;

    @FXML
    private TextField searchStudentNameField; // 新增字段

    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    private ObservableList<Map> leaveList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new MapValueFactory<>("id"));
        studentIdColumn.setCellValueFactory(new MapValueFactory<>("studentId"));
        studentNameColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        collegeColumn.setCellValueFactory(new MapValueFactory<>("college"));
        startDateColumn.setCellValueFactory(new MapValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new MapValueFactory<>("endDate"));
        reasonColumn.setCellValueFactory(new MapValueFactory<>("reason"));
        approverIdColumn.setCellValueFactory(new MapValueFactory<>("approverId"));
        isApprovedColumn.setCellValueFactory(new MapValueFactory<>("isApproved"));

        leaveTableView.setItems(leaveList);
        loadLeaveList();
    }

    private void loadLeaveList() {
        DataRequest req = new DataRequest();
        req.add("studentName", "");
        DataResponse res = HttpRequestUtil.request("/api/studentLeave/getLeaveList", req);
        if (res != null && res.getCode() == 0) {
            leaveList.setAll(FXCollections.observableArrayList((ArrayList<Map>) res.getData()));
        } else {
            MessageDialog.showDialog("加载数据失败：" + (res != null ? res.getMsg() : ""));
        }
    }

    @FXML
    protected void onAddButtonClick() {
        clearForm();
    }

    @FXML
    protected void onDeleteButtonClick() {
        Map<String, Object> selectedItem = leaveTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            MessageDialog.showDialog("请选择要删除的记录");
            return;
        }
        DataRequest req = new DataRequest();
        req.add("leaveId", selectedItem.get("id"));
        DataResponse res = HttpRequestUtil.request("/api/studentLeave/deleteLeave", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("删除成功！");
            loadLeaveList();
        } else {
            MessageDialog.showDialog("删除失败：" + (res != null ? res.getMsg() : ""));
        }
    }

    @FXML
    protected void onQueryButtonClick() {
        loadLeaveList();
    }

    @FXML
    protected void onSaveButtonClick() {
        Map<String, Object> form = new HashMap<>();
        form.put("leaveId", null); // 如果需要更新，需传递已有的 leaveId
        form.put("studentId", studentIdField.getText().trim());
        form.put("studentName", studentNameField.getText().trim());
        form.put("college", collegeField.getText().trim());
        form.put("startDate", startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "");
        form.put("endDate", endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "");
        form.put("reason", reasonField.getText().trim());
        form.put("approverId", approverIdField.getText().trim());
        form.put("isApproved", approvedCheckBox.isSelected());

        DataRequest req = new DataRequest();
        req.add("form", form);
        DataResponse res = HttpRequestUtil.request("/api/studentLeave/saveLeave", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("保存成功！");
            loadLeaveList();
            clearForm();
        } else {
            MessageDialog.showDialog("保存失败：" + (res != null ? res.getMsg() : "res==null"));
        }
    }

    @FXML
    protected void onSearchByNameButtonClick() {
        String studentName = searchStudentNameField.getText().trim(); // 使用新的 ID
        DataRequest req = new DataRequest();
        req.add("studentName", studentName);
        DataResponse res = HttpRequestUtil.request("/api/studentLeave/getLeaveList", req);
        if (res != null && res.getCode() == 0) {
            leaveList.setAll(FXCollections.observableArrayList((ArrayList<Map>) res.getData()));
        } else {
            MessageDialog.showDialog("查询失败：" + (res != null ? res.getMsg() : ""));
        }
    }

    private void clearForm() {
        studentIdField.setText("");
        studentNameField.setText(""); // 清空学生姓名字段
        collegeField.setText("");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        reasonField.setText("");
        approverIdField.setText("");
        approvedCheckBox.setSelected(false);
    }
}
