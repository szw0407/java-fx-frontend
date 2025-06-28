package com.teach.javafx.controller;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.cell.MapValueFactory;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.FlowPane;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CourseController 课程管理交互控制类 对应 course-panel.fxml
 *  @FXML  属性 对应fxml文件中的
 *  @FXML 方法 对应于fxml文件中的 on***Click的属性
 */
public class CourseController {
    @FXML
    public TableColumn<Map, String> courseTypeColumn;
    @FXML
    public TableColumn<Map, String> departmentColumn;
    @FXML
    public TableColumn<Map, String> descriptionColumn;
    @FXML
    private TableView<Map<String, Object>> dataTableView;
    @FXML
    private TableColumn<Map, String> numColumn;
    @FXML
    private TableColumn<Map, String> nameColumn;
    @FXML
    private TableColumn<Map, String> creditColumn;
    @FXML
    private TableColumn<Map, String> preCourseColumn;
    @FXML
    private TableColumn<Map,FlowPane> operateColumn;
      // 查询条件字段
    @FXML
    private TextField queryNumField;
    @FXML
    private TextField queryNameField;

    private List<Map<String,Object>> courseList = new ArrayList<>();  // 课程信息列表数据
    private final ObservableList<Map<String,Object>> observableList= FXCollections.observableArrayList();  // TableView渲染列表
    private boolean hasNewRow = false; // 标记是否已经有新建行@FXML
    @FXML
    private void onQueryButtonClick(){
        DataResponse res;
        DataRequest req = new DataRequest();
        
        // 添加查询条件
        String queryNum = queryNumField.getText();
        if (queryNum != null && !queryNum.trim().isEmpty()) {
            req.add("numName", queryNum.trim());
        }
        
        res = HttpRequestUtil.request("/api/course/getCourseList", req); //从后台获取所有课程信息列表集合
        if(res != null && res.getCode() == 0) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) res.getData();
            courseList = data;        }
        hasNewRow = false; // 重新查询时清除新建行标记
        setTableViewData();
    }
    private void setTableViewData() {
       observableList.clear();
       Map<String,Object> map;
        FlowPane flowPane;
        Button saveButton,deleteButton;
        
        // 首先添加现有课程数据
        for (int j = 0; j < courseList.size(); j++) {
            map = courseList.get(j);
            flowPane = new FlowPane();
            flowPane.setHgap(10);
            flowPane.setAlignment(Pos.CENTER);
            saveButton = new Button("修改保存");
            saveButton.setId("save"+j);
            saveButton.setOnAction(e->{
                saveItem(((Button)e.getSource()).getId());
            });
            deleteButton = new Button("删除");
            deleteButton.setId("delete"+j);
            deleteButton.setOnAction(e->{
                deleteItem(((Button)e.getSource()).getId());
            });
            flowPane.getChildren().addAll(saveButton,deleteButton);
            map.put("operate",flowPane);
            observableList.add(map);
        }
        
        dataTableView.setItems(observableList);
    }
      /**
     * 新建课程按钮点击事件 - 在表格下方添加一行新建行
     */
    @FXML
    private void onAddButtonClick() {
        if (hasNewRow) {
            // 已经有新建行了，显示提示
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("提示");
            alert.setHeaderText("已有新建行");
            alert.setContentText("请先保存当前新建的课程，再添加新的课程。");
            alert.showAndWait();
            return;
        }
        
        // 创建新建行数据
        Map<String, Object> newCourse = new HashMap<>();
        newCourse.put("num", "");
        newCourse.put("name", "");
        newCourse.put("credit", "");
        newCourse.put("preCourse", "");
        newCourse.put("courseType", "");
        newCourse.put("department", "");
        newCourse.put("description", "");
        newCourse.put("isNew", true); // 标记为新建行
        
        // 创建保存按钮
        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(10);
        flowPane.setAlignment(Pos.CENTER);
        Button saveButton = new Button("保存");
        Button cancleButton = new Button("取消");
        saveButton.setId("saveNew");
        saveButton.setOnAction(e -> saveNewItem());
        cancleButton.setId("cancelNew");
        cancleButton.setOnAction(e->{
            // 取消新建行，移除最后一行
            observableList.removeLast();
            hasNewRow = false;
            dataTableView.scrollTo(observableList.size() - 1); // 滚动到最后一行

        });
        flowPane.getChildren().add(saveButton);
        flowPane.getChildren().add(cancleButton);
        newCourse.put("operate", flowPane);
        
        // 添加到表格末尾
        observableList.add(newCourse);
        hasNewRow = true;
        
        // 滚动到最后一行
        dataTableView.scrollTo(observableList.size() - 1);
    }

    public void saveItem(String name){
        if(name == null)
            return;
        int j = Integer.parseInt(name.substring(4));
        Map<String,Object> data = courseList.get(j);
//        System.out.println(data);
        // send request
        DataRequest req = new DataRequest();
        for (String key : data.keySet()) {
            if (key.equals("operate") ){
                continue;
            }
            req.add(key, data.get(key));
        }
        DataResponse res = HttpRequestUtil.request("/api/course/courseSave", req);
        if (res != null && res.getCode() == 0) {
            // alert
            var a =new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("保存成功");
            a.setHeaderText("课程信息已保存");
            a.setContentText("课程编号: " + data.get("num") + "\n课程名称: " + data.get("name"));
            a.showAndWait();
        } else {
            // alert
            var a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("保存失败");
            a.setHeaderText("课程信息保存失败");
            a.setContentText("请检查输入信息是否正确或联系管理员。");
            a.showAndWait();
        }
    }
    public void deleteItem(String name){
        if(name == null)
            return;
        int j = Integer.parseInt(name.substring(6));
        Map<String,Object> data = courseList.get(j);
//        System.out.println(data);
        DataRequest req = new DataRequest();
        req.add("courseId", data.get("courseId"));
        DataResponse res = HttpRequestUtil.request("/api/course/courseDelete", req);
        if (res != null && res.getCode() == 0) {
            var a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("删除成功");
            a.setHeaderText("课程信息已删除");
            a.setContentText("课程编号: " + data.get("num") + "\n课程名称: " + data.get("name"));
            a.showAndWait();
            courseList.remove(j);
            setTableViewData(); // 更新表格数据
        } else {
            var a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("删除失败");
            a.setHeaderText("课程信息删除失败");
            a.setContentText("请检查输入信息是否正确或联系管理员。");
            a.showAndWait();
        }
    }    @FXML
    public void initialize() {
        numColumn.setCellValueFactory(new MapValueFactory<>("num"));
        numColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        numColumn.setOnEditCommit(event -> {
            var map = event.getRowValue();
            map.put("num", event.getNewValue());
        });
        nameColumn.setCellValueFactory(new MapValueFactory<>("name"));
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameColumn.setOnEditCommit(event -> {
            var map = event.getRowValue();
            map.put("name", event.getNewValue());
        });
        creditColumn.setCellValueFactory(new MapValueFactory<>("credit"));
        creditColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        creditColumn.setOnEditCommit(event -> {
            Map map = event.getRowValue();
            map.put("credit", event.getNewValue());
        });
        preCourseColumn.setCellValueFactory(new MapValueFactory<>("preCourse"));
        preCourseColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        preCourseColumn.setOnEditCommit(event -> {
            Map<String, Object> map = event.getRowValue();
            map.put("preCourse", event.getNewValue());
        });
        courseTypeColumn.setCellValueFactory(new MapValueFactory<>("courseType"));
        courseTypeColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        courseTypeColumn.setOnEditCommit(event -> {
            Map<String, Object> map = event.getRowValue();
            map.put("courseType", event.getNewValue());
        });
        departmentColumn.setCellValueFactory(new MapValueFactory<>("department"));
        departmentColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        departmentColumn.setOnEditCommit(event -> {
            Map<String, Object> map = event.getRowValue();
            map.put("department", event.getNewValue());
        });
        descriptionColumn.setCellValueFactory(new MapValueFactory<>("description"));
        descriptionColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        descriptionColumn.setOnEditCommit(event -> {
            Map<String, Object> map = event.getRowValue();
            map.put("description", event.getNewValue());
        });
        dataTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        operateColumn.setCellValueFactory(new MapValueFactory<>("operate"));
        dataTableView.setEditable(true);
        onQueryButtonClick();
    }

    /**
     * 保存新建课程
     */
    private void saveNewItem() {
        // 找到新建行（最后一行）
        Map<String, Object> newCourse = observableList.get(observableList.size() - 1);
        
        // 验证必填字段
        String num = String.valueOf(newCourse.get("num"));
        String name = String.valueOf(newCourse.get("name"));
        if (num == null || num.trim().isEmpty() || "null".equals(num) ||
            name == null || name.trim().isEmpty() || "null".equals(name)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("验证失败");
            alert.setHeaderText("必填字段不能为空");
            alert.setContentText("课程号和课程名是必填字段，请填写完整。");
            alert.showAndWait();
            return;
        }
        
        // 发送保存请求
        DataRequest req = new DataRequest();
        for (String key : newCourse.keySet()) {
            if (!key.equals("operate") && !key.equals("isNew")) {
                if (key.equals("preCourse")) {
                    // 如果是前序课程，改成id
                    var preCourseName = String.valueOf(newCourse.get(key));
//                    courseList.get()
                    var id = courseList.stream()
                            .filter(course -> preCourseName.equals(course.get("name")))
                            .map(course -> course.get("courseId"))
                            .findFirst()
                            .orElse(null);
                    req.add("preCourseId", id);
                } else if (key.equals("credit")) {
                    // 学分字段转换为整数
                    String creditStr = String.valueOf(newCourse.get(key));
                    Integer credit = creditStr != null && !creditStr.trim().isEmpty() ? Integer.parseInt(creditStr) : null;
                    req.add(key, credit);
                } else {
                req.add(key, newCourse.get(key));}
            }
        }
        
        DataResponse res = HttpRequestUtil.request("/api/course/courseSave", req);
        if (res != null && res.getCode() == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("保存成功");
            alert.setHeaderText("课程信息已保存");
            alert.setContentText("课程编号: " + num + "\n课程名称: " + name);
            alert.showAndWait();
            
            // 保存成功后，重新加载数据，这样新建的课程就会显示为常规行
            hasNewRow = false;
            onQueryButtonClick();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("保存失败");
            alert.setHeaderText("课程信息保存失败");
            alert.setContentText("请检查输入信息是否正确或联系管理员。");
            alert.showAndWait();
        }
    }
}
