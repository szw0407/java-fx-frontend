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
//        initMockData();

        termComboBox.setItems(FXCollections.observableArrayList("1", "2"));
        var cal = Calendar.getInstance();
        // set year
        yearField.setText(String.valueOf(cal.get(Calendar.YEAR)));
        // set term
        switch (cal.get(Calendar.MONTH)) {
            case Calendar.JULY, Calendar.FEBRUARY, Calendar.MARCH, Calendar.APRIL, Calendar.MAY, Calendar.JUNE -> {
                termComboBox.setValue("2");
            }
            case Calendar.JANUARY, Calendar.AUGUST, Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER -> {
                termComboBox.setValue("1");
            }

        }
        initData();
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
        // reload data all

        String year = yearField.getText().trim();
        String term = termComboBox.getValue();
        Student stu = studentComboBox.getValue();
        Course course = courseComboBox.getValue();
        initData();
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

        VBox content = new VBox(10,
                new Label("学生:"),
                stuField, stuBox,
                new Label("课程:"),
                courseField, courseBox,
                new Label("教学班:"),
                teachClassBox
        );
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                Student selectedStudent = stuBox.getValue();
                Course selectedCourse = courseBox.getValue();
                TeachingClass selectedClass = teachClassBox.getValue();
                if (selectedStudent != null && selectedCourse != null && selectedClass != null) {
                    // try to connect server
                    var dr = new DataRequest();
                    dr.add("studentNum", selectedStudent.getStudentNum());
                    dr.add("courseNum", selectedCourse.getCourseNum());
                    dr.add("classNum", selectedClass.getTeachClassNum());
                    dr.add("year", yearField.getText().trim());
                    dr.add("semester", termComboBox.getValue());
                    var a = HttpRequestUtil.request("/api/courseSelection/selectCourseForStudent", dr);
                    if (a == null || a.getCode() != 0) {
                        // show error message
                        Alert alert = null;
                        if (a != null) {
                            alert = new Alert(Alert.AlertType.ERROR, "添加选课记录失败，请稍后重试。" + a.getMsg() + "<UNK>", ButtonType.OK);
                        } else {
                            alert = new Alert(Alert.AlertType.ERROR, "添加选课记录失败，请稍后重试。", ButtonType.OK);
                        }
                        alert.showAndWait();
                        return null;
                    }
                    return new CourseSelectionRecord(
                            selectedStudent.getStudentName(),
                            selectedStudent.getStudentNum(),
                            selectedCourse.getCourseName(),
                            selectedCourse.getCourseNum(),
                            selectedClass.getTeachClassNum(),
                            String.join(",", selectedClass.getTeachers()),
                            selectedClass.getClassTime(),
                            selectedClass.getClassLocation(),
                            yearField.getText().trim(),
                            termComboBox.getValue()
                    );
                }
            }
            return null;
        });

        dialog.showAndWait();

        // 如果添加成功，更新表格
        Optional<CourseSelectionRecord> result = Optional.ofNullable(dialog.getResult());
        result.ifPresent(record -> {
            allSelections.add(record);
            selectionTableView.setItems(FXCollections.observableArrayList(allSelections));
        });
    }

    @FXML
    public void onDeleteButtonClick(ActionEvent event) {
        CourseSelectionRecord selected = selectionTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // try to connect server to delete
            var dr = new DataRequest();
            dr.add("studentNum", selected.getStudentNum());
            dr.add("courseNum", selected.getCourseNum());
            dr.add("classNum", selected.getTeachClassNum());
            dr.add("year", selected.getYear());
            dr.add("semester", selected.getTerm());
            var response = HttpRequestUtil.request("/api/courseSelection/dropCourseForStudent", dr);
            if (response == null || response.getCode() != 0) {
                // show error message
                Alert alert = null;
                if (response != null) {
                    alert = new Alert(Alert.AlertType.ERROR, "删除选课记录失败，请稍后重试。" + response.getMsg() + "<UNK>", ButtonType.OK);
                } else {
                    alert = new Alert(Alert.AlertType.ERROR, "删除选课记录失败，请稍后重试。", ButtonType.OK);
                }
                alert.showAndWait();

                return;
            }
            allSelections.remove(selected);
            selectionTableView.setItems(FXCollections.observableArrayList(allSelections));
        } else {
            // show warning message
            Alert alert = new Alert(Alert.AlertType.WARNING, "请先选择要删除的选课记录。", ButtonType.OK);
            alert.showAndWait();
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

    private void initData() {

        // real data
        // students list
        var dr = new DataRequest();
        allStudents.clear();
        dr.add("numName", ""); // empty means all students
        var s = HttpRequestUtil.request("/api/student/getStudentList", dr);
        if (s != null && s.getCode() == 0) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) s.getData();

            for (Map<String, Object> m : dataList) {
                String num = (String) m.get("num");
                String name = (String) m.get("name");
                allStudents.add(new Student(num, name));
            }
        }

        // courses list
//        dr = new DataRequest();
        allCourses.clear();
        var cl = HttpRequestUtil.request("/api/course/getCourseList", dr);
        if (cl != null && cl.getCode() == 0) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) cl.getData();

            for (Map<String, Object> m : dataList) {
                String num = (String) m.get("num");
                String name = (String) m.get("name");
                allCourses.add(new Course(num, name));

            }
        }
        dr = new DataRequest();
        dr.add("semester", termComboBox.getValue());
        dr.add("year", yearField.getText());
        courseTeachClassMap.clear();
        var c = HttpRequestUtil.request("/api/courseSelection/getAvailableCoursesAll", dr);
        if (c != null && c.getCode() == 0) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) c.getData();
//
            allSelections.clear();
            for (Map<String, Object> m : dataList) {
                String courseNum = (String) m.get("courseNum");
                String courseName = (String) m.get("courseName");
                String teachClassNum = (String) m.get("classNumber");
                String classTime = (String) m.get("classTime");
                String classLocation = (String) m.get("classLocation");
//                String teachers = (String) m.get("teachers");
                String year = (String) m.get("year");
                String term = (String) m.get("semester");
                List<String> teachers = (List<String>) m.get("teachers");
                Course course = new Course(courseNum, courseName);
                TeachingClass tc = new TeachingClass(teachClassNum, course,
                        teachers, classTime, classLocation);
                StringBuilder teacher_csv = new StringBuilder();
                for (var teacher:teachers) {
                    if (!teacher_csv.isEmpty()) {
                        teacher_csv.append(",");
                    }
                    teacher_csv.append(teacher);
                }
                allSelections.add(new CourseSelectionRecord(
                        "", "", courseName, courseNum, teachClassNum, teacher_csv.toString()
                        , classTime, classLocation, year, term
                ));

                courseTeachClassMap.computeIfAbsent(courseNum, k -> new ArrayList<>()).add(tc);
            }
        }

        // course selection records
        allSelections.clear();
        var sc = HttpRequestUtil.request("/api/courseSelection/getSelectedCoursesAll", dr);
        if (sc != null && sc.getCode() == 0) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) sc.getData();

            for (Map<String, Object> m : dataList) {
                String studentName = (String) m.get("studentName");
                String studentNum = (String) m.get("studentNum");
                String courseName = (String) m.get("courseName");
                String courseNum = (String) m.get("courseNum");
                String teachClassNum = (String) m.get("classNumber");
                String classTime = (String) m.get("classTime");
                String classLocation = (String) m.get("classLocation");
                String year = (String) m.get("year");
                String term = (String) m.get("semester");
                String teachers = ((List<String>) m.get("teachers")).stream()
                        .collect(Collectors.joining(","));
                allSelections.add(new CourseSelectionRecord(
                        studentName, studentNum, courseName, courseNum, teachClassNum,
                        teachers, classTime, classLocation, year, term
                ));
            }
        }


    }
}