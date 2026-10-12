package cl.duoc.dsy2207.roles.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class OracleConnectionFactory {
    private OracleConnectionFactory() {
    }

    private static String env(String clave, String valorPorDefecto) {
        String valor = System.getenv(clave);
        return (valor == null || valor.isBlank()) ? valorPorDefecto : valor;
    }

    public static Connection obtenerConexion() throws SQLException {
        String url = env("ORACLE_JDBC_URL", "jdbc:oracle:thin:@localhost:1521/XEPDB1");
        String usuario = env("ORACLE_USER", "appuser");
        String password = env("ORACLE_PASSWORD", "appuser123");
        return DriverManager.getConnection(url, usuario, password);
    }
}
