package com.teach.javafx.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GradeEntry {
    private final StringProperty studentName;
    private final StringProperty studentId;
    private final StringProperty classId;
    private final StringProperty courseName;
    private final ObjectProperty<Integer> score; // 使用ObjectProperty<Integer>以支持null

    public GradeEntry(String studentName, String studentId, String classId, String courseName, Integer score) {
        this.studentName = new SimpleStringProperty(studentName);
        this.studentId = new SimpleStringProperty(studentId);
        this.classId = new SimpleStringProperty(classId);
        this.courseName = new SimpleStringProperty(courseName);
        this.score = new SimpleObjectProperty<>(score);
    }

    // 学生姓名
    public String getStudentName() {
        return studentName.get();
    }
    public StringProperty studentNameProperty() {
        return studentName;
    }
    public void setStudentName(String studentName) {
        this.studentName.set(studentName);
    }

    // 学号
    public String getStudentId() {
        return studentId.get();
    }
    public StringProperty studentIdProperty() {
        return studentId;
    }
    public void setStudentId(String studentId) {
        this.studentId.set(studentId);
    }

    // 授课班号
    public String getClassId() {
        return classId.get();
    }
    public StringProperty classIdProperty() {
        return classId;
    }
    public void setClassId(String classId) {
        this.classId.set(classId);
    }

    // 课程名称
    public String getCourseName() {
        return courseName.get();
    }
    public StringProperty courseNameProperty() {
        return courseName;
    }
    public void setCourseName(String courseName) {
        this.courseName.set(courseName);
    }

    // 成绩
    public Integer getScore() {
        return score.get();
    }
    public ObjectProperty<Integer> scoreProperty() {
        return score;
    }
    public void setScore(Integer score) {
        this.score.set(score);
    }
}