package org.concessionaria.dao;

import org.concessionaria.exception.PersistenciaException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Prepara o banco local (arquivo SQLite) na inicialização:
 * <ul>
 *   <li>{@code db/schema.sql}: cria as tabelas que ainda não existem (sempre executado);</li>
 *   <li>{@code db/dados_iniciais.sql}: carga de exemplo, executada SOMENTE quando o arquivo do banco
 *       acabou de ser criado (assim, dados apagados pelo usuário nunca "ressuscitam").</li>
 * </ul>
 */
public final class BancoDeDados {
    private static final String SCHEMA = "/db/schema.sql";
    private static final String DADOS_INICIAIS = "/db/dados_iniciais.sql";

    private BancoDeDados() {}

    public static void inicializar() {
        try {
            Path arquivo = Path.of(ConexaoFactory.ARQUIVO_BANCO).toAbsolutePath();
            if (arquivo.getParent() != null) {
                Files.createDirectories(arquivo.getParent());
            }
            boolean bancoNovo = Files.notExists(arquivo) || Files.size(arquivo) == 0;

            try (Connection con = ConexaoFactory.obter()) {
                executarScript(con, lerRecurso(SCHEMA));
                if (bancoNovo) {
                    executarScript(con, lerRecurso(DADOS_INICIAIS));
                }
            }
        } catch (IOException | SQLException e) {
            throw new PersistenciaException("Não foi possível inicializar o banco de dados.", e);
        }
    }

    /** Executa todos os comandos do script em uma única transação (tudo ou nada). */
    private static void executarScript(Connection con, String script) throws SQLException {
        boolean autoCommitOriginal = con.getAutoCommit();
        con.setAutoCommit(false);
        try (Statement st = con.createStatement()) {
            for (String comando : script.split(";")) {
                if (!comando.isBlank()) {
                    st.execute(comando.trim());
                }
            }
            con.commit();
        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(autoCommitOriginal);
        }
    }

    private static String lerRecurso(String caminho) throws IOException {
        try (InputStream in = BancoDeDados.class.getResourceAsStream(caminho)) {
            if (in == null) {
                throw new IOException("Arquivo " + caminho + " não encontrado nos recursos.");
            }
            String texto = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // remove linhas de comentário para não atrapalhar a separação por ';'
            return Arrays.stream(texto.split("\\R"))
                    .filter(l -> !l.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
        }
    }
}
