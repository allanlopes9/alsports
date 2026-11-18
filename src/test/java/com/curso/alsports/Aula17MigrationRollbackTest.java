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
 * Aplica e reverte a migration 010 em um banco PostgreSQL temporário.
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

    @Test
    void migration010DeveAplicarReverterEReaplicarEmBancoIsolado()
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

            reverter(urlTemporario, usuario, senha);
            verificarRevertida(urlTemporario, usuario, senha);

            aplicar(urlTemporario, usuario, senha);
            verificarAplicada(urlTemporario, usuario, senha);
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

    private void reverter(String url, String usuario, String senha)
            throws Exception {

        try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
            Liquibase liquibase = criarLiquibase(conexao);
            try {
                liquibase.rollback(3, (String) null);
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
                assertThat(existe(conexao,
                        "select exists(select 1 from pg_constraint "
                        + "where conname = ?)", restricao))
                        .as("restrição %s", restricao).isTrue();
            }
            for (String indice : INDICES) {
                assertThat(existe(conexao,
                        "select exists(select 1 from pg_indexes "
                        + "where schemaname = 'public' and indexname = ?)",
                        indice))
                        .as("índice %s", indice).isTrue();
            }
            assertThat(contarChangesets010(conexao)).isEqualTo(3);
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
                assertThat(existe(conexao,
                        "select exists(select 1 from pg_constraint "
                        + "where conname = ?)", restricao))
                        .as("restrição %s removida", restricao).isFalse();
            }
            for (String indice : INDICES) {
                assertThat(existe(conexao,
                        "select exists(select 1 from pg_indexes "
                        + "where schemaname = 'public' and indexname = ?)",
                        indice))
                        .as("índice %s removido", indice).isFalse();
            }
            assertThat(contarChangesets010(conexao)).isZero();
        }
    }

    private void verificarRestricoesAtivas(
            String url, String usuario, String senha) throws Exception {

        try (Connection conexao = DriverManager.getConnection(url, usuario, senha)) {
            assertThat(falhaAoExecutar(conexao,
                    "insert into protecao_autenticacao "
                    + "(chave_login, falhas_consecutivas) "
                    + "values ('abc', 0)"))
                    .as("hash inválido deve ser rejeitado").isTrue();
            assertThat(falhaAoExecutar(conexao,
                    "insert into protecao_autenticacao "
                    + "(chave_login, falhas_consecutivas) "
                    + "values (repeat('a', 64), -1)"))
                    .as("contador negativo deve ser rejeitado").isTrue();
            assertThat(falhaAoExecutar(conexao,
                    "insert into evento_seguranca "
                    + "(tipo, ocorrido_em, endereco_origem) "
                    + "values ('TIPO_LIVRE', now(), null)"))
                    .as("tipo não enumerado deve ser rejeitado").isTrue();
            assertThat(falhaAoExecutar(conexao,
                    "insert into evento_seguranca "
                    + "(tipo, ocorrido_em, endereco_origem) "
                    + "values ('LOGIN_FALHA', now(), 'x' || chr(10) || 'y')"))
                    .as("quebra de linha na origem deve ser rejeitada").isTrue();
        }
    }

    private boolean existeTabela(Connection conexao, String tabela)
            throws SQLException {

        return existe(conexao, "select to_regclass(?) is not null",
                "public." + tabela);
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

    private long contarChangesets010(Connection conexao) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(
                "select count(*) from databasechangelog where id like '010-%'");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private boolean falhaAoExecutar(Connection conexao, String sql) {
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.executeUpdate();
            return false;
        } catch (SQLException ex) {
            return true;
        }
    }

    private void executar(Connection conexao, String sql) throws SQLException {
        try (Statement st = conexao.createStatement()) {
            st.executeUpdate(sql);
        }
    }
}
