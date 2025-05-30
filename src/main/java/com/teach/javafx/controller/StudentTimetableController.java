package com.teach.javafx.controller;

                                                    import com.teach.javafx.request.DataRequest;
                                                    import com.teach.javafx.request.HttpRequestUtil;
                                                    import javafx.beans.property.SimpleStringProperty;
                                                    import javafx.collections.FXCollections;
                                                    import javafx.collections.ObservableList;
                                                    import javafx.fxml.FXML;
                                                    import javafx.geometry.Pos;
                                                    import javafx.scene.control.*;
                                                    import javafx.scene.input.MouseButton;
                                                    import javafx.scene.layout.VBox;

                                                    import java.time.LocalDate;
                                                    import java.util.*;
                                                    import java.util.stream.Collectors;

                                                    /**
                                                     * 学生个人课程表
                                                     * 按学年学期显示，自动填充上课时间到对应格子
                                                     * 支持双击查看详情
                                                     */
                                                    public class StudentTimetableController {
                                                        // 这两个字段在UI中已不可见，但在逻辑中仍需使用
                                                        private TextField yearField = new TextField();
                                                        private ComboBox<String> termComboBox = new ComboBox<>();

                                                        @FXML private Label semesterLabel;
                                                        @FXML private Button refreshButton;
                                                        @FXML private TableView<TimetableRow> timetableTableView;
                                                        @FXML private TableColumn<TimetableRow, String> timeColumn;
                                                        @FXML private TableColumn<TimetableRow, String> monColumn, tueColumn, wedColumn, thuColumn, friColumn, satColumn, sunColumn;

                                                        // mock已选课程教学班
                                                        private List<TeachingClassVO> mySelectedClasses = new ArrayList<>();

                                                        // 标准时间段格式
                                                        private static final String[] TIME_SLOTS = {
                                                                "第1-2节\n8:00-9:50",
                                                                "第3-4节\n10:10-12:00",
                                                                "第5-6节\n14:00-15:50",
                                                                "第7-8节\n16:10-18:00",
                                                                "第9-10节\n19:00-20:50"
                                                        };

                                                        // 简化时间格式（用于匹配原始数据）
                                                        private static final String[] SIMPLE_TIMES = {
                                                                "8:00-10:00", "10:00-12:00", "14:00-16:00", "16:00-18:00", "19:00-21:00"
                                                        };

                                                        private static final String[] DAYS = {
                                                                "周一","周二","周三","周四","周五","周六","周日"
                                                        };

                                                        // 时间段到课程的映射
                                                        private Map<String, Map<String, List<TeachingClassVO>>> courseTable = new HashMap<>();

                                                        // 课程名称到颜色的映射
                                                        private Map<String, Integer> courseColorMap = new HashMap<>();
                                                        private int colorCounter = 0;
                                                        private static final String[] COLOR_CLASSES = {
                                                            "course-color-1", "course-color-2", "course-color-3",
                                                            "course-color-4", "course-color-5", "course-color-6", "course-color-7"
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

                                                            // 更新学年学期标签
                                                            updateSemesterLabel();

                                                            // 设置列
                                                            timeColumn.setCellValueFactory(cell -> cell.getValue().timeLabel);
                                                            monColumn.setCellValueFactory(cell -> cell.getValue().mon);
                                                            tueColumn.setCellValueFactory(cell -> cell.getValue().tue);
                                                            wedColumn.setCellValueFactory(cell -> cell.getValue().wed);
                                                            thuColumn.setCellValueFactory(cell -> cell.getValue().thu);
                                                            friColumn.setCellValueFactory(cell -> cell.getValue().fri);
                                                            satColumn.setCellValueFactory(cell -> cell.getValue().sat);
                                                            sunColumn.setCellValueFactory(cell -> cell.getValue().sun);

                                                            // 自定义时间列显示
                                                            timeColumn.setCellFactory(column -> new TableCell<>() {
                                                                @Override
                                                                protected void updateItem(String item, boolean empty) {
                                                                    super.updateItem(item, empty);
                                                                    if (empty || item == null) {
                                                                        setText(null);
                                                                        setGraphic(null);
                                                                    } else {
                                                                        String[] parts = item.split("\n");
                                                                        Label mainLabel = new Label(parts[0]);
                                                                        mainLabel.getStyleClass().add("time-detail");

                                                                        Label subLabel = new Label(parts.length > 1 ? parts[1] : "");
                                                                        subLabel.getStyleClass().add("time-secondary");

                                                                        VBox vbox = new VBox(5, mainLabel, subLabel);
                                                                        vbox.setAlignment(Pos.CENTER);

                                                                        setGraphic(vbox);
                                                                        setText(null);
                                                                    }
                                                                }
                                                            });

                                                            // 设置课程单元格工厂（所有星期列通用）
                                                            setupCourseCellFactory(monColumn);
                                                            setupCourseCellFactory(tueColumn);
                                                            setupCourseCellFactory(wedColumn);
                                                            setupCourseCellFactory(thuColumn);
                                                            setupCourseCellFactory(friColumn);
                                                            setupCourseCellFactory(satColumn);
                                                            setupCourseCellFactory(sunColumn);

                                                            // 初始化数据并显示
                                                            initData();
                                                            displayTimetable();
                                                        }

                                                        /**
                                                         * 更新学年学期标签
                                                         */
                                                        private void updateSemesterLabel() {
                                                            String year = yearField.getText().trim();
                                                            String term = termComboBox.getValue();
                                                            semesterLabel.setText(year + "学年 第" + term + "学期");
                                                        }

                                                        /**
                                                         * 为课程分配颜色
                                                         */
                                                        private String getColorForCourse(String courseName) {
                                                            if (!courseColorMap.containsKey(courseName)) {
                                                                // 分配新颜色
                                                                int colorIndex = colorCounter % COLOR_CLASSES.length;
                                                                courseColorMap.put(courseName, colorIndex);
                                                                colorCounter++;
                                                            }

                                                            int colorIndex = courseColorMap.get(courseName);
                                                            return COLOR_CLASSES[colorIndex];
                                                        }

                                                        /**
                                                         * 设置课程单元格工厂，支持双击查看详情
                                                         */
                                                        private void setupCourseCellFactory(TableColumn<TimetableRow, String> column) {
                                                            column.setCellFactory(col -> {
                                                                TableCell<TimetableRow, String> cell = new TableCell<>() {
                                                                    @Override
                                                                    protected void updateItem(String item, boolean empty) {
                                                                        super.updateItem(item, empty);

                                                                        if (empty || item == null || item.trim().isEmpty()) {
                                                                            setText(null);
                                                                            setGraphic(null);
                                                                            getStyleClass().removeAll("course-cell", "course-color-1", "course-color-2", "course-color-3",
                                                                                    "course-color-4", "course-color-5", "course-color-6", "course-color-7");
                                                                            return;
                                                                        }

                                                                        setText(item);

                                                                        // 获取课程名称（取第一行文本，去掉班号部分）
                                                                        String courseName = item.split("\n")[0];
                                                                        if (courseName.contains("[")) {
                                                                            courseName = courseName.substring(0, courseName.indexOf("[")).trim();
                                                                        }

                                                                        // 设置样式类
                                                                        getStyleClass().add("course-cell");

                                                                        // 移除所有颜色类
                                                                        getStyleClass().removeAll("course-color-1", "course-color-2", "course-color-3",
                                                                                "course-color-4", "course-color-5", "course-color-6", "course-color-7");

                                                                        // 添加对应的颜色类
                                                                        getStyleClass().add(getColorForCourse(courseName));
                                                                    }
                                                                };

                                                                // 添加双击事件处理
                                                                cell.setOnMouseClicked(event -> {
                                                                    if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                                                                        if (!cell.isEmpty()) {
                                                                            String columnHeader = cell.getTableColumn().getText();
                                                                            int rowIndex = cell.getIndex();
                                                                            if (rowIndex >= 0 && rowIndex < SIMPLE_TIMES.length) {
                                                                                String timeSlot = SIMPLE_TIMES[rowIndex];
                                                                                showCourseDetails(columnHeader, timeSlot);
                                                                            }
                                                                        }
                                                                    }
                                                                });

                                                                return cell;
                                                            });
                                                        }

                                                        /**
                                                         * 显示课程详情对话框
                                                         */
                                                        private void showCourseDetails(String day, String timeSlot) {
                                                            List<TeachingClassVO> courses = courseTable.getOrDefault(timeSlot, Collections.emptyMap())
                                                                    .getOrDefault(day, Collections.emptyList());

                                                            if (courses.isEmpty()) {
                                                                return;
                                                            }

                                                            StringBuilder details = new StringBuilder();
                                                            for (TeachingClassVO course : courses) {
                                                                details.append("课程名称: ").append(course.getCourseName()).append("\n");
                                                                details.append("教学班号: ").append(course.getTeachClassNum()).append("\n");
                                                                details.append("课程编号: ").append(course.getCourseNum()).append("\n");
                                                                details.append("授课教师: ").append(course.getTeachers()).append("\n");
                                                                details.append("上课地点: ").append(course.getClassLocation()).append("\n");
                                                                details.append("上课时间: ").append(course.getClassTime()).append("\n");
                                                                details.append("学年学期: ").append(course.getYear()).append("-").append(course.getTerm()).append("\n");
                                                                details.append("---------------------------\n");
                                                            }

                                                            Dialog<String> dialog = new Dialog<>();
                                                            dialog.setTitle("课程详情");
                                                            dialog.setHeaderText(day + " " + timeSlot + " 的课程信息");

                                                            ButtonType closeButton = new ButtonType("关闭", ButtonBar.ButtonData.OK_DONE);
                                                            dialog.getDialogPane().getButtonTypes().add(closeButton);

                                                            TextArea textArea = new TextArea(details.toString());
                                                            textArea.setEditable(false);
                                                            textArea.setPrefWidth(400);
                                                            textArea.setPrefHeight(300);

                                                            dialog.getDialogPane().setContent(textArea);
                                                            dialog.showAndWait();
                                                        }

                                                        @FXML
                                                        public void onQueryButtonClick() {
                                                            courseColorMap.clear();
                                                            colorCounter = 0;
                                                            initData();
                                                            displayTimetable();
                                                            updateSemesterLabel();
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

                                                            // 重置课程表映射
                                                            courseTable.clear();

                                                            // 课程表数据结构: [时间段][星期] = 课程信息
                                                            Map<String, Map<String, List<String>>> table = new LinkedHashMap<>();
                                                            for (String time : SIMPLE_TIMES) {
                                                                Map<String, List<String>> dayMap = new HashMap<>();
                                                                courseTable.put(time, new HashMap<>());

                                                                for (String day : DAYS) {
                                                                    dayMap.put(day, new ArrayList<>());
                                                                    courseTable.get(time).put(day, new ArrayList<>());
                                                                }
                                                                table.put(time, dayMap);
                                                            }

                                                            // 分配课程到格子
                                                            for (TeachingClassVO tc : filtered) {
                                                                String[] timeSlots = tc.getClassTime().split(",");
                                                                for (String slot : timeSlots) {
                                                                    slot = slot.trim();
                                                                    for (String day : DAYS) {
                                                                        if (slot.startsWith(day)) {
                                                                            String timeInfo = slot.substring(day.length()).trim();
                                                                            for (int i = 0; i < SIMPLE_TIMES.length; i++) {
                                                                                if (timeInfo.contains(SIMPLE_TIMES[i])) {
                                                                                    String time = SIMPLE_TIMES[i];
                                                                                    String displayText = tc.getCourseName() + "\n[" + tc.getTeachClassNum() + "]\n" + tc.getClassLocation();
                                                                                    table.get(time).get(day).add(displayText);
                                                                                    courseTable.get(time).get(day).add(tc);
                                                                                    break;
                                                                                }
                                                                            }
                                                                            break;
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            ObservableList<TimetableRow> rows = FXCollections.observableArrayList();
                                                            for (int i = 0; i < TIME_SLOTS.length; i++) {
                                                                String timeSlot = TIME_SLOTS[i];
                                                                String simpleTime = SIMPLE_TIMES[i];
                                                                rows.add(new TimetableRow(timeSlot, table.get(simpleTime)));
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

                                                            public String getTimeLabel() {
                                                                return timeLabel.get();
                                                            }
                                                        }

                                                        // ---- 数据模型 ----
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
                                                            // 先清空数据
                                                            mySelectedClasses.clear();

                                                            try {
                                                                var dr = new DataRequest();
                                                                dr.add("year", yearField.getText().trim());
                                                                dr.add("semester", termComboBox.getValue());
                                                                var l = HttpRequestUtil.request("/api/me/PlanList", dr);
                                                                if (l != null && l.getCode() == 0) {
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
                                                                    // 如果接口调用失败，使用模拟数据
                                                                    initMockData();

                                                                    // 显示错误提示
                                                                    if (l != null) {
                                                                        Alert alert = new Alert(Alert.AlertType.WARNING,
                                                                                "获取课程表数据失败: " + l.getMsg() + "\n已加载测试数据");
                                                                        alert.showAndWait();
                                                                    }
                                                                }
                                                            } catch (Exception e) {
                                                                // 异常处理，使用模拟数据
                                                                initMockData();
                                                                Alert alert = new Alert(Alert.AlertType.WARNING,
                                                                        "获取课程表数据出错: " + e.getMessage() + "\n已加载测试数据");
                                                                alert.showAndWait();
                                                            }
                                                        }
                                                    }