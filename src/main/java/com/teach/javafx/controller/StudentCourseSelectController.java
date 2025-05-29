package com.teach.javafx.controller;

import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.HttpRequestUtil;
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
    private Set<ScoreRec> mySelectedClassIds = new HashSet<>();

    // 假设当前学生ID
    private final String currentStudentId = "2023001";

    @FXML
    public void initialize() {
//        initMockData();

        termComboBox.setItems(FXCollections.observableArrayList("1", "2"));
        var cal = Calendar.getInstance();
        yearField.setText(cal.get(Calendar.YEAR) + "");
        yearField.setPromptText("请输入学年");
        switch (cal.get(Calendar.MONTH)) {
            case Calendar.JULY, Calendar.FEBRUARY, Calendar.MARCH, Calendar.APRIL, Calendar.MAY, Calendar.JUNE -> termComboBox.setValue("2");
            case Calendar.JANUARY, Calendar.AUGUST, Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER -> termComboBox.setValue("1");
        }
        initData();
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
                    if (mySelectedClassIds.stream().map(ScoreRec::getClassId).anyMatch(id -> id.equals(tc.getId()))) {
                        // try communicate with api
                        var dr =new DataRequest();
                        dr.add("scoreId", mySelectedClassIds.stream().filter(tcid -> tcid.classId.equals(tc.getId())).findFirst().get().scoreId);
                        var a = HttpRequestUtil.request("/api/courseSelection/dropCourse", dr);
                        if (a == null || a.getCode() != 0) {

                                showAlert("退课失败", "退课失败: " + (a!=null? a.getMsg():""));

                            return;
                        }
initData();
                        showAlert("退课成功", "你已退选该教学班！");
                    } else {
                        // 选课
                        var dr =new DataRequest();
                        dr.add("classScheduleId", tc.getId());
                        var a = HttpRequestUtil.request("/api/courseSelection/selectCourse", dr);
                        if (a == null || a.getCode() != 0) {
                            showAlert("选课失败", "选课失败: " +(a!=null? a.getMsg():""));
                            return;
                        }
                        initData();
                        //

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
                    if (mySelectedClassIds.stream().map(ScoreRec::getClassId).anyMatch(id -> id.equals(vo.getId()))) {
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
        initData();
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
    public static class ScoreRec {
        private final String classId;
        private final String scoreId;

        public ScoreRec(String classId, String scoreId) {
            this.classId = classId;
            this.scoreId = scoreId;
        }

        public String getClassId() {
            return classId;
        }

        public String getScoreId() {
            return scoreId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ScoreRec scoreRec)) return false;
            return Objects.equals(classId, scoreRec.classId) && Objects.equals(scoreId, scoreRec.scoreId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(classId, scoreId);
        }
    }
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
//        mySelectedClassIds.add("TCID-1");
//        mySelectedClassIds.add("TCID-4");
    }

    private void initData() {
        var dr = new DataRequest();
        // add year and semester
        dr.add("year", yearField.getText());
        dr.add("semester", termComboBox.getValue());
        var avail = HttpRequestUtil.request("/api/me/AvailableCourseList", dr);
        if (avail != null && avail.getCode() == 0) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) avail.getData();
            allTeachingClasses.clear();
            for (Map<String, Object> item : data) {
                String id = item.get("classScheduleId").toString();
                String cname = item.get("courseName").toString();
                String cnum = item.get("courseNum").toString();
                String tcNum = item.get("classNumber").toString();
                String teachers = item.get("teachers").toString();
                String time = item.get("classTime").toString();
                String location = item.get("classLocation").toString();
                String year = item.get("year").toString();
                String term = item.get("semester").toString();
                allTeachingClasses.add(new TeachingClassVO(id, cname, cnum, tcNum, teachers, time, location, year, term));
            }
        } else {
            showAlert("错误", "获取可选课程失败: " + avail.getMsg());
        }

        var mysel = HttpRequestUtil.request("/api/me/ScoreList", dr);
        if (mysel != null &&mysel.getCode() == 0) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) mysel.getData();
            mySelectedClassIds.clear();
            for (Map<String, Object> item : data) {
                String classId = item.get("classScheduleId").toString();
                String scoreId = item.get("scoreId").toString();
                mySelectedClassIds.add(new ScoreRec(classId, scoreId));
            }
        } else {
            showAlert("错误", "获取已选课程失败: " + mysel.getMsg());
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}