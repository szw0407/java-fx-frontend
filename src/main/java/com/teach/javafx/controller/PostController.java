package com.teach.javafx.controller;

import com.teach.javafx.AppStore;
import com.teach.javafx.controller.base.MessageDialog;
import com.teach.javafx.controller.base.ToolController;
import com.teach.javafx.request.DataRequest;
import com.teach.javafx.request.DataResponse;
import com.teach.javafx.request.HttpRequestUtil;
import com.teach.javafx.request.JwtResponse;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostController extends ToolController {
    @FXML
    public TableColumn<Map, String> userNameColumn;
    @FXML
    private Label titleLabel;
    @FXML
    private Button queryButton;
    @FXML
    private Button addButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button saveButton;
    @FXML
    private TableView<Map> dataTableView;
//    @FXML
//    private TableColumn<Map, String> postIdColumn;
    @FXML
    private TableColumn<Map, String> titleColumn;
    @FXML
    private TableColumn<Map, String> contentColumn;
    @FXML
    private TableColumn<Map, String> createTimeColumn;
    @FXML
    private TableView<Map> commentTableView;
    @FXML
    private TableColumn<Map, String> commentIdColumn;
    @FXML
    private TableColumn<Map, String> commentContentColumn;
    @FXML
    private TableColumn<Map, String> commentUserColumn;
    @FXML
    private TableColumn<Map, String> commentTimeColumn;
    @FXML
    private TextField commentField;

    private List<Map> commentList = new ArrayList<>();
    private ObservableList<Map> commentObservableList = FXCollections.observableArrayList();
    @FXML
    private TextField searchTextField;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentField;
    @FXML
    private TextField userIdField;

    private Integer postId = null;
    private ArrayList<Map> postList = new ArrayList<>();
    private ObservableList<Map> observableList = FXCollections.observableArrayList();

    private void setTableViewData() {
        observableList.clear();
        observableList.addAll(FXCollections.observableArrayList(postList));
        dataTableView.setItems(observableList);
    }

    private void setCommentTableViewData() {
        commentObservableList.clear();
        commentObservableList.addAll(FXCollections.observableArrayList(commentList));
        commentTableView.setItems(commentObservableList);
    }


    @FXML
    public void initialize() {
        // 设置列绑定
        titleColumn.setCellValueFactory(new MapValueFactory<>("title"));
        userNameColumn.setCellValueFactory(new MapValueFactory<>("userName"));
        createTimeColumn.setCellValueFactory(new MapValueFactory<>("createTime"));

        // 新增是否置顶列
        TableColumn<Map, String> isTopColumn = new TableColumn<>("是否置顶");
        isTopColumn.setCellValueFactory(cellData -> {
            Boolean isTop = (Boolean) cellData.getValue().get("isTop");
            return new SimpleStringProperty(isTop != null && isTop ? "是" : "否");
        });
        dataTableView.getColumns().add(isTopColumn);

        // 设置行工厂，合并背景颜色和双击事件逻辑
        dataTableView.setRowFactory(tv -> {
            TableRow<Map> row = new TableRow<>() {
                @Override
                protected void updateItem(Map item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setStyle(""); // 清除样式
                    } else {
                        Boolean isTop = (Boolean) item.get("isTop");
                        if (Boolean.TRUE.equals(isTop)) {
                            setStyle("-fx-background-color: #ffeb3b;"); // 设置置顶帖子的背景颜色
                        } else {
                            setStyle(""); // 非置顶帖子清除样式
                        }
                    }
                }
            };

            // 添加双击事件
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Map<String, Object> rowData = row.getItem();
                    showPostDetails(rowData); // 调用显示帖子详情的方法
                }
            });

            return row;
        });

        // 加载帖子数据并排序
        DataRequest req = new DataRequest();
        req.add("searchText", "");
        DataResponse res = HttpRequestUtil.request("/api/post/getPostList", req);
        if (res != null && res.getCode() == 0) {
            postList = (ArrayList<Map>) res.getData();
            // 按 isTop 字段排序，置顶的帖子排在最前面
            postList.sort((a, b) -> {
                Boolean isTopA = (Boolean) a.get("isTop");
                Boolean isTopB = (Boolean) b.get("isTop");
                return Boolean.compare(isTopB != null && isTopB, isTopA != null && isTopA);
            });
        }
        // 设置用户ID输入栏默认值和可编辑性
        String currentUserId = getCurrentUserId();
        String currentUserName = AppStore.getJwt().getUsername();

        if (!"admin".equals(currentUserName)) {
            userIdField.setText(currentUserId); // 默认填入当前用户ID
            userIdField.setEditable(false); // 设置为不可编辑
        } else {
            userIdField.setEditable(true); // 管理员可编辑
        }
        setTableViewData();
    }

    private void showPostDetails(Map<String, Object> post) {
        String title = (String) post.get("title");
        String content = (String) post.get("content");

        DataRequest req = new DataRequest();
        req.add("postId", post.get("postId"));
        DataResponse res = HttpRequestUtil.request("/api/post/getComments", req);
        List<Map<String, Object>> comments = new ArrayList<>();
        if (res != null && res.getCode() == 0) {
            comments = (List<Map<String, Object>>) res.getData();
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("帖子详情");
        dialog.getDialogPane().setPrefSize(600, 600);

        VBox dialogContent = new VBox(15);
        dialogContent.setPadding(new Insets(20));

        Label titleLabel = new Label("标题：");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label titleValue = new Label(title);
        titleValue.setStyle("-fx-font-size: 14px;");

        Separator separator = new Separator();

        Label contentLabel = new Label("内容：");
        contentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label contentValue = new Label(content);
        contentValue.setStyle("-fx-font-size: 14px;");
        contentValue.setWrapText(true);

        ScrollPane contentScrollPane = new ScrollPane(contentValue);
        contentScrollPane.setFitToWidth(true);
        contentScrollPane.setPrefHeight(150);

        Label commentLabel = new Label("评论：");
        commentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        VBox commentBox = new VBox(10);
        for (Map<String, Object> comment : comments) {
            String userName = (String) comment.get("userName");
            String commentTime = (String) comment.get("createTime");
            String commentContentText = (String) comment.get("content");

            // 如果 commentTime 不为 null，去掉 T 并截取到分钟
            if (commentTime != null) {
                commentTime = commentTime.replace("T", " ");
                if (commentTime.length() >= 16) {
                    commentTime = commentTime.substring(0, 16);
                }
            }

            VBox commentItem = new VBox(5);
            Label userInfo = new Label(userName + " - " + commentTime);
            userInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            Label commentContent = new Label(commentContentText);
            commentContent.setStyle("-fx-font-size: 16px; -fx-line-spacing: 5px; -fx-padding: 10px; -fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 5px; -fx-background-radius: 5px;");
            commentContent.setWrapText(true);

            commentItem.getChildren().addAll(userInfo, commentContent);
            commentBox.getChildren().add(commentItem);
        }

        ScrollPane commentScrollPane = new ScrollPane(commentBox);
        commentScrollPane.setFitToWidth(true);
        commentScrollPane.setPrefHeight(200);

        TextField commentInputField = new TextField();
        commentInputField.setPromptText("输入评论内容...");
        TextField userIdInputField = new TextField();
        String currentUserId = getCurrentUserId();
        String currentUserName = AppStore.getJwt().getUsername();
        if (!"admin".equals(currentUserName)) {
            userIdInputField.setText(currentUserId); // 默认填入当前用户ID
            userIdInputField.setEditable(false); // 设置为不可编辑
        } else {
            userIdInputField.setEditable(true); // 管理员可编辑
        }
        userIdInputField.setPromptText("输入用户ID...");
        Button addCommentButton = new Button("添加评论");
        addCommentButton.setOnAction(event -> {
            String commentContent = commentInputField.getText();
            String userId = userIdInputField.getText();
            if (commentContent.isEmpty()) {
                MessageDialog.showDialog("评论内容不能为空");
                return;
            }
            if (userId.isEmpty()) {
                MessageDialog.showDialog("用户ID不能为空");
                return;
            }

            DataRequest addCommentReq = new DataRequest();
            Map<String, Object> form = new HashMap<>();
            form.put("content", commentContent);
            form.put("postId", post.get("postId"));
            form.put("userId", userId);
            addCommentReq.add("form", form);
            DataResponse addCommentRes = HttpRequestUtil.request("/api/post/addComment", addCommentReq);
            if (addCommentRes != null && addCommentRes.getCode() == 0) {
                MessageDialog.showDialog("评论添加成功！");
                DataResponse refreshRes = HttpRequestUtil.request("/api/post/getComments", req);
                if (refreshRes != null && refreshRes.getCode() == 0) {
                    List<Map<String, Object>> updatedComments = (List<Map<String, Object>>) refreshRes.getData();
                    commentBox.getChildren().clear();
                    for (Map<String, Object> updatedComment : updatedComments) {
                        String updatedUserName = (String) updatedComment.get("userName");
                        String updatedCommentTime = (String) updatedComment.get("createTime");
                        String updatedCommentContentText = (String) updatedComment.get("content");

                        // 如果 updatedCommentTime 不为 null，去掉 T 并截取到分钟
                        if (updatedCommentTime != null) {
                            updatedCommentTime = updatedCommentTime.replace("T", " ");
                            if (updatedCommentTime.length() >= 16) {
                                updatedCommentTime = updatedCommentTime.substring(0, 16);
                            }
                        }

                        VBox updatedCommentItem = new VBox(5);
                        Label updatedUserInfo = new Label(updatedUserName + " - " + updatedCommentTime);
                        updatedUserInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
                        Label updatedCommentContent = new Label(updatedCommentContentText);
                        updatedCommentContent.setStyle("-fx-font-size: 16px; -fx-line-spacing: 5px; -fx-padding: 10px; -fx-background-color: #f9f9f9; -fx-border-color: #ddd; -fx-border-radius: 5px; -fx-background-radius: 5px;");
                        updatedCommentContent.setWrapText(true);

                        updatedCommentItem.getChildren().addAll(updatedUserInfo, updatedCommentContent);
                        commentBox.getChildren().add(updatedCommentItem);
                    }
                }
            } else {
                MessageDialog.showDialog(addCommentRes.getMsg());
            }
        });

        HBox commentInputBox = new HBox(10, commentInputField, userIdInputField, addCommentButton);
        commentInputBox.setPadding(new Insets(10, 0, 0, 0));

        dialogContent.getChildren().addAll(titleLabel, titleValue, separator, contentLabel, contentScrollPane, commentLabel, commentScrollPane, commentInputBox);

        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    @FXML
    protected void onQueryButtonClick() {
        String searchText = searchTextField.getText();
        DataRequest req = new DataRequest();
        req.add("searchText", searchText);
        DataResponse res = HttpRequestUtil.request("/api/post/getPostList", req);
        if (res != null && res.getCode() == 0) {
            postList = (ArrayList<Map>) res.getData();
            // 按 isTop 字段排序，置顶的帖子排在最前面
            postList.sort((a, b) -> {
                Boolean isTopA = (Boolean) a.get("isTop");
                Boolean isTopB = (Boolean) b.get("isTop");
                return Boolean.compare(isTopB != null && isTopB, isTopA != null && isTopA);
            });
            setTableViewData(); // 刷新表格
        } else {
            MessageDialog.showDialog(res != null ? res.getMsg() : "请求失败，服务器无响应");
        }
    }

    @FXML
    protected void onAddButtonClick() {
        titleField.clear();
        contentField.clear();
        if (!userIdField.isEditable()) {
            userIdField.setText(getCurrentUserId()); // 默认填入当前用户ID
        } else {
            userIdField.clear(); // 管理员可编辑，清空输入框
        }
        //userIdField.clear();
        postId = null;
    }
    private String getCurrentStudentId() {
        JwtResponse jwtResponse = AppStore.getJwt();
        //System.out.println("当前学生ID: " + jwtResponse.);
        if (jwtResponse != null) {
            return String.valueOf(jwtResponse.getUsername());
        }
        System.out.println("获取当前学生ID失败");
        return null;
    }

    @FXML
    protected void onDeleteButtonClick() {
        Map<String, Object> selected = dataTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请先选择要删除的帖子");
            return;
        }

        String currentUserId = getCurrentStudentId(); // 获取当前用户ID

        String postCreatorId = String.valueOf(selected.get("userId")); // 获取帖子创建者ID
        //System.out.println(postCreatorId);

        // 判断用户身份
        if (!"admin".equals(currentUserId) && !currentUserId.equals(postCreatorId)) {
            MessageDialog.showDialog("您无权删除该帖子");
            return;
        }

        int ret = MessageDialog.choiceDialog("确认要删除该帖子吗?");
        if (ret != MessageDialog.CHOICE_YES) return;

        DataRequest req = new DataRequest();
        req.add("postId", selected.get("postId"));
        DataResponse res = HttpRequestUtil.request("/api/post/deletePost", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("删除成功！");
            onQueryButtonClick();
        } else {
            MessageDialog.showDialog(res.getMsg());
        }
    }
    private String getCurrentUserId() {
        JwtResponse jwtResponse = AppStore.getJwt();
        //System.out.println("当前学生ID: " + jwtResponse.);
        if (jwtResponse != null) {
            return String.valueOf(jwtResponse.getId());
        }
        System.out.println("获取当前学生ID失败");
        return null;
    }
    @FXML
    protected void onSaveButtonClick() {
        if (titleField.getText().isEmpty()) {
            MessageDialog.showDialog("标题不能为空");
            return;
        }

        String currentUserId = getCurrentUserId(); // 获取当前用户 ID
        String currentUserName= AppStore.getJwt().getUsername(); // 获取当前用户名
        String inputUserId = userIdField.getText();  // 获取输入的用户 ID

        // 校验用户 ID
        if (!"admin".equals(currentUserName) && !currentUserId.equals(inputUserId)) {
            MessageDialog.showDialog("您只能添加自己的帖子");
            return;
        }

        Map<String, Object> form = new HashMap<>();
        form.put("title", titleField.getText());
        form.put("content", contentField.getText());
        form.put("userId", inputUserId);

        DataRequest req = new DataRequest();
        req.add("postId", postId);
        req.add("form", form);

        DataResponse res = HttpRequestUtil.request("/api/post/create", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("保存成功！");
            onQueryButtonClick();
        } else {
            MessageDialog.showDialog(res != null ? res.getMsg() : "请求失败，服务器无响应");
        }
    }

    public void doNew() {
        onAddButtonClick();
    }

    public void doSave() {
        onSaveButtonClick();
    }

    public void doDelete() {
        onDeleteButtonClick();
    }

    @FXML
    protected void onQueryCommentsClick() {
        if (postId == null) {
            MessageDialog.showDialog("请先选择帖子");
            return;
        }
        DataRequest req = new DataRequest();
        req.add("postId", postId);
        DataResponse res = HttpRequestUtil.request("/api/post/getComments", req);
        if (res != null && res.getCode() == 0) {
            commentList = (List<Map>) res.getData();
            setCommentTableViewData();
        } else {
            MessageDialog.showDialog(res.getMsg());
        }
    }

    @FXML
    protected void onAddCommentClick() {
        if (postId == null) {
            MessageDialog.showDialog("请先选择帖子");
            return;
        }
        String content = commentField.getText();
        if (content.isEmpty()) {
            MessageDialog.showDialog("评论内容不能为空");
            return;
        }
        DataRequest req = new DataRequest();
        Map<String, Object> form = new HashMap<>();
        form.put("content", content);
        form.put("postId", postId);
        form.put("userId", userIdField.getText());
        req.add("form", form);
        DataResponse res = HttpRequestUtil.request("/api/post/addComment", req);
        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog("评论添加成功！");
            onQueryCommentsClick();
        } else {
            MessageDialog.showDialog(res.getMsg());
        }
    }
    @FXML
    protected void onToggleTopButtonClick() {
        String currentUserName = AppStore.getJwt().getUsername();

        // 验证管理员权限
        if (!"admin".equals(currentUserName)) {
            MessageDialog.showDialog("您无权置顶帖子");
            return;
        }

        Map<String, Object> selected = dataTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageDialog.showDialog("请先选择要置顶的帖子");
            return;
        }

        Boolean currentIsTop = (Boolean) selected.get("isTop");
        boolean newIsTop = currentIsTop == null || !currentIsTop; // 切换置顶状态

        DataRequest req = new DataRequest();
        req.add("postId", selected.get("postId"));
        req.add("isTop", newIsTop); // 设置新的置顶状态
        DataResponse res = HttpRequestUtil.request("/api/post/toggleTop", req);

        if (res != null && res.getCode() == 0) {
            MessageDialog.showDialog(newIsTop ? "置顶成功！" : "取消置顶成功！");
            selected.put("isTop", newIsTop); // 更新本地数据

            // 重新排序 postList，将置顶帖子排在最前面
            postList.sort((a, b) -> {
                Boolean isTopA = (Boolean) a.get("isTop");
                Boolean isTopB = (Boolean) b.get("isTop");
                return Boolean.compare(isTopB != null && isTopB, isTopA != null && isTopA);
            });

            setTableViewData(); // 刷新表格
        } else {
            MessageDialog.showDialog(res != null ? res.getMsg() : "请求失败，服务器无响应");
        }
    }
}