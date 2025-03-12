module com.example.roundrobintunier {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.chocosolver.solver;


    opens com.example.roundrobintunier to javafx.fxml;
    exports com.example.roundrobintunier;
}