package com.teach.javafx.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生选课界面
 * 支持学年、学期、课程模糊搜索，显示所有教学班级，支持选课/退课
 * mock数据可替换为真实API
 */
public class StudentCourseSelectController {
    @FXML private TextField yearField;
    @FXML private ComboBox<String> termComboBox;
    @FXML private TextField courseQueryField;
    @FXML private TableView<TeachingClassVO> teachingClassTableView;
    @FXML private TableColumn<TeachingClassVO, String> courseNameColumn, courseNumColumn, teachClassNumColumn, teachersColumn, classTimeColumn, classLocationColumn, actionColumn;

    // mock数据
    private ObservableList<TeachingClassVO> allTeachingClasses = FXCollections.observableArrayList();
    // 已选教学班id
    private Set<String> mySelectedClassIds = new HashSet<>();

    // 假设当前学生ID
    private final String currentStudentId = "2023001";

    @FXML
    public void initialize() {
        initMockData();

        termComboBox.setItems(FXCollections.observableArrayList("1", "2"));

        courseNameColumn.setCellValueFactory(cell -> cell.getValue().courseNameProperty());
        courseNumColumn.setCellValueFactory(cell -> cell.getValue().courseNumProperty());
        teachClassNumColumn.setCellValueFactory(cell -> cell.getValue().teachClassNumProperty());
        teachersColumn.setCellValueFactory(cell -> cell.getValue().teachersProperty());
        classTimeColumn.setCellValueFactory(cell -> cell.getValue().classTimeProperty());
        classLocationColumn.setCellValueFactory(cell -> cell.getValue().classLocationProperty());

        actionColumn.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button();
            {
                btn.setOnAction(e -> {
                    TeachingClassVO tc = getTableView().getItems().get(getIndex());
                    if (mySelectedClassIds.contains(tc.getId())) {
                        // 退课
                        mySelectedClassIds.remove(tc.getId());
                        showAlert("退课成功", "你已退选该教学班！");
                    } else {
                        // 选课
                        mySelectedClassIds.add(tc.getId());
                        showAlert("选课成功", "你已成功选上该教学班！");
                    }
                    teachingClassTableView.refresh();
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TeachingClassVO vo = getTableView().getItems().get(getIndex());
                    if (mySelectedClassIds.contains(vo.getId())) {
                        btn.setText("退课");
                    } else {
                        btn.setText("选课");
                    }
                    setGraphic(btn);
                }
            }
        });

        teachingClassTableView.setItems(allTeachingClasses);
    }

    @FXML
    public void onQueryButtonClick(ActionEvent event) {
        String year = yearField.getText().trim();
        String term = termComboBox.getValue();
        String query = courseQueryField.getText().trim();

        List<TeachingClassVO> filtered = allTeachingClasses.stream().filter(tc ->
                (year.isEmpty() || tc.getYear().equals(year)) &&
                        (term == null || tc.getTerm().equals(term)) &&
                        (query.isEmpty() ||
                                tc.getCourseName().contains(query) ||
                                tc.getCourseNum().contains(query)
                        )
        ).collect(Collectors.toList());
        teachingClassTableView.setItems(FXCollections.observableArrayList(filtered));
    }

    // ==== 数据结构 ====
    public static class TeachingClassVO {
        private final String id;
        private final SimpleStringProperty courseName, courseNum, teachClassNum, teachers, classTime, classLocation, year, term;
        public TeachingClassVO(String id, String cname, String cnum, String tcNum, String teachers, String time, String location, String year, String term) {
            this.id = id;
            this.courseName = new SimpleStringProperty(cname);
            this.courseNum = new SimpleStringProperty(cnum);
            this.teachClassNum = new SimpleStringProperty(tcNum);
            this.teachers = new SimpleStringProperty(teachers);
            this.classTime = new SimpleStringProperty(time);
            this.classLocation = new SimpleStringProperty(location);
            this.year = new SimpleStringProperty(year);
            this.term = new SimpleStringProperty(term);
        }
        public String getId() { return id; }
        public String getCourseName() { return courseName.get(); }
        public String getCourseNum() { return courseNum.get(); }
        public String getTeachClassNum() { return teachClassNum.get(); }
        public String getTeachers() { return teachers.get(); }
        public String getClassTime() { return classTime.get(); }
        public String getClassLocation() { return classLocation.get(); }
        public String getYear() { return year.get(); }
        public String getTerm() { return term.get(); }
        public SimpleStringProperty courseNameProperty() { return courseName; }
        public SimpleStringProperty courseNumProperty() { return courseNum; }
        public SimpleStringProperty teachClassNumProperty() { return teachClassNum; }
        public SimpleStringProperty teachersProperty() { return teachers; }
        public SimpleStringProperty classTimeProperty() { return classTime; }
        public SimpleStringProperty classLocationProperty() { return classLocation; }
    }

    // ==== mock数据 ====
    private void initMockData() {
        // 假设有四个教学班
        allTeachingClasses.add(new TeachingClassVO(
                "TCID-1", "程序设计", "CS101", "101A", "王老师,赵老师", "周一 8:00-10:00", "A101", "2024", "1"
        ));
        allTeachingClasses.add(new TeachingClassVO(
                "TCID-2", "高等数学", "MA101", "102B", "李老师", "周三 10:00-12:00", "B201", "2024", "1"
        ));
        allTeachingClasses.add(new TeachingClassVO(
                "TCID-3", "程序设计", "CS101", "103C", "孙老师", "周四 14:00-16:00", "A102", "2024", "2"
        ));
        allTeachingClasses.add(new TeachingClassVO(
                "TCID-4", "大学英语", "EN101", "201A", "刘老师,陈老师", "周五 8:00-10:00", "C301", "2024", "1"
        ));
        // 假设当前学生已选第一个和第四个教学班
        mySelectedClassIds.add("TCID-1");
        mySelectedClassIds.add("TCID-4");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}