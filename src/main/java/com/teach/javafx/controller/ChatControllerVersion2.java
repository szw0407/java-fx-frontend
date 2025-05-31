package com.teach.javafx.controller;

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
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class ChatControllerVersion2 {

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
            .connectTimeout(Duration.ofSeconds(20))  // 增加连接超时
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

        // 修改绑定逻辑，同时考虑文本是否为空和是否正在发送
        sendButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> promptTextArea.getText().trim().isEmpty() || isSending.get(),
                        promptTextArea.textProperty(),
                        isSending
                )
        );

        // 添加一条欢迎消息
        Platform.runLater(() -> {
            addAIMessage("你好！我是AI助手，有什么可以帮助你的吗？");
        });
    }

    @FXML
    public void sendPrompt() {
        String prompt = promptTextArea.getText().trim();
        if (prompt.isEmpty()) {
            return;
        }

        // 添加用户问题到历史记录
        addUserMessage(prompt);

        // 清空问题输入框
        promptTextArea.clear();

        // 使用属性设置发送状态，而不是直接设置按钮
        isSending.set(true);
        loadingIndicator.setVisible(true);

        // 使用异步请求处理
        CompletableFuture.runAsync(() -> {
            try {
                // 构造请求体
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "deepseek");

                JSONArray messages = new JSONArray();
                JSONObject messageObj = new JSONObject();
                messageObj.put("role", "user");
                messageObj.put("content", prompt+" WARN: Do not use emoji in your reply.");
                messages.put(messageObj);

                requestBody.put("messages", messages);
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 2000);

                // 创建HTTP请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(OPENAI_API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + API_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .timeout(Duration.ofSeconds(60))  // 增加请求超时到60秒
                        .build();

                // 发送请求并获取响应
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    String aiResponse = extractResponseContent(response);

                    Platform.runLater(() -> {
                        addAIMessage(aiResponse);
                        loadingIndicator.setVisible(false);
                        isSending.set(false);
                    });
                } catch (IOException | InterruptedException e) {
                    handleRequestError(e, prompt);
                }
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
                addErrorMessage("请求超时，服务器没有及时响应");
                // 提供本地模拟回复，增强用户体验
                addAIMessage("很抱歉，服务器响应超时。我可以本地回答简单问题，但复杂问题可能需要重试。\n\n您问的是关于：" + prompt);
            } else {
                addErrorMessage("请求失败: " + e.getMessage());
            }
            loadingIndicator.setVisible(false);
            isSending.set(false);
        });
    }

    @FXML
    public void clearChat() {
        chatHistoryVBox.getChildren().clear();
        // 添加一条欢迎消息
        Platform.runLater(() -> {
            addAIMessage("你好！我是AI助手，有什么可以帮助你的吗？");
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
        // 修正字体加载路径，使用正确的资源路径
        try {
            String fontPath = getClass().getResource("/com/teach/javafx/fonts/NotoSans.ttf").toExternalForm();
            Font.loadFont(fontPath, 14);
        } catch (Exception e) {
            System.err.println("无法加载Noto Sans字体: " + e.getMessage());
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
                        font-family: 'Noto Sans', system-ui, -apple-system, BlinkMacSystemFont, sans-serif;
                        font-size: 14px;
                        line-height: 1.6;
                        color: #333;
                        margin: 0;
                        padding: 0;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        color: #2c3e50;
                    }
                    a {
                        color: #007bff;
                        text-decoration: none;
                    }
                    a:hover {
                        text-decoration: underline;
                    }
                    code {
                        background-color: #f8f9fa;
                        padding: 2px 4px;
                        border-radius: 4px;
                        font-family: 'Courier New', monospace;
                    }
                    pre {
                        background-color: #f8f9fa;
                        padding: 10px;
                        border-radius: 4px;
                        overflow-x: auto;
                    }
                    blockquote {
                        border-left: 4px solid #dfe2e5;
                        padding-left: 10px;
                        color: #6a737d;
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
                String responseBody = response.body();
                JSONObject json = new JSONObject(responseBody);
                JSONArray choices = json.getJSONArray("choices");

                if (choices.isEmpty()) {
                    return "解析失败: choices 为空";
                }

                JSONObject messageObj = choices.getJSONObject(0).getJSONObject("message");
                return messageObj.getString("content");
            } else {
                return "请求失败: 状态码 " + response.statusCode() + "\n" + response.body();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "解析响应失败: " + e.getMessage();
        }
    }
}

