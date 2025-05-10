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
import com.teach.javafx.request.JwtResponse;
import com.teach.javafx.AppStore;



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



//    @FXML
//    public void initialize() {
//        idColumn.setCellValueFactory(new MapValueFactory<>("id"));
//        studentIdColumn.setCellValueFactory(new MapValueFactory<>("studentId"));
//        titleColumn.setCellValueFactory(new MapValueFactory<>("title"));
//        descriptionColumn.setCellValueFactory(new MapValueFactory<>("description"));
//        honorTableView.setItems(honorList);
//        loadHonorList();
//    }
//
//    private void loadHonorList() {
//        DataRequest req = new DataRequest();
//        req.add("numName", "");
//        DataResponse res = HttpRequestUtil.request("/api/studentHonor/getStudentHonorList", req);
//        if (res != null && res.getCode() == 0) {
//            honorList.setAll((ObservableList<Map>) res.getData());
//        }
//    }


    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new MapValueFactory<>("id"));
        studentIdColumn.setCellValueFactory(new MapValueFactory<>("studentId"));
        titleColumn.setCellValueFactory(new MapValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new MapValueFactory<>("description"));
        honorTableView.setItems(honorList);
        loadHonorList();
    }

//    private void loadHonorList() {
//        DataRequest req = new DataRequest();
//        req.add("numName", "");
//        DataResponse res = HttpRequestUtil.request("/api/studentHonor/getStudentHonorList", req);
//        if (res != null && res.getCode() == 0) {
//            // 将ArrayList转换为ObservableList
//            honorList.setAll(FXCollections.observableArrayList((ArrayList<Map>) res.getData()));
//        } else {
//            MessageDialog.showDialog("加载数据失败：" + res.getMsg());
//        }
//    }

    private void loadHonorList() {
        DataRequest req = new DataRequest();
        req.add("numName", "");
        DataResponse res = HttpRequestUtil.request("/api/studentHonor/getStudentHonorList", req);
        if (res != null && res.getCode() == 0) {
            String currentStudentId = getCurrentStudentId();
            ArrayList<Map> filteredList = new ArrayList<>();
            for (Map record : (ArrayList<Map>) res.getData()) {
                // 将 record.get("studentId") 转换为字符串后再比较
                if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                    filteredList.add(record);
                }
            }
            honorList.setAll(FXCollections.observableArrayList(filteredList));
        } else {
            MessageDialog.showDialog("加载数据失败：" + (res != null ? res.getMsg() : "未知错误"));
        }
    }

    private String getCurrentStudentId() { // 用于得到登录者ID

        JwtResponse jwtResponse = AppStore. getJwt(); // AppStore 中存储了当前登录用户的 JwtResponse
        if (jwtResponse != null) {
//            System.out.println("Username:"+ jwtResponse.getUsername());
//            System.out.println("ID:"+ jwtResponse.getId());
//            System.out.println("Token:"+ jwtResponse.getToken());
//            System.out.println("Role:"+ jwtResponse.getRole());
//            System.out.println("获取当前学生ID成功");
            return String.valueOf(jwtResponse.getId()); // 返回用户的 ID
        }
        System.out.println("获取当前学生ID失败");
        return null; //
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
        Map<String, Object> form = new HashMap<>();
        form.put("studentId", studentIdField.getText());
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