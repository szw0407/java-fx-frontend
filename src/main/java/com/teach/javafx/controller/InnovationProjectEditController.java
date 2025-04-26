package com.teach.javafx.controller;

import com.teach.javafx.request.OptionItem;
import com.teach.javafx.util.CommonMethod;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InnovationProjectEditController {
    @FXML
    private ComboBox<OptionItem> studentComboBox;
    private List<OptionItem> studentList;
    @FXML
    private TextField timeField;
    @FXML
    private TextField typeField;
    @FXML
    private TextField descriptionField;

    private Integer id = null;
    private InnovationProjectTableController innovationProjectTableController = null;

    @FXML
    public void initialize() {
    }

    @FXML
    public void okButtonClick() {
        Map<String,Object> data = new HashMap<>();
        OptionItem op;
        op = studentComboBox.getSelectionModel().getSelectedItem();
        if(op != null) {
            data.put("studentId",Integer.parseInt(op.getValue()));
        }
        data.put("id",id);
        data.put("time",timeField.getText());
        data.put("type",typeField.getText());
        data.put("description",descriptionField.getText());
        innovationProjectTableController.doClose("ok",data);
    }
    @FXML
    public void cancelButtonClick() {
        innovationProjectTableController.doClose("cancel",null);
    }
    public void setInnovationProjectTableController(InnovationProjectTableController innovationProjectTableController){
        this.innovationProjectTableController = innovationProjectTableController;
    }
    public void init() {
        studentList = innovationProjectTableController.getStudentList();
        studentComboBox.getItems().addAll(studentList);
    }
    public void showDialog(Map data) {
        if(data == null) {
            id = null;
            studentComboBox.getSelectionModel().select(-1);
            timeField.setText("");
            typeField.setText("");
            descriptionField.setText("");
        }else{
            id = CommonMethod.getInteger(data,"id");
            studentComboBox.getSelectionModel().select(CommonMethod.getOptionItemIndexByValue(studentList, CommonMethod.getString(data, "studentId")));
            studentComboBox.setDisable(true);
            timeField.setText(CommonMethod.getString(data,"time"));
            typeField.setText(CommonMethod.getString(data,"type"));
            descriptionField.setText(CommonMethod.getString(data,"description"));
        }
    }

}
