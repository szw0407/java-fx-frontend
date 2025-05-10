package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.JwtResponse;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.util.Map;

public class CurrentTeacherInformationController {
    @FXML
    private TextField teacherIdField;

    @FXML
    private TextField titleField;
    @FXML
    private TextField degreeField;
    @FXML
    private TextField personIdField;
    @FXML
    private TextField numField;
    @FXML
    private TextField nameField;
    @FXML
    private TextField deptField;
    @FXML
    private TextField cardField;
    @FXML
    private TextField genderField;
    @FXML
    private TextField genderNameField;
    @FXML
    private TextField birthdayField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField introduceField;

    @FXML
    public void initialize() {
        loadTeacherData(getCurrentTeacherId()); // 示例学生 ID
    }

    private void loadTeacherData(String teacherId) {
        DataRequest req = new DataRequest();
        req.add("teacherId", teacherId);
        DataResponse res = HttpRequestUtil.request("/api/teacher/getCurrentTeacherData", req);

        if (res != null && res.getCode() == 0) {
            Map<String, Object> data = (Map<String, Object>) res.getData();
            populateFields(data);
        } else {
            MessageDialog.showDialog("加载老师信息失败：" + (res != null ? res.getMsg() : "未知错误"));
        }
    }
    private String getCurrentTeacherId() { // 用于得到登录者ID

        JwtResponse jwtResponse = AppStore. getJwt(); // AppStore 中存储了当前登录用户的 JwtResponse
        if (jwtResponse != null) {
            return String.valueOf(jwtResponse.getId()); // 返回用户的 ID
        }
        System.out.println("获取当前老师ID失败");
        return null; //
    }

    private void populateFields(Map<String, Object> data) {
        teacherIdField.setText(String.valueOf(data.get("teacherId")));
        titleField.setText(String.valueOf(data.get("title")));
        degreeField.setText(String.valueOf(data.get("degree")));
        personIdField.setText(String.valueOf(data.get("personId")));
        numField.setText(String.valueOf(data.get("num")));
        nameField.setText(String.valueOf(data.get("name")));
        deptField.setText(String.valueOf(data.get("dept")));
        cardField.setText(String.valueOf(data.get("card")));
        genderField.setText(String.valueOf(data.get("gender")));
        genderNameField.setText(String.valueOf(data.get("genderName")));
        birthdayField.setText(String.valueOf(data.get("birthday")));
        emailField.setText(String.valueOf(data.get("email")));
        phoneField.setText(String.valueOf(data.get("phone")));
        addressField.setText(String.valueOf(data.get("address")));
        introduceField.setText(String.valueOf(data.get("introduce")));
    }

}
