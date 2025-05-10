package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.MainApplication;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.*;
import com.teach.javafx.util.CommonMethod;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InnovationProjectTableController {
    @FXML
    private TableView<Map> dataTableView;
    @FXML
    private TableColumn<Map, String> idColumn;
    @FXML
    private TableColumn<Map, String> nameColumn;
    @FXML
    private TableColumn<Map, String> studentIdColumn;
    @FXML
    private TableColumn<Map, String> typeColumn;
    @FXML
    private TableColumn<Map, String> timeColumn;
    @FXML
    private TableColumn<Map, String> descriptionColumn;
    @FXML
    private TableColumn<Map, String> editColumn;

    private ArrayList<Map> iList  = new ArrayList<>();
    private ObservableList<Map> observableList = FXCollections.observableArrayList();

    @FXML
    private ComboBox<OptionItem> studentComboBox;
    private List<OptionItem> studentList;

    private InnovationProjectEditController innovationProjectEditController=null;
    private Stage stage=null;
    public List<OptionItem> getStudentList(){return studentList;}

    @FXML
    private void onQueryButtonClick() {
        JwtResponse jwtResponse = AppStore. getJwt();
        String currentStudentId = String.valueOf(jwtResponse.getId());
        Integer studentId=0;
        OptionItem op;
        op = studentComboBox.getSelectionModel().getSelectedItem();
        if(op != null)
            studentId = Integer.parseInt(op.getValue());
        DataResponse res;
        DataRequest req =new DataRequest();
        req.add("studentId",studentId);
        res = HttpRequestUtil.request("/api/innovationProject/getInnovationProjectList",req);
        if(res != null && res.getCode()== 0) {
            ArrayList<Map> filteredList = new ArrayList<>();
            for(Map record : (ArrayList<Map>) res.getData()){
                if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                    filteredList.add(record);
                }
            }
            iList = filteredList;
            if (jwtResponse.getRole().equals("ROLE_ADMIN")) iList = (ArrayList<Map>) res.getData();
        }
        setTableViewData();
    }
    private void setTableViewData(){
        observableList.clear();
        Map map;
        Button editButton;
        for (int j = 0; j < iList.size(); j++) {
            map = iList.get(j);
            editButton = new Button("编辑");
            editButton.setId("edit"+j);
            editButton.setOnAction(e->{
                editItem(((Button)e.getSource()).getId());
            });
            map.put("edit",editButton);
            observableList.addAll(FXCollections.observableArrayList(map));
        }
        dataTableView.setItems(observableList);
    }
    public void editItem(String name) {
        if(name == null)
            return;
        int j = Integer.parseInt(name.substring(4,name.length()));
        Map data = iList.get(j);
        initDialog();
        innovationProjectEditController.showDialog(data);
        MainApplication.setCanClose(false);
        stage.showAndWait();
    }
    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new MapValueFactory<>("id"));
        studentIdColumn.setCellValueFactory(new MapValueFactory<>("studentId"));
        nameColumn.setCellValueFactory(new MapValueFactory<>("name"));
        typeColumn.setCellValueFactory(new MapValueFactory<>("type"));
        timeColumn.setCellValueFactory(new MapValueFactory<>("time"));
        descriptionColumn.setCellValueFactory(new MapValueFactory<>("description"));
        editColumn.setCellValueFactory(new MapValueFactory<>("edit"));

        DataRequest req =new DataRequest();
        studentList = HttpRequestUtil.requestOptionItemList("/api/innovationProject/getStudentItemOptionList",req);
        OptionItem item = new OptionItem(null,"0","请选择");
        studentComboBox.getItems().addAll(item);
        studentComboBox.getItems().addAll(studentList);
        dataTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        onQueryButtonClick();
    }
    private void initDialog(){
        if(stage!= null)
            return;
        FXMLLoader fxmlLoader ;
        Scene scene = null;
        try {
            fxmlLoader = new FXMLLoader(MainApplication.class.getResource("innovation-project-edit-dialog.fxml"));
            scene = new Scene(fxmlLoader.load(), 300, 240);
            stage = new Stage();
            stage.initOwner(MainApplication.getMainStage());
            stage.initModality(Modality.NONE);
            stage.setAlwaysOnTop(true);
            stage.setScene(scene);
            stage.setTitle("创新项目录入对话框！");
            stage.setOnCloseRequest(event -> {
                MainApplication.setCanClose(true);
            });
            innovationProjectEditController = (InnovationProjectEditController) fxmlLoader.getController();
            innovationProjectEditController.setInnovationProjectTableController(this);
            innovationProjectEditController.init();
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }
    public void doClose(String cmd, Map<String, Object> data) {
        MainApplication.setCanClose(true);
        stage.close();
        if(!"ok".equals(cmd))
            return;
        DataResponse res;
        Integer personId = CommonMethod.getInteger(data,"studentId");
        if(personId == null) {
            MessageDialog.showDialog("没有选中学生不能添加保存！");
        }
        DataRequest req =new DataRequest();
        req.add("id",CommonMethod.getInteger(data,"id"));
        req.add("studentId",personId);
        req.add("name",CommonMethod.getString(data,"name"));
        req.add("type",CommonMethod.getString(data,"type"));
        req.add("time",CommonMethod.getString(data,"time"));
        req.add("description",CommonMethod.getString(data,"description"));
        res = HttpRequestUtil.request("/api/innovationProject/innovationProjectSave",req);
        if(res != null && res.getCode()== 0) {
            onQueryButtonClick();
        }
    }
    @FXML
    private void onAddButtonClick() {
        initDialog();
        innovationProjectEditController.showDialog(null);
        MainApplication.setCanClose(false);
        stage.showAndWait();
    }
    @FXML
    private void onEditButtonClick() {
        dataTableView.getSelectionModel().getSelectedItems();
        Map data = dataTableView.getSelectionModel().getSelectedItem();
        if(data == null) {
            MessageDialog.showDialog("没有选中，不能修改！");
            return;
        }
        initDialog();
        innovationProjectEditController.showDialog(data);
        MainApplication.setCanClose(false);
        stage.showAndWait();
    }
    @FXML
    private void onDeleteButtonClick() {
        Map<String,Object> form = dataTableView.getSelectionModel().getSelectedItem();
        if(form == null) {
            MessageDialog.showDialog("没有选择，不能删除");
            return;
        }
        int ret = MessageDialog.choiceDialog("确认要删除吗?");
        if(ret != MessageDialog.CHOICE_YES) {
            return;
        }
        Integer id = CommonMethod.getInteger(form,"id");
        DataRequest req = new DataRequest();
        req.add("id", id);
        DataResponse res = HttpRequestUtil.request("/api/innovationProject/innovationProjectDelete", req);
        if(res.getCode()==0){
            onQueryButtonClick();
        }else{
            MessageDialog.showDialog(res.getMsg());
        }
    }

}
