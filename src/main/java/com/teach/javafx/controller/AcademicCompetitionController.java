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

public class AcademicCompetitionController extends ToolController {
    @FXML
    private TableView<Map> dataTableView;

    @FXML
    private TableColumn<Map, String> studentIdColumn;
    @FXML
    private TableColumn<Map, String> timeColumn;
    @FXML
    private TableColumn<Map, String> subjectColumn;
    @FXML
    private TableColumn<Map, String> achievementColumn;

    @FXML
    private TextField studentIdField;
    @FXML
    private javafx.scene.control.DatePicker timeField;
    @FXML
    private TextField subjectField;
    @FXML
    private TextField achievementField;

    @FXML
    private TextField searchField;

    @FXML
    private Button saveButton; // 新增保存按钮的引用
    @FXML
    private Button deleteButton; // 新增删除按钮的引用

    @FXML
    private Button addButton;

    private Integer id = null;
    private ArrayList<Map> competitionList = new ArrayList<>();
    private ObservableList<Map> competitionObservableList = FXCollections.observableArrayList();

    private void setTableViewData() {
        competitionObservableList.clear();
        for(int j = 0; j < competitionList.size(); j++) {
            competitionObservableList.addAll(FXCollections.observableArrayList(competitionList.get(j)));
        }
        dataTableView.setItems(competitionObservableList);
    }

    @FXML
    public void initialize() {
        JwtResponse jwtResponse = AppStore.getJwt();
        String currentStudentId = String.valueOf(jwtResponse.getId());
        DataResponse res;
        DataRequest req = new DataRequest();
        req.add("numName", "");
        res = HttpRequestUtil.request("/api/academicCompetition/getAcademicCompetitionList", req);
        if (res != null && res.getCode() == 0) {
            ArrayList<Map> filteredList = new ArrayList<>();
            for(Map record : (ArrayList<Map>) res.getData()) {
                if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                    filteredList.add(record);
                }
            }
            competitionList = filteredList;
            if (jwtResponse.getRole().equals("ROLE_ADMIN") || jwtResponse.getRole().equals("ROLE_TEACHER")) {
                competitionList = (ArrayList<Map>) res.getData();
            }
        }
        studentIdColumn.setCellValueFactory(new MapValueFactory<>("studentId"));
        timeColumn.setCellValueFactory(new MapValueFactory<>("time"));
        subjectColumn.setCellValueFactory(new MapValueFactory<>("subject"));
        achievementColumn.setCellValueFactory(new MapValueFactory<>("achievement"));

        timeField.setConverter(new LocalDateStringConverter("yyyy-MM-dd"));

        TableView.TableViewSelectionModel<Map> tsm = dataTableView.getSelectionModel();
        ObservableList<Integer> list = tsm.getSelectedIndices();
        list.addListener(this::onTableRowSelect);
        setTableViewData();

        // 根据角色控制按钮的可用性
        if (jwtResponse.getRole().equals("ROLE_STUDENT")) {
            addButton.setDisable(true);
            deleteButton.setDisable(true);
            saveButton.setDisable(true);
        } else {
            addButton.setDisable(false);
            saveButton.setDisable(false);
            deleteButton.setDisable(false);
        }
    }

    public void clearPanel() {
        id = null;
        studentIdField.setText("");
        timeField.getEditor().setText("");
        subjectField.setText("");
        achievementField.setText("");
    }

    public void changeStudentInfo() {
        Map<String,Object> form = dataTableView.getSelectionModel().getSelectedItem();
        if (form == null) {
            clearPanel();
            return;
        }
        id = CommonMethod.getInteger(form, "id");
        DataRequest req = new DataRequest();
        req.add("id", id);
        DataResponse res = HttpRequestUtil.request("/api/academicCompetition/getAcademicCompetitionInfo", req);
        if(res.getCode()!=0) {
            MessageDialog.showDialog(res.getMsg());
            return;
        }
        form = (Map) res.getData();
        studentIdField.setText(CommonMethod.getString(form, "studentId"));
        String dateStr = CommonMethod.getString(form, "time");
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        timeField.setValue(date);
        subjectField.setText(CommonMethod.getString(form, "subject"));
        achievementField.setText(CommonMethod.getString(form, "achievement"));
    }

    public void onTableRowSelect(ListChangeListener.Change<? extends Integer> change) {
        changeStudentInfo();
    }

    @FXML
    protected void onQueryButtonClick() {
        JwtResponse jwtResponse = AppStore.getJwt();
        String currentStudentId = String.valueOf(jwtResponse.getId());
        String search = searchField.getText();
        DataRequest req = new DataRequest();
        req.add("numName", search);
        DataResponse res = HttpRequestUtil.request("/api/academicCompetition/getAcademicCompetitionList", req);
        if (res != null && res.getCode() == 0) {
            ArrayList<Map> filteredList = new ArrayList<>();
            for(Map record : (ArrayList<Map>) res.getData()) {
                if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                    filteredList.add(record);
                }
            }
            competitionList = filteredList;
            if (jwtResponse.getRole().equals("ROLE_ADMIN") || jwtResponse.getRole().equals("ROLE_TEACHER")) {
                competitionList = (ArrayList<Map>) res.getData();
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
            MessageDialog.showDialog("没有选择，无法删除");
            return;
        }
        int ret = MessageDialog.choiceDialog("确认要删除吗?");
        if (ret != MessageDialog.CHOICE_YES) {
            return;
        }
        id = CommonMethod.getInteger(form, "id");
        DataRequest req = new DataRequest();
        req.add("id", id);
        DataResponse res = HttpRequestUtil.request("/api/academicCompetition/academicCompetitionDelete", req);
        if(res!=null) {
            if(res.getCode()==0) {
                MessageDialog.showDialog("删除成功！");
                onQueryButtonClick();
            } else {
                MessageDialog.showDialog(res.getMsg());
            }
        }
    }

    @FXML
    protected void onSaveButtonClick() {
        if(studentIdColumn.getText().isEmpty()) {
            MessageDialog.showDialog("学号为空，不能修改");
            return;
        }
        Map<String,Object> form = new HashMap<>();
        form.put("id",id);
        form.put("studentId",studentIdField.getText());
        form.put("time",timeField.getEditor().getText());
        form.put("subject",subjectField.getText());
        form.put("achievement",achievementField.getText());
        DataRequest req = new DataRequest();
        req.add("form",form);
        req.add("id",id);
        DataResponse res = HttpRequestUtil.request("/api/academicCompetition/academicCompetitionEditSave", req);
        if(res.getCode()==0) {
            id = CommonMethod.getIntegerFromObject(res.getData());
            MessageDialog.showDialog("保存成功！");
            onQueryButtonClick();
        } else {
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