module me.piitex.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires RenEngine;

    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.json;
    requires metadata.extractor;
    requires org.apache.commons.io;
    requires com.dustinredmond.fxtrayicon;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;
    requires atlantafx.base;
    requires java.logging;
    requires org.apache.logging.log4j;
    requires annotations;
    requires org.fxmisc.richtext;
    requires javafx.graphics;

    opens me.piitex.app to javafx.fxml;
    exports me.piitex.app;
    exports me.piitex.app.backend;
    exports me.piitex.app.backend.server;
    exports me.piitex.app.configuration;
}