package com.teach.javafx.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教学班管理页面控制器
 * 支持课程/老师搜索选择，增删查改
 * 老师选择为“可搜索-可多选后锁定-可移除”双列表模式
 */
public class TeachingClassManageController {
    @FXML private ComboBox<Course> courseComboBox;
    @FXML private TextField yearField;
    @FXML private ComboBox<String> termComboBox;
    @FXML private TableView<TeachingClassRecord> teachingClassTableView;
    @FXML private TableColumn<TeachingClassRecord, String> idColumn, courseNameColumn, courseNumColumn, yearColumn, termColumn, teachClassNumColumn, teachersColumn, classTimeColumn, classLocationColumn, actionColumn;

    private ObservableList<TeachingClassRecord> allTeachingClasses = FXCollections.observableArrayList();
    private ObservableList<Course> allCourses = FXCollections.observableArrayList();
    private ObservableList<Teacher> allTeachers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        initMockData();

        termComboBox.setItems(FXCollections.observableArrayList("1", "2"));
        courseComboBox.setItems(allCourses);

        idColumn.setCellValueFactory(cell -> cell.getValue().idProperty());
        courseNameColumn.setCellValueFactory(cell -> cell.getValue().courseNameProperty());
        courseNumColumn.setCellValueFactory(cell -> cell.getValue().courseNumProperty());
        yearColumn.setCellValueFactory(cell -> cell.getValue().yearProperty());
        termColumn.setCellValueFactory(cell -> cell.getValue().termProperty());
        teachClassNumColumn.setCellValueFactory(cell -> cell.getValue().teachClassNumProperty());
        teachersColumn.setCellValueFactory(cell -> cell.getValue().teachersProperty());
        classTimeColumn.setCellValueFactory(cell -> cell.getValue().classTimeProperty());
        classLocationColumn.setCellValueFactory(cell -> cell.getValue().classLocationProperty());
        actionColumn.setCellFactory(col -> new TableCell<>() {
            final Button editBtn = new Button("编辑");
            final Button delBtn = new Button("删除");
            {
                editBtn.setOnAction(e -> onEditButtonClick(null));
                delBtn.setOnAction(e -> onDeleteButtonClick(null));
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, editBtn, delBtn));
            }
        });

        teachingClassTableView.setItems(allTeachingClasses);
    }

    @FXML
    public void onQueryButtonClick(ActionEvent event) {
        Course course = courseComboBox.getValue();
        String year = yearField.getText().trim();
        String term = termComboBox.getValue();

        List<TeachingClassRecord> filtered = allTeachingClasses.stream().filter(t ->
                (course == null || t.getCourseNum().equals(course.getCourseNum())) &&
                        (year.isEmpty() || t.getYear().equals(year)) &&
                        (term == null || t.getTerm().equals(term))
        ).collect(Collectors.toList());
        teachingClassTableView.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    public void onAddButtonClick(ActionEvent event) {
        showTeachingClassDialog(null);
    }

    @FXML
    public void onEditButtonClick(ActionEvent event) {
        TeachingClassRecord selected = teachingClassTableView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showTeachingClassDialog(selected);
    }

    private void showTeachingClassDialog(TeachingClassRecord editRecord) {
        Dialog<TeachingClassRecord> dialog = new Dialog<>();
        dialog.setTitle(editRecord == null ? "添加教学班" : "编辑教学班");

        // ----课程搜索选择----
        TextField courseSearchField = new TextField();
        courseSearchField.setPromptText("课程名/号搜索");
        ComboBox<Course> courseBox = new ComboBox<>(allCourses);
        courseBox.setEditable(false);
        courseBox.setPrefWidth(200);
        courseSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            List<Course> result = allCourses.stream().filter(
                    c -> c.getCourseName().contains(newVal) || c.getCourseNum().contains(newVal)
            ).collect(Collectors.toList());
            courseBox.setItems(FXCollections.observableArrayList(result));
        });
        if (editRecord != null) {
            courseBox.setValue(allCourses.stream()
                    .filter(c -> c.getCourseNum().equals(editRecord.getCourseNum()))
                    .findFirst().orElse(null));
        }

        // ----老师搜索选择，双列表锁定模式----
        TextField teacherSearchField = new TextField();
        teacherSearchField.setPromptText("教师姓名搜索");
        ObservableList<Teacher> filteredTeachers = FXCollections.observableArrayList(allTeachers);
        ListView<Teacher> allTeacherListView = new ListView<>(filteredTeachers);
        allTeacherListView.setPrefHeight(110);

        ObservableList<Teacher> selectedTeachers = FXCollections.observableArrayList();
        ListView<Teacher> selectedTeacherListView = new ListView<>(selectedTeachers);
        selectedTeacherListView.setPrefHeight(110);

        // 初始化编辑时已选老师
        if (editRecord != null) {
            Set<String> selectedNames = new HashSet<>(Arrays.asList(editRecord.getTeachers().split(",")));
            selectedTeachers.addAll(
                    allTeachers.stream()
                            .filter(t -> selectedNames.contains(t.getName()))
                            .collect(Collectors.toList())
            );
        }

        // 搜索过滤全部老师
        teacherSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            List<Teacher> filtered = allTeachers.stream()
                    .filter(t -> t.getName().contains(newVal) && !selectedTeachers.contains(t))
                    .collect(Collectors.toList());
            filteredTeachers.setAll(filtered);
        });
        // 初次过滤
        teacherSearchField.setText("");

        // “添加到已选”
        Button btnAddTeacher = new Button("→");
        btnAddTeacher.setOnAction(e -> {
            Teacher t = allTeacherListView.getSelectionModel().getSelectedItem();
            if (t != null && !selectedTeachers.contains(t)) {
                selectedTeachers.add(t);
                filteredTeachers.remove(t);
            }
        });

        // “移除已选”
        Button btnRemoveTeacher = new Button("←");
        btnRemoveTeacher.setOnAction(e -> {
            Teacher t = selectedTeacherListView.getSelectionModel().getSelectedItem();
            if (t != null) {
                selectedTeachers.remove(t);
                // 若当前搜索结果包含该老师则添加回可选
                String filter = teacherSearchField.getText();
                if (t.getName().contains(filter)) filteredTeachers.add(t);
            }
        });

        VBox allBox = new VBox(5, new Label("可选老师(搜索):"), teacherSearchField, allTeacherListView);
        VBox btnBox = new VBox(10, btnAddTeacher, btnRemoveTeacher);
        btnBox.setPrefWidth(40);
        VBox selBox = new VBox(5, new Label("已选老师:"), selectedTeacherListView);

        HBox teacherChooser = new HBox(8, allBox, btnBox, selBox);
        teacherChooser.setPrefHeight(130);

        // ----其他信息----
        TextField yearField = new TextField(editRecord == null ? "" : editRecord.getYear());
        ComboBox<String> termBox = new ComboBox<>(FXCollections.observableArrayList("1", "2"));
        if (editRecord != null) termBox.setValue(editRecord.getTerm());
        TextField teachClassNumField = new TextField(editRecord == null ? "" : editRecord.getTeachClassNum());
        teachClassNumField.setPromptText("班级号码");
        TextField classTimeField = new TextField(editRecord == null ? "" : editRecord.getClassTime());
        classTimeField.setPromptText("上课时间");
        TextField classLocationField = new TextField(editRecord == null ? "" : editRecord.getClassLocation());
        classLocationField.setPromptText("上课地点");

        VBox leftBox = new VBox(8,
                new Label("课程:"), courseSearchField, courseBox,
                new Label("负责老师:"), teacherChooser
        );

        VBox rightBox = new VBox(8,
                new Label("学年:"), yearField,
                new Label("学期:"), termBox,
                new Label("班级号码:"), teachClassNumField,
                new Label("上课时间:"), classTimeField,
                new Label("上课地点:"), classLocationField
        );

        HBox content = new HBox(20, leftBox, rightBox);
        content.setPadding(new Insets(10, 10, 10, 10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && courseBox.getValue() != null &&
                    !yearField.getText().isEmpty() && termBox.getValue() != null &&
                    !teachClassNumField.getText().isEmpty() &&
                    !selectedTeachers.isEmpty() &&
                    !classTimeField.getText().isEmpty() && !classLocationField.getText().isEmpty()) {
                Course course = courseBox.getValue();
                String teachers = selectedTeachers.stream()
                        .map(Teacher::getName).collect(Collectors.joining(","));
                return new TeachingClassRecord(
                        editRecord == null ? UUID.randomUUID().toString().substring(0, 8) : editRecord.getId(),
                        course.getCourseName(),
                        course.getCourseNum(),
                        yearField.getText().trim(),
                        termBox.getValue(),
                        teachClassNumField.getText().trim(),
                        teachers,
                        classTimeField.getText().trim(),
                        classLocationField.getText().trim()
                );
            }
            return null;
        });

        Optional<TeachingClassRecord> result = dialog.showAndWait();
        result.ifPresent(record -> {
            if (editRecord == null) {
                allTeachingClasses.add(record);
            } else {
                int idx = allTeachingClasses.indexOf(editRecord);
                allTeachingClasses.set(idx, record);
            }
            teachingClassTableView.setItems(FXCollections.observableArrayList(allTeachingClasses));
        });
    }

    @FXML
    public void onDeleteButtonClick(ActionEvent event) {
        TeachingClassRecord selected = teachingClassTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            allTeachingClasses.remove(selected);
            teachingClassTableView.setItems(FXCollections.observableArrayList(allTeachingClasses));
        }
    }

    // ==== 数据结构 ====
    public static class Course {
        private final String courseNum, courseName;
        public Course(String num, String name) { courseNum = num; courseName = name; }
        public String getCourseNum() { return courseNum; }
        public String getCourseName() { return courseName; }
        @Override public String toString() { return courseName + "(" + courseNum + ")"; }
    }

    public static class Teacher {
        private final String id, name;
        public Teacher(String id, String name) { this.id = id; this.name = name; }
        public String getId() { return id; }
        public String getName() { return name; }
        @Override public String toString() { return name; }
    }

    public static class TeachingClassRecord {
        private final SimpleStringProperty id, courseName, courseNum, year, term, teachClassNum, teachers, classTime, classLocation;
        public TeachingClassRecord(String id, String courseName, String courseNum, String year, String term, String teachClassNum, String teachers, String classTime, String classLocation) {
            this.id = new SimpleStringProperty(id);
            this.courseName = new SimpleStringProperty(courseName);
            this.courseNum = new SimpleStringProperty(courseNum);
            this.year = new SimpleStringProperty(year);
            this.term = new SimpleStringProperty(term);
            this.teachClassNum = new SimpleStringProperty(teachClassNum);
            this.teachers = new SimpleStringProperty(teachers);
            this.classTime = new SimpleStringProperty(classTime);
            this.classLocation = new SimpleStringProperty(classLocation);
        }
        public String getId() { return id.get(); }
        public String getCourseName() { return courseName.get(); }
        public String getCourseNum() { return courseNum.get(); }
        public String getYear() { return year.get(); }
        public String getTerm() { return term.get(); }
        public String getTeachClassNum() { return teachClassNum.get(); }
        public String getTeachers() { return teachers.get(); }
        public String getClassTime() { return classTime.get(); }
        public String getClassLocation() { return classLocation.get(); }

        public SimpleStringProperty idProperty() { return id; }
        public SimpleStringProperty courseNameProperty() { return courseName; }
        public SimpleStringProperty courseNumProperty() { return courseNum; }
        public SimpleStringProperty yearProperty() { return year; }
        public SimpleStringProperty termProperty() { return term; }
        public SimpleStringProperty teachClassNumProperty() { return teachClassNum; }
        public SimpleStringProperty teachersProperty() { return teachers; }
        public SimpleStringProperty classTimeProperty() { return classTime; }
        public SimpleStringProperty classLocationProperty() { return classLocation; }
    }

    // ==== mock数据 ====
    private void initMockData() {
        // 课程
        Course c1 = new Course("CS101", "程序设计");
        Course c2 = new Course("MA101", "高等数学");
        Course c3 = new Course("EN101", "大学英语");
        allCourses.addAll(c1, c2, c3);

        // 教师
        Teacher t1 = new Teacher("T001", "王老师");
        Teacher t2 = new Teacher("T002", "李老师");
        Teacher t3 = new Teacher("T003", "赵老师");
        Teacher t4 = new Teacher("T004", "刘老师");
        allTeachers.addAll(t1, t2, t3, t4);

        // 教学班
        allTeachingClasses.add(new TeachingClassRecord(
                "1", c1.getCourseName(), c1.getCourseNum(), "2024", "1", "101A", "王老师,赵老师", "周一 8:00-10:00", "A101"));
        allTeachingClasses.add(new TeachingClassRecord(
                "2", c2.getCourseName(), c2.getCourseNum(), "2024", "2", "102B", "李老师", "周三 10:00-12:00", "B201"));
        allTeachingClasses.add(new TeachingClassRecord(
                "3", c1.getCourseName(), c1.getCourseNum(), "2023", "2", "103C", "孙老师", "周四 14:00-16:00", "A102"));
        allTeachingClasses.add(new TeachingClassRecord(
                "4", c3.getCourseName(), c3.getCourseNum(), "2024", "1", "201A", "刘老师,陈老师", "周五 8:00-10:00", "C301"));
    }
}