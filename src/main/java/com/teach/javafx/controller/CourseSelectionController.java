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
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 选课管理页面控制器
 * 支持学年、学期、学生、课程多条件筛选
 * 支持添加/删除选课记录
 * mock数据可用于替换为真实API
 */
public class CourseSelectionController {
    @FXML private TextField yearField;
    @FXML private ComboBox<String> termComboBox;
    @FXML private ComboBox<Student> studentComboBox;
    @FXML private ComboBox<Course> courseComboBox;
    @FXML private TableView<CourseSelectionRecord> selectionTableView;
    @FXML private TableColumn<CourseSelectionRecord, String> studentNameColumn, studentNumColumn,
            courseNameColumn, courseNumColumn, teachClassNumColumn, teachersColumn, classTimeColumn, classLocationColumn, deleteColumn;

    private ObservableList<CourseSelectionRecord> allSelections = FXCollections.observableArrayList();
    private ObservableList<Student> allStudents = FXCollections.observableArrayList();
    private ObservableList<Course> allCourses = FXCollections.observableArrayList();
    private Map<String, List<TeachingClass>> courseTeachClassMap = new HashMap<>(); // courseNum -> teachingClasses

    @FXML
    public void initialize() {
        initMockData();

        termComboBox.setItems(FXCollections.observableArrayList("1", "2"));

        studentComboBox.setItems(allStudents);
        courseComboBox.setItems(allCourses);

        studentNameColumn.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
        studentNumColumn.setCellValueFactory(cell -> cell.getValue().studentNumProperty());
        courseNameColumn.setCellValueFactory(cell -> cell.getValue().courseNameProperty());
        courseNumColumn.setCellValueFactory(cell -> cell.getValue().courseNumProperty());
        teachClassNumColumn.setCellValueFactory(cell -> cell.getValue().teachClassNumProperty());
        teachersColumn.setCellValueFactory(cell -> cell.getValue().teachersProperty());
        classTimeColumn.setCellValueFactory(cell -> cell.getValue().classTimeProperty());
        classLocationColumn.setCellValueFactory(cell -> cell.getValue().classLocationProperty());
        deleteColumn.setCellFactory(col -> new TableCell<>() {
            final Button delBtn = new Button("删除");
            {
                delBtn.setOnAction(e -> onDeleteButtonClick(null));
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });

        selectionTableView.setItems(allSelections);
    }

    @FXML
    public void onQueryButtonClick(ActionEvent event) {
        String year = yearField.getText().trim();
        String term = termComboBox.getValue();
        Student stu = studentComboBox.getValue();
        Course course = courseComboBox.getValue();

        List<CourseSelectionRecord> filtered = allSelections.stream().filter(r ->
                (year.isEmpty() || r.getYear().equals(year)) &&
                        (term == null || r.getTerm().equals(term)) &&
                        (stu == null || r.getStudentNum().equals(stu.getStudentNum())) &&
                        (course == null || r.getCourseNum().equals(course.getCourseNum()))
        ).collect(Collectors.toList());
        selectionTableView.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void onAddButtonClick(ActionEvent event) {
        Dialog<CourseSelectionRecord> dialog = new Dialog<>();
        dialog.setTitle("添加选课记录");

        // 搜索学生
        TextField stuField = new TextField();
        ComboBox<Student> stuBox = new ComboBox<>(allStudents);
        stuBox.setEditable(false);
        stuField.setPromptText("输入学生姓名或学号搜索");
        stuField.textProperty().addListener((obs, oldVal, newVal) -> {
            List<Student> result = allStudents.stream().filter(
                    s -> s.getStudentName().contains(newVal) || s.getStudentNum().contains(newVal)
            ).collect(Collectors.toList());
            stuBox.setItems(FXCollections.observableArrayList(result));
        });

        // 搜索课程及选择教学班
        TextField courseField = new TextField();
        ComboBox<Course> courseBox = new ComboBox<>(allCourses);
        ComboBox<TeachingClass> teachClassBox = new ComboBox<>();
        courseField.setPromptText("输入课程名或课程号搜索");
        courseField.textProperty().addListener((obs, oldVal, newVal) -> {
            List<Course> result = allCourses.stream().filter(
                    c -> c.getCourseName().contains(newVal) || c.getCourseNum().contains(newVal)
            ).collect(Collectors.toList());
            courseBox.setItems(FXCollections.observableArrayList(result));
        });

        courseBox.setOnAction(e -> {
            Course c = courseBox.getValue();
            if (c != null) {
                teachClassBox.setItems(FXCollections.observableArrayList(
                        courseTeachClassMap.getOrDefault(c.getCourseNum(), List.of())
                ));
            }
        });

        ComboBox<String> yearBox = new ComboBox<>(FXCollections.observableArrayList("2023", "2024"));
        ComboBox<String> termBox = new ComboBox<>(FXCollections.observableArrayList("1", "2"));

        VBox content = new VBox(10,
                new Label("学生:"),
                stuField, stuBox,
                new Label("课程:"),
                courseField, courseBox,
                new Label("教学班:"),
                teachClassBox,
                new Label("学年:"), yearBox,
                new Label("学期:"), termBox
        );
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && stuBox.getValue() != null && teachClassBox.getValue() != null &&
                    yearBox.getValue() != null && termBox.getValue() != null) {
                Student stu = stuBox.getValue();
                TeachingClass tc = teachClassBox.getValue();
                Course c = tc.getCourse();
                return new CourseSelectionRecord(
                        stu.getStudentName(), stu.getStudentNum(),
                        c.getCourseName(), c.getCourseNum(),
                        tc.getTeachClassNum(),
                        String.join(",", tc.getTeachers()),
                        tc.getClassTime(), tc.getClassLocation(),
                        yearBox.getValue(), termBox.getValue()
                );
            }
            return null;
        });

        Optional<CourseSelectionRecord> result = dialog.showAndWait();
        result.ifPresent(record -> {
            allSelections.add(record);
            selectionTableView.setItems(FXCollections.observableArrayList(allSelections));
        });
    }

    @FXML
    public void onDeleteButtonClick(ActionEvent event) {
        CourseSelectionRecord selected = selectionTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            allSelections.remove(selected);
            selectionTableView.setItems(FXCollections.observableArrayList(allSelections));
        }
    }

    // ==== 数据结构 ====

    public static class Student {
        private final String studentNum, studentName;
        public Student(String num, String name) { studentNum = num; studentName = name; }
        public String getStudentNum() { return studentNum; }
        public String getStudentName() { return studentName; }
        @Override public String toString() { return studentName + "(" + studentNum + ")"; }
    }

    public static class Course {
        private final String courseNum, courseName;
        public Course(String num, String name) { courseNum = num; courseName = name; }
        public String getCourseNum() { return courseNum; }
        public String getCourseName() { return courseName; }
        @Override public String toString() { return courseName + "(" + courseNum + ")"; }
    }

    public static class TeachingClass {
        private final String teachClassNum;
        private final Course course;
        private final List<String> teachers;
        private final String classTime, classLocation;
        public TeachingClass(String num, Course course, List<String> teachers, String time, String location) {
            this.teachClassNum = num; this.course = course; this.teachers = teachers;
            this.classTime = time; this.classLocation = location;
        }
        public String getTeachClassNum() { return teachClassNum; }
        public Course getCourse() { return course; }
        public List<String> getTeachers() { return teachers; }
        public String getClassTime() { return classTime; }
        public String getClassLocation() { return classLocation; }
        @Override public String toString() { return teachClassNum + " - " + course.getCourseName(); }
    }

    public static class CourseSelectionRecord {
        private final SimpleStringProperty studentName, studentNum, courseName, courseNum,
                teachClassNum, teachers, classTime, classLocation, year, term;
        public CourseSelectionRecord(String sName, String sNum, String cName, String cNum, String tcNum,
                                     String teachers, String time, String location, String year, String term) {
            this.studentName = new SimpleStringProperty(sName);
            this.studentNum = new SimpleStringProperty(sNum);
            this.courseName = new SimpleStringProperty(cName);
            this.courseNum = new SimpleStringProperty(cNum);
            this.teachClassNum = new SimpleStringProperty(tcNum);
            this.teachers = new SimpleStringProperty(teachers);
            this.classTime = new SimpleStringProperty(time);
            this.classLocation = new SimpleStringProperty(location);
            this.year = new SimpleStringProperty(year);
            this.term = new SimpleStringProperty(term);
        }
        public String getStudentName() { return studentName.get(); }
        public String getStudentNum() { return studentNum.get(); }
        public String getCourseName() { return courseName.get(); }
        public String getCourseNum() { return courseNum.get(); }
        public String getTeachClassNum() { return teachClassNum.get(); }
        public String getTeachers() { return teachers.get(); }
        public String getClassTime() { return classTime.get(); }
        public String getClassLocation() { return classLocation.get(); }
        public String getYear() { return year.get(); }
        public String getTerm() { return term.get(); }
        // property
        public SimpleStringProperty studentNameProperty() { return studentName; }
        public SimpleStringProperty studentNumProperty() { return studentNum; }
        public SimpleStringProperty courseNameProperty() { return courseName; }
        public SimpleStringProperty courseNumProperty() { return courseNum; }
        public SimpleStringProperty teachClassNumProperty() { return teachClassNum; }
        public SimpleStringProperty teachersProperty() { return teachers; }
        public SimpleStringProperty classTimeProperty() { return classTime; }
        public SimpleStringProperty classLocationProperty() { return classLocation; }
    }

    // ==== mock数据 ====
    private void initMockData() {
        // 学生
        Student s1 = new Student("2023001", "张三");
        Student s2 = new Student("2023002", "李四");
        Student s3 = new Student("2023003", "王五");
        allStudents.addAll(s1, s2, s3);

        // 课程
        Course c1 = new Course("CS101", "程序设计");
        Course c2 = new Course("MA101", "高等数学");
        Course c3 = new Course("EN101", "大学英语");
        allCourses.addAll(c1, c2, c3);

        // 教学班
        TeachingClass t1 = new TeachingClass("T101A", c1, Arrays.asList("王老师", "赵老师"), "周一 8:00-10:00", "A101");
        TeachingClass t2 = new TeachingClass("T102B", c2, List.of("李老师"), "周三 10:00-12:00", "B201");
        TeachingClass t3 = new TeachingClass("T103C", c1, List.of("孙老师"), "周四 14:00-16:00", "A102");
        TeachingClass t4 = new TeachingClass("T201A", c3, Arrays.asList("刘老师", "陈老师"), "周五 8:00-10:00", "C301");

        courseTeachClassMap.put(c1.getCourseNum(), Arrays.asList(t1, t3));
        courseTeachClassMap.put(c2.getCourseNum(), List.of(t2));
        courseTeachClassMap.put(c3.getCourseNum(), List.of(t4));

        // 选课记录
        allSelections.add(new CourseSelectionRecord(
                s1.getStudentName(), s1.getStudentNum(),
                c1.getCourseName(), c1.getCourseNum(), t1.getTeachClassNum(),
                String.join(",", t1.getTeachers()), t1.getClassTime(), t1.getClassLocation(),
                "2024", "1"
        ));
        allSelections.add(new CourseSelectionRecord(
                s2.getStudentName(), s2.getStudentNum(),
                c2.getCourseName(), c2.getCourseNum(), t2.getTeachClassNum(),
                String.join(",", t2.getTeachers()), t2.getClassTime(), t2.getClassLocation(),
                "2024", "1"
        ));
        allSelections.add(new CourseSelectionRecord(
                s3.getStudentName(), s3.getStudentNum(),
                c1.getCourseName(), c1.getCourseNum(), t3.getTeachClassNum(),
                String.join(",", t3.getTeachers()), t3.getClassTime(), t3.getClassLocation(),
                "2023", "2"
        ));
    }
    private void initData() {
        // real data
        // students list
        var dr = new DataRequest();
        var s = HttpRequestUtil.request("/api/student/getStudentList", dr);
        if (s != null && s.getCode() == 200) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) s.getData();
            allStudents.clear();
            for (Map<String, Object> m : dataList) {
                String num = (String) m.get("num");
                String name = (String) m.get("name");
                allStudents.add(new Student(num, name));
            }
        }

        // courses list
        dr = new DataRequest();
        var cl = HttpRequestUtil.

    }
}