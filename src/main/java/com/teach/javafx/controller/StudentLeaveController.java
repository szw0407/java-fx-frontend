package com.teach.javafx.controller;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import java.util.HashMap;
import java.util.Map;
import com.teach.javafx.request.*;
import java.util.ArrayList;

public class StudentLeaveController extends ToolController {
    @FXML
    private TableView<Map> leaveTableView;
    @FXML
    private TableColumn<Map, String> idColumn;
    @FXML
    private TableColumn<Map, String> studentIdColumn;
    @FXML
    private TableColumn<Map, String> reasonColumn;
    @FXML
    private TableColumn<Map, String> startDateColumn;
    @FXML
    private TableColumn<Map, String> endDateColumn;
    @FXML
    private TextField studentIdField;
    @FXML
    private TextField reasonField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    private ObservableList<Map> leaveList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new MapValueFactory<>("id"));
        studentIdColumn.setCellValueFactory(new MapValueFactory<>("studentId"));
        reasonColumn.setCellValueFactory(new MapValueFactory<>("reason"));
        startDateColumn.setCellValueFactory(new MapValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new MapValueFactory<>("endDate"));
        leaveTableView.setItems(leaveList);
        loadLeaveList();
    }

    private void loadLeaveList() {
        DataRequest req = new DataRequest();
        req.add("numName", "");
        DataResponse res = HttpRequestUtil.request("/api/studentLeave/getStudentLeaveList", req);
        if (res != null && res.getCode() == 0) {
            // 将ArrayList转换为ObservableList
            leaveList.setAll(FXCollections.observableArrayList((ArrayList<Map>) res.getData()));
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
        req.add("id", selectedItem.get("id"));
        DataResponse res = HttpRequestUtil.request("/api/studentLeave/studentLeaveDelete", req);
        if (res.getCode() == 0) {
            MessageDialog.showDialog("删除成功！");
            loadLeaveList();
        } else {
            MessageDialog.showDialog("删除失败：" + res.getMsg());
        }
    }

    @FXML
    protected void onQueryButtonClick() {
        loadLeaveList();
    }

    @FXML
    protected void onSaveButtonClick() {
        if (!validateInput()) {
            return;
        }

        // 检查学生ID是否存在
        if (!isStudentIdExists(studentIdField.getText())) {
            MessageDialog.showDialog("学生不存在");
            return;
        }

        Map<String, Object> form = new HashMap<>();
        form.put("studentId", studentIdField.getText());
        form.put("reason", reasonField.getText());
        form.put("startDate", startDatePicker.getValue().toString());
        form.put("endDate", endDatePicker.getValue().toString());

        DataRequest req = new DataRequest();
        req.add("form", form);
        DataResponse res = HttpRequestUtil.request("/api/studentLeave/studentLeaveEditSave", req);
        if (res.getCode() == 0) {
            MessageDialog.showDialog("保存成功！");
            loadLeaveList();
            clearForm();
        } else {
            MessageDialog.showDialog("保存失败：" + res.getMsg());
        }
    }

    private boolean isStudentIdExists(String studentId) {
        DataRequest req = new DataRequest();
        req.add("studentId", studentId);
        DataResponse res = HttpRequestUtil.request("/api/student/checkStudentIdExists", req);
        return res != null && res.getCode() == 0 && (boolean) res.getData();
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (studentIdField.getText().isBlank()) errors.append("学生ID不能为空\n");
        if (reasonField.getText().isBlank()) errors.append("请假原因不能为空\n");
        if (startDatePicker.getValue() == null) errors.append("开始日期不能为空\n");
        if (endDatePicker.getValue() == null) errors.append("结束日期不能为空\n");

        if (errors.length() > 0) {
            MessageDialog.showDialog(errors.toString());
            return false;
        }
        return true;
    }

    private void clearForm() {
        studentIdField.setText("");
        reasonField.setText("");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
    }
}
