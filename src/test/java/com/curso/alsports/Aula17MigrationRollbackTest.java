package com.curso.alsports;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Aplica e reverte as migrations 010 e 011 em um banco PostgreSQL temporário.
 *
 * <p>O teste cria e remove um database exclusivo. O banco de testes compartilhado
 * {@code alsports_test} é usado apenas como ponto administrativo de conexão
 * para {@code CREATE DATABASE}/{@code DROP DATABASE}; nenhum objeto dele é
 * alterado. A senha vem exclusivamente da variável de ambiente já carregada
 * no processo e nunca é registrada.</p>
 */
class Aula17MigrationRollbackTest {

    private static final String CHANGELOG =
            "db/changelog/db.changelog-master.yaml";
    private static final String HOST = "jdbc:postgresql://localhost:5432/";
    private static final String BASE = HOST + "alsports_test";

    private static final String[] TABELAS = {
            "protecao_autenticacao",
            "evento_seguranca"
    };

    private static final String[] RESTRICOES = {
            "uk_protecao_autenticacao_chave_login",
            "ck_protecao_autenticacao_chave_hash",
            "ck_protecao_autenticacao_falhas_nao_negativas",
            "ck_evento_seguranca_tipo",
            "ck_evento_seguranca_ids_positivos",
            "ck_evento_seguranca_hash",
            "ck_evento_seguranca_origem_sem_quebra_linha"
    };

    private static final String[] INDICES = {
            "idx_protecao_autenticacao_bloqueada_ate",
            "idx_evento_seguranca_ocorrido",
            "idx_evento_seguranca_tipo_ocorrido"
    };

    private static final String COLUNA_GERACAO = "sessao_geracao";
    private static final String CK_GERACAO =
            "ck_conta_acesso_sessao_geracao_nao_negativa";
    private static final String CHECK_VIOLATION = "23514";

    @Test
    void migrations010E011DevemAplicarReverterEReaplicarEmBancoIsolado()
            throws Exception {

        String usuario = System.getenv("DB_TEST_USERNAME");
        if (usuario == null || usuario.isBlank()) {
            usuario = "alsports_app";
        }
        String senha = System.getenv("DB_TEST_PASSWORD");
        assertThat(senha).as("DB_TEST_PASSWORD deve estar disponível")
                .isNotBlank();

        String nomeBanco = "alsports_a17_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String urlTemporario = HOST + nomeBanco;

        try (Connection administrativa =
                DriverManager.getConnection(BASE, usuario, senha)) {
            executar(administrativa, "create database " + nomeBanco);
        }

        try {
            aplicar(urlTemporario, usuario, senha);
            verificarAplicada(urlTemporario, usuario, senha);
            verificarRestricoesAtivas(urlTemporario, usuario, senha);
            verificarMigration011Aplicada(urlTemporario, usuario, senha);

            reverter(1, urlTemporario, usuario, senha);
            verificarMigration011Revertida(urlTemporario, usuario, senha);

            aplicar(urlTemporario, usuario, senha);
            verificarAplicada(urlTemporario, usuario, senha);
            verificarMigration011Aplicada(urlTemporario, usuario, senha);

            reverter(4, urlTemporario, usuario, senha);
            verificarRevertida(urlTemporario, usuario, senha);

            aplicar(urlTemporario, usuario, senha);
            verificarAplicada(urlTemporario, usuario, senha);
            verificarMigration011Aplicada(urlTemporario, usuario, senha);
        } finally {
            try (Connection administrativa =
                    DriverManager.getConnection(BASE, usuario, senha)) {
                executar(administrativa,
                        "drop database if exists " + nomeBanco + " with (force)");
            }
        }
    }

    private void aplicar(String url, String usuario, String senha)
            throws Exception {

        try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
            Liquibase liquibase = criarLiquibase(conexao);
            try {
                liquibase.update();
            } finally {
                liquibase.close();
            }
        }
    }

    private void reverter(
            int quantidade, String url, String usuario, String senha)
            throws Exception {

        try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
            Liquibase liquibase = criarLiquibase(conexao);
            try {
                liquibase.rollback(quantidade, (String) null);
            } finally {
                liquibase.close();
            }
        }
    }

    private Liquibase criarLiquibase(Connection conexao) throws Exception {
        return new Liquibase(
                CHANGELOG,
                new ClassLoaderResourceAccessor(
                        Thread.currentThread().getContextClassLoader()),
                new JdbcConnection(conexao));
    }

    private void verificarAplicada(String url, String usuario, String senha)
            throws Exception {

        try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
            for (String tabela : TABELAS) {
                assertThat(existeTabela(conexao, tabela))
                        .as("tabela %s", tabela).isTrue();
            }
            for (String restricao : RESTRICOES) {
                assertThat(existeRestricao(conexao, restricao))
                        .as("restrição %s", restricao).isTrue();
            }
            for (String indice : INDICES) {
                assertThat(existeIndice(conexao, indice))
                        .as("índice %s", indice).isTrue();
            }
            assertThat(contarChangesets(conexao, "010-")).isEqualTo(3);
            assertThat(contarChangesets(conexao, "011-")).isEqualTo(1);
        }
    }

    private void verificarRevertida(String url, String usuario, String senha)
            throws Exception {

        try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
            for (String tabela : TABELAS) {
                assertThat(existeTabela(conexao, tabela))
                        .as("tabela %s removida", tabela).isFalse();
            }
            for (String restricao : RESTRICOES) {
                assertThat(existeRestricao(conexao, restricao))
                        .as("restrição %s removida", restricao).isFalse();
            }
            for (String indice : INDICES) {
                assertThat(existeIndice(conexao, indice))
                        .as("índice %s removido", indice).isFalse();
            }
            assertThat(contarChangesets(conexao, "010-")).isZero();
            assertThat(contarChangesets(conexao, "011-")).isZero();
        }
    }

    private void verificarRestricoesAtivas(
            String url, String usuario, String senha) throws Exception {

        try (Connection conexao = DriverManager.getConnection(
                url, usuario, senha)) {

            assertThat(sqlStateAoExecutar(conexao,
                    "insert into protecao_autenticacao "
                    + "(chave_login, falhas_consecutivas) "
                    + "values ('abc', 0)"))
                    .as("hash inválido deve ser rejeitado")
                    .isEqualTo(CHECK_VIOLATION);
            assertThat(sqlStateAoExecutar(conexao,
                    "insert into protecao_autenticacao "
                    + "(chave_login, falhas_consecutivas) "
                    + "values (repeat('a', 64), -1)"))
                    .as("contador negativo deve ser rejeitado")
                    .isEqualTo(CHECK_VIOLATION);
            assertThat(sqlStateAoExecutar(conexao,
                    "insert into evento_seguranca "
                    + "(tipo, ocorrido_em, endereco_origem) "
                    + "values ('TIPO_LIVRE', now(), null)"))
                    .as("tipo não enumerado deve ser rejeitado")
                    .isEqualTo(CHECK_VIOLATION);
            assertThat(sqlStateAoExecutar(conexao,
                    "insert into evento_seguranca "
                    + "(tipo, ocorrido_em, endereco_origem) "
                    + "values ('LOGIN_FALHA', now(), 'x' || chr(10) || 'y')"))
                    .as("quebra de linha na origem deve ser rejeitada")
                    .isEqualTo(CHECK_VIOLATION);
        }
    }

    private void verificarMigration011Aplicada(
            String url, String usuario, String senha) throws Exception {

        try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
            assertThat(existeColuna(conexao, "conta_acesso", COLUNA_GERACAO))
                    .as("coluna sessao_geracao").isTrue();
            assertThat(existeRestricao(conexao, CK_GERACAO))
                    .as("restrição de geração não negativa").isTrue();

            assertThat(coluna(conexao, "conta_acesso", COLUNA_GERACAO, "data_type"))
                    .isEqualTo("bigint");
            assertThat(coluna(conexao, "conta_acesso", COLUNA_GERACAO,
                    "is_nullable")).isEqualTo("NO");
            assertThat(coluna(conexao, "conta_acesso", COLUNA_GERACAO,
                    "column_default")).contains("0");

            executar(conexao, "insert into pessoa "
                    + "(nome, email, cpf, data_cadastro, status) values "
                    + "('Pessoa Migration', 'migration@example.com', "
                    + "'52998224725', current_date, 'ATIVO')");
            Long pessoaId = consultarLong(conexao,
                    "select id from pessoa where cpf = '52998224725'");

            assertThat(sqlStateAoExecutar(conexao,
                    "insert into conta_acesso "
                    + "(pessoa_id, login, senha_hash, status, sessao_geracao) "
                    + "values (" + pessoaId + ", 'migration.login', "
                    + "'{noop}hash', 'ATIVA', -1)"))
                    .as("geração negativa deve ser rejeitada")
                    .isEqualTo(CHECK_VIOLATION);

            assertThat(sqlStateAoExecutar(conexao,
                    "insert into conta_acesso "
                    + "(pessoa_id, login, senha_hash, status, sessao_geracao) "
                    + "values (" + pessoaId + ", 'migration.login', "
                    + "'{noop}hash', 'ATIVA', 0)"))
                    .as("geração zero deve ser aceita")
                    .isNull();

            assertThat(sqlStateAoExecutar(conexao,
                    "insert into evento_seguranca "
                    + "(tipo, ocorrido_em, endereco_origem) "
                    + "values ('OPERACAO_RECUSADA', now(), null)"))
                    .as("novo tipo de evento deve ser aceito")
                    .isNull();
            executar(conexao,
                    "delete from evento_seguranca where tipo = 'OPERACAO_RECUSADA'");
            executar(conexao, "delete from conta_acesso where login = 'migration.login'");
            executar(conexao, "delete from pessoa where cpf = '52998224725'");
        }
    }

    private void verificarMigration011Revertida(
            String url, String usuario, String senha) throws Exception {

        try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
            assertThat(existeColuna(conexao, "conta_acesso", COLUNA_GERACAO))
                    .as("coluna sessao_geracao removida").isFalse();
            assertThat(existeRestricao(conexao, CK_GERACAO))
                    .as("restrição de geração removida").isFalse();

            assertThat(sqlStateAoExecutar(conexao,
                    "insert into evento_seguranca "
                    + "(tipo, ocorrido_em, endereco_origem) "
                    + "values ('OPERACAO_RECUSADA', now(), null)"))
                    .as("rollback deve restaurar o vocabulário original")
                    .isEqualTo(CHECK_VIOLATION);
        }
    }

    private boolean existeTabela(Connection conexao, String tabela)
            throws SQLException {

        return existe(conexao, "select to_regclass(?) is not null",
                "public." + tabela);
    }

    private boolean existeRestricao(Connection conexao, String restricao)
            throws SQLException {

        return existe(conexao,
                "select exists(select 1 from pg_constraint where conname = ?)",
                restricao);
    }

    private boolean existeIndice(Connection conexao, String indice)
            throws SQLException {

        return existe(conexao,
                "select exists(select 1 from pg_indexes "
                + "where schemaname = 'public' and indexname = ?)",
                indice);
    }

    private boolean existeColuna(
            Connection conexao, String tabela, String coluna)
            throws SQLException {

        try (PreparedStatement ps = conexao.prepareStatement(
                "select exists(select 1 from information_schema.columns "
                + "where table_schema = 'public' and table_name = ? "
                + "and column_name = ?)")) {
            ps.setString(1, tabela);
            ps.setString(2, coluna);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    private String coluna(
            Connection conexao, String tabela, String coluna, String campo)
            throws SQLException {

        String sql = "select " + campo + " from information_schema.columns "
                + "where table_schema = 'public' and table_name = ? "
                + "and column_name = ?";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, tabela);
            ps.setString(2, coluna);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private Long consultarLong(Connection conexao, String sql)
            throws SQLException {

        try (PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private boolean existe(Connection conexao, String sql, String parametro)
            throws SQLException {

        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    private long contarChangesets(Connection conexao, String prefixo)
            throws SQLException {

        try (PreparedStatement ps = conexao.prepareStatement(
                "select count(*) from databasechangelog where id like ?")) {
            ps.setString(1, prefixo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * @return {@code null} quando o comando é aceito; caso contrário, o
     *         SQLState real devolvido pelo PostgreSQL.
     */
    private String sqlStateAoExecutar(Connection conexao, String sql) {
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.executeUpdate();
            return null;
        } catch (SQLException ex) {
            return ex.getSQLState();
        }
    }

    private void executar(Connection conexao, String sql) throws SQLException {
        try (Statement st = conexao.createStatement()) {
            st.executeUpdate(sql);
        }
    }
}
