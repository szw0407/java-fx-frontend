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

public class TechnicalAchieveController {
    @FXML
    private TableView<Map> dataTableView;
    @FXML
    private TableColumn<Map,String> studentIdColumn;
    @FXML
    private TableColumn<Map,String> subjectColumn;
    @FXML
    private TableColumn<Map, String> descriptionColumn;
    @FXML
    private TableColumn<Map, String> achievementColumn;

    @FXML
    private TextField studentIdField;

    @FXML
    private TextField subjectField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField achievementField;

    @FXML
    private TextField searchField;

    private Integer id = null;  //主键
    private ArrayList<Map> techList = new ArrayList<>();  // 实践数据列表
    private ObservableList<Map> observableList = FXCollections.observableArrayList();

    private void setTableViewData() {
        observableList.clear();
        for (int j = 0; j < techList.size(); j++) {
            observableList.addAll(FXCollections.observableArrayList(techList.get(j)));
        }
        dataTableView.setItems(observableList);
    }

    public void initialize() {
        JwtResponse jwtResponse = AppStore. getJwt();
        String currentStudentId = String.valueOf(jwtResponse.getId());
        DataResponse res;
        DataRequest req = new DataRequest();
        req.add("numName", "");
        res = HttpRequestUtil.request("/api/technicalAchieve/getTechnicalAchieveList", req); //从后台获取所有学生信息列表集合（现在是获取所有列表，因为要查询的为‘’，也就是where中的条?1=‘’）
        if (res != null && res.getCode() == 0) {
            ArrayList<Map> filteredList = new ArrayList<>();
            for(Map record : (ArrayList<Map>) res.getData()){
                if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                    filteredList.add(record);
                }
            }
            techList = filteredList;//把得到的键值列表返回
            if (jwtResponse.getRole().equals("ROLE_ADMIN")) techList = (ArrayList<Map>) res.getData();
        }

        studentIdColumn.setCellValueFactory(new MapValueFactory<>("studentId"));
        subjectColumn.setCellValueFactory(new MapValueFactory<>("subject"));
        descriptionColumn.setCellValueFactory(new MapValueFactory<>("description"));
        achievementColumn.setCellValueFactory(new MapValueFactory<>("achievement"));

        // 监听表格行选择事件
        TableView.TableViewSelectionModel<Map> tsm = dataTableView.getSelectionModel();
        ObservableList<Integer> list = tsm.getSelectedIndices();
        list.addListener(this::onTableRowSelect);
        loadList();
        setTableViewData();

    }
    private void loadList() {
        DataRequest req = new DataRequest();
        req.add("numName", "");
        DataResponse res = HttpRequestUtil.request("/api/technicalAchieve/getTechnicalAchieveList", req);
        if (res != null && res.getCode() == 0) {
            // 将ArrayList转换为ObservableList
            observableList.setAll(FXCollections.observableArrayList((ArrayList<Map>) res.getData()));
        } else {
            MessageDialog.showDialog("加载数据失败：" + res.getMsg());
        }
    }

    public void clearPanel() {
        id = null;
        studentIdField.setText("");
        subjectField.setText("");
        descriptionArea.setText("");
        achievementField.setText("");
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
        DataResponse res = HttpRequestUtil.request("/api/technicalAchieve/getTechnicalAchieveInfo", req);
        if(res.getCode()!=0){
            MessageDialog.showDialog(res.getMsg());
            return;
        }
        form = (Map) res.getData();
        studentIdField.setText(CommonMethod.getString(form, "studentId"));
        subjectField.setText(CommonMethod.getString(form, "subject"));
        descriptionArea.setText(CommonMethod.getString(form, "description"));
        achievementField.setText(CommonMethod.getString(form, "achievement"));

    }
    public void onTableRowSelect(ListChangeListener.Change<? extends Integer> change) {
        changePracticeInfo();
    }
    @FXML
    protected void onQueryButtonClick() {
        JwtResponse jwtResponse = AppStore. getJwt();
        String currentStudentId = String.valueOf(jwtResponse.getId());
        String search = searchField.getText();
        DataRequest req = new DataRequest();
        req.add("numName", search);
        DataResponse res = HttpRequestUtil.request("/api/technicalAchieve/getTechnicalAchieveList", req);
        if (res != null && res.getCode() == 0) {
            ArrayList<Map> filteredList = new ArrayList<>();
            for(Map record : (ArrayList<Map>) res.getData()){
                if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                    filteredList.add(record);
                }
            }
            techList = filteredList;
            if (jwtResponse.getRole().equals("ROLE_ADMIN")) techList = (ArrayList<Map>) res.getData();
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
        DataResponse res = HttpRequestUtil.request("/api/technicalAchieve/technicalAchieveDelete", req);
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
        form.put("subject", subjectField.getText());
        form.put("description", descriptionArea.getText());
        form.put("achievement", achievementField.getText());
        DataRequest req = new DataRequest();
        req.add("form", form);
        req.add("id", id);
        DataResponse res = HttpRequestUtil.request("/api/technicalAchieve/technicalAchieveEditSave", req);
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
