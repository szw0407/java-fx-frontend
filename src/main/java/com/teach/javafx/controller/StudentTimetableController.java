package com.teach.javafx.controller;

import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生个人课程表
 * 按学年学期显示，自动填充上课时间到对应格子
 * 上课时间字段示例："周一 8:00-10:00,周五 19:00-21:00"
 */
public class StudentTimetableController {
    @FXML private TextField yearField;
    @FXML private ComboBox<String> termComboBox;
    @FXML private TableView<TimetableRow> timetableTableView;
    @FXML private TableColumn<TimetableRow, String> monColumn, tueColumn, wedColumn, thuColumn, friColumn, satColumn, sunColumn;

    // mock已选课程教学班
    private List<TeachingClassVO> mySelectedClasses = new ArrayList<>();

    // 时间段横轴
    private static final String[] times = {
            "8:00-10:00", "10:00-12:00", "14:00-16:00", "16:00-18:00", "19:00-21:00"
    };
    private static final String[] days = {
            "周一","周二","周三","周四","周五","周六","周日"
    };

    @FXML
    public void initialize() {
        // 默认学年学期
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        yearField.setText(String.valueOf(year));
        termComboBox.setItems(FXCollections.observableArrayList("1", "2"));
        // 习惯上，下半年为1学期，上半年为2学期
        termComboBox.setValue((month >= 7) ? "1" : "2");

        // 设置列
        monColumn.setCellValueFactory(cell -> cell.getValue().mon);
        tueColumn.setCellValueFactory(cell -> cell.getValue().tue);
        wedColumn.setCellValueFactory(cell -> cell.getValue().wed);
        thuColumn.setCellValueFactory(cell -> cell.getValue().thu);
        friColumn.setCellValueFactory(cell -> cell.getValue().fri);
        satColumn.setCellValueFactory(cell -> cell.getValue().sat);
        sunColumn.setCellValueFactory(cell -> cell.getValue().sun);

        initData();
//        initMockData();
        displayTimetable();
    }

    @FXML
    public void onQueryButtonClick() {
        initData();displayTimetable();
    }

    // 构建课程表
    private void displayTimetable() {
        // 过滤当前学年学期
        String year = yearField.getText().trim();
        String term = termComboBox.getValue();
        List<TeachingClassVO> filtered = mySelectedClasses.stream().filter(tc ->
                (year.isEmpty() || tc.getYear().equals(year)) &&
                        (term == null || tc.getTerm().equals(term))
        ).toList();

        // 课程表数据结构: [时间段][星期] = 课程信息
        Map<String, Map<String, List<String>>> table = new LinkedHashMap<>();
        for (String time : times) {
            Map<String, List<String>> dayMap = new HashMap<>();
            for (String day : days) {
                dayMap.put(day, new ArrayList<>());
            }
            table.put(time, dayMap);
        }
        // 分配课程到格子
        for (TeachingClassVO tc : filtered) {
            String[] timeSlots = tc.getClassTime().split(",");
            for (String slot : timeSlots) {
                slot = slot.trim();
                for (String day : days) {
                    if (slot.startsWith(day)) {
                        String timeStr = slot.substring(day.length()).trim();
                        for (String t : times) {
                            if (t.equals(timeStr)) {
                                String info = tc.getCourseName() + " [" + tc.getTeachClassNum() + "]\n" + tc.getClassLocation() + "\n" + tc.getTeachers() + "\n";
                                table.get(t).get(day).add(info);
                            }
                        }
                    }
                }
            }
        }

        ObservableList<TimetableRow> rows = FXCollections.observableArrayList();
        for (String time : times) {
            rows.add(new TimetableRow(time, table.get(time)));
        }
        timetableTableView.setItems(rows);
    }

    // TableView每行
    public static class TimetableRow {
        public final SimpleStringProperty mon = new SimpleStringProperty("");
        public final SimpleStringProperty tue = new SimpleStringProperty("");
        public final SimpleStringProperty wed = new SimpleStringProperty("");
        public final SimpleStringProperty thu = new SimpleStringProperty("");
        public final SimpleStringProperty fri = new SimpleStringProperty("");
        public final SimpleStringProperty sat = new SimpleStringProperty("");
        public final SimpleStringProperty sun = new SimpleStringProperty("");
        public final SimpleStringProperty timeLabel = new SimpleStringProperty("");

        public TimetableRow(String time, Map<String, List<String>> dayMap) {
            timeLabel.set(time);
            mon.set(String.join("\n", dayMap.get("周一")));
            tue.set(String.join("\n", dayMap.get("周二")));
            wed.set(String.join("\n", dayMap.get("周三")));
            thu.set(String.join("\n", dayMap.get("周四")));
            fri.set(String.join("\n", dayMap.get("周五")));
            sat.set(String.join("\n", dayMap.get("周六")));
            sun.set(String.join("\n", dayMap.get("周日")));
        }
    }

    // ---- mock数据结构 ----
    public static class TeachingClassVO {
        private final String id;
        private final String courseName, courseNum, teachClassNum, teachers, classTime, classLocation, year, term;
        public TeachingClassVO(String id, String cname, String cnum, String tcNum, String teachers, String time, String location, String year, String term) {
            this.id = id;
            this.courseName = cname;
            this.courseNum = cnum;
            this.teachClassNum = tcNum;
            this.teachers = teachers;
            this.classTime = time;
            this.classLocation = location;
            this.year = year;
            this.term = term;
        }
        public String getId() { return id; }
        public String getCourseName() { return courseName; }
        public String getCourseNum() { return courseNum; }
        public String getTeachClassNum() { return teachClassNum; }
        public String getTeachers() { return teachers; }
        public String getClassTime() { return classTime; }
        public String getClassLocation() { return classLocation; }
        public String getYear() { return year; }
        public String getTerm() { return term; }
    }

    private void initMockData() {
        // 假设已选三门课
        mySelectedClasses.add(new TeachingClassVO(
                "TCID-1", "程序设计", "CS101", "101A", "王老师,赵老师",
                "周一 8:00-10:00,周五 19:00-21:00", "A101", "2024", "1"));
        mySelectedClasses.add(new TeachingClassVO(
                "TCID-2", "高等数学", "MA101", "102B", "李老师",
                "周三 10:00-12:00", "B201", "2024", "1"));
        mySelectedClasses.add(new TeachingClassVO(
                "TCID-3", "大学英语", "EN101", "201A", "刘老师,陈老师",
                "周五 8:00-10:00", "C301", "2024", "1"));
        mySelectedClasses.add(new TeachingClassVO(
                "TCID-4", "线性代数", "MA102", "104C", "孙老师",
                "周二 8:00-10:00,周四 14:00-16:00", "A102", "2024", "2"));
    }
    private void initData() {
        var dr = new DataRequest();
        dr.add("year", yearField.getText().trim());
        dr.add("semester", termComboBox.getValue());
        var l = HttpRequestUtil.request("/api/me/PlanList", dr);
        if (l.getCode() == 0) {
            // 解析返回的课程表数据
            mySelectedClasses = ((List<Map>)l.getData()).stream().map(item -> {
                Map<String, String> m = (Map<String, String>) item;
                return new TeachingClassVO(
                        m.get("classScheduleId"),
                        m.get("courseName"),
                        m.get("courseNum"),
                        m.get("classNum"),
                        m.get("teachers"),
                        m.get("classTime"),
                        m.get("classLocation"),
                        m.get("year"),
                        m.get("semester")
                );
            }).collect(Collectors.toList());
        } else {
            // 错误处理
            Alert alert = new Alert(Alert.AlertType.ERROR, "获取课程表失败: " + l.getMsg());
            alert.showAndWait();
        }

    }
}