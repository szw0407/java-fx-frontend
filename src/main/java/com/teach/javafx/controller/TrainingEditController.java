package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.JwtResponse;
import com.teach.javafx.request.OptionItem;
import com.teach.javafx.util.CommonMethod;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TrainingEditController {
    @FXML
    private ComboBox<OptionItem> studentComboBox;
    private  List<OptionItem> studentList;
    @FXML
    private TextField timeField;
    @FXML
    private TextField locationField;
    @FXML
    private TextField themeField;
    @FXML
    private TextField descriptionField;

    private Integer id = null;
    private TrainingTableController trainingTableController = null;

    @FXML
    public void initialize() {
    }
    public void okButtonClick() {
        JwtResponse jwtResponse = AppStore. getJwt();
        String currentStudentId = String.valueOf(jwtResponse.getId());
        Map<String,Object> data = new HashMap<>();
        OptionItem op;
        op = studentComboBox.getSelectionModel().getSelectedItem();
        if(op != null) {
            if (!op.getValue().equals(currentStudentId) && !jwtResponse.getRole().equals("ROLE_ADMIN")){
                MessageDialog.showDialog("不能添加其他学生的信息！");
                return;
            }
            data.put("studentId",Integer.parseInt(op.getValue()));
        }
        data.put("id",id);
        data.put("time",timeField.getText());
        data.put("location",locationField.getText());
        data.put("theme",themeField.getText());
        data.put("description",descriptionField.getText());
        trainingTableController.doClose("ok",data);
    }
    @FXML
    public void cancelButtonClick() {
        trainingTableController.doClose("cancel",null);
    }
    public void setTrainingTableController(TrainingTableController trainingTableController) {
        this.trainingTableController = trainingTableController;
    }
    public void init() {
        studentList = trainingTableController.getStudentList();
        studentComboBox.getItems().addAll(studentList);
    }
    public void showDialog(Map data) {
        if(data == null) {
            id = null;
            studentComboBox.getSelectionModel().select(-1);
            themeField.setText("");
            timeField.setText("");
            locationField.setText("");
            descriptionField.setText("");
        }else{
            id = CommonMethod.getInteger(data,"id");
            studentComboBox.getSelectionModel().select(CommonMethod.getOptionItemIndexById(studentList, CommonMethod.getInteger(data, "studentId")));
            studentComboBox.setDisable(true);
            themeField.setText(CommonMethod.getString(data,"theme"));
            timeField.setText(CommonMethod.getString(data,"time"));
            locationField.setText(CommonMethod.getString(data,"location"));
            descriptionField.setText(CommonMethod.getString(data,"description"));
        }
    }
}
