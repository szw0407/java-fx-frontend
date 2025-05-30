package com.teach.javafx.controller.base;

import com.teach.javafx.MainApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * MessageDialog 消息对话框工具类 可以显示提示信息，用户选择确认信息和PDF显示
 */
public class MessageDialog {
    public final static int CHOICE_OK = 1;
    public final static int CHOICE_CANCEL = 2;
    public final static int CHOICE_YES = 3;
    public final static int CHOICE_NO = 4;

    private  MessageController messageController= null;
    private  ChoiceController choiceController= null;
    private static MessageDialog instance = new MessageDialog();

    /**
     * 初始加载三个页面
     * base/message-dialog.fxml  用于显示提示信息
     * base/choice-dialog.fxml 用于选择 是，否 取消 消息对话框
     * base/pdf-viewer-dialog.fxml 查看PDF效果对话框
     */
    private MessageDialog() {
        FXMLLoader fxmlLoader ;
        Scene scene = null;
        Stage stage;
        try {
            fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/message-dialog.fxml"));
            scene = new Scene(fxmlLoader.load(), 300, 260);
            stage = new Stage();
            stage.initOwner(MainApplication.getMainStage());
            stage.setAlwaysOnTop(true);
            stage.initModality(Modality.NONE);
            stage.setOnCloseRequest(e->{
                MainApplication.setCanClose(true);
            });
            stage.setScene(scene);
            stage.setTitle("信息显示对话框");
            messageController = (MessageController) fxmlLoader.getController();
            messageController.setStage(stage);

            fxmlLoader = new FXMLLoader(MainApplication.class.getResource("base/choice-dialog.fxml"));
            scene = new Scene(fxmlLoader.load(), 300, 260);
            stage = new Stage();
            stage.initOwner(MainApplication.getMainStage());
            stage.setAlwaysOnTop(true);
            stage.initModality(Modality.NONE);
            stage.setOnCloseRequest(e->{
                MainApplication.setCanClose(true);
            });
            stage.setScene(scene);
            stage.setTitle("信息显示对话框");
            choiceController = (ChoiceController) fxmlLoader.getController();
            choiceController.setStage(stage);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * showDialog 信息提示
     * @param msg  提示的信息
     */
    public static void showDialog(String msg) {
        if(instance == null)
            return;
        if(instance.messageController == null)
            return;
        instance.messageController.showDialog(msg);
        MainApplication.setCanClose(false);
    }
    /**
     * choiceDialog 显示提示信息和是 否 取消按钮， 用户可选择
     * 点击 是 返回 CHOICE_YES = 3;
     * 点击 否 返回 CHOICE_NO = 4;
     * 点击 取消 返回 CHOICE_CANCEL = 2;
     * @param msg  提示的信息
     */
    public static int choiceDialog(String msg) {
        if(instance == null)
            return 0;
        if(instance.choiceController == null)
            return 0;
        MainApplication.setCanClose(false);
        return instance.choiceController.choiceDialog(msg);
    }


    /**
     * 显示确认对话框
     * @param title 对话框标题
     * @param message 提示信息
     * @return 用户点击"确认"返回true，点击"取消"或关闭窗口返回false
     */
    public static boolean showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // 不显示头部文本
        alert.setContentText(message);

        // 设置按钮文本
        ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(ButtonType.OK)).setText("确认");
        ((javafx.scene.control.Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("取消");

        // 应用样式
        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(
                MessageDialog.class.getResource("/com/teach/javafx/css/leave-style.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");

        // 阻止应用程序关闭
        MainApplication.setCanClose(false);

        // 显示对话框并等待用户响应
        boolean result = alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;

        // 恢复应用程序可关闭状态
        MainApplication.setCanClose(true);

        return result;
    }

}
