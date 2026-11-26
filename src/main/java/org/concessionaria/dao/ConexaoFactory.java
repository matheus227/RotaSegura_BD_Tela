package org.concessionaria.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fábrica de conexões JDBC. O arquivo do banco fica em ./data/rota_segura.db
 * (pode ser trocado com -Drotasegura.db=caminho/arquivo.db).
 */
public final class ConexaoFactory {
    public static final String ARQUIVO_BANCO =
            System.getProperty("rotasegura.db", "data/rota_segura.db");
    private static final String URL = "jdbc:sqlite:" + ARQUIVO_BANCO;

    private ConexaoFactory() {}

    public static Connection obter() throws SQLException {
        Connection con = DriverManager.getConnection(URL);
        // O SQLite só respeita chaves estrangeiras se isto for ativado a cada conexão.
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return con;
    }
}
