package com.teach.javafx.controller;

        import javafx.application.Platform;
        import javafx.beans.binding.Bindings;
        import javafx.fxml.FXML;
        import javafx.scene.control.Alert;
        import javafx.scene.control.Label;
        import javafx.scene.control.ScrollPane;
        import javafx.scene.control.TextArea;
        import javafx.scene.layout.VBox;
        import javafx.scene.web.WebView;

        import java.net.URI;
        import java.net.http.HttpClient;
        import java.net.http.HttpRequest;
        import java.net.http.HttpResponse;

        public class ChatController {

            @FXML
            private TextArea promptTextArea;
            @FXML
            private ScrollPane chatScrollPane;
            @FXML
            private VBox chatHistoryVBox;

            @FXML
            private Label loadingLabel;

            private static final String OPENAI_API_URL = "https://deepseek.wxmbz.com/v1/chat/completions";
            private static final String API_KEY = "sk-sFkRY3HYKTXvWWDheAPdVYwabHHAgiF1Q0nw6FbvJSSaoG0n";

            @FXML
            public void sendPrompt() {
                String prompt = promptTextArea.getText();
                if (prompt.isEmpty()) {
                    showAlert();
                    return;
                }

                // 添加用户问题到历史记录
                addMessageToHistory("用户: " + prompt);

                // 清空问题输入框
                promptTextArea.clear();

                // 显示加载状态
                loadingLabel.setVisible(true);

                new Thread(() -> {
                    try {
                        // 构造请求体
                        String requestBody = """
                            {
                              "model": "deepseek",
                              "messages": [
                                {
                                  "role": "user",
                                  "content": "%s"
                                }
                              ],
                              "max_tokens": 100
                            }
                        """.formatted(prompt.replace("\n", "").trim());

                        // 创建 HTTP 请求
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(OPENAI_API_URL))
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + API_KEY)
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody)).version(HttpClient.Version.HTTP_1_1)
                                .build();

                        // 使用同步请求以便调试
                        HttpClient client = HttpClient.newHttpClient();
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        // 更新 UI
                        String replyContent = extractResponseContent(response);
                        Platform.runLater(() -> {
                            addMessageToHistory("AI: " + renderMarkdownToHtml(replyContent));
                            loadingLabel.setVisible(false);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> {
                            addMessageToHistory("请求失败: " + e.getMessage());
                            loadingLabel.setVisible(false);
                        });
                    }
                }).start();
            }

            public void initialize() {
                // 初始化时隐藏加载标签
                loadingLabel.setVisible(false);
                chatScrollPane.setContent(chatHistoryVBox);
                chatScrollPane.setFitToWidth(true);
                // 设置滚动条自动滚动到底部
//                chatScrollPane.vvalueProperty().bind(Bindings.createDoubleBinding(() -> 1.0, chatHistoryVBox.heightProperty()));
            }
            // Markdown 转 HTML 工具方法
            private String renderMarkdownToHtml(String markdown) {
                // 这里建议引入 commonmark-java 或 flexmark-java 依赖进行 Markdown 转换
                // 示例使用 flexmark-java
                try {
                    return org.commonmark.renderer.html.HtmlRenderer.builder()
                            .build()
                            .render(org.commonmark.parser.Parser.builder().build().parse(markdown));
                } catch (Exception e) {
                    return markdown; // 转换失败时回退为原文本
                }
            }
            private String extractResponseContent(HttpResponse<String> response) {
                try {
                    if (response.statusCode() == 200) {
                        String responseBody = response.body();
                        try {
                            // 使用 org.json 解析
                            org.json.JSONObject json = new org.json.JSONObject(responseBody);
                            org.json.JSONArray choices = json.getJSONArray("choices");
                            if (choices.isEmpty()) {
                                return "解析失败: choices 为空";
                            }
                            org.json.JSONObject messageObj = choices.getJSONObject(0).getJSONObject("message");
                            return messageObj.getString("content");
                        } catch (Exception e) {
                            return "解析失败: " + e.getMessage();
                        }
                    } else {
                        return "请求失败: " + response.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return "解析响应失败: " + e.getMessage();
                }
            }

            private void addMessageToHistory(String message) {
                WebView webView = new WebView();
                webView.getEngine().loadContent(message, "text/html");

                // 设置最大高度限制
                webView.setMaxHeight(500); // 或根据需求调整

                // 动态高度绑定（保留）
                webView.prefHeightProperty().bind(Bindings.createDoubleBinding(() -> {
                    Object result = webView.getEngine().executeScript(
                            "document.body.scrollHeight");
                    return Math.min(((Number) result).doubleValue(), 500); // 限制最大高度
                }, webView.getEngine().documentProperty()));

                chatHistoryVBox.getChildren().add(webView);
            }

            private void showAlert() {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("错误");
                alert.setContentText("请输入问题！");
                alert.showAndWait();
            }
        }