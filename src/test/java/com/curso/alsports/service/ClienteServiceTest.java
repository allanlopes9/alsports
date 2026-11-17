package com.curso.alsports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.curso.alsports.exception.RecursoDuplicadoException;
import com.curso.alsports.exception.RecursoNaoEncontradoException;
import com.curso.alsports.model.Cliente;
import com.curso.alsports.model.Pessoa;
import com.curso.alsports.repository.ClienteRepository;
import com.curso.alsports.repository.PessoaRepository;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @Mock
    private PessoaRepository pessoaRepository;

    private ClienteService service;
    private Pessoa pessoa;

    @BeforeEach
    void setUp() {
        service = new ClienteService(repository, pessoaRepository);
        pessoa = new Pessoa(
                "Ana Souza",
                "ana@example.com",
                "52998224725",
                LocalDate.of(2026, 11, 17));
    }

    @Test
    void deveCadastrarClienteParaPessoaAtivaNormalizandoTelefone() {
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.existsByPessoaId(1L)).thenReturn(false);
        when(repository.save(any(Cliente.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        Cliente cliente = service.cadastrar(1L, "  (11) 99999-0000  ");

        assertAll(
                () -> assertThat(cliente.getPessoa()).isSameAs(pessoa),
                () -> assertThat(cliente.getTelefone())
                        .isEqualTo("(11) 99999-0000"));

        verify(repository).save(cliente);
    }

    @Test
    void naoDeveCadastrarClienteParaPessoaInexistente() {
        when(pessoaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cadastrar(99L, null))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Pessoa não encontrada: 99");

        verify(repository, never()).save(any(Cliente.class));
    }

    @Test
    void naoDeveCadastrarClienteParaPessoaInativa() {
        pessoa.inativar();
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));

        assertThatThrownBy(() -> service.cadastrar(1L, "1133334444"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Pessoa deve estar ativa para receber o papel de cliente");

        verify(repository, never()).existsByPessoaId(any());
        verify(repository, never()).save(any(Cliente.class));
    }

    @Test
    void naoDeveCadastrarClienteDuplicadoParaAMesmaPessoa() {
        when(pessoaRepository.findById(1L)).thenReturn(Optional.of(pessoa));
        when(repository.existsByPessoaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(1L, "1133334444"))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Pessoa já possui cadastro de cliente");

        verify(repository, never()).save(any(Cliente.class));
    }

    @Test
    void deveBuscarClientePorId() {
        Cliente cliente = new Cliente(pessoa, "1133334444");
        when(repository.findOneById(10L)).thenReturn(Optional.of(cliente));

        assertThat(service.buscarPorId(10L)).isSameAs(cliente);
    }

    @Test
    void naoDeveBuscarClienteInexistente() {
        when(repository.findOneById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado: 99");
    }

    @Test
    void naoDevePesquisarComCampoDeOrdenacaoInvalido() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("senha"));

        assertThatThrownBy(() -> service.pesquisar(null, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Campo de ordenação inválido: senha");

        verify(repository, never())
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void naoDevePesquisarSemPaginacao() {
        assertThatThrownBy(() -> service.pesquisar("ana", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A paginação deve ser informada");
    }

    @Test
    void devePesquisarComOrdenacaoPermitida() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("pessoa.nome"));
        Page<Cliente> esperado = Page.empty(pageable);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(esperado);

        Page<Cliente> encontrados = service.pesquisar("  Ana  ", pageable);

        assertThat(encontrados).isSameAs(esperado);
    }

    @Test
    void deveAtualizarTelefoneDoCliente() {
        Cliente cliente = new Cliente(pessoa, "1133334444");
        when(repository.findOneById(10L)).thenReturn(Optional.of(cliente));

        Cliente atualizado = service.atualizar(10L, "  (11) 98888-7777  ");

        assertThat(atualizado).isSameAs(cliente);
        assertThat(atualizado.getTelefone()).isEqualTo("(11) 98888-7777");
    }

    @Test
    void naoDeveAtualizarClienteInexistente() {
        when(repository.findOneById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(99L, "1133334444"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Cliente não encontrado: 99");
    }
}
