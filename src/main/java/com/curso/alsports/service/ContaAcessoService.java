package com.curso.alsports.service;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.curso.alsports.dto.ContaAcessoRequest;
import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.ContaAcesso;
import com.curso.alsports.model.Pessoa;
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

    private static String validarLogin(String login) {
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

    private static void validarSenha(String senha) {
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
