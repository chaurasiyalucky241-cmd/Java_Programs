import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class DBConnection {
    static Connection connect() throws SQLException{
        String url ="jdbc:mysql://localhost:3306/inventory";
        String user ="root";
        String password ="122626";
        return DriverManager.getConnection(url,user,password);
    }
}
