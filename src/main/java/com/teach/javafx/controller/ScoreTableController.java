package com.teach.javafx.controller;

import com.teach.javafx.MainApplication;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.OptionItem;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

public class ScoreTableController {
    @FXML private TableView<ScoreRecord> dataTableView;
    @FXML private TableColumn<ScoreRecord, String> studentNameColumn, classNameColumn, courseNumColumn, courseNameColumn, creditColumn,
            teachClassNumColumn, yearColumn, termColumn, markColumn, editColumn;
    @FXML private ComboBox<Student> studentComboBox;
    @FXML private ComboBox<Course> courseComboBox;

    private ObservableList<ScoreRecord> allScores = FXCollections.observableArrayList();
    private ObservableList<Student> allStudents = FXCollections.observableArrayList();
    private ObservableList<Course> allCourses = FXCollections.observableArrayList();
    private Map<String, List<TeachingClass>> studentTeachClassMap = new HashMap<>();

    @FXML
    public void initialize() {
        initMockData();

        // 绑定表格列
        studentNameColumn.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
        classNameColumn.setCellValueFactory(cell -> cell.getValue().classNameProperty());
        courseNumColumn.setCellValueFactory(cell -> cell.getValue().courseNumProperty());
        courseNameColumn.setCellValueFactory(cell -> cell.getValue().courseNameProperty());
        creditColumn.setCellValueFactory(cell -> cell.getValue().creditProperty());
        teachClassNumColumn.setCellValueFactory(cell -> cell.getValue().teachClassNumProperty());
        yearColumn.setCellValueFactory(cell -> cell.getValue().yearProperty());
        termColumn.setCellValueFactory(cell -> cell.getValue().termProperty());
        markColumn.setCellValueFactory(cell -> cell.getValue().markProperty());
        editColumn.setCellFactory(col -> new TableCell<>() {
            final Button editBtn = new Button("编辑");
            final Button delBtn = new Button("删除");
            {
                editBtn.setOnAction(e -> onEditButtonClick(null));
                delBtn.setOnAction(e -> onDeleteButtonClick(null));
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(new HBox(5, editBtn, delBtn));
            }
        });

        // 绑定下拉框
        studentComboBox.setItems(allStudents);
        courseComboBox.setItems(allCourses);

        dataTableView.setItems(allScores);
    }

    // 查询按钮
    @FXML
    public void onQueryButtonClick(ActionEvent event) {
        Student stu = studentComboBox.getValue();
        Course course = courseComboBox.getValue();
        List<ScoreRecord> filtered = allScores.stream().filter(s ->
                (stu == null || s.getStudentName().equals(stu.getStudentName())) &&
                        (course == null || s.getCourseNum().equals(course.getCourseNum()))
        ).collect(Collectors.toList());
        dataTableView.setItems(FXCollections.observableArrayList(filtered));
    }

    // 添加成绩
    @FXML
    public void onAddButtonClick(ActionEvent event) {
        Dialog<ScoreRecord> dialog = new Dialog<>();
        dialog.setTitle("添加成绩");

        ComboBox<Student> studentBox = new ComboBox<>(allStudents);
        ComboBox<TeachingClass> teachClassBox = new ComboBox<>();
        TextField markField = new TextField();
        markField.setPromptText("成绩");

        studentBox.setOnAction(e -> {
            Student stu = studentBox.getValue();
            if (stu != null) {
                teachClassBox.setItems(FXCollections.observableArrayList(
                        studentTeachClassMap.getOrDefault(stu.getStudentNum(), List.of())));
            }
        });

        VBox content = new VBox(10, new Label("学生:"), studentBox, new Label("教学班:"), teachClassBox, new Label("成绩:"), markField);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && studentBox.getValue() != null && teachClassBox.getValue() != null && !markField.getText().isEmpty()) {
                Student stu = studentBox.getValue();
                TeachingClass tc = teachClassBox.getValue();
                Course c = tc.getCourse();
                return new ScoreRecord(stu.getStudentNum(), stu.getStudentName(), stu.getClassName(),
                        c.getCourseNum(), c.getCourseName(), String.valueOf(c.getCredit()),
                        tc.getTeachClassNum(), tc.getYear(), tc.getTerm(), markField.getText());
            } else return null;
        });

        Optional<ScoreRecord> result = dialog.showAndWait();
        result.ifPresent(record -> {
            allScores.add(record);
            dataTableView.setItems(FXCollections.observableArrayList(allScores));
        });
    }

    // 修改成绩
    @FXML
    public void onEditButtonClick(ActionEvent event) {
        ScoreRecord selected = dataTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        TextInputDialog dialog = new TextInputDialog(selected.getMark());
        dialog.setTitle("修改成绩");
        dialog.setHeaderText("请输入新成绩：");
        Optional<String> newMark = dialog.showAndWait();
        newMark.ifPresent(mark -> {
            selected.setMark(mark);
            dataTableView.refresh();
        });
    }

    // 删除成绩
    @FXML
    public void onDeleteButtonClick(ActionEvent event) {
        ScoreRecord selected = dataTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            allScores.remove(selected);
            dataTableView.setItems(FXCollections.observableArrayList(allScores));
        }
    }

    // mock数据结构体和初始化
    public static class Student {
        private final String studentNum, studentName, className;
        public Student(String sn, String name, String cls) { studentNum = sn; studentName = name; className = cls; }
        public String getStudentNum() { return studentNum; }
        public String getStudentName() { return studentName; }
        public String getClassName() { return className; }
        @Override public String toString() { return studentName + "(" + studentNum + ")"; }
    }
    public static class Course {
        private final String courseNum, courseName;
        private final int credit;
        public Course(String n, String name, int c) { courseNum = n; courseName = name; credit = c; }
        public String getCourseNum() { return courseNum; }
        public String getCourseName() { return courseName; }
        public int getCredit() { return credit; }
        @Override public String toString() { return courseName + "(" + courseNum + ")"; }
    }
    public static class TeachingClass {
        private final String teachClassNum, year, term;
        private final Course course;
        public TeachingClass(String t, String y, String tm, Course c) { teachClassNum = t; year = y; term = tm; course = c; }
        public String getTeachClassNum() { return teachClassNum; }
        public String getYear() { return year; }
        public String getTerm() { return term; }
        public Course getCourse() { return course; }
        @Override public String toString() { return teachClassNum + " " + course.getCourseName() + " " + year + "-" + term; }
    }

    // JavaFX BeanProperty写法（简化，实际开发建议用SimpleStringProperty等）
    public static class ScoreRecord {
        private String studentNum, studentName, className, courseNum, courseName, credit,
                teachClassNum, year, term, mark;
        public ScoreRecord(String sn, String name, String cls, String cnum, String cname, String cr, String tnum, String y, String t, String m) {
            studentNum = sn; studentName = name; className = cls; courseNum = cnum; courseName = cname;
            credit = cr; teachClassNum = tnum; year = y; term = t; mark = m;
        }
        public String getStudentNum() { return studentNum; }
        public String getStudentName() { return studentName; }
        public String getClassName() { return className; }
        public String getCourseNum() { return courseNum; }
        public String getCourseName() { return courseName; }
        public String getCredit() { return credit; }
        public String getTeachClassNum() { return teachClassNum; }
        public String getYear() { return year; }
        public String getTerm() { return term; }
        public String getMark() { return mark; }
        public void setMark(String m) { mark = m; }
        // JavaFX Property方法（如需支持TableView双向绑定可用SimpleStringProperty）
        public javafx.beans.property.SimpleStringProperty studentNameProperty() { return new javafx.beans.property.SimpleStringProperty(studentName); }
        public javafx.beans.property.SimpleStringProperty classNameProperty() { return new javafx.beans.property.SimpleStringProperty(className); }
        public javafx.beans.property.SimpleStringProperty courseNumProperty() { return new javafx.beans.property.SimpleStringProperty(courseNum); }
        public javafx.beans.property.SimpleStringProperty courseNameProperty() { return new javafx.beans.property.SimpleStringProperty(courseName); }
        public javafx.beans.property.SimpleStringProperty creditProperty() { return new javafx.beans.property.SimpleStringProperty(credit); }
        public javafx.beans.property.SimpleStringProperty teachClassNumProperty() { return new javafx.beans.property.SimpleStringProperty(teachClassNum); }
        public javafx.beans.property.SimpleStringProperty yearProperty() { return new javafx.beans.property.SimpleStringProperty(year); }
        public javafx.beans.property.SimpleStringProperty termProperty() { return new javafx.beans.property.SimpleStringProperty(term); }
        public javafx.beans.property.SimpleStringProperty markProperty() { return new javafx.beans.property.SimpleStringProperty(mark); }
    }

    private void initMockData() {
        // 学生
        Student s1 = new Student("2023001", "张三", "软件1班");
        Student s2 = new Student("2023002", "李四", "软件1班");
        Student s3 = new Student("2023003", "王五", "软件2班");
        allStudents.addAll(s1, s2, s3);

        // 课程
        Course c1 = new Course("CS101", "程序设计", 4);
        Course c2 = new Course("MA101", "高等数学", 5);
        Course c3 = new Course("EN101", "大学英语", 3);
        allCourses.addAll(c1, c2, c3);

        // 教学班
        TeachingClass tc1 = new TeachingClass("TC101A", "2023", "1", c1);
        TeachingClass tc2 = new TeachingClass("TC102B", "2023", "2", c2);
        TeachingClass tc3 = new TeachingClass("TC103C", "2024", "1", c1); // 同一课程不同学期

        // 学生对应教学班
        studentTeachClassMap.put(s1.getStudentNum(), Arrays.asList(tc1, tc2));
        studentTeachClassMap.put(s2.getStudentNum(), Arrays.asList(tc1, tc3));
        studentTeachClassMap.put(s3.getStudentNum(), List.of(tc2));

        // mock成绩
        allScores.add(new ScoreRecord(s1.getStudentNum(), s1.getStudentName(), s1.getClassName(),
                c1.getCourseNum(), c1.getCourseName(), String.valueOf(c1.getCredit()),
                tc1.getTeachClassNum(), tc1.getYear(), tc1.getTerm(), "90"));
        allScores.add(new ScoreRecord(s2.getStudentNum(), s2.getStudentName(), s2.getClassName(),
                c1.getCourseNum(), c1.getCourseName(), String.valueOf(c1.getCredit()),
                tc3.getTeachClassNum(), tc3.getYear(), tc3.getTerm(), "82"));
        allScores.add(new ScoreRecord(s1.getStudentNum(), s1.getStudentName(), s1.getClassName(),
                c2.getCourseNum(), c2.getCourseName(), String.valueOf(c2.getCredit()),
                tc2.getTeachClassNum(), tc2.getYear(), tc2.getTerm(), "77"));
        allScores.add(new ScoreRecord(s3.getStudentNum(), s3.getStudentName(), s3.getClassName(),
                c2.getCourseNum(), c2.getCourseName(), String.valueOf(c2.getCredit()),
                tc2.getTeachClassNum(), tc2.getYear(), tc2.getTerm(), "92"));
    }
}