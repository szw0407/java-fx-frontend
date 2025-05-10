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

public class InternshipEditController {
    @FXML
    private ComboBox<OptionItem> studentComboBox;
    private List<OptionItem> studentList;
    @FXML
    private TextField startTime;
    @FXML
    private TextField endTime;
    @FXML
    private TextField company;
    @FXML
    private TextField position;
    @FXML
    private TextField description;


    private InternshipTableController internshipTableController = null;
    private Integer id = null;


    @FXML
    public void initialize(){}

    @FXML
    public void okButtonClick(){
        JwtResponse jwtResponse = AppStore. getJwt();
        String currentStudentId = String.valueOf(jwtResponse.getId());
        Map<String,Object> data = new HashMap<>();
        OptionItem op;
        op = studentComboBox.getSelectionModel().getSelectedItem();
        if(op != null) {
            if (!op.getValue().equals(currentStudentId)){
                MessageDialog.showDialog("不能添加其他学生的信息！");
                return;
            }
            data.put("studentId",Integer.parseInt(op.getValue()));
        }
        data.put("id",id);
        data.put("startTime",startTime.getText());
        data.put("endTime",endTime.getText());
        data.put("company",company.getText());
        data.put("position",position.getText());
        data.put("description",description.getText());
        internshipTableController.doClose("ok",data);
    }
    @FXML
    public void cancelButtonClick(){
        internshipTableController.doClose("cancel",null);
    }
    public void setInternshipTableController(InternshipTableController internshipTableController){
        this.internshipTableController = internshipTableController;
    }
    public void init(){
        studentList = internshipTableController.getStudentList();
        studentComboBox.getItems().addAll(studentList);
    }
    public void showDialog(Map data){
        if(data == null) {
            id = null;
            studentComboBox.getSelectionModel().select(-1);
            studentComboBox.setDisable(false);
            startTime.setText("");
            endTime.setText("");
            company.setText("");
            position.setText("");
            description.setText("");

        }else{
            id = CommonMethod.getInteger(data,"id");
            studentComboBox.getSelectionModel().select(CommonMethod.getOptionItemIndexByValue(studentList, CommonMethod.getString(data, "studentId")));
            studentComboBox.setDisable(true);
            //将 studentComboBox 控件设置为禁用状态，这意味着用户不能与该控件进行交互，通常表现为控件显示为灰色，且无法点击或选择其中的内容
            startTime.setText(CommonMethod.getString(data,"startTime"));
            endTime.setText(CommonMethod.getString(data,"endTime"));
            company.setText(CommonMethod.getString(data,"company"));
            position.setText(CommonMethod.getString(data,"position"));
            description.setText(CommonMethod.getString(data,"description"));
        }
    }
}
