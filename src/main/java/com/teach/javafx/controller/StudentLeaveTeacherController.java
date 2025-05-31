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

import java.util.ArrayList;
import java.util.Map;

public class StudentLeaveTeacherController extends ToolController {
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
        String currentTeacherId = getCurrentTeacherId();
        ArrayList<Map> filteredList = new ArrayList<>();
        for (Map record : (ArrayList<Map>) res.getData()) {
            // 过滤 approverId 为当前老师 ID 的记录
            if (currentTeacherId.equals(String.valueOf(record.get("approverId")).replace(".0", ""))) {
                filteredList.add(record);
            }
        }
        leaveList.setAll(FXCollections.observableArrayList(filteredList));
    } else {
        MessageDialog.showDialog("加载数据失败：" + (res != null ? res.getMsg() : "未知错误"));
    }
}

@FXML
protected void onApproveButtonClick() {
    // 获取选中的请假记录
    Map<String, Object> selectedItem = leaveTableView.getSelectionModel().getSelectedItem();
    if (selectedItem == null) {
        MessageDialog.showDialog("请选择要审批的记录！");
        return;
    }

    // 获取请假记录的 ID 和审批状态
    Double leaveIdDouble = (Double) selectedItem.get("id");
    int leaveId = leaveIdDouble != null ? leaveIdDouble.intValue() : 0;
    boolean isApproved = approvedCheckBox.isSelected(); // true: 同意, false: 拒绝

    // 构造请求数据
    DataRequest req = new DataRequest();
    req.add("leaveId", leaveId);
    req.add("isApproved", isApproved);

    // 调用后端接口
    DataResponse res = HttpRequestUtil.request("/api/studentLeave/approveLeave", req);
    if (res != null && res.getCode() == 0) {
        String message = isApproved ? "审批成功！" : "拒绝成功！";
        MessageDialog.showDialog(message);
        loadLeaveList(); // 刷新请假记录列表
    } else {
        MessageDialog.showDialog("审批失败：" + (res != null ? res.getMsg() : "未知错误"));
    }
}


    private String getCurrentTeacherId() {
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