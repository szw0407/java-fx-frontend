package com.teach.javafx.controller;
import  java.util.ArrayList;
import com.teach.javafx.MainApplication;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.control.cell.MapValueFactory;
import javafx.collections.ObservableList;



public class StudentHonorController extends ToolController {
    @FXML
    private TableView<Map> honorTableView;
    @FXML
    private TableColumn<Map, String> idColumn;
    @FXML
    private TableColumn<Map, String> studentIdColumn;
    @FXML
    private TableColumn<Map, String> titleColumn;
    @FXML
    private TableColumn<Map, String> descriptionColumn;
    @FXML
    private TextField studentIdField;
    @FXML
    private TextField titleField;
    @FXML
    private TextField descriptionField;

    private ObservableList<Map> honorList = FXCollections.observableArrayList();

    private ObservableList<Map> observableList = FXCollections.observableArrayList();  // TableView渲染列表

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (studentIdField.getText().isBlank()) errors.append("学生ID不能为空\n");
        if (titleField.getText().isBlank()) errors.append("荣誉标题不能为空\n");
        if (descriptionField.getText().isBlank()) errors.append("荣誉描述不能为空\n");

        if (errors.length() > 0) {
            MessageDialog.showDialog(errors.toString());
            return false;
        }
        return true;
    }

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new MapValueFactory<>("id"));
        studentIdColumn.setCellValueFactory(new MapValueFactory<>("studentId"));
        titleColumn.setCellValueFactory(new MapValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new MapValueFactory<>("description"));
        honorTableView.setItems(honorList);
        loadHonorList();
    }

    private void loadHonorList() {
        DataRequest req = new DataRequest();
        req.add("numName", "");
        DataResponse res = HttpRequestUtil.request("/api/studentHonor/getStudentHonorList", req);
        if (res != null && res.getCode() == 0) {
            // 将ArrayList转换为ObservableList
            honorList.setAll(FXCollections.observableArrayList((ArrayList<Map>) res.getData()));
        } else {
            MessageDialog.showDialog("加载数据失败：" + res.getMsg());
        }
    }

    @FXML
    protected void onAddButtonClick() {
        clearForm();
    }

    @FXML
    protected void onDeleteButtonClick() {
        Map<String, Object> selectedItem = honorTableView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            MessageDialog.showDialog("请选择要删除的记录");
            return;
        }
        DataRequest req = new DataRequest();
        req.add("id", selectedItem.get("id"));
        DataResponse res = HttpRequestUtil.request("/api/studentHonor/studentHonorDelete", req);
        if (res.getCode() == 0) {
            MessageDialog.showDialog("删除成功！");
            loadHonorList();
        } else {
            MessageDialog.showDialog("删除失败：" + res.getMsg());
        }
    }

    @FXML
    protected void onQueryButtonClick() {
        loadHonorList();
    }

    @FXML
    protected void onSaveButtonClick() {
        if (!validateInput()) {
            return;
        }

        String studentId = studentIdField.getText();
        // 验证学生ID是否存在
        DataRequest checkReq = new DataRequest();
        checkReq.add("studentId", studentId);
        DataResponse checkRes = HttpRequestUtil.request("/api/studentHonor/checkStudentIdExists", checkReq);
        if (checkRes == null || checkRes.getCode() != 0 || !(Boolean) checkRes.getData()) {
            MessageDialog.showDialog("学生ID无效");
            return;
        }

        Map<String, Object> form = new HashMap<>();
        form.put("studentId", studentId);
        form.put("title", titleField.getText());
        form.put("description", descriptionField.getText());

        DataRequest req = new DataRequest();
        req.add("form", form);
        DataResponse res = HttpRequestUtil.request("/api/studentHonor/studentHonorEditSave", req);
        if (res.getCode() == 0) {
            MessageDialog.showDialog("保存成功！");
            loadHonorList();
            clearForm();
        } else {
            MessageDialog.showDialog("保存失败：" + res.getMsg());
        }
    }

    private void clearForm() {
        studentIdField.setText("");
        titleField.setText("");
        descriptionField.setText("");
    }
}
