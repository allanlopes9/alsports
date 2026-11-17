package com.curso.alsports.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Credencial de acesso associada a uma pessoa.
 *
 * <p>A conta é separada dos papéis de negócio ({@link Cliente} e
 * {@link Colaborador}). Uma pessoa pode ser cliente ou colaboradora sem
 * possuir acesso ao sistema, e uma conta não representa uma função
 * profissional.</p>
 *
 * <p>O hash da senha nunca deve ser exposto em respostas, logs ou
 * representações textuais.</p>
 */
@Entity
@Table(
        name = "conta_acesso",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conta_acesso_pessoa",
                columnNames = "pessoa_id"))
public class ContaAcesso {

    private static final int LOGIN_MAX_LENGTH = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "pessoa_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_conta_acesso_pessoa"))
    private Pessoa pessoa;

    @Column(nullable = false, length = LOGIN_MAX_LENGTH)
    private String login;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusContaAcesso status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "conta_acesso_perfil",
            joinColumns = @JoinColumn(
                    name = "conta_acesso_id",
                    nullable = false,
                    foreignKey = @ForeignKey(
                            name = "fk_conta_acesso_perfil_conta")))
    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 30)
    private Set<PerfilAcesso> perfis = new LinkedHashSet<>();

    protected ContaAcesso() {
    }

    public ContaAcesso(Pessoa pessoa, String login, String senhaHash) {
        this.pessoa = Objects.requireNonNull(pessoa, "Pessoa é obrigatória");
        this.login = validarLogin(login);
        this.senhaHash = validarSenhaHash(senhaHash);
        this.status = StatusContaAcesso.ATIVA;
        this.perfis.add(PerfilAcesso.OPERADOR);
    }

    public boolean podeAutenticar() {
        return status == StatusContaAcesso.ATIVA && pessoa.isAtiva();
    }

    public void alterarSenha(String senhaHash) {
        this.senhaHash = validarSenhaHash(senhaHash);
    }

    public void bloquear() {
        this.status = StatusContaAcesso.BLOQUEADA;
    }

    public void ativar() {
        this.status = StatusContaAcesso.ATIVA;
    }

    /**
     * Concede um perfil à conta. Usado pelo provisionamento da primeira
     * conta administrativa.
     */
    public void concederPerfil(PerfilAcesso perfil) {
        this.perfis.add(Objects.requireNonNull(
                perfil, "Perfil é obrigatório"));
    }

    /**
     * Substitui integralmente os perfis da conta.
     *
     * <p>A conta nunca pode ficar sem ao menos um perfil. A decisão sobre
     * remover o último administrador ativo pertence ao service, que conhece
     * o estado global do sistema.</p>
     */
    public void substituirPerfis(Set<PerfilAcesso> novosPerfis) {
        if (novosPerfis == null || novosPerfis.isEmpty()) {
            throw new IllegalArgumentException(
                    "A conta deve possuir ao menos um perfil");
        }

        // Set.of(...) e outras coleções imutáveis lançam NullPointerException
        // em contains(null); por isso a validação percorre os elementos.
        for (PerfilAcesso perfil : novosPerfis) {
            if (perfil == null) {
                throw new IllegalArgumentException("Perfil é obrigatório");
            }
        }

        this.perfis.clear();
        this.perfis.addAll(novosPerfis);
    }

    public boolean possuiPerfil(PerfilAcesso perfil) {
        return perfil != null && this.perfis.contains(perfil);
    }

    private static String validarLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login é obrigatório");
        }

        String valor = login.trim();

        if (valor.length() > LOGIN_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Login deve possuir no máximo 80 caracteres");
        }

        return valor;
    }

    private static String validarSenhaHash(String senhaHash) {
        if (senhaHash == null || senhaHash.isBlank()) {
            throw new IllegalArgumentException("Hash da senha é obrigatório");
        }

        return senhaHash;
    }

    public Long getId() {
        return id;
    }

    public Pessoa getPessoa() {
        return pessoa;
    }

    public String getLogin() {
        return login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public StatusContaAcesso getStatus() {
        return status;
    }

    public Set<PerfilAcesso> getPerfis() {
        return Collections.unmodifiableSet(perfis);
    }

    @Override
    public String toString() {
        return "ContaAcesso{id=" + id
                + ", login='" + login + '\''
                + ", status=" + status
                + ", perfis=" + perfis
                + '}';
    }
}
