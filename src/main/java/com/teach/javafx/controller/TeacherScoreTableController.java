package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.HttpRequestUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos; // Import for Pos
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class TeacherScoreTableController {
    boolean isTeacher = AppStore.getJwt().getRole().equals("ROLE_TEACHER");
    @FXML
    public TextField yearField; // Keep public if FXML needs it, or make private and use @FXML on getter/setter if preferred
    @FXML
    public ComboBox<String> termComboBox; // Parameterize ComboBox
    @FXML
    private TableView<ScoreRecord> dataTableView;
    @FXML
    private TableColumn<ScoreRecord, String> studentNameColumn, courseNumColumn, courseNameColumn, creditColumn,
            teachClassNumColumn, yearColumn, termColumn, markColumn, editColumn;
    @FXML
    private ComboBox<Student> studentComboBox;
    @FXML
    private ComboBox<Course> courseComboBox;
    @FXML
    private Button viewStatisticsButton; // Button to view statistics
    private final ObservableList<ScoreRecord> allScores = FXCollections.observableArrayList();
    private final ObservableList<Student> allStudents = FXCollections.observableArrayList();
    private final ObservableList<Course> allCourses = FXCollections.observableArrayList();
    // private Map<String, List<TeachingClass>> studentTeachClassMap = new HashMap<>(); // Commented out as unused

    private static final double PASSING_GRADE = 60.0;
    private static final DecimalFormat df = new DecimalFormat("#.##");


    // Helper class for numeric calculations
    private static class NumericScoreEntry {
        String studentNum, studentName, className, courseNum, courseName, originalMark;
        double credit, mark;

        public NumericScoreEntry(ScoreRecord sr) throws NumberFormatException {
            this.studentNum = sr.getStudentNum();
            this.studentName = sr.getStudentName();
            this.className = sr.getClassName();
            this.courseNum = sr.getCourseNum();
            this.courseName = sr.getCourseName();
            this.originalMark = sr.getMark(); // Keep original mark string for display

            try {
                this.credit = Double.parseDouble(sr.getCredit());
                this.mark = Double.parseDouble(sr.getMark());
            } catch (NumberFormatException e) {
                // Rethrow if mark or credit is not a valid number, so it can be skipped.
                throw e;
            }
        }

        // Getters
        public String getStudentNum() { return studentNum; } // Keep for potential future use or if accessed by other parts not shown
        public String getStudentName() { return studentName; }
        public String getClassName() { return className; } // Keep for potential future use
        public String getCourseNum() { return courseNum; }
        public String getCourseName() { return courseName; }
        public double getCredit() { return credit; }
        public double getMark() { return mark; }
        public String getOriginalMark() { return originalMark; } // Keep for potential future use

        @Override
        public String toString() {
            return String.format("学生: %s (%s), 课程: %s (%s), 成绩: %s, 学分: %.1f",
                    studentName, studentNum, courseName, courseNum, originalMark, credit);
        }
    }


    @FXML
    public void initialize() {
        var cal = Calendar.getInstance();
        String year = String.valueOf(cal.get(Calendar.YEAR));
        termComboBox.setItems(FXCollections.observableArrayList("1", "2"));
        String sem = "";
        switch (cal.get(Calendar.MONTH)) {
            case Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH, Calendar.APRIL, Calendar.MAY, Calendar.JUNE -> {
                sem = "2";
            }
            case Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER,
                 Calendar.DECEMBER -> {
                sem = "1";
            }
        }
        termComboBox.setValue(sem);
        yearField.setText(year);

        initData();
        // 绑定表格列
        studentNameColumn.setCellValueFactory(cell -> cell.getValue().studentNameProperty());
//        classNameColumn.setCellValueFactory(cell -> cell.getValue().classNameProperty());
        courseNumColumn.setCellValueFactory(cell -> cell.getValue().courseNumProperty());
        courseNameColumn.setCellValueFactory(cell -> cell.getValue().courseNameProperty());
        creditColumn.setCellValueFactory(cell -> cell.getValue().creditProperty());
        teachClassNumColumn.setCellValueFactory(cell -> cell.getValue().teachClassNumProperty());
        yearColumn.setCellValueFactory(cell -> cell.getValue().yearProperty());
        termColumn.setCellValueFactory(cell -> cell.getValue().termProperty());
        markColumn.setCellValueFactory(cell -> cell.getValue().markProperty());
        if (isTeacher) {
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
        }

        // 绑定下拉框
        studentComboBox.setItems(allStudents);
        courseComboBox.setItems(allCourses);

        dataTableView.setItems(allScores);

        // Add statistics button programmatically
        // Button viewStatisticsButton = new Button("查看统计数据");
//        viewStatisticsButton.setOnAction(this::onViewStatisticsButtonClick);

        // Add the button to the layout. This is an example.
        // You might need to adjust this based on your FXML structure.
        // Assuming the parent of the TableView is a VBox or similar Pane.
        if (dataTableView.getParent() instanceof VBox) {
            VBox parentVBox = (VBox) dataTableView.getParent();
            HBox buttonBox = new HBox(viewStatisticsButton);
            buttonBox.setAlignment(Pos.CENTER_RIGHT); // Align to right, for example
            buttonBox.setPadding(new javafx.geometry.Insets(10, 0, 10, 0)); // Add some padding
            // Insert before the table or at a specific position if needed
            // For simplicity, adding it as one of the last children.
            // Check if there's already a control HBox, otherwise add new.
            // This part is highly dependent on the actual FXML structure.
            // A common pattern is to have a VBox root, then HBoxes for controls, then the TableView.
            // Let's try to add it to the VBox that contains yearField, termComboBox etc.
            // This requires knowing the FXML structure.
            // For now, let's add it above the table view if its parent is a VBox.
            int tableViewIndex = parentVBox.getChildren().indexOf(dataTableView);
            if (tableViewIndex != -1) {
                parentVBox.getChildren().add(tableViewIndex, buttonBox);
            } else {
                parentVBox.getChildren().add(buttonBox); // Fallback
            }
        } else {
            // Fallback: if parent is not VBox, log or handle differently
            System.out.println("Could not add statistics button dynamically. Parent of TableView is not a VBox.");
            // As a simple fallback, you could try adding it to the root if it's a VBox
            // Or, the user needs to add this button via FXML.
            // For this example, I'll assume the user will integrate it.
            // If you have a specific HBox for buttons, add it there.
            // e.g., if studentComboBox.getParent() is an HBox for controls:
            // ((HBox)studentComboBox.getParent()).getChildren().add(viewStatisticsButton);
        }
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
        dr.add("year", yearField.getText());
        dr.add("semester", termComboBox.getValue()); // getValue() is fine for ComboBox<String>
        var response = HttpRequestUtil.request("/api/me/ScoreList", dr);
        if (response == null || response.getCode() != 0) {
            var msg = response != null ? response.getMsg() : "<UNK>";
            var a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("查询失败");
            a.setHeaderText("查询成绩失败");
            a.setContentText("错误信息: " + msg);
            a.showAndWait();
            return;
        }
        @SuppressWarnings("unchecked") // Suppress cast warning, assuming API contract
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.getData();
        allScores.clear();
        for (Map<String, Object> m : dataList) {
            String sn = (String) m.get("studentNum");
            String name = (String) m.get("studentName");
            String cls = (String) m.get("className");
            String cnum = (String) m.get("courseNum");
            String cname = (String) m.get("courseName");
            String credit = Integer.toString(((Double) m.get("credit")).intValue());
            String tnum = ((Double) m.get("classNumber")).intValue() + "";
            String year = (String) m.get("year");
            String term = (String) m.get("semester");
            String mark = (String) m.get("mark");

            allScores.add(new ScoreRecord(sn, name, cls, cnum, cname, credit, tnum, year, term, mark));
        }
        List<ScoreRecord> filtered = allScores.stream().filter(s ->
                (stu == null || s.getStudentName().equals(stu.getStudentName())) &&
                        (course == null || s.getCourseNum().equals(course.getCourseNum()))

        ).toList();
        dataTableView.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void onViewStatisticsButtonClick(ActionEvent event) {
        if (allScores.isEmpty()) {
            showAlert("统计数据", "没有可供统计的成绩记录。", Alert.AlertType.INFORMATION);
            return;
        }

        List<NumericScoreEntry> numericScores = new ArrayList<>();
        int skippedRecords = 0;
        for (ScoreRecord sr : allScores) {
            if (sr.getMark() == null || sr.getMark().trim().isEmpty()) {
                skippedRecords++;
                continue;
            }
            try {
                numericScores.add(new NumericScoreEntry(sr));
            } catch (NumberFormatException e) {
                skippedRecords++;
            }
        }

        if (numericScores.isEmpty()) {
            showAlert("统计数据", "没有有效的数字成绩记录可供统计。" + (skippedRecords > 0 ? " " + skippedRecords + " 条记录因成绩为空或非数字而被跳过。" : ""), Alert.AlertType.INFORMATION);
            return;
        }

        StringBuilder statsBuilder = new StringBuilder();
        if (skippedRecords > 0) {
            statsBuilder.append(skippedRecords).append(" 条记录因成绩为空或非数字而被跳过。\n\n");
        }

        // 1. Overall Weighted Average (Credit-based)
        statsBuilder.append("--- 全局统计 ---\n");
        double totalWeightedSum = 0;
        double totalCredits = 0;
        for (NumericScoreEntry nse : numericScores) {
            totalWeightedSum += nse.getMark() * nse.getCredit();
            totalCredits += nse.getCredit();
        }
        if (totalCredits > 0) {
            statsBuilder.append("全部成绩加权平均分: ").append(df.format(totalWeightedSum / totalCredits)).append("\n");
        } else {
            statsBuilder.append("全部成绩加权平均分: N/A (无有效学分)\n");
        }
        // 合并相同课程并且得到每门课的最大值求平均数
        Map<String, OptionalDouble> maxScoresByCourse = numericScores.stream()
            .collect(Collectors.groupingBy(
            NumericScoreEntry::getCourseNum,
            Collectors.mapping(
                NumericScoreEntry::getMark,
                Collectors.reducing(
                OptionalDouble.empty(),
                score -> OptionalDouble.of(score),
                (opt1, opt2) -> {
                    if (opt1.isEmpty()) return opt2;
                    if (opt2.isEmpty()) return opt1;
                    return OptionalDouble.of(Math.max(opt1.getAsDouble(), opt2.getAsDouble()));
                }
                )
            )
            ));
        
        double maxScoresSum = 0;
        int validCourseCount = 0;
        for (OptionalDouble maxScore : maxScoresByCourse.values()) {
            if (maxScore.isPresent()) {
            maxScoresSum += maxScore.getAsDouble();
            validCourseCount++;
            }
        }
        
        if (validCourseCount > 0) {
            statsBuilder.append("各课程最高分平均数: ").append(df.format(maxScoresSum / validCourseCount)).append("\n");
        } else {
            statsBuilder.append("各课程最高分平均数: N/A (无有效课程)\n");
        }

        // 及格率统计
        long passingCount = numericScores.stream()
            .mapToLong(nse -> nse.getMark() >= PASSING_GRADE ? 1 : 0)
            .sum();
        double passingRate = numericScores.size() > 0 ? (double) passingCount / numericScores.size() * 100 : 0;
        statsBuilder.append("及格率: ").append(df.format(passingRate)).append("% (")
            .append(passingCount).append("/").append(numericScores.size()).append(")\n");

        statsBuilder.append("\n");

        // Group scores by course
        Map<String, List<NumericScoreEntry>> scoresByCourse = numericScores.stream()
                .collect(Collectors.groupingBy(NumericScoreEntry::getCourseNum));

        // 总学分统计（如果用户是学生显示）
        if (!isTeacher) {
            // 对于学生，计算已获得学分（及格课程的学分）
            double earnedCredits = numericScores.stream()
            .filter(nse -> nse.getMark() >= PASSING_GRADE)
            .collect(Collectors.groupingBy(NumericScoreEntry::getCourseNum))
            .values()
            .stream()
            .mapToDouble(courseScores -> {
                // 取该课程的最高分记录的学分
                return courseScores.stream()
                .max(Comparator.comparingDouble(NumericScoreEntry::getMark))
                .map(NumericScoreEntry::getCredit)
                .orElse(0.0);
            })
            .sum();
            
            totalCredits = scoresByCourse.values()
            .stream()
            .mapToDouble(courseScores -> {
                // 取该课程任意一条记录的学分（假设同一课程学分相同）
                return courseScores.get(0).getCredit();
            })
            .sum();
            
            statsBuilder.append("已获得学分: ").append(df.format(earnedCredits)).append("\n");
            statsBuilder.append("总学分: ").append(df.format(totalCredits)).append("\n");
        }

        statsBuilder.append("--- 按课程统计 ---\n");

        for (Map.Entry<String, List<NumericScoreEntry>> entry : scoresByCourse.entrySet()) {
            String courseNum = entry.getKey();
            List<NumericScoreEntry> courseScores = entry.getValue();
            String courseName = courseScores.get(0).getCourseName(); // Assuming all entries for a courseNum have the same name

            statsBuilder.append("课程: ").append(courseName).append(" (").append(courseNum).append(")\n");

            // 2. Per-Course Weighted Average (Highest Score)
            Optional<NumericScoreEntry> highestScoreEntry = courseScores.stream()
                    .max(Comparator.comparingDouble(NumericScoreEntry::getMark));
            if (highestScoreEntry.isPresent()) {
                double highestMark = highestScoreEntry.get().getMark();
                double creditForHighest = highestScoreEntry.get().getCredit(); // Assuming credit is same for the course
                // If credit varies per record for the same course, this logic might need adjustment.
                // For now, using the credit of the record that has the highest score.
                // A more robust way might be to average credits or use a predefined course credit.
                // The current ScoreRecord structure implies credit is per record.
                statsBuilder.append("  - 最高分加权平均 (使用该课程最高分): ");
                // This interpretation of "Per-Course Weighted Average (Highest Score)" might be tricky.
                // If it means "use the highest score for this course, and its credit, as one entry in a larger average",
                // that's different. If it means "the highest score itself weighted by its credit", it's just highestMark * credit.
                // Assuming it's about the highest score for *this* course:
                statsBuilder.append(df.format(highestMark)); // Or highestMark * creditForHighest / creditForHighest
                statsBuilder.append(" (最高分: ").append(df.format(highestMark)).append(", 学分: ").append(df.format(creditForHighest)).append(")\n");
            } else {
                statsBuilder.append("  - 最高分加权平均: N/A\n");
            }


            // 3. Per-Course Weighted Average (All Records)
            double courseWeightedSum = 0;
            double courseTotalCredits = 0;
            for (NumericScoreEntry nse : courseScores) {
                courseWeightedSum += nse.getMark() * nse.getCredit();
                courseTotalCredits += nse.getCredit();
            }
            if (courseTotalCredits > 0) {
                statsBuilder.append("  - 所有记录加权平均分: ").append(df.format(courseWeightedSum / courseTotalCredits)).append("\n");
            } else {
                statsBuilder.append("  - 所有记录加权平均分: N/A\n");
            }

            // 4. Per-Course Highest and Lowest Score
            OptionalDouble maxMarkOpt = courseScores.stream().mapToDouble(NumericScoreEntry::getMark).max();
            OptionalDouble minMarkOpt = courseScores.stream().mapToDouble(NumericScoreEntry::getMark).min();
            statsBuilder.append("  - 最高分: ").append(maxMarkOpt.isPresent() ? df.format(maxMarkOpt.getAsDouble()) : "N/A").append("\n");
            statsBuilder.append("  - 最低分: ").append(minMarkOpt.isPresent() ? df.format(minMarkOpt.getAsDouble()) : "N/A").append("\n");

            // For "Courses with Only Failing Records"
            boolean allFailingInCourse = !courseScores.isEmpty() && courseScores.stream().allMatch(s -> s.getMark() < PASSING_GRADE);
            if (allFailingInCourse) {
                 statsBuilder.append("  - !!! 该课程所有记录均不及格 !!!\n");
            }
            statsBuilder.append("\n");
        }


        // 5. All Failing Records
        statsBuilder.append("--- 不及格记录 (低于 ").append(PASSING_GRADE).append(" 分) ---\n");
        List<NumericScoreEntry> failingScores = numericScores.stream()
                .filter(s -> s.getMark() < PASSING_GRADE)
                .sorted(Comparator.comparing(NumericScoreEntry::getStudentName).thenComparing(NumericScoreEntry::getCourseName))
                .collect(Collectors.toList());
        if (failingScores.isEmpty()) {
            statsBuilder.append("无不及格记录。\n");
        } else {
            for (NumericScoreEntry fs : failingScores) {
                statsBuilder.append(fs.toString()).append("\n");
            }
        }
        statsBuilder.append("\n");

        // 6. Courses with Only Failing Records
        statsBuilder.append("--- 所有记录均不及格的课程 ---\n");
        List<String> coursesWithOnlyFailures = scoresByCourse.entrySet().stream()
                .filter(entry -> {
                    List<NumericScoreEntry> cs = entry.getValue();
                    return !cs.isEmpty() && cs.stream().allMatch(s -> s.getMark() < PASSING_GRADE);
                })
                .map(entry -> entry.getValue().get(0).getCourseName() + " (" + entry.getKey() + ")")
                .sorted()
                .collect(Collectors.toList());

        if (coursesWithOnlyFailures.isEmpty()) {
            statsBuilder.append("无此类课程。\n");
        } else {
            for (String courseDesc : coursesWithOnlyFailures) {
                statsBuilder.append(courseDesc).append("\n");
            }
        }



        showStatisticsDialog(statsBuilder.toString());
    }

    private void showStatisticsDialog(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("成绩统计数据");
        alert.setHeaderText("详细统计信息如下:");

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(textArea, Priority.ALWAYS);
        HBox.setHgrow(textArea, Priority.ALWAYS);


        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        // Ensure the dialog is large enough
        alert.getDialogPane().setPrefSize(600, 700);


        alert.getDialogPane().setContent(scrollPane);
        alert.setResizable(true);
        alert.showAndWait();
    }

     private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // 重置按钮
    @FXML
    public void onResetButtonClick(ActionEvent event) {
        // 这个按钮改成了重置按钮
        studentComboBox.setValue(null);
        courseComboBox.setValue(null);
        termComboBox.setValue(null);
        yearField.setText(null);
    }

    // 修改成绩
    @FXML
    public void onEditButtonClick(ActionEvent event) {
        ScoreRecord selected = dataTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            var a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("修改失败");
            a.setHeaderText("没有选中成绩");
            a.setContentText("请先选择一条成绩记录进行修改");
            a.showAndWait();
            return;
        }
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
            // 重置上方筛选器
            studentComboBox.setValue(null);
            courseComboBox.setValue(null);
            termComboBox.setValue(null);
            yearField.setText(null);
            // 刷新表格
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
        dr.add("semester", termComboBox.getValue());
        dr.add("year", yearField.getText());

        // courses list
//        dr = new DataRequest();
        allCourses.clear();
        var cl = HttpRequestUtil.request("/api/me/CourseList", dr);

        if (cl != null && cl.getCode() == 0) {
            @SuppressWarnings("unchecked") // Suppress cast warning
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) cl.getData();

            for (Map<String, Object> m : dataList) {
                String num = (String) m.get("courseNum");
                String name = (String) m.get("courseName");
                int credit = ((Double) m.get("credit")).intValue();
                allCourses.add(new Course(num, name, credit));

            }

        }
        var s = HttpRequestUtil.request("/api/me/StudentList", dr);

        if (s != null && s.getCode() == 0) {
            @SuppressWarnings("unchecked") // Suppress cast warning
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