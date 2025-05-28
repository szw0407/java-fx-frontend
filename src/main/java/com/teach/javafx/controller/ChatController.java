package com.teach.javafx.controller;

        import javafx.application.Platform;
        import javafx.fxml.FXML;
        import javafx.scene.control.Alert;
        import javafx.scene.control.Label;
        import javafx.scene.control.TextArea;
        import javafx.scene.layout.VBox;

        import java.net.URI;
        import java.net.http.HttpClient;
        import java.net.http.HttpRequest;
        import java.net.http.HttpResponse;

        public class ChatController {

            @FXML
            private TextArea promptTextArea;

            @FXML
            private VBox chatHistoryVBox;

            @FXML
            private Label loadingLabel;

            private static final String OPENAI_API_URL = "https://api.nuwaapi.com/v1/chat/completions";
            private static final String API_KEY = "sk-sFkRY3HYKTXvWWDheAPdVYwabHHAgiF1Q0nw6FbvJSSaoG0n";

            @FXML
            public void sendPrompt() {
                String prompt = promptTextArea.getText();
                if (prompt.isEmpty()) {
                    showAlert("错误", "请输入问题！");
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
                              "model": "gpt-4",
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
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .build();

                        // 使用同步请求以便调试
                        HttpClient client = HttpClient.newHttpClient();
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        // 更新 UI
                        String replyContent = extractResponseContent(response);
                        Platform.runLater(() -> {
                            addMessageToHistory("AI: " + replyContent);
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

            private String extractResponseContent(HttpResponse<String> response) {
                try {
                    if (response.statusCode() == 200) {
                        String responseBody = response.body();
                        // 查找 "choices" 数组
                        int choicesStart = responseBody.indexOf("\"choices\":");
                        if (choicesStart == -1) {
                            return "解析失败: 未找到 choices";
                        }
                        int arrayStart = responseBody.indexOf("[", choicesStart);
                        int arrayEnd = responseBody.indexOf("]", arrayStart);
                        if (arrayStart == -1 || arrayEnd == -1) {
                            return "解析失败: choices 数组格式错误";
                        }
                        String choicesArray = responseBody.substring(arrayStart + 1, arrayEnd);

                        // 查找 "message" 对象
                        int messageStart = choicesArray.indexOf("\"message\":");
                        if (messageStart == -1) {
                            return "解析失败: 未找到 message";
                        }
                        int contentStart = choicesArray.indexOf("\"content\":", messageStart);
                        if (contentStart == -1) {
                            return "解析失败: 未找到 content";
                        }
                        int contentValueStart = choicesArray.indexOf("\"", contentStart + 10) + 1;
                        int contentValueEnd = choicesArray.indexOf("\"", contentValueStart);
                        if (contentValueStart == -1 || contentValueEnd == -1) {
                            return "解析失败: content 格式错误";
                        }
                        return choicesArray.substring(contentValueStart, contentValueEnd);
                    } else {
                        return "请求失败: " + response.body();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return "解析响应失败: " + e.getMessage();
                }
            }

            private void addMessageToHistory(String message) {
                Label messageLabel = new Label(message);
                messageLabel.setWrapText(true);
                messageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
                chatHistoryVBox.getChildren().add(messageLabel);
            }

            private void showAlert(String title, String message) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(title);
                alert.setContentText(message);
                alert.showAndWait();
            }
        }