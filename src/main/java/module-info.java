module com.teach.javafx {
    requires transitive javafx.base;
    requires transitive javafx.controls; // Added transitive here
    requires javafx.fxml;
    requires javafx.swing;
    requires java.logging;
    requires com.google.gson;
    requires java.net.http;
    requires org.json;
    requires org.commonmark;
    requires javafx.web;
    
    // SSL/TLS and crypto modules
    requires java.base;
    requires jdk.crypto.ec;
    requires jdk.crypto.cryptoki;

    opens com.teach.javafx to javafx.fxml;
    opens com.teach.javafx.request to com.google.gson, javafx.fxml;
    opens com.teach.javafx.controller.base to com.google.gson, javafx.fxml;
    opens com.teach.javafx.controller to com.google.gson, javafx.fxml;
    opens com.teach.javafx.models to javafx.base,com.google.gson;
    opens com.teach.javafx.util to com.google.gson, javafx.fxml;

    exports com.teach.javafx;
    exports com.teach.javafx.controller;
    exports com.teach.javafx.controller.base;
    exports com.teach.javafx.request;
    exports com.teach.javafx.util;

}