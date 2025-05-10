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

public class CurrentStudentInformationController {

    @FXML
    private TextField studentIdField;
    @FXML
    private TextField majorField;
    @FXML
    private TextField classNameField;
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
        loadStudentData(getCurrentStudentId()); // 示例学生 ID
    }

    private void loadStudentData(String studentId) {
        DataRequest req = new DataRequest();
        req.add("studentId", studentId);
        DataResponse res = HttpRequestUtil.request("/api/student/getCurrentStudentData", req);

        if (res != null && res.getCode() == 0) {
            Map<String, Object> data = (Map<String, Object>) res.getData();
            populateFields(data);
        } else {
            MessageDialog.showDialog("加载学生信息失败：" + (res != null ? res.getMsg() : "未知错误"));
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

    private void populateFields(Map<String, Object> data) {
        studentIdField.setText(String.valueOf(data.get("studentId")));
        majorField.setText(String.valueOf(data.get("major")));
        classNameField.setText(String.valueOf(data.get("className")));
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