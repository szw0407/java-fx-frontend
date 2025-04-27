package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.JwtResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.DatePicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StudentLeaveStudentController extends ToolController {
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
        String currentStudentId = getCurrentStudentId();
        ArrayList<Map> filteredList = new ArrayList<>();
        for (Map record : (ArrayList<Map>) res.getData()) {
            // 将 record.get("studentId") 转换为字符串后再比较
            if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                // 检查 isApproved 字段是否为 null
                if (record.get("isApproved") == null) {
                    record.put("isApproved", ""); // 设置为空字符串或其他默认值
                }
                filteredList.add(record);
            }
        }
        leaveList.setAll(FXCollections.observableArrayList(filteredList));
    } else {
        MessageDialog.showDialog("加载数据失败：" + (res != null ? res.getMsg() : ""));
    }
}

    @FXML
  protected void onSaveButtonClick() {
        String currentStudentId = getCurrentStudentId();
        String enteredStudentId = studentIdField.getText().trim();

        // 检查申请者 ID 是否与当前登录用户 ID 一致
        if (!currentStudentId.equals(enteredStudentId)) {
            MessageDialog.showDialog("申请者 ID 与当前登录用户不一致，无法提交申请！"+'\n'+"不要给别人请假！");
            return;
        }

        Map<String, Object> form = new HashMap<>();
        form.put("leaveId", null); // 新增记录
        form.put("studentId", currentStudentId);
        form.put("studentName", studentNameField.getText().trim());
        form.put("college", collegeField.getText().trim());
        form.put("startDate", startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "");
        form.put("endDate", endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "");
        form.put("reason", reasonField.getText().trim());
        form.put("approverId", approverIdField.getText().trim());
        form.put("isApproved", null); // 默认未审批

        DataRequest req = new DataRequest();
        req.add("form", form);
        DataResponse res = HttpRequestUtil.request("/api/studentLeave/saveLeave", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("申请成功！");
            loadLeaveList();
            clearForm();
        } else {
            MessageDialog.showDialog("申请失败：" + (res != null ? res.getMsg() : "res==null"));
        }
    }

    private String getCurrentStudentId() {
        JwtResponse jwtResponse = AppStore.getJwt();
        if (jwtResponse != null) {
            return String.valueOf(jwtResponse.getId());
        }
        System.out.println("获取当前学生ID失败");
        return null;
    }

    private void clearForm() {
        studentNameField.setText("");
        collegeField.setText("");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        reasonField.setText("");
        approverIdField.setText("");
    }
}