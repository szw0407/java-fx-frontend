package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.JwtResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableRow;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StudentLeaveStudentController extends ToolController {
    @FXML
    private TableView<Map> leaveTableView;

    @FXML
    private TableColumn<Map, String> idColumn;
    @FXML
    private TableColumn<Map, String> studentIdColumn;
    @FXML
    private TableColumn<Map, String> studentNameColumn;
    @FXML
    private TableColumn<Map, String> collegeColumn;
    @FXML
    private TableColumn<Map, String> startDateColumn;
    @FXML
    private TableColumn<Map, String> endDateColumn;
    @FXML
    private TableColumn<Map, String> reasonColumn;
    @FXML
    private TableColumn<Map, String> approverIdColumn;
    @FXML
    private TableColumn<Map, String> isApprovedColumn;

    @FXML
    private TextField studentIdField;
    @FXML
    private TextField studentNameField;
    @FXML
    private TextField collegeField;
    @FXML
    private TextArea reasonField;
    @FXML
    private TextField approverIdField;

    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Button submitButton;
    @FXML
    private Button clearButton;

    @FXML
    private Pagination leavePagination;

    private final int ROWS_PER_PAGE = 10;
    private final ObservableList<Map> masterLeaveList = FXCollections.observableArrayList(); // 存储所有数据
    private final ObservableList<Map> leaveList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configureTableColumns();
        setupTableRowFactory();
        setupFormValidation();
        setupDatePickers();
        setupDatePickerListeners();
        setupPagination();

        // 自动填充学生ID
        String currentStudentId = getCurrentStudentId();
        if (currentStudentId != null) {
            studentIdField.setText(currentStudentId);
            studentIdField.setEditable(false); // 锁定学生ID不可编辑
        }

        loadLeaveList();
    }

    private void configureTableColumns() {
        idColumn.setCellValueFactory(new MapValueFactory<>("id"));
        studentIdColumn.setCellValueFactory(new MapValueFactory<>("studentId"));
        studentNameColumn.setCellValueFactory(new MapValueFactory<>("studentName"));
        collegeColumn.setCellValueFactory(new MapValueFactory<>("college"));
        startDateColumn.setCellValueFactory(new MapValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new MapValueFactory<>("endDate"));
        reasonColumn.setCellValueFactory(new MapValueFactory<>("reason"));
        approverIdColumn.setCellValueFactory(new MapValueFactory<>("approverId"));
        isApprovedColumn.setCellValueFactory(new MapValueFactory<>("isApproved"));

        // 设置列宽自动调整
        leaveTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 为审批状态列添加自定义单元格工厂
        isApprovedColumn.setCellFactory(column -> new TableCell<Map, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("status-pending", "status-approved", "status-rejected");
                } else {
                    // 直接使用后端返回的状态文本
                    String status = String.valueOf(item);
                    if ("同意".equals(status)) {
                        setText("已同意");
                        getStyleClass().removeAll("status-pending", "status-rejected");
                        getStyleClass().add("status-approved");
                    } else if ("拒绝".equals(status)) {
                        setText("已拒绝");
                        getStyleClass().removeAll("status-pending", "status-approved");
                        getStyleClass().add("status-rejected");
                    } else {
                        setText("未审批");
                        getStyleClass().removeAll("status-approved", "status-rejected");
                        getStyleClass().add("status-pending");
                    }
                }
            }
        });

        leaveTableView.setItems(leaveList);
    }

    private void setupTableRowFactory() {
        leaveTableView.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Map item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null) {
                    // 为不同状态的行添加不同样式
                    String status = String.valueOf(item.get("isApproved"));
                    if ("1".equals(status) || "1.0".equals(status) || "true".equals(status)) {
                        getStyleClass().add("row-approved");
                    } else if ("0".equals(status) || "0.0".equals(status) || "false".equals(status)) {
                        getStyleClass().add("row-rejected");
                    } else {
                        getStyleClass().add("row-pending");
                    }
                }
            }
        });
    }

    private void setupDatePickerListeners() {
        // 监听开始日期变化
        startDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            // 如果结束日期早于新的开始日期，调整结束日期
            if (endDatePicker.getValue() != null && endDatePicker.getValue().isBefore(newValue)) {
                endDatePicker.setValue(newValue);
                MessageDialog.showDialog("结束日期不能早于开始日期，已自动调整");
            }
        });

        // 监听结束日期变化
        endDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            // 如果新的结束日期早于开始日期，重置为原来的值或设为开始日期
            if (startDatePicker.getValue() != null && newValue != null && newValue.isBefore(startDatePicker.getValue())) {
                if (oldValue != null && !oldValue.isBefore(startDatePicker.getValue())) {
                    endDatePicker.setValue(oldValue);
                } else {
                    endDatePicker.setValue(startDatePicker.getValue());
                }
                MessageDialog.showDialog("结束日期不能早于开始日期");
            }
        });
    }

    private void setupFormValidation() {
        // 添加实时表单验证
        submitButton.disableProperty().bind(
            studentIdField.textProperty().isEmpty()
            .or(studentNameField.textProperty().isEmpty())
            .or(collegeField.textProperty().isEmpty())
            .or(approverIdField.textProperty().isEmpty())
            .or(reasonField.textProperty().isEmpty())
            .or(startDatePicker.valueProperty().isNull())
            .or(endDatePicker.valueProperty().isNull())
        );
    }

    private void setupDatePickers() {
        // 设置日期选择器默认值和约束
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));

        // 确保结束日期不早于开始日期
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && endDatePicker.getValue() != null &&
                endDatePicker.getValue().isBefore(newVal)) {
                endDatePicker.setValue(newVal.plusDays(1));
            }
        });
    }

    private void setupPagination() {
        // 设置分页控件的页面切换事件
        leavePagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            updateTableData(newIndex.intValue());
        });
    }

    private void loadLeaveList() {
        DataRequest req = new DataRequest();
        req.add("studentName", "");
        DataResponse res = HttpRequestUtil.request("/api/studentLeave/getLeaveList", req);
        if (res != null && res.getCode() == 0) {
            String currentStudentId = getCurrentStudentId();
            ArrayList<Map> filteredList = new ArrayList<>();
            for (var record : (ArrayList<Map>) res.getData()) {
                // 将 record.get("studentId") 转换为字符串后再比较
                if (currentStudentId.equals(String.valueOf(record.get("studentId")).replace(".0", ""))) {
                    // 检查 isApproved 字段是否为 null
                    record.putIfAbsent("isApproved", ""); // 设置为空字符串或其他默认值
                    filteredList.add(record);
                }
            }

            // 存储所有数据到主列表
            masterLeaveList.setAll(FXCollections.observableArrayList(filteredList));

            // 计算总页数
            int pageCount = (masterLeaveList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
            pageCount = pageCount == 0 ? 1 : pageCount; // 至少1页
            leavePagination.setPageCount(pageCount);

            // 更新表格数据，显示第一页
            updateTableData(0);
            leavePagination.setCurrentPageIndex(0);
        } else {
            MessageDialog.showDialog("加载数据失败：" + (res != null ? res.getMsg() : ""));
        }
    }

    private void updateTableData(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, masterLeaveList.size());

        // 如果数据集为空，处理边界情况
        if (masterLeaveList.isEmpty() || fromIndex >= masterLeaveList.size()) {
            leaveList.clear();
            return;
        }

        // 取出当前页的数据
        leaveList.setAll(FXCollections.observableArrayList(
                masterLeaveList.subList(fromIndex, toIndex)));
    }

    @FXML
    public void refreshData() {
        loadLeaveList();
    }

    @FXML
    public void onSaveButtonClick() {
        if (!validateForm()) {
            return;
        }

        String currentStudentId = getCurrentStudentId();
        String enteredStudentId = studentIdField.getText().trim();

        // 检查申请者 ID 是否与当前登录用户 ID 一致
        if (!currentStudentId.equals(enteredStudentId)) {
            MessageDialog.showDialog("申请者 ID 与当前登录用户不一致，无法提交申请！"+'\n'+"不要给别人请假！");
            return;
        }

        Map<String, Object> form = new HashMap<>();
        form.put("leaveId", null); // 新增记录
        form.put("studentId", currentStudentId);
        form.put("studentName", studentNameField.getText().trim());
        form.put("college", collegeField.getText().trim());
        form.put("startDate", startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "");
        form.put("endDate", endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "");
        form.put("reason", reasonField.getText().trim());
        form.put("approverId", approverIdField.getText().trim());
        form.put("isApproved", null); // 默认未审批

        DataRequest req = new DataRequest();
        req.add("form", form);
        DataResponse res = HttpRequestUtil.request("/api/studentLeave/saveLeave", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("请假申请提交成功！");
            loadLeaveList();
            clearForm();
        } else {
            MessageDialog.showDialog("申请失败：" + (res != null ? res.getMsg() : "res==null"));
        }
    }

    private boolean validateForm() {
        // 验证日期
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                MessageDialog.showDialog("结束日期不能早于开始日期！");
                return false;
            }
        }

        // 验证请假理由长度
        if (reasonField.getText().length() < 5) {
            MessageDialog.showDialog("请假理由太短，请详细描述请假原因");
            return false;
        }

        return true;
    }

    private String getCurrentStudentId() {
        JwtResponse jwtResponse = AppStore.getJwt();
        if (jwtResponse != null) {
            return String.valueOf(jwtResponse.getId());
        }
        System.out.println("获取当前学生ID失败");
        return null;
    }

    @FXML
    public void clearForm() {
        // 保留学生ID
        String currentId = studentIdField.getText();

        studentNameField.clear();
        collegeField.clear();
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));
        reasonField.clear();
        approverIdField.clear();

        // 恢复学生ID
        studentIdField.setText(currentId);
    }
}