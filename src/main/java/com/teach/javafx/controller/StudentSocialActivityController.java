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

import java.time.LocalDate;
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
    private DatePicker rangeDatePicker;

    @FXML
    private Label startDateLabel;

    @FXML
    private Label endDateLabel;

    @FXML
    private TextField locationField;

    @FXML
    private TextField descriptionField;

    @FXML
    private ComboBox<OptionItem> roleComboBox;

    private Integer Id = null;  //当前编辑修改的学生的主键
    private ArrayList<Map> studentSocialActList = new ArrayList();  // 学生信息列表数据
    private List<OptionItem> typelist; //性别选择列表数据
    private List<OptionItem> rolelist;   //性别选择列表数据
    private ObservableList<Map> observableList = FXCollections.observableArrayList();  // TableView渲染列表

    private void setStudentSocialActData() {
        observableList.clear();
        observableList.addAll(studentSocialActList);
        dataTableView.setItems(observableList);
    }

    private String getCurrentStudentId() {
        JwtResponse jwtResponse = AppStore.getJwt();
        if (jwtResponse != null) {
            return String.valueOf(jwtResponse.getUsername());
        }
        System.out.println("获取当前学生ID失败");
        return null;
    }

    @FXML
    public void initialize() {
        DataResponse res;
        DataRequest req = new DataRequest();
        res = HttpRequestUtil.request("/api/studentSocialAct/getlist", req);
        if (res != null && res.getCode() == 0) {
            String currentStudentId = getCurrentStudentId();
            ArrayList<Map> filteredList = new ArrayList<>();

            if (currentStudentId.equals("admin")) {
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
                    if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                        filteredList.add(record);
                    }
                    studentSocialActList = filteredList;
                    for (Map records : studentSocialActList) {
                        if (records.get("type").equals("1")) {
                            records.put("type", "志愿服务");
                        } else if (records.get("type").equals("2")) {
                            records.put("type", "体育活动");
                        } else if (records.get("type").equals("3")) {
                            records.put("type", "文艺演出");
                        }

                        if (records.get("role").equals("1")) {
                            records.put("role", "负责人");
                        } else if (records.get("role").equals("2")) {
                            records.put("role", "队员");
                        }
                    }
                }
            }
        }

        id.setCellValueFactory(new MapValueFactory<>("id"));
        nameColumn.setCellValueFactory(new MapValueFactory<>("name"));
        studentId.setCellValueFactory(new MapValueFactory<>("studentId"));
        typeColumn.setCellValueFactory(new MapValueFactory<>("type"));
        startTimeColumn.setCellValueFactory(new MapValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new MapValueFactory<>("endTime"));
        locationColumn.setCellValueFactory(new MapValueFactory<>("location"));
        descriptionColumn.setCellValueFactory(new MapValueFactory<>("description"));
        roleColumn.setCellValueFactory(new MapValueFactory<>("role"));

        rangeDatePicker.setPromptText("选择日期范围");
        startDateLabel.setText("未选择");
        endDateLabel.setText("未选择");

        final LocalDate[] selectedStartDate = {null};
        final LocalDate[] selectedEndDate = {null};

        rangeDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (empty || date == null) {
                    return;
                }

                // 禁用过去的日期（可选）
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                }

                // 高亮显示选中的范围
                if (selectedStartDate[0] != null && selectedEndDate[0] != null) {
                    if (date.isAfter(selectedStartDate[0]) && date.isBefore(selectedEndDate[0])) {
                        setStyle("-fx-background-color: #a9d0f5;");
                    }
                }

                // 高亮显示开始和结束日期
                if (date.equals(selectedStartDate[0]) || date.equals(selectedEndDate[0])) {
                    setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white;");
                }
            }
        });

        rangeDatePicker.setOnAction(event -> {
            LocalDate selectedDate = rangeDatePicker.getValue();

            // 添加null检查
            if (selectedDate == null) {
                return;
            }

            if (selectedStartDate[0] == null && selectedEndDate[0] == null) {
                // 第一次选择 - 设为开始日期
                selectedStartDate[0] = selectedDate;
                startDateLabel.setText(selectedDate.toString());
                endDateLabel.setText("请选择结束日期");
                rangeDatePicker.setValue(null);
            } else if (selectedStartDate[0] != null && selectedEndDate[0] == null) {
                // 选择结束日期
                if (selectedDate.isBefore(selectedStartDate[0])) {
                    // 如果结束日期早于开始日期，交换它们
                    selectedEndDate[0] = selectedStartDate[0];
                    selectedStartDate[0] = selectedDate;
                } else {
                    selectedEndDate[0] = selectedDate;
                }
                endDateLabel.setText(selectedEndDate[0].toString());

                // 重置选择状态，准备新的选择
                rangeDatePicker.setValue(null);
                selectedStartDate[0] = null;
                selectedEndDate[0] = null;
            }
        });

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
        typeComboBox.getSelectionModel().select(-1);
        locationField.setText("");
        descriptionField.setText("");
        roleComboBox.getSelectionModel().select(-1);
        startDateLabel.setText("未选择");
        endDateLabel.setText("未选择");
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

        if (typeComboBox.getSelectionModel() != null && typeComboBox.getSelectionModel().getSelectedItem() != null)
            newSocialAct.put("type", typeComboBox.getSelectionModel().getSelectedItem().getValue());

        // 获取日期
        String startTimeText = startDateLabel.getText();
        String endTimeText = endDateLabel.getText();

        if ("未选择".equals(startTimeText) || "未选择".equals(endTimeText)) {
            MessageDialog.showDialog("请选择完整的日期范围");
            return;
        }

        // 转换日期格式从 "2025-05-25" 到 "2025/05/25"
        startTimeText = startTimeText.replace("-", "/");
        endTimeText = endTimeText.replace("-", "/");

        newSocialAct.put("startTime", startTimeText);
        newSocialAct.put("endTime", endTimeText);
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

                if (currentStudentId.equals("admin")) {
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
                        if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                            filteredList.add(record);
                        }
                        studentSocialActList = filteredList;
                        for (Map records : studentSocialActList) {
                            if (records.get("type").equals("1")) {
                                records.put("type", "志愿服务");
                            } else if (records.get("type").equals("2")) {
                                records.put("type", "体育活动");
                            } else if (records.get("type").equals("3")) {
                                records.put("type", "文艺演出");
                            }

                            if (records.get("role").equals("1")) {
                                records.put("role", "负责人");
                            } else if (records.get("role").equals("2")) {
                                records.put("role", "队员");
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
        Id = CommonMethod.getInteger(form, "id");
        DataRequest req = new DataRequest();
        req.add("Id", Id);
        DataResponse res = HttpRequestUtil.request("/api/studentSocialAct/SocialActDelete", req);
        if(res != null) {
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