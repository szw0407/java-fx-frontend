package com.teach.javafx.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * 字体工具类，用于处理字体相关操作
 */
public class FontUtil {

    /**
     * 将字体文件转换为Base64编码
     * @param fontPath 字体资源路径
     * @return Base64编码的字体数据
     */
    public static String fontToBase64(String fontPath) {
        try (InputStream is = FontUtil.class.getResourceAsStream(fontPath)) {
            if (is == null) {
                System.err.println("找不到字体文件: " + fontPath);
                return "";
            }

            // 读取字体文件内容
            byte[] fontBytes = is.readAllBytes();

            // 转换为Base64
            return Base64.getEncoder().encodeToString(fontBytes);
        } catch (IOException e) {
            System.err.println("读取字体文件失败: " + e.getMessage());
            return "";
        }
    }

    /**
     * 生成字体的CSS @font-face规则，使用Base64编码
     * @param fontFamily 字体名称
     * @param fontPath 字体资源路径
     * @return 包含Base64编码的@font-face CSS规则
     */
    public static String generateBase64FontFace(String fontFamily, String fontPath) {
        String base64Font = fontToBase64(fontPath);
        if (base64Font.isEmpty()) {
            return "";
        }

        // 根据字体文件扩展名确定font-format
        String format = "truetype"; // 默认为truetype (.ttf)
        if (fontPath.endsWith(".woff")) {
            format = "woff";
        } else if (fontPath.endsWith(".woff2")) {
            format = "woff2";
        } else if (fontPath.endsWith(".otf")) {
            format = "opentype";
        }

        return String.format("""
            @font-face {
                font-family: '%s';
                src: url('data:font/%s;base64,%s') format('%s');
                font-weight: normal;
                font-style: normal;
                font-display: swap;
            }
            """, fontFamily, format, base64Font, format);
    }
}
