package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.*;
import com.teach.javafx.util.CommonMethod;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentSocialActivityController {

    @FXML
    private TextField studentIdTextField;
    @FXML
    private TableView<Map> dataTableView;

    @FXML
    private TableColumn<Map, String> id;

    @FXML
    private TableColumn<Map, String> nameColumn;

    @FXML
    private TableColumn<Map, String> studentId;


    @FXML
    private TableColumn<Map, String> typeColumn;

    @FXML
    private TableColumn<Map, String> startTimeColumn;

    @FXML
    private TableColumn<Map, String> endTimeColumn;

    @FXML
    private TableColumn<Map, String> locationColumn;

    @FXML
    private TableColumn<Map, String> descriptionColumn;

    @FXML
    private TableColumn<Map, String> roleColumn;


    @FXML
    private TextField idField;

    @FXML
    private TextField nameField;

    @FXML
    public TextField studentField;

    @FXML
    private ComboBox<OptionItem> typeComboBox;

    @FXML
    private DatePicker startTimePicker;

    @FXML
    private DatePicker endTimePicker;

    @FXML
    private TextField locationField;

    @FXML
    private TextField descriptionField;

    @FXML
    private ComboBox<OptionItem> roleComboBox;

    private Integer Id = null;  //当前编辑修改的学生的主键

    private ArrayList<Map> studentSocialActList = new ArrayList();  // 学生信息列表数据
    private List<OptionItem> typelist;//性别选择列表数据
    private List<OptionItem> rolelist;   //性别选择列表数据
    private ObservableList<Map> observableList = FXCollections.observableArrayList();  // TableView渲染列表

    private void setStudentSocialActData() {
        observableList.clear();
        observableList.addAll(studentSocialActList);
        dataTableView.setItems(observableList);//这里的dataTableView的数据源是observableList
        //observablelist的数据源是studentSocialActList，所以datatableview的数据源是studentSocialActList
    }

    private String getCurrentStudentId() { // 用于得到登录者ID
        JwtResponse jwtResponse = AppStore. getJwt();
        // AppStore 中存储了当前登录用户的 JwtResponse
        if (jwtResponse != null) {
            return String.valueOf(jwtResponse.getUsername()); // 返回用户的 ID
        }
        System.out.println("获取当前学生ID失败");
        return null;
    }

    @FXML
    public void initialize() {
        DataResponse res;
        DataRequest req = new DataRequest();
        //req.add("numName", "");
        res = HttpRequestUtil.request("/api/studentSocialAct/getlist", req); //从后台获取所有学生社会活动信息列表集合
        if (res != null && res.getCode() == 0) {
            String currentStudentId = getCurrentStudentId();
            ArrayList<Map> filteredList = new ArrayList<>();

            if (currentStudentId.equals("admin")) {//如果是管理员，则显示所有学生的社会活动信息
                //这里我就使用了一个姓名的判断
                studentSocialActList = (ArrayList<Map>) res.getData();
                for (Map record : studentSocialActList) {
                    if (record.get("type").equals("1")) {
                        record.put("type", "志愿服务");
                    } else if (record.get("type").equals("2")) {
                        record.put("type", "体育活动");
                    } else if (record.get("type").equals("3")) {
                        record.put("type", "文艺演出");
                    }

                    if (record.get("role").equals("1")) {
                        record.put("role", "负责人");
                    } else if (record.get("role").equals("2")) {
                        record.put("role", "队员");
                    }

                }
            } else {
                for (Map record : (ArrayList<Map>) res.getData()) {
                    // 将 record.get("studentId") 转换为字符串后再比较
                    if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                        filteredList.add(record);
                    }
                    studentSocialActList = filteredList;
                    for (Map records : studentSocialActList) {
                        if (records.get("type").equals("1")) {
                            records.put("type", "志愿服务");
                        } else if (record.get("type").equals("2")) {
                            records.put("type", "体育活动");
                        } else if (record.get("type").equals("3")) {
                            records.put("type", "文艺演出");
                        }

                        if (record.get("role").equals("1")) {
                            record.put("role", "负责人");
                        } else if (record.get("role").equals("2")) {
                            record.put("role", "队员");
                        }

                    }
                }
            }
        }
        id.setCellValueFactory(new MapValueFactory<>("id"));  //设置列值工程属性
        nameColumn.setCellValueFactory(new MapValueFactory<>("name"));
        studentId.setCellValueFactory(new MapValueFactory<>("studentId"));
        typeColumn.setCellValueFactory(new MapValueFactory<>("type"));
        startTimeColumn.setCellValueFactory(new MapValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new MapValueFactory<>("endTime"));
        locationColumn.setCellValueFactory(new MapValueFactory<>("location"));
        descriptionColumn.setCellValueFactory(new MapValueFactory<>("description"));
        roleColumn.setCellValueFactory(new MapValueFactory<>("role"));
        TableView.TableViewSelectionModel<Map> tsm = dataTableView.getSelectionModel();

        typelist = HttpRequestUtil.getDictionaryOptionItemList("HDM");
        rolelist = HttpRequestUtil.getDictionaryOptionItemList("HDR");
        typeComboBox.getItems().addAll(typelist);
        roleComboBox.getItems().addAll(rolelist);
        ObservableList<Integer> list = tsm.getSelectedIndices();
        setStudentSocialActData();
    }
    public void clearPanel() {
        idField.setText("");
        nameField.setText("");
        studentField.setText("");
        typeComboBox.getSelectionModel().select(-1);//这里将活动类型认为是可选的
        startTimePicker.getEditor().setText("");
        endTimePicker.getEditor().setText("");
        locationField.setText("");
        descriptionField.setText("");
        roleComboBox.getSelectionModel().select(-1);
    }
    @FXML
    protected void onSaveButtonClick() {
        if (idField.getText().isEmpty()) {
            MessageDialog.showDialog("序号为空，不能保存");
            return;
        }
        Map<String, String> newSocialAct = new HashMap<>();
        newSocialAct.put("id", idField.getText());
        newSocialAct.put("name", nameField.getText());
        newSocialAct.put("studentId", studentField.getText());
        //这里就是学着学生中的性别将活动类型作为一个可选的

        if (typeComboBox.getSelectionModel() != null && typeComboBox.getSelectionModel().getSelectedItem() != null)
            newSocialAct.put("type", typeComboBox.getSelectionModel().getSelectedItem().getValue());

        newSocialAct.put("startTime", startTimePicker.getEditor().getText());
        //System.out.println(startTimePicker.getEditor().getText());
        //System.out.println(endTimePicker.getEditor().getText());
        newSocialAct.put("endTime", endTimePicker.getEditor().getText());
        newSocialAct.put("location", locationField.getText());
        if (roleComboBox.getSelectionModel() != null && roleComboBox.getSelectionModel().getSelectedItem() != null)
            newSocialAct.put("role", roleComboBox.getSelectionModel().getSelectedItem().getValue());
        newSocialAct.put("description", descriptionField.getText());
        DataRequest req = new DataRequest();
        req.add("form", newSocialAct);
        DataResponse res = HttpRequestUtil.request("/api/studentSocialAct/SocialActEditsave", req);
        if (res != null) {
            if (res.getCode() == 0) {
                MessageDialog.showDialog("保存成功！");
                onQueryButtonClick();
            } else {
                MessageDialog.showDialog(res.getMsg());
            }
        } else {
            MessageDialog.showDialog("请求失败，服务器无响应");
        }
    }
    @FXML
    protected void onQueryButtonClick() {
        DataRequest req = new DataRequest();
        String studentId = studentIdTextField.getText();

        req.add("studentId", studentId);
        DataResponse res = HttpRequestUtil.request("/api/studentSocialAct/getlist", req);
        if (res != null) {
            if (res.getCode() == 0) {
                String currentStudentId = getCurrentStudentId();
                ArrayList<Map> filteredList = new ArrayList<>();

                if (currentStudentId.equals("admin")) {//如果是管理员，则显示所有学生的社会活动信息
                    //这里我就使用了一个姓名的判断
                    studentSocialActList = (ArrayList<Map>) res.getData();
                    for (Map record : studentSocialActList) {
                        if (record.get("type").equals("1")) {
                            record.put("type", "志愿服务");
                        } else if (record.get("type").equals("2")) {
                            record.put("type", "体育活动");
                        } else if (record.get("type").equals("3")) {
                            record.put("type", "文艺演出");
                        }

                        if (record.get("role").equals("1")) {
                            record.put("role", "负责人");
                        } else if (record.get("role").equals("2")) {
                            record.put("role", "队员");
                        }

                    }
                } else {
                    for (Map record : (ArrayList<Map>) res.getData()) {
                        // 将 record.get("studentId") 转换为字符串后再比较
                        if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                            filteredList.add(record);
                        }
                        studentSocialActList = filteredList;
                        for (Map records : studentSocialActList) {
                            if (records.get("type").equals("1")) {
                                records.put("type", "志愿服务");
                            } else if (record.get("type").equals("2")) {
                                records.put("type", "体育活动");
                            } else if (record.get("type").equals("3")) {
                                records.put("type", "文艺演出");
                            }

                            if (record.get("role").equals("1")) {
                                record.put("role", "负责人");
                            } else if (record.get("role").equals("2")) {
                                record.put("role", "队员");
                            }
                        }
                    }
                }
                setStudentSocialActData();
            } else {
                MessageDialog.showDialog(res.getMsg());
            }
        } else {
            MessageDialog.showDialog("请求失败，服务器无响应");
        }
    }

    @FXML
    protected void onDeleteButtonClick() {
        Map form = dataTableView.getSelectionModel().getSelectedItem();
        if (form == null) {
            MessageDialog.showDialog("没有选择，不能删除");
            return;
        }
        int ret = MessageDialog.choiceDialog("确认要删除吗?");
        if (ret != MessageDialog.CHOICE_YES) {
            return;
        }
        Id = CommonMethod.getInteger(form, "id");//这里把id作为学生活动的主键
        //这里的key一定要输入已经存在的，不能是自己编造的，而下面的是可以自己编的，怎么起名都没有问题
//        System.out.println(Id);
        DataRequest req = new DataRequest();
        req.add("Id", Id);
        DataResponse res = HttpRequestUtil.request("/api/studentSocialAct/SocialActDelete", req);
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
    protected void onAddButtonClick() {
        clearPanel();
    }

}
