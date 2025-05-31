package com.teach.javafx.controller;

            import com.teach.javafx.AppStore;
            import com.teach.javafx.request.DataRequest;
            import com.teach.javafx.request.DataResponse;
            import com.teach.javafx.request.HttpRequestUtil;
            import com.teach.javafx.request.JwtResponse;
            import javafx.application.Platform;
            import javafx.beans.binding.Bindings;
            import javafx.beans.property.BooleanProperty;
            import javafx.beans.property.SimpleBooleanProperty;
            import javafx.fxml.FXML;
            import javafx.geometry.Pos;
            import javafx.scene.control.*;
            import javafx.scene.input.KeyCode;
            import javafx.scene.input.KeyEvent;
            import javafx.scene.layout.HBox;
            import javafx.scene.layout.VBox;
            import javafx.scene.text.Font;  // 添加缺少的Font导入
            import javafx.scene.web.WebView;
            import org.json.JSONArray;
            import org.json.JSONObject;

            import java.net.URI;
            import java.net.http.HttpClient;
            import java.net.http.HttpRequest;
            import java.net.http.HttpResponse;
            import java.time.Duration;
            import java.time.LocalDate;
            import java.time.LocalDateTime;
            import java.time.format.DateTimeFormatter;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.Map;
            import java.util.concurrent.CompletableFuture;

            public class ChatStudentController {

                @FXML
                private TextArea promptTextArea;
                @FXML
                private ScrollPane chatScrollPane;
                @FXML
                private VBox chatHistoryVBox;
                @FXML
                private HBox loadingIndicator;
                @FXML
                private Button sendButton;
                @FXML
                private Button clearChatButton;

                private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
                private static final String OPENAI_API_URL = "https://deepseek.wxmbz.com/v1/chat/completions";
                private static final String API_KEY = "sk-sFkRY3HYKTXvWWDheAPdVYwabHHAgiF1Q0nw6FbvJSSaoG0n";

                // 创建更稳健的HttpClient实例，增加超时时间
                private final HttpClient httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(20))
                        .build();

                // 添加发送状态属性
                private final BooleanProperty isSending = new SimpleBooleanProperty(false);

                @FXML
                public void initialize() {
                    // 初始化时隐藏加载指示器
                    loadingIndicator.setVisible(false);

                    // 设置滚动面板属性
                    chatScrollPane.setFitToWidth(true);

                    // 设置文本区域按键事件
                    promptTextArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                        if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                            event.consume();
                            sendPrompt();
                        }
                    });

                    // 绑定发送按钮状态
                    sendButton.disableProperty().bind(
                            Bindings.createBooleanBinding(
                                    () -> promptTextArea.getText().trim().isEmpty() || isSending.get(),
                                    promptTextArea.textProperty(),
                                    isSending
                            )
                    );

                    // 添加欢迎消息
                    Platform.runLater(() -> {
                        addAIMessage("你好！我是AI助手，我已经了解了你的学生信息，有什么可以帮助你的吗？");
                    });
                }

                // 获取学生课程表信息
                private List<Map<String, String>> loadStudentCourses() {
                    try {
                        // 获取当前学年学期
                        LocalDate now = LocalDate.now();
                        int year = now.getYear();
                        int month = now.getMonthValue();
                        String currentYear = String.valueOf(year);
                        String currentTerm = (month >= 7) ? "1" : "2"; // 习惯上，下半年为1学期，上半年为2学期

                        var dr = new DataRequest();
                        dr.add("year", currentYear);
                        dr.add("semester", currentTerm);
                        var response = HttpRequestUtil.request("/api/me/PlanList", dr);

                        if (response != null && response.getCode() == 0) {
                            return (List<Map<String, String>>) response.getData();
                        }
                    } catch (Exception e) {
                        System.out.println("获取学生课程表失败: " + e.getMessage());
                    }
                    return new ArrayList<>();
                }

                private String getCurrentStudentId() {
                    JwtResponse jwtResponse = AppStore.getJwt();
                    if (jwtResponse != null) {
                        return String.valueOf(jwtResponse.getId());
                    }
                    System.out.println("获取当前学生ID失败");
                    return null;
                }

                private Map<String, Object> loadStudentData(String studentId) {
                    DataRequest req = new DataRequest();
                    req.add("studentId", studentId);
                    DataResponse res = HttpRequestUtil.request("/api/student/getCurrentStudentData", req);

                    if (res != null && res.getCode() == 0) {
                        return (Map<String, Object>) res.getData();
                    } else {
                        System.out.println("获取学生数据失败：" + (res != null ? res.getMsg() : "未知错误"));
                        return null;
                    }
                }

                @FXML
                public void sendPrompt() {
                    String prompt = promptTextArea.getText().trim();
                    if (prompt.isEmpty()) {
                        return;
                    }

                    String studentId = getCurrentStudentId();
                    if (studentId == null) {
                        showAlert("错误", "无法获取学生ID！");
                        return;
                    }

                    Map<String, Object> studentInfo = loadStudentData(studentId);
                    if (studentInfo == null || studentInfo.isEmpty()) {
                        showAlert("错误", "无法获取学生信息！");
                        return;
                    }

                    // 添加用户问题到历史记录
                    addUserMessage(prompt);

                    // 清空问题输入框
                    promptTextArea.clear();

                    // 设置发送状态和加载指示器
                    isSending.set(true);
                    loadingIndicator.setVisible(true);

                    // 构建包含学生信息的提示词
                    StringBuilder studentInfoText = new StringBuilder("学生信息: ");
                    studentInfo.forEach((key, value) -> studentInfoText.append(key).append("=").append(value).append(", "));

                    // 添加学生课程信息
                    List<Map<String, String>> courses = loadStudentCourses();
                    StringBuilder courseInfoText = new StringBuilder("\n学生课程表: ");

                    if (courses != null && !courses.isEmpty()) {
                        for (Map<String, String> course : courses) {
                            courseInfoText.append("\n课程: ").append(course.get("courseName"))
                                         .append(", 教师: ").append(course.get("teachers"))
                                         .append(", 上课时间: ").append(course.get("classTime"))
                                         .append(", 地点: ").append(course.get("classLocation"));
                        }
                    } else {
                        courseInfoText.append("无课程信息");
                    }

                    // 完整提示词 - 修复StringBuilder连接问题
                    String fullPrompt = studentInfoText.toString() + courseInfoText.toString() + "\n用户问题: " + prompt;

                    // 使用异步请求处理
                    CompletableFuture.runAsync(() -> {
                        try {
                            // 构建API请求
                            JSONObject requestBody = new JSONObject();
                            requestBody.put("model", "deepseek-chat");

                            JSONArray messages = new JSONArray();
                            JSONObject systemMessage = new JSONObject();
                            systemMessage.put("role", "system");
                            systemMessage.put("content", "你是一个学生助手，你需要基于提供的学生信息和课程表，提供有针对性的回答。");

                            JSONObject userMessage = new JSONObject();
                            userMessage.put("role", "user");
                            userMessage.put("content", fullPrompt);

                            messages.put(systemMessage);
                            messages.put(userMessage);
                            requestBody.put("messages", messages);

                            // 发送API请求
                            HttpRequest request = HttpRequest.newBuilder()
                                    .uri(URI.create(OPENAI_API_URL))
                                    .header("Content-Type", "application/json")
                                    .header("Authorization", "Bearer " + API_KEY)
                                    .timeout(Duration.ofSeconds(60))
                                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                                    .build();

                            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                            // 处理响应
                            String responseContent = extractResponseContent(response);
                            Platform.runLater(() -> {
                                addAIMessage(responseContent);
                                loadingIndicator.setVisible(false);
                                isSending.set(false);
                            });
                        } catch (Exception e) {
                            handleRequestError(e, prompt);
                        }
                    });
                }

                // 处理请求错误
                private void handleRequestError(Exception e, String prompt) {
                    e.printStackTrace();

                    // 检测是否为超时错误
                    String errorMessage = e.getMessage();
                    boolean isTimeout = e instanceof java.net.http.HttpTimeoutException ||
                            (errorMessage != null && errorMessage.contains("timed out"));

                    Platform.runLater(() -> {
                        if (isTimeout) {
                            addErrorMessage("请求超时，请稍后再试。");
                        } else {
                            addErrorMessage("发送请求时出错：" + errorMessage);
                        }
                        loadingIndicator.setVisible(false);
                        isSending.set(false);
                    });
                }

                @FXML
                public void clearChat() {
                    chatHistoryVBox.getChildren().clear();
                    // 添加欢迎消息
                    Platform.runLater(() -> {
                        addAIMessage("你好！我是AI助手，我已经了解了你的学生信息，有什么可以帮助你的吗？");
                    });
                }

                private void addUserMessage(String message) {
                    VBox messageContainer = new VBox(5);
                    messageContainer.setMaxWidth(chatScrollPane.getWidth() * 0.7);
                    messageContainer.getStyleClass().add("user-message");

                    Label timeLabel = new Label(LocalDateTime.now().format(TIME_FORMATTER));
                    timeLabel.getStyleClass().add("time-label");

                    WebView webView = new WebView();
                    webView.getEngine().loadContent("<div style='font-family: system-ui; font-size: 14px;'>" + message + "</div>", "text/html");
                    webView.setPrefWidth(messageContainer.getMaxWidth() - 30);
                    webView.setPrefHeight(calculateTextHeight(message, 300));

                    messageContainer.getChildren().addAll(webView, timeLabel);

                    HBox wrapper = new HBox(messageContainer);
                    wrapper.setAlignment(Pos.CENTER_RIGHT);
                    wrapper.setMaxWidth(Double.MAX_VALUE);

                    Platform.runLater(() -> {
                        chatHistoryVBox.getChildren().add(wrapper);
                        scrollToBottom();
                    });
                }

                private void addAIMessage(String message) {
                    VBox messageContainer = new VBox(5);
                    messageContainer.setMaxWidth(chatScrollPane.getWidth() * 0.7);
                    messageContainer.getStyleClass().add("ai-message");

                    Label senderLabel = new Label("AI助手");
                    senderLabel.getStyleClass().add("sender-label");

                    Label timeLabel = new Label(LocalDateTime.now().format(TIME_FORMATTER));
                    timeLabel.getStyleClass().add("time-label");

                    WebView webView = new WebView();
                    // 尝试加载字体
                    try {
                        Font.loadFont(getClass().getResourceAsStream("/com/teach/javafx/fonts/NotoSans.ttf"), 14);
                    } catch (Exception e) {
                        System.out.println("加载字体失败: " + e.getMessage());
                    }
                    webView.getEngine().loadContent(renderMarkdownToHtml(message), "text/html");
                    webView.setPrefWidth(messageContainer.getMaxWidth() - 30);
                    webView.setPrefHeight(calculateTextHeight(message, 500));

                    messageContainer.getChildren().addAll(senderLabel, webView, timeLabel);

                    Platform.runLater(() -> {
                        chatHistoryVBox.getChildren().add(messageContainer);
                        scrollToBottom();
                    });
                }

                private void addErrorMessage(String message) {
                    Label errorLabel = new Label(message);
                    errorLabel.getStyleClass().add("error-message");

                    Platform.runLater(() -> {
                        chatHistoryVBox.getChildren().add(errorLabel);
                        scrollToBottom();
                    });
                }

                private void scrollToBottom() {
                    chatScrollPane.setVvalue(1.0);
                }

                // 根据文本长度计算WebView高度
                private double calculateTextHeight(String text, double maxHeight) {
                    int lineCount = text.split("\n").length;
                    double estimatedHeight = lineCount * 20 + 20; // 每行大约20像素加上边距
                    return Math.min(estimatedHeight, maxHeight);
                }

                // Markdown转HTML方法
                private String renderMarkdownToHtml(String markdown) {
                    // 使用 org.commonmark 渲染 Markdown
                    org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder().build();
                    org.commonmark.renderer.html.HtmlRenderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder().build();
                    String htmlContent = renderer.render(parser.parse(markdown));

                    // 使用FontUtil工具生成base64编码的字体CSS
                    String base64FontFace = com.teach.javafx.util.FontUtil.generateBase64FontFace(
                        "Noto Sans",
                        "/com/teach/javafx/fonts/NotoSans.ttf"
                    );

                    // 包装 HTML 内容，设置字体和样式
                    return """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                %s
                                body {
                                    font-family: 'Noto Sans', sans-serif;
                                    font-size: 14px;
                                    line-height: 1.6;
                                    color: #333;
                                    margin: 0;
                                    padding: 0;
                                }
                                pre {
                                    background-color: #f5f5f5;
                                    border: 1px solid #ddd;
                                    border-radius: 3px;
                                    padding: 10px;
                                    overflow-x: auto;
                                }
                                code {
                                    font-family: Consolas, monospace;
                                    background-color: #f5f5f5;
                                    padding: 2px 4px;
                                    border-radius: 3px;
                                }
                            </style>
                        </head>
                        <body>
                            %s
                        </body>
                        </html>
                        """.formatted(base64FontFace, htmlContent);
                }

                // 解析API响应
                private String extractResponseContent(HttpResponse<String> response) {
                    try {
                        if (response.statusCode() == 200) {
                            JSONObject jsonResponse = new JSONObject(response.body());
                            JSONArray choices = jsonResponse.getJSONArray("choices");
                            if (choices.length() > 0) {
                                JSONObject choice = choices.getJSONObject(0);
                                JSONObject message = choice.getJSONObject("message");
                                return message.getString("content");
                            }
                        }
                        return "无法解析AI响应，HTTP状态码: " + response.statusCode();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "解析AI响应时出错: " + e.getMessage();
                    }
                }

                private void showAlert(String title, String message) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(title);
                    alert.setContentText(message);
                    alert.showAndWait();
                }
            }