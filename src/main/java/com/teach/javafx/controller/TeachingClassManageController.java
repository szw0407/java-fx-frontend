package com.teach.javafx.controller;

import com.teach.javafx.request.*;
import com.teach.javafx.util.DateTimeTool;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;


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
    private ObservableList<Teacher> allTeachers = FXCollections.observableArrayList();    @FXML
    public void initialize() {


        yearField.setPromptText("学年");
        termComboBox.setPromptText("学期");
        // decide the current semester
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH is zero-based
        if (month >= 2 && month <= 7) {
            termComboBox.setValue("2");
        } else {
            termComboBox.setValue("1");
        }
        yearField.setText(
                calendar.get(Calendar.YEAR) + ""
        );
        loadDataFromAPIs();
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
    }    @FXML
    public void onQueryButtonClick(ActionEvent event) {
        // refresh all
        loadTeachingClasses();
        // 可以改为使用后端API进行查询，当前保持前端过滤
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
        // refresh teacher list
        loadTeachers();
        // refresh course list
        loadCourses();
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
        teachClassNumField.setPromptText("班级号码");        // ----上课时间选择网格----
        String[] weekdays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        String[] timeSlots = {"8:00-10:00", "10:00-12:00", "14:00-16:00", "16:00-18:00", "19:00-21:00"};
        
        GridPane timeGrid = new GridPane();
        timeGrid.setHgap(5);
        timeGrid.setVgap(5);
        timeGrid.setPadding(new Insets(5));
        
        // 添加表头
        for (int j = 0; j < timeSlots.length; j++) {
            Label header = new Label(timeSlots[j]);
            header.setStyle("-fx-font-weight: bold;");
            timeGrid.add(header, j + 1, 0);
        }
        
        // 创建复选框矩阵
        CheckBox[][] timeCheckBoxes = new CheckBox[weekdays.length][timeSlots.length];
        for (int i = 0; i < weekdays.length; i++) {
            Label dayLabel = new Label(weekdays[i]);
            dayLabel.setStyle("-fx-font-weight: bold;");
            timeGrid.add(dayLabel, 0, i + 1);
            
            for (int j = 0; j < timeSlots.length; j++) {
                CheckBox cb = new CheckBox();
                timeCheckBoxes[i][j] = cb;
                timeGrid.add(cb, j + 1, i + 1);
            }
        }
        
        // 如果是编辑模式，解析已有的上课时间并设置复选框
        if (editRecord != null) {
            parseClassTimeToCheckBoxes(editRecord.getClassTime(), timeCheckBoxes, weekdays, timeSlots);
            // 解码当前的课程信息
            courseBox.setValue(allCourses.stream()
                    .filter(c -> c.getCourseNum().equals(editRecord.getCourseNum()))
                    .findFirst().orElse(null));
        }

        
        ScrollPane timeScrollPane = new ScrollPane(timeGrid);
        timeScrollPane.setPrefHeight(180);
        timeScrollPane.setFitToWidth(true);
        
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
                new Label("上课时间:"), timeScrollPane,
                new Label("上课地点:"), classLocationField
        );
        // set default values
        Calendar cal = Calendar.getInstance();
        yearField.setText(cal.get(Calendar.YEAR) + "");
        termBox.setValue(cal.get(Calendar.MONTH) < 7 && cal.get(Calendar.MONTH) > 1 ? "2" : "1");
        HBox content = new HBox(20, leftBox, rightBox);
        content.setPadding(new Insets(10, 10, 10, 10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && courseBox.getValue() != null &&
                    !yearField.getText().isEmpty() && termBox.getValue() != null &&
                    !teachClassNumField.getText().isEmpty()) {
                Course course = courseBox.getValue();
                String teachers = selectedTeachers.stream()
                        .map(Teacher::getName).collect(Collectors.joining(","));
                String classTime = convertCheckBoxesToClassTime(timeCheckBoxes, weekdays, timeSlots);
                return new TeachingClassRecord(
                        editRecord == null ? UUID.randomUUID().toString().substring(0, 8) : editRecord.getId(),
                        course.getCourseName(),
                        course.getCourseNum(),
                        yearField.getText().trim(),
                        termBox.getValue(),
                        teachClassNumField.getText().trim(),
                        teachers,
                        classTime,
                        classLocationField.getText().trim()
                );
            }
            return null;
        });        Optional<TeachingClassRecord> result = dialog.showAndWait();
        result.ifPresent(record -> {
            if (editRecord == null) {
                // 创建新教学班
                saveTeachingClass(record, selectedTeachers);
            } else {
                // 更新现有教学班
                updateTeachingClass(record, selectedTeachers);
            }
        });
    }

    /**
     * 保存新的教学班到后端
     */
    private void saveTeachingClass(TeachingClassRecord record, ObservableList<Teacher> selectedTeachers) {
        try {
            DataRequest request = new DataRequest();
            
            // 根据课程编号获取课程ID
            Course course = allCourses.stream()
                    .filter(c -> c.getCourseNum().equals(record.getCourseNum()))
                    .findFirst().orElse(null);
            
            if (course != null && course.getCourseId() != null) {
                request.add("courseId", course.getCourseId());
            }
            
            request.add("year", record.getYear());
            request.add("semester", record.getTerm());
            request.add("classNumber", Integer.parseInt(record.getTeachClassNum()));
            request.add("classTime", record.getClassTime());
            request.add("classLocation", record.getClassLocation());
            
            // 添加教师ID列表
            List<Integer> teacherIds = selectedTeachers.stream()
                    .map(Teacher::getId)
                    .collect(Collectors.toList());
            request.add("teacherIds", teacherIds);
            
            DataResponse response = HttpRequestUtil.request("/api/teachplan/createClassSchedule", request);
              if (response != null && response.getCode() == 0) {
                // 保存成功，重新加载数据
                loadTeachingClasses();
                // click query button
                showSuccessAlert("保存成功", "教学班创建成功！");
            } else {
                showErrorAlert("保存失败", response != null ? response.getMsg() : "服务器错误");
            }        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("保存失败", "保存过程中发生错误：" + e.getMessage());
        }
    }

    /**
     * 更新现有教学班到后端
     */
    private void updateTeachingClass(TeachingClassRecord record, ObservableList<Teacher> selectedTeachers) {
        try {
            DataRequest request = new DataRequest();
            request.add("classScheduleId", Integer.parseInt(record.getId()));
            
            // 根据课程编号获取课程ID
            Course course = allCourses.stream()
                    .filter(c -> c.getCourseNum().equals(record.getCourseNum()))
                    .findFirst().orElse(null);
            
            if (course != null && course.getCourseId() != null) {
                request.add("courseId", course.getCourseId());
            }
            
            request.add("year", record.getYear());
            request.add("semester", record.getTerm());
            request.add("classNumber", Integer.parseInt(record.getTeachClassNum()));
            request.add("classTime", record.getClassTime());
            request.add("classLocation", record.getClassLocation());
            
            // 添加教师ID列表
            List<Integer> teacherIds = selectedTeachers.stream()
                    .map(Teacher::getId)
                    .collect(Collectors.toList());
            request.add("teacherIds", teacherIds);
            
            DataResponse response = HttpRequestUtil.request("/api/teachplan/updateClassSchedule", request);
            
            if (response != null && response.getCode() == 0) {
                // 更新成功，重新加载数据
                loadTeachingClasses();
                showAlert("更新成功", "教学班信息更新成功！");
            } else {
                showAlert("更新失败", response != null ? response.getMsg() : "服务器错误");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("更新失败", "更新过程中发生错误：" + e.getMessage());
        }
    }    /**
     * 显示提示对话框
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示错误提示对话框
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示成功提示对话框
     */
    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }@FXML
    public void onDeleteButtonClick(ActionEvent event) {
        TeachingClassRecord selected = teachingClassTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 显示确认对话框
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("确认删除");
            alert.setHeaderText(null);
            alert.setContentText("确定要删除教学班 " + selected.getTeachClassNum() + " 吗？");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteTeachingClass(selected);
            }
        }
    }

    /**
     * 删除教学班
     */
    private void deleteTeachingClass(TeachingClassRecord record) {
        try {
            DataRequest request = new DataRequest();
            request.add("classScheduleId", Integer.parseInt(record.getId()));
            
            // 注意：后端可能没有删除API，这里使用removeTeacherPlan作为示例
            // 实际应该有专门的删除教学班的API
            DataResponse response = HttpRequestUtil.request("/api/teachplan/removeTeacherPlan", request);
            
            if (response != null && response.getCode() == 0) {
                // 删除成功，重新加载数据
                loadTeachingClasses();
                showAlert("删除成功", "教学班删除成功！");
            } else {
                showAlert("删除失败", response != null ? response.getMsg() : "服务器错误");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("删除失败", "删除过程中发生错误：" + e.getMessage());
            // 如果API调用失败，回退到本地删除
            allTeachingClasses.remove(record);
            teachingClassTableView.setItems(FXCollections.observableArrayList(allTeachingClasses));        }
    }

    // ==== 时间选择辅助方法 ====
    
    /**
     * 检查是否选择了至少一个时间段
     */
    private boolean hasSelectedTime(CheckBox[][] timeCheckBoxes) {
        for (CheckBox[] row : timeCheckBoxes) {
            for (CheckBox cb : row) {
                if (cb.isSelected()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 将复选框选择转换为时间字符串
     * 格式：周一 8:00-10:00,周四 14:00-16:00
     */
    private String convertCheckBoxesToClassTime(CheckBox[][] timeCheckBoxes, String[] weekdays, String[] timeSlots) {
        List<String> selectedTimes = new ArrayList<>();
        for (int i = 0; i < weekdays.length; i++) {
            for (int j = 0; j < timeSlots.length; j++) {
                if (timeCheckBoxes[i][j].isSelected()) {
                    selectedTimes.add(weekdays[i] + " " + timeSlots[j]);
                }
            }
        }
        return String.join(",", selectedTimes);
    }
    
    /**
     * 解析时间字符串并设置复选框状态
     * 格式：周一 8:00-10:00,周四 14:00-16:00
     */
    private void parseClassTimeToCheckBoxes(String classTime, CheckBox[][] timeCheckBoxes, String[] weekdays, String[] timeSlots) {
        if (classTime == null || classTime.trim().isEmpty()) {
            return;
        }
        
        String[] times = classTime.split(",");
        for (String time : times) {
            time = time.trim();
            for (int i = 0; i < weekdays.length; i++) {
                for (int j = 0; j < timeSlots.length; j++) {
                    if (time.equals(weekdays[i] + " " + timeSlots[j])) {
                        timeCheckBoxes[i][j].setSelected(true);
                        break;
                    }
                }
            }
        }
    }    // ==== 数据结构 ====
    public static class Course {
        private final String courseNum, courseName;
        private Integer courseId;
        
        public Course(String num, String name) { 
            courseNum = num; 
            courseName = name; 
        }
        
        public String getCourseNum() { return courseNum; }
        public String getCourseName() { return courseName; }
        public Integer getCourseId() { return courseId; }
        public void setCourseId(Integer courseId) { this.courseId = courseId; }
        
        @Override public String toString() { return courseName + "(" + courseNum + ")"; }
    }

    public static class Teacher {
        private final String num, name;
        Integer id;
        public Teacher(Integer id,String num, String name) { this.id = id; this.name = name; this.num = num; }
        public Integer getId() { return id; }
        public String getName() { return name; }
        public String getNum() { return num; }
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
    }    // ==== API数据加载 ====
    private void loadDataFromAPIs() {
        loadCourses();
        loadTeachers();
        loadTeachingClasses();
    }

    /**
     * 从后端API加载课程数据
     */
    private void loadCourses() {
        try {
            DataRequest request = new DataRequest();
            DataResponse response = HttpRequestUtil.request("/api/course/getCourseList", request);
            
            if (response != null && response.getCode() == 0) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> courseData = gson.fromJson(gson.toJson(response.getData()), listType);
                
                allCourses.clear();
                for (Map<String, Object> courseMap : courseData) {
                    String courseNum = String.valueOf(courseMap.get("num"));
                    String courseName = String.valueOf(courseMap.get("name"));
                    Integer courseId = Integer.parseInt(courseMap.get("courseId").toString());
                    
                    Course course = new Course(courseNum, courseName);
                    course.setCourseId(courseId);
                    allCourses.add(course);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 加载失败时使用部分模拟数据
        }
    }    /**
     * 从后端API加载教师数据
     */
    private void loadTeachers() {
        try {
            DataRequest request = new DataRequest();
            List<OptionItem> response = HttpRequestUtil.requestOptionItemList("/api/teachplan/getTeacherOptionList", request);
            
            if (response != null) {
                allTeachers.clear();
                for (OptionItem item : response) {
                    String v = item.getTitle();
                    Integer id = Integer.valueOf(item.getValue());

                    // split v on -
                    String[] parts = v.split("-");
                    var name = parts[1].trim();
                    var num = parts[0].trim();
                    allTeachers.add(new Teacher(id,num, name));

                }}
        } catch (Exception e) {
            e.printStackTrace();
            // 加载失败时使用部分模拟数据
        }
    }    /**
     * 从后端API加载教学班数据
     */
    @SuppressWarnings("unchecked")
    private void loadTeachingClasses() {
        try {
            DataRequest request = new DataRequest();
            request.add("year", yearField.getText().trim());
            request.add("semester", termComboBox.getValue());
            DataResponse response = HttpRequestUtil.request("/api/teachplan/getCurrentSemesterClasses", request);
            
            if (response != null && response.getCode() == 0) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                List<Map<String, Object>> classData = gson.fromJson(gson.toJson(response.getData()), listType);
                
                allTeachingClasses.clear();
                for (Map<String, Object> classMap : classData) {
                    Integer classScheduleId = Integer.parseInt( classMap.get("classScheduleId").toString());
                    String year = String.valueOf(classMap.get("year"));
                    String semester = String.valueOf(classMap.get("semester"));
                    String classTime = String.valueOf(classMap.get("classTime"));
                    String classLocation = String.valueOf(classMap.get("classLocation"));
                    Integer classNumber =((Double) classMap.get("classNumber")).intValue();
                    String courseId =Integer.toString(((Double) classMap.get("courseId")).intValue());
                    String courseNum = classMap.get("courseNumber").toString();
                    String courseName = classMap.get("courseName").toString();
                    
                    // 获取教师信息
                    List<Double> teacherIds = (List<Double>) classMap.get("teacherIds");
                    StringBuilder teachers = new StringBuilder();
                    for (var teacherId : teacherIds) {

                        allTeachers.stream()
                                .filter(t ->t.getId() == teacherId.intValue())
                                .findFirst().ifPresent(teacher -> teachers.append(teacher.getName()).append(","));
                    }
                    
                    TeachingClassRecord record = new TeachingClassRecord(
                        String.valueOf(classScheduleId),
                        courseName,
                        courseNum,
                        year,
                        semester,
                        String.valueOf(classNumber),
                            teachers.toString(),
                        classTime != null ? classTime : "",
                        classLocation != null ? classLocation : ""
                    );
                    allTeachingClasses.add(record);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 加载失败时使用部分模拟数据
          }
    }

    @FXML
    public void onRefreshButtonClick(ActionEvent event) {
        loadDataFromAPIs();
        teachingClassTableView.setItems(allTeachingClasses);
        showAlert("刷新完成", "数据已从服务器重新加载");
    }
}