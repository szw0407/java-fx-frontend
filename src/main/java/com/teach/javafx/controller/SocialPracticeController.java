package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.controller.base.LocalDateStringConverter;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.stage.FileChooser;
import com.teach.javafx.util.CommonMethod;
import com.teach.javafx.util.DateTimeTool;
import com.teach.javafx.controller.base.MessageDialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocialPracticeController extends ToolController {

    @FXML
    private TableView<Map> dataTableView;  // 社会实践表
    @FXML
    private TableColumn<Map, String> studentIdColumn;
    @FXML
    private TableColumn<Map, String> timeColumn;   // 实践时间列
    @FXML
    private TableColumn<Map, String> locationColumn; // 实践地点列
    @FXML
    private TableColumn<Map, String> organizationColumn; // 实践单位列
    @FXML
    private TableColumn<Map, String> durationColumn;  // 持续天数列

    @FXML
    private DatePicker timePicker;  // 实践时间选择器
    @FXML
    private TextField studentIdField;

    @FXML
    private TextField locationField; // 实践地点输入
    @FXML
    private TextField organizationField; // 实践单位输入域
    @FXML
    private TextArea descriptionArea; // 实践描述输入域
    @FXML
    private TextField durationField; // 持续天数输入域

    @FXML
    private TextField searchField;  // 查询输入域

    @FXML
    private Button addButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button saveButton;

    private Integer id = null;  // 当前社会实践编号主键
    private ArrayList<Map> practiceList = new ArrayList<>();  // 实践数据列表
    private ObservableList<Map> observableList = FXCollections.observableArrayList();

    private void setTableViewData() {
        observableList.clear();
        for (int j = 0; j < practiceList.size(); j++) {
            observableList.addAll(FXCollections.observableArrayList(practiceList.get(j)));
        }
        dataTableView.setItems(observableList);
    }

    @FXML
    public void initialize() {
        JwtResponse jwtResponse = AppStore.getJwt();
        String currentStudentId = String.valueOf(jwtResponse.getId());
        DataResponse res;
        DataRequest req = new DataRequest();
        req.add("numName", "");
        res = HttpRequestUtil.request("/api/socialPractice/getPracticeList", req);
        if (res != null && res.getCode() == 0) {
            ArrayList<Map> filteredList = new ArrayList<>();
            for(Map record : (ArrayList<Map>) res.getData()){
                if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                    filteredList.add(record);
                }
            }
            practiceList = filteredList;
            if (jwtResponse.getRole().equals("ROLE_ADMIN") || jwtResponse.getRole().equals("ROLE_TEACHER")) {
                practiceList = (ArrayList<Map>) res.getData();
            }
        }

        studentIdColumn.setCellValueFactory(new MapValueFactory<>("studentId"));
        timeColumn.setCellValueFactory(new MapValueFactory<>("practiceTime"));
        locationColumn.setCellValueFactory(new MapValueFactory<>("location"));
        organizationColumn.setCellValueFactory(new MapValueFactory<>("organization"));
        durationColumn.setCellValueFactory(new MapValueFactory<>("durationDays"));

        // 初始化日期格式
        timePicker.setConverter(new LocalDateStringConverter("yyyy-MM-dd"));

        // 监听表格行选择事件
        TableView.TableViewSelectionModel<Map> tsm = dataTableView.getSelectionModel();
        ObservableList<Integer> list = tsm.getSelectedIndices();
        list.addListener(this::onTableRowSelect);
        setTableViewData();

        // 根据用户角色控制按钮状态
        if (jwtResponse.getRole().equals("ROLE_STUDENT")) {
            addButton.setDisable(true);
            deleteButton.setDisable(true);
            saveButton.setDisable(true);
        } else {
            addButton.setDisable(false);
            deleteButton.setDisable(false);
            saveButton.setDisable(false);
        }
    }


    public void clearPanel() {
        id = null;
        timePicker.getEditor().setText("");
        studentIdField.setText("");
        locationField.setText("");
        organizationField.setText("");
        descriptionArea.setText("");
        durationField.setText("");
    }

    protected void changePracticeInfo(){
        Map<String, Object> form = dataTableView.getSelectionModel().getSelectedItem();
        if(form == null){
            clearPanel();
            return;
        }

        id = CommonMethod.getInteger(form, "id");
        DataRequest req = new DataRequest();
        req.add("id", id);
        DataResponse res = HttpRequestUtil.request("/api/socialPractice/getPracticeInfo", req);
        if(res.getCode()!=0){
            MessageDialog.showDialog(res.getMsg());
            return;
        }
        form = (Map) res.getData();
        String dateStr = CommonMethod.getString(form, "practiceTime");
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        timePicker.setValue(date);
        studentIdField.setText(CommonMethod.getString(form, "studentId"));
        locationField.setText(CommonMethod.getString(form, "location"));
        organizationField.setText(CommonMethod.getString(form, "organization"));
        descriptionArea.setText(CommonMethod.getString(form, "description"));
        durationField.setText(CommonMethod.getString(form, "durationDays"));

    }

    public void onTableRowSelect(ListChangeListener.Change<? extends Integer> change) {
        changePracticeInfo();
    }

    @FXML
    protected void onQueryButtonClick() {
        JwtResponse jwtResponse = AppStore.getJwt();
        String currentStudentId = String.valueOf(jwtResponse.getId());
        String search = searchField.getText();
        DataRequest req = new DataRequest();
        req.add("numName", search);
        DataResponse res = HttpRequestUtil.request("/api/socialPractice/getPracticeList", req);
        if (res != null && res.getCode() == 0) {
            ArrayList<Map> filteredList = new ArrayList<>();
            for(Map record : (ArrayList<Map>) res.getData()){
                if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                    filteredList.add(record);
                }
            }
            practiceList = filteredList;
            if (jwtResponse.getRole().equals("ROLE_ADMIN") || jwtResponse.getRole().equals("ROLE_TEACHER")) {
                practiceList = (ArrayList<Map>) res.getData();
            }
            setTableViewData();
        }
    }

    @FXML
    protected void onAddButtonClick() {
        clearPanel();
    }

    @FXML
    protected void onDeleteButtonClick() {
        Map form = dataTableView.getSelectionModel().getSelectedItem();
        if (form == null) {
            MessageDialog.showDialog("请选择要删除的数据！");
            return;
        }
        int ret = MessageDialog.choiceDialog("确认要删除吗?");
        if (ret != MessageDialog.CHOICE_YES) {
            return;
        }
        id = CommonMethod.getInteger(form, "id");
        DataRequest req = new DataRequest();
        req.add("id", id);
        DataResponse res = HttpRequestUtil.request("/api/socialPractice/practiceDelete", req);
        if(res!= null) {
            if (res.getCode() == 0) {
                MessageDialog.showDialog("删除成功！");
                onQueryButtonClick();
            } else {
                MessageDialog.showDialog(res.getMsg());
            }
        }
    }

    @FXML
    protected void onSaveButtonClick() {
        if (studentIdField.getText().isEmpty()) {
            MessageDialog.showDialog("学号为空，不能修改");
            return;
        }
        Map<String, Object> form = new HashMap<>();
        form.put("id", id);
        form.put("studentId", studentIdField.getText());
        form.put("practiceTime", timePicker.getEditor().getText());
        form.put("location", locationField.getText());
        form.put("organization", organizationField.getText());
        form.put("description", descriptionArea.getText());
        form.put("durationDays", durationField.getText());
        DataRequest req = new DataRequest();
        req.add("form", form);
        req.add("id", id);
        DataResponse res = HttpRequestUtil.request("/api/socialPractice/practiceEditSave", req);
        if(res.getCode()==0){
            id = CommonMethod.getIntegerFromObject(res.getData());
            MessageDialog.showDialog("保存成功！");
            onQueryButtonClick();
        }else{
            MessageDialog.showDialog(res.getMsg());
        }
    }

    public void doNew() {
        clearPanel();
    }

    public void doSave() {
        onSaveButtonClick();
    }

    public void doDelete() {
        onDeleteButtonClick();
    }
}