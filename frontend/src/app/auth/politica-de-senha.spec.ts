import { requisitosDaSenha } from './politica-de-senha';

/** Mesmos exemplos do backend (PoliticaDeSenhaTest). A regra 6 (lista de senhas comuns) fica só no servidor. */
function ok(id: 'tamanho' | 'nome-email' | 'padrao', senha: string, nome = 'Ana Souza', email = 'ana@x.com'): boolean {
  return requisitosDaSenha(senha, nome, email).find((r) => r.id === id)!.ok;
}

describe('requisitosDaSenha', () => {
  it('devolve os 3 requisitos, na ordem, com os textos da tela', () => {
    const itens = requisitosDaSenha('', '', '');
    expect(itens.map((r) => r.id)).toEqual(['tamanho', 'nome-email', 'padrao']);
    expect(itens.map((r) => r.texto)).toEqual([
      '15 caracteres ou mais',
      'Sem o seu nome nem o seu e-mail',
      'Sem sequências nem repetições (como 123456 ou abcabc)',
    ]);
  });

  it('regra 1: 14 caracteres falha, 15 passa; espaços contam; emoji conta como 1', () => {
    expect(ok('tamanho', 'dragao na tave')).toBe(false);
    expect(ok('tamanho', 'dragao na taver')).toBe(true);
    expect(ok('tamanho', '🐉'.repeat(14))).toBe(false);
    expect(ok('tamanho', '🐉'.repeat(15))).toBe(true);
  });

  it('regra 3: trecho repetido falha', () => {
    for (const senha of ['aaaaaaaaaaaaaaa', 'senhasenhasenha', 'abcabcabcabcabc', 'Rolar Dados Rolar Dados ']) {
      expect(ok('padrao', senha)).toBe(false);
    }
    expect(ok('padrao', 'dragao e dragao e mais')).toBe(true);
  });

  it('regra 4: sequências e inversas falham, mesmo com espaços ou maiúsculas', () => {
    for (const senha of ['123456789012345', 'qwertyuiopasdfg', 'zyxwvutsrqponml', 'ABCDEFGHIJKLMNOPQRST', '1234 5678 9012 345', '9876543210987654']) {
      expect(ok('padrao', senha)).toBe(false);
    }
  });

  it('padrão: uma frase comum passa', () => {
    expect(ok('padrao', 'dragão vermelho na taverna')).toBe(true);
  });

  it('regra 5: palavra do nome (4+ letras, sem acento) ou parte local do e-mail na senha falha', () => {
    expect(ok('nome-email', 'o arthur rola dados demais', 'Arthur Spironello', 'a@x.com')).toBe(false);
    expect(ok('nome-email', 'Spironello na taverna!', 'Arthur Spironello', 'a@x.com')).toBe(false);
    expect(ok('nome-email', 'a Conceição chegou ontem', 'Maria da Conceicao', 'm@x.com')).toBe(false);
    expect(ok('nome-email', 'meu login mestre.kai fica aqui', 'Kai', 'mestre.kai@x.com')).toBe(false);
    expect(ok('nome-email', 'o ana rola dados demais', 'Ana Li', 'ana@x.com')).toBe(true);
    expect(ok('nome-email', 'dragão vermelho na taverna', '', '')).toBe(true);
  });
});
