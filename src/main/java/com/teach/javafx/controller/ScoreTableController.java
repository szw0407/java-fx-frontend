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
    @FXML
    private TableView<ScoreRecord> dataTableView;
    @FXML
    private TableColumn<ScoreRecord, String> studentNameColumn, classNameColumn, courseNumColumn, courseNameColumn, creditColumn,
            teachClassNumColumn, yearColumn, termColumn, markColumn, editColumn;
    @FXML
    private ComboBox<Student> studentComboBox;
    @FXML
    private ComboBox<Course> courseComboBox;

    private ObservableList<ScoreRecord> allScores = FXCollections.observableArrayList();
    private ObservableList<Student> allStudents = FXCollections.observableArrayList();
    private ObservableList<Course> allCourses = FXCollections.observableArrayList();
    private Map<String, List<TeachingClass>> studentTeachClassMap = new HashMap<>();

    @FXML
    public void initialize() {
        initData();

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

            @Override
            protected void updateItem(String item, boolean empty) {
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
        // 获得选中学生的学号
        var dr = new DataRequest();
        dr.add("studentNum", stu != null ? stu.getStudentNum() : null);
        dr.add("courseNum", course != null ? course.getCourseNum() : null);
        var response = HttpRequestUtil.request("/api/score/getScoreListOfStudent", dr);
        if (response == null || response.getCode() != 0) {
            var msg = response != null ? response.getMsg() : "<UNK>";
            var a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("查询失败");
            a.setHeaderText("查询成绩失败");
            a.setContentText("错误信息: " + msg);
            a.showAndWait();
            return;
        }
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.getData();
        allScores.clear();
        for (Map<String, Object> m : dataList) {
            String sn = (String) m.get("studentNum");
            String name = (String) m.get("studentName");
            String cls = (String) m.get("className");
            String cnum = (String) m.get("courseNum");
            String cname = (String) m.get("courseName");
            String credit = (String) m.get("credit");
            String tnum = ((Double) m.get("teachClassNum")).intValue() + "";
            String year = (String) m.get("year");
            String term = (String) m.get("term");
            Double mark = (Double) m.get("mark");
            if (mark == null) {
                continue;
            }
            allScores.add(new ScoreRecord(sn, name, cls, cnum, cname, credit, tnum, year, term, Integer.toString(mark.intValue())));
        }
        List<ScoreRecord> filtered = allScores.stream().filter(s ->
                (stu == null || s.getStudentName().equals(stu.getStudentName())) &&
                        (course == null || s.getCourseNum().equals(course.getCourseNum())) &&
                        s.getMark() != null // 只显示有成绩的记录
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
                // 查询这个学生选课的教学班
                var dr = new DataRequest();

                dr.add("studentNum", stu.getStudentNum());

                var response = HttpRequestUtil.request("/api/teachplan/getStudentPlanList", dr);
                if (response == null || response.getCode() != 0) {
                    var msg = response != null ? response.getMsg() : "<UNK>";
                    var a = new Alert(Alert.AlertType.WARNING);
                    a.setTitle("查询失败");
                    a.setHeaderText("查询教学班失败");
                    a.setContentText("错误信息: " + msg);
                    a.showAndWait();
                    return;
                }
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.getData();
                List<TeachingClass> teachClasses = new ArrayList<>();
                for (Map<String, Object> m : dataList) {
                    // create studentteachclassmap accordingly
                    String tnum = Integer.toString(((Double) m.get("classNum")).intValue());
                    String year = (String) m.get("year");
                    String term = (String) m.get("semester");
                    String courseNum = (String) m.get("courseNumber");
                    String courseName = (String) m.get("courseName");
                    int credit = ((Double) m.get("credit")).intValue();
                    teachClasses.add(
                            new TeachingClass(
                                    tnum, year, term, allCourses.stream().filter(c -> c.getCourseNum().equals(courseNum))
                                            .findFirst()
                                            .orElse(new Course(courseNum, courseName, credit)) // 如果没有找到课程，创建一个空课程
                            )
                    );
                }
                // 更新学生教学班映射
                studentTeachClassMap.put(stu.getStudentNum(), teachClasses);

                // 更新教学班下拉框
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
            // upload to server
            var dr = new DataRequest();
            // search for id
            dr.add("studentNum", record.getStudentNum());
            dr.add("courseNum", record.getCourseNum());
            dr.add("classNumber", record.getTeachClassNum());
            dr.add("year", record.getYear());
            dr.add("semester", record.getTerm());
            var c = HttpRequestUtil.request("/api/teachplan/checkTeachPlanByInfo", dr);
            if (c == null || c.getCode() != 0) {
                var msg = c != null ? c.getMsg() : "";
                var a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("添加失败");
                a.setHeaderText("课程信息有误");
                a.setContentText("错误信息: " + msg);
                a.showAndWait();
                return;
            }
            Integer id = ((Double) ((Map<String, Object>) c.getData()).get("classScheduleId")).intValue();
            dr.add("classId", id);
            dr.add("mark", record.getMark());
            var response = HttpRequestUtil.request("/api/score/scoreSave", dr);
            if (response == null || response.getCode() != 0) {
                var msg = response != null ? response.getMsg() : "";
                var a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("添加失败");
                a.setHeaderText("添加成绩失败");
                a.setContentText("错误信息: " + msg);
                a.showAndWait();
                return;
            }
            // 成功后添加到表格
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
            // upload to server
            var dr = new DataRequest();
            dr.add("studentNum", selected.getStudentNum());
            dr.add("courseNum", selected.getCourseNum());
            dr.add("classNum", selected.getTeachClassNum());
            dr.add("year", selected.getYear());
            dr.add("semester", selected.getTerm());
            var rec = HttpRequestUtil.request("/api/courseSelection/verifyStudentCourseSelection", dr);
            // check if rec is ok or data
            if (rec == null || rec.getCode() != 0 || rec.getData() == null) {
                var msg = rec != null ? rec.getMsg() : "<UNK>";
                var a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("修改失败");
                a.setHeaderText("修改成绩失败");
                a.setContentText("错误信息: " + msg);
                a.showAndWait();
                return;
            }
            Integer id = ((Double) rec.getData()).intValue();
            dr.add("scoreId", id);
            dr.add("mark", mark);
            var response = HttpRequestUtil.request("/api/score/scoreSave", dr);
            if (response == null || response.getCode() != 0) {
                var msg = response != null ? response.getMsg() : "<UNK>";
                var a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("修改失败");
                a.setHeaderText("修改成绩失败");
                a.setContentText("错误信息: " + msg);
                a.showAndWait();
                return;
            }

            selected.setMark(mark);
            dataTableView.refresh();
        });
    }

    // 删除成绩
    @FXML
    public void onDeleteButtonClick(ActionEvent event) {
        ScoreRecord selected = dataTableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // upload to server
            var dr = new DataRequest();
            dr.add("studentNum", selected.getStudentNum());
            dr.add("courseNum", selected.getCourseNum());
            dr.add("classNum", selected.getTeachClassNum());
            dr.add("year", selected.getYear());
            dr.add("semester", selected.getTerm());
            var rec = HttpRequestUtil.request("/api/courseSelection/verifyStudentCourseSelection", dr);
            // check if rec is ok or data
            if (rec == null || rec.getCode() != 0 || rec.getData() == null) {
                var msg = rec != null ? rec.getMsg() : "<UNK>";
                var a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("修改失败");
                a.setHeaderText("修改成绩失败");
                a.setContentText("错误信息: " + msg);
                a.showAndWait();
                return;
            }
            Integer id = ((Double) rec.getData()).intValue();
            dr.add("scoreId", id);
            var dl = HttpRequestUtil.request("/api/score/scoreDelete", dr);
            if (dl == null || dl.getCode() != 0) {
                var msg = dl != null ? dl.getMsg() : "<UNK>";
                var a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("删除失败");
                a.setHeaderText("删除成绩失败");
                a.setContentText("错误信息: " + msg);
                a.showAndWait();
                return;
            }
            allScores.remove(selected);
            dataTableView.setItems(FXCollections.observableArrayList(allScores));
        }
    }

    // mock数据结构体和初始化
    public static class Student {
        private final String studentNum, studentName, className;

        public Student(String sn, String name, String cls) {
            studentNum = sn;
            studentName = name;
            className = cls;
        }

        public String getStudentNum() {
            return studentNum;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getClassName() {
            return className;
        }

        @Override
        public String toString() {
            return studentName + "(" + studentNum + ")";
        }
    }

    public static class Course {
        private final String courseNum, courseName;
        private final int credit;

        public Course(String n, String name, int c) {
            courseNum = n;
            courseName = name;
            credit = c;
        }

        public String getCourseNum() {
            return courseNum;
        }

        public String getCourseName() {
            return courseName;
        }

        public int getCredit() {
            return credit;
        }

        @Override
        public String toString() {
            return courseName + "(" + courseNum + ")";
        }
    }

    public static class TeachingClass {
        private final String teachClassNum, year, term;
        private final Course course;

        public TeachingClass(String t, String y, String tm, Course c) {
            teachClassNum = t;
            year = y;
            term = tm;
            course = c;
        }

        public String getTeachClassNum() {
            return teachClassNum;
        }

        public String getYear() {
            return year;
        }

        public String getTerm() {
            return term;
        }

        public Course getCourse() {
            return course;
        }

        @Override
        public String toString() {
            return teachClassNum + " " + course.getCourseName() + " " + year + "-" + term;
        }
    }

    // JavaFX BeanProperty写法（简化，实际开发建议用SimpleStringProperty等）
    public static class ScoreRecord {
        private String studentNum, studentName, className, courseNum, courseName, credit,
                teachClassNum, year, term, mark;

        public ScoreRecord(String sn, String name, String cls, String cnum, String cname, String cr, String tnum, String y, String t, String m) {
            studentNum = sn;
            studentName = name;
            className = cls;
            courseNum = cnum;
            courseName = cname;
            credit = cr;
            teachClassNum = tnum;
            year = y;
            term = t;
            mark = m;
        }

        public String getStudentNum() {
            return studentNum;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getClassName() {
            return className;
        }

        public String getCourseNum() {
            return courseNum;
        }

        public String getCourseName() {
            return courseName;
        }

        public String getCredit() {
            return credit;
        }

        public String getTeachClassNum() {
            return teachClassNum;
        }

        public String getYear() {
            return year;
        }

        public String getTerm() {
            return term;
        }

        public String getMark() {
            return mark;
        }

        public void setMark(String m) {
            mark = m;
        }

        // JavaFX Property方法（如需支持TableView双向绑定可用SimpleStringProperty）
        public javafx.beans.property.SimpleStringProperty studentNameProperty() {
            return new javafx.beans.property.SimpleStringProperty(studentName);
        }

        public javafx.beans.property.SimpleStringProperty classNameProperty() {
            return new javafx.beans.property.SimpleStringProperty(className);
        }

        public javafx.beans.property.SimpleStringProperty courseNumProperty() {
            return new javafx.beans.property.SimpleStringProperty(courseNum);
        }

        public javafx.beans.property.SimpleStringProperty courseNameProperty() {
            return new javafx.beans.property.SimpleStringProperty(courseName);
        }

        public javafx.beans.property.SimpleStringProperty creditProperty() {
            return new javafx.beans.property.SimpleStringProperty(credit);
        }

        public javafx.beans.property.SimpleStringProperty teachClassNumProperty() {
            return new javafx.beans.property.SimpleStringProperty(teachClassNum);
        }

        public javafx.beans.property.SimpleStringProperty yearProperty() {
            return new javafx.beans.property.SimpleStringProperty(year);
        }

        public javafx.beans.property.SimpleStringProperty termProperty() {
            return new javafx.beans.property.SimpleStringProperty(term);
        }

        public javafx.beans.property.SimpleStringProperty markProperty() {
            return new javafx.beans.property.SimpleStringProperty(mark);
        }
    }


    private void initData() {

        // real data
        // students list
        var dr = new DataRequest();
        allStudents.clear();
        dr.add("numName", ""); // empty means all students


        // courses list
//        dr = new DataRequest();
        allCourses.clear();
        var cl = HttpRequestUtil.request("/api/course/getCourseList", dr);
        if (cl != null && cl.getCode() == 0) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) cl.getData();

            for (Map<String, Object> m : dataList) {
                String num = (String) m.get("num");
                String name = (String) m.get("name");
                int credit = Integer.parseInt((String) m.get("credit"));
                allCourses.add(new Course(num, name, credit));

            }

        }
        var s = HttpRequestUtil.request("/api/student/getStudentList", dr);
        if (s != null && s.getCode() == 0) {
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) s.getData();

            for (Map<String, Object> m : dataList) {
                String num = (String) m.get("num");
                String name = (String) m.get("name");
                String cls = (String) m.get("className");
                allStudents.add(new Student(num, name, cls));

            }
        }

    }
}