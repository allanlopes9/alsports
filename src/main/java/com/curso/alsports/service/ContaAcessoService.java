package com.curso.alsports.service;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.dto.ContaAcessoRequest;
import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.PerfilAcesso;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.model.StatusContaAcesso;
import com.curso.alsports.repository.ContaAcessoRepository;
import com.curso.alsports.repository.PessoaRepository;

@Service
public class ContaAcessoService {

    private static final int LOGIN_MIN_CARACTERES = 3;
    private static final int LOGIN_MAX_CARACTERES = 80;
    private static final int SENHA_MIN_CARACTERES = 12;
    private static final int SENHA_MAX_CARACTERES = 72;
    private static final int SENHA_MAX_BYTES_UTF8 = 72;
    private static final Pattern LOGIN_PERMITIDO =
            Pattern.compile("^[A-Za-z0-9._-]+$");

    private final ContaAcessoRepository repository;
    private final PessoaRepository pessoaRepository;
    private final PasswordEncoder passwordEncoder;

    public ContaAcessoService(
            ContaAcessoRepository repository,
            PessoaRepository pessoaRepository,
            PasswordEncoder passwordEncoder) {

        this.repository = repository;
        this.pessoaRepository = pessoaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso cadastrar(ContaAcessoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Dados da conta de acesso são obrigatórios");
        }

        if (request.pessoaId() == null) {
            throw new IllegalArgumentException("Pessoa é obrigatória");
        }

        String login = validarLogin(request.login());
        validarSenha(request.senha());

        Pessoa pessoa = pessoaRepository.findById(request.pessoaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pessoa não encontrada: " + request.pessoaId()));

        if (!pessoa.isAtiva()) {
            throw new IllegalArgumentException(
                    "Pessoa inativa não pode possuir conta de acesso");
        }

        if (repository.existsByPessoaId(request.pessoaId())) {
            throw new RecursoDuplicadoException(
                    "Pessoa já possui conta de acesso");
        }

        if (repository.existsByLoginIgnoreCase(login)) {
            throw new RecursoDuplicadoException("Login já cadastrado");
        }

        String senhaHash = passwordEncoder.encode(request.senha());

        return repository.save(new ContaAcesso(pessoa, login, senhaHash));
    }

    @Transactional(readOnly = true)
    public ContaAcesso buscarPorLogin(String login) {
        String loginNormalizado = login == null ? "" : login.trim();

        if (loginNormalizado.isEmpty()) {
            throw new RecursoNaoEncontradoException(
                    "Conta de acesso não encontrada");
        }

        return repository.findByLoginIgnoreCase(loginNormalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Conta de acesso não encontrada"));
    }

    @Transactional
    public void alterarSenha(
            String login,
            String senhaAtual,
            String novaSenha) {

        if (senhaAtual == null || senhaAtual.isBlank()) {
            throw new IllegalArgumentException("Senha atual é obrigatória");
        }

        validarSenha(novaSenha);

        ContaAcesso conta = buscarPorLogin(login);

        if (!passwordEncoder.matches(senhaAtual, conta.getSenhaHash())) {
            throw new IllegalArgumentException("Senha atual inválida");
        }

        if (passwordEncoder.matches(novaSenha, conta.getSenhaHash())) {
            throw new IllegalArgumentException(
                    "Nova senha deve ser diferente da senha atual");
        }

        conta.alterarSenha(passwordEncoder.encode(novaSenha));
    }

    /**
     * Substitui os perfis de uma conta. A conta autenticada é identificada
     * pelo login do token e não por dados enviados no corpo da requisição.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso alterarPerfis(
            Long contaId,
            Set<PerfilAcesso> novosPerfis,
            String loginAdministrador) {

        if (contaId == null) {
            throw new IllegalArgumentException(
                    "Identificador da conta é obrigatório");
        }

        if (novosPerfis == null || novosPerfis.isEmpty()) {
            throw new IllegalArgumentException(
                    "A conta deve possuir ao menos um perfil");
        }

        ContaAcesso conta = buscarDetalhada(contaId);
        String administrador = validarLoginAdministrador(loginAdministrador);

        boolean removeAdministrador = conta.possuiPerfil(
                PerfilAcesso.ADMINISTRADOR)
                && !novosPerfis.contains(PerfilAcesso.ADMINISTRADOR);

        if (removeAdministrador
                && conta.getLogin().equalsIgnoreCase(administrador)) {
            throw new IllegalArgumentException(
                    "O administrador não pode remover o próprio perfil "
                            + "ADMINISTRADOR");
        }

        if (removeAdministrador
                && conta.getStatus() == StatusContaAcesso.ATIVA
                && contarAdministradoresAtivos() <= 1) {
            throw new IllegalArgumentException(
                    "O sistema deve manter ao menos um administrador ativo");
        }

        conta.substituirPerfis(novosPerfis);

        return conta;
    }

    /**
     * Ativa ou bloqueia uma conta. O bloqueio da própria conta e a remoção do
     * último administrador ativo são recusados para preservar a capacidade de
     * administração do sistema.
     */
    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ContaAcesso alterarStatus(
            Long contaId,
            StatusContaAcesso novoStatus,
            String loginAdministrador) {

        if (contaId == null) {
            throw new IllegalArgumentException(
                    "Identificador da conta é obrigatório");
        }

        if (novoStatus == null) {
            throw new IllegalArgumentException("Status é obrigatório");
        }

        ContaAcesso conta = buscarDetalhada(contaId);
        String administrador = validarLoginAdministrador(loginAdministrador);

        if (novoStatus == StatusContaAcesso.BLOQUEADA) {
            if (conta.getLogin().equalsIgnoreCase(administrador)) {
                throw new IllegalArgumentException(
                        "O administrador não pode bloquear a própria conta");
            }

            if (conta.getStatus() == StatusContaAcesso.ATIVA
                    && conta.possuiPerfil(PerfilAcesso.ADMINISTRADOR)
                    && contarAdministradoresAtivos() <= 1) {
                throw new IllegalArgumentException(
                        "O sistema deve manter ao menos um administrador ativo");
            }

            conta.bloquear();
        } else {
            conta.ativar();
        }

        return conta;
    }

    private ContaAcesso buscarDetalhada(Long contaId) {
        return repository.findById(contaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Conta de acesso não encontrada: " + contaId));
    }

    private long contarAdministradoresAtivos() {
        return repository.contarPorPerfilEStatus(
                PerfilAcesso.ADMINISTRADOR,
                StatusContaAcesso.ATIVA);
    }

    private static String validarLoginAdministrador(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException(
                    "Administrador autenticado é obrigatório");
        }

        return login.trim();
    }

    static String validarLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Login é obrigatório");
        }

        String loginNormalizado = login.trim();

        if (loginNormalizado.length() < LOGIN_MIN_CARACTERES
                || loginNormalizado.length() > LOGIN_MAX_CARACTERES) {

            throw new IllegalArgumentException(
                    "Login deve possuir entre 3 e 80 caracteres");
        }

        if (!LOGIN_PERMITIDO.matcher(loginNormalizado).matches()) {
            throw new IllegalArgumentException(
                    "Login deve conter apenas letras, números, ponto, "
                            + "sublinhado ou hífen");
        }

        return loginNormalizado;
    }

    static void validarSenha(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória");
        }

        if (senha.length() < SENHA_MIN_CARACTERES
                || senha.length() > SENHA_MAX_CARACTERES) {

            throw new IllegalArgumentException(
                    "Senha deve possuir entre 12 e 72 caracteres");
        }

        if (senha.getBytes(StandardCharsets.UTF_8).length
                > SENHA_MAX_BYTES_UTF8) {

            throw new IllegalArgumentException(
                    "Senha deve possuir no máximo 72 bytes em UTF-8");
        }
    }
}
