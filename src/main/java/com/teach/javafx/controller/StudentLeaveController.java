package com.teach.javafx.controller;

        import com.teach.javafx.controller.base.MessageDialog;
        import com.teach.javafx.controller.base.ToolController;
        import com.teach.javafx.request.DataRequest;
        import com.teach.javafx.request.DataResponse;
        import com.teach.javafx.request.HttpRequestUtil;
        import javafx.collections.FXCollections;
        import javafx.collections.ObservableList;
        import javafx.fxml.FXML;
        import javafx.scene.control.*;
        import javafx.scene.control.cell.MapValueFactory;
        import javafx.scene.control.DatePicker;

        import java.time.LocalDate;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.Map;

        public class StudentLeaveController extends ToolController {
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
            private TextField searchStudentNameField;

            @FXML
            private DatePicker startDatePicker;
            @FXML
            private DatePicker endDatePicker;

            @FXML
            private RadioButton pendingRadio;
            @FXML
            private RadioButton approvedRadio;
            @FXML
            private RadioButton rejectedRadio;
            @FXML
            private ToggleGroup approvalGroup;

            @FXML
            private Label formTitleLabel;

            @FXML
            private Label modeIndicator;

            @FXML
            private Pagination leavePagination;

            private final int ROWS_PER_PAGE = 10;
            private ObservableList<Map> masterLeaveList = FXCollections.observableArrayList(); // 存储所有数据
            private ObservableList<Map> leaveList = FXCollections.observableArrayList();
            private Map<String, Object> currentLeave; // 当前选中的请假记录

            @FXML
            public void initialize() {
                configureTableColumns();
                setupTableRowFactory();
                setupPagination();
                setupTableSelectionListener();

                pendingRadio.setUserData("");
                approvedRadio.setUserData("1");
                rejectedRadio.setUserData("0");

                // 初始化表单模式
                updateFormMode("view");

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
                            if ("1".equals(item) || "1.0".equals(item) || "true".equals(item)) {
                                setText("已批准");
                                getStyleClass().add("status-approved");
                            } else if ("0".equals(item) || "0.0".equals(item) || "false".equals(item)) {
                                setText("已拒绝");
                                getStyleClass().add("status-rejected");
                            } else {
                                setText("待审批");
                                getStyleClass().add("status-pending");
                            }
                        }
                    }
                });

                leaveTableView.setItems(leaveList);
            }

            private void setupTableRowFactory() {
                leaveTableView.setRowFactory(tv -> new TableRow<Map>() {
                    @Override
                    protected void updateItem(Map item, boolean empty) {
                        super.updateItem(item, empty);

                        getStyleClass().removeAll("row-approved", "row-rejected", "row-pending");

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

            private void setupTableSelectionListener() {
                // 设置表格行点击事件，用于表单编辑
                leaveTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        currentLeave = newSelection;
                        populateForm(newSelection);
                        updateFormMode("edit");
                    }
                });
            }

            private void updateFormMode(String mode) {
                // 清除之前的模式样式
                modeIndicator.getStyleClass().removeAll("mode-add", "mode-edit", "mode-view");
                formTitleLabel.getStyleClass().remove("highlight-title");

                switch (mode) {
                    case "add":
                        formTitleLabel.setText("新增请假申请");
                        formTitleLabel.getStyleClass().add("highlight-title");
                        modeIndicator.setText("当前模式：新增");
                        modeIndicator.getStyleClass().add("mode-add");
                        break;
                    case "edit":
                        formTitleLabel.setText("编辑请假申请");
                        modeIndicator.setText("当前模式：编辑");
                        modeIndicator.getStyleClass().add("mode-edit");
                        break;
                    case "view":
                    default:
                        formTitleLabel.setText("请假申请详情");
                        modeIndicator.setText("当前模式：查看");
                        modeIndicator.getStyleClass().add("mode-view");
                        break;
                }
            }

            private void setupPagination() {
                // 设置分页控件的页面切换事件
                leavePagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
                    updateTableData(newIndex.intValue());
                });
            }
            @FXML
            public void refreshData() {
                loadLeaveList();
            }
            private void loadLeaveList() {
                DataRequest req = new DataRequest();
                req.add("studentName", "");
                DataResponse res = HttpRequestUtil.request("/api/studentLeave/getLeaveList", req);
                if (res != null && res.getCode() == 0) {
                    ArrayList<Map> allLeaves = (ArrayList<Map>) res.getData();

                    // 处理可能的空值
                    for (Map leave : allLeaves) {
                        if (leave.get("isApproved") == null) {
                            leave.put("isApproved", "");
                        }
                    }

                    // 存储所有数据到主列表
                    masterLeaveList.setAll(FXCollections.observableArrayList(allLeaves));

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
            protected void onAddButtonClick() {
                clearForm();
                currentLeave = null; // 新增模式
                updateFormMode("add");
            }

            @FXML
            protected void onDeleteButtonClick() {
                Map<String, Object> selectedItem = leaveTableView.getSelectionModel().getSelectedItem();
                if (selectedItem == null) {
                    MessageDialog.showDialog("请选择要删除的记录");
                    return;
                }

                // 确认删除
                if (!MessageDialog.showConfirmDialog("确认删除", "确定要删除这条请假记录吗？")) {
                    return;
                }

                DataRequest req = new DataRequest();
                req.add("leaveId", selectedItem.get("id"));
                DataResponse res = HttpRequestUtil.request("/api/studentLeave/deleteLeave", req);
                if (res != null && res.getCode() == 0) {
                    MessageDialog.showDialog("删除成功！");
                    loadLeaveList();
                    clearForm();
                } else {
                    MessageDialog.showDialog("删除失败：" + (res != null ? res.getMsg() : ""));
                }
            }

            @FXML
            protected void onQueryButtonClick() {
                loadLeaveList();
            }

            @FXML
            protected void onSearchByNameButtonClick() {
                String studentName = searchStudentNameField.getText().trim();
                DataRequest req = new DataRequest();
                req.add("studentName", studentName);
                DataResponse res = HttpRequestUtil.request("/api/studentLeave/getLeaveList", req);
                if (res != null && res.getCode() == 0) {
                    ArrayList<Map> searchResults = (ArrayList<Map>) res.getData();

                    // 处理可能的空值
                    for (Map leave : searchResults) {
                        if (leave.get("isApproved") == null) {
                            leave.put("isApproved", "");
                        }
                    }

                    // 存储查询结果到主数据列表
                    masterLeaveList.setAll(FXCollections.observableArrayList(searchResults));

                    // 计算总页数
                    int pageCount = (masterLeaveList.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
                    pageCount = pageCount == 0 ? 1 : pageCount; // 至少1页
                    leavePagination.setPageCount(pageCount);

                    // 更新表格数据，显示第一页
                    updateTableData(0);
                    leavePagination.setCurrentPageIndex(0);
                } else {
                    MessageDialog.showDialog("查询失败：" + (res != null ? res.getMsg() : ""));
                }
            }

            @FXML
            protected void onSaveButtonClick() {
                if (!validateForm()) {
                    return;
                }

                String leaveId = currentLeave != null ? String.valueOf(currentLeave.get("id")) : null;

                Map<String, Object> form = new HashMap<>();
                form.put("leaveId", leaveId); // 更新时需要传递ID
                form.put("studentId", studentIdField.getText().trim());
                form.put("studentName", studentNameField.getText().trim());
                form.put("college", collegeField.getText().trim());
                form.put("startDate", startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "");
                form.put("endDate", endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "");
                form.put("reason", reasonField.getText().trim());
                form.put("approverId", approverIdField.getText().trim());

                // 获取审批状态
                Toggle selectedToggle = approvalGroup.getSelectedToggle();
                String approvalStatus = selectedToggle != null ?
                                        (String) selectedToggle.getUserData() : "";
                form.put("isApproved", approvalStatus);

                DataRequest req = new DataRequest();
                req.add("form", form);
                DataResponse res = HttpRequestUtil.request("/api/studentLeave/saveLeave", req);
                if (res != null && res.getCode() == 0) {
                    MessageDialog.showDialog("保存成功！");
                    loadLeaveList();
                    clearForm();
                } else {
                    MessageDialog.showDialog("保存失败：" + (res != null ? res.getMsg() : "未知错误"));
                }
            }

            private boolean validateForm() {
                // 验证必填字段
                if (studentIdField.getText().trim().isEmpty() ||
                    studentNameField.getText().trim().isEmpty() ||
                    collegeField.getText().trim().isEmpty() ||
                    approverIdField.getText().trim().isEmpty() ||
                    reasonField.getText().trim().isEmpty() ||
                    startDatePicker.getValue() == null ||
                    endDatePicker.getValue() == null) {

                    MessageDialog.showDialog("请填写所有必填字段！");
                    return false;
                }

                // 验证日期
                if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                    MessageDialog.showDialog("结束日期不能早于开始日期！");
                    return false;
                }

                return true;
            }

            private void populateForm(Map item) {
                studentIdField.setText(String.valueOf(item.get("studentId")).replace(".0", ""));
                studentNameField.setText(String.valueOf(item.get("studentName")));
                collegeField.setText(String.valueOf(item.get("college")));
                reasonField.setText(String.valueOf(item.get("reason")));
                approverIdField.setText(String.valueOf(item.get("approverId")).replace(".0", ""));

                // 处理日期
                try {
                    String startDate = String.valueOf(item.get("startDate"));
                    String endDate = String.valueOf(item.get("endDate"));
                    if (startDate != null && !startDate.equals("null")) {
                        startDatePicker.setValue(LocalDate.parse(startDate));
                    } else {
                        startDatePicker.setValue(null);
                    }

                    if (endDate != null && !endDate.equals("null")) {
                        endDatePicker.setValue(LocalDate.parse(endDate));
                    } else {
                        endDatePicker.setValue(null);
                    }
                } catch (Exception e) {
                    System.out.println("日期解析错误: " + e.getMessage());
                }

                // 设置审批状态
                String status = String.valueOf(item.get("isApproved"));
                if ("1".equals(status) || "1.0".equals(status) || "true".equals(status)) {
                    approvedRadio.setSelected(true);
                } else if ("0".equals(status) || "0.0".equals(status) || "false".equals(status)) {
                    rejectedRadio.setSelected(true);
                } else {
                    pendingRadio.setSelected(true);
                }
            }

            @FXML
            protected void clearForm() {
                studentIdField.clear();
                studentNameField.clear();
                collegeField.clear();
                startDatePicker.setValue(LocalDate.now());
                endDatePicker.setValue(LocalDate.now().plusDays(1));
                reasonField.clear();
                approverIdField.clear();
                pendingRadio.setSelected(true); // 默认为待审批

                // 清除当前选中项
                currentLeave = null;
                leaveTableView.getSelectionModel().clearSelection();

                // 更新表单模式
                updateFormMode("view");
            }
        }