import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiError } from '../core/api-error';
import { FichaApi } from './ficha-api';
import { espacosZerados } from './testes/espacos-zerados';
import { FichaOrdemEntrada, FichaResumo, ItemOrdem, MagiaDndEntrada, RitualOrdemEntrada } from './ficha.model';

const API = 'http://localhost:8080';

const ENTRADA: FichaOrdemEntrada = {
  sistema: 'ORDEM_PARANORMAL',
  jogador: 'Arthur',
  personagem: 'Lia',
  genero: 'F',
  idade: 34,
  classe: 'OCULTISTA',
  origem: 'Acadêmica',
  nex: 35,
  atributos: { AGI: 1, FOR: 1, INT: 3, PRE: 2, VIG: 1 },
  recursos: { PV: { atual: 9, maximo: 12 }, SAN: { atual: 8, maximo: 10 }, PE: { atual: null, maximo: null } },
  pericias: { INVESTIGACAO: 'VETERANO' },
};

describe('FichaApi', () => {
  let api: FichaApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(FichaApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listar faz GET /fichas', () => {
    api.listar().subscribe();
    const req = http.expectOne(`${API}/fichas`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('listar devolve o resumo das fichas (sistema e recursos), sem os dados completos', () => {
    let resumo: FichaResumo[] = [];
    api.listar().subscribe((r) => (resumo = r));
    const corpo: FichaResumo[] = [
      {
        id: 1, sistema: 'ORDEM_PARANORMAL', jogador: 'Arthur', personagem: 'Lia', idade: null, retratoVersao: null,
        recursos: [{ codigo: 'PV', atual: 9, maximo: 12 }],
      },
    ];
    http.expectOne(`${API}/fichas`).flush(corpo);
    expect(resumo[0].sistema).toBe('ORDEM_PARANORMAL');
    expect(resumo[0].recursos[0].codigo).toBe('PV');
  });

  it('criar faz POST /fichas com o corpo de Ordem, sem id', () => {
    api.criar(ENTRADA).subscribe();
    const req = http.expectOne(`${API}/fichas`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(ENTRADA);
    expect('id' in req.request.body).toBe(false);
    req.flush({ id: 1, ...ENTRADA });
  });

  it('atualizar faz PUT /fichas/7', () => {
    api.atualizar(7, ENTRADA).subscribe();
    const req = http.expectOne(`${API}/fichas/7`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(ENTRADA);
    req.flush({ id: 7, ...ENTRADA });
  });

  it('excluir faz DELETE /fichas/7', () => {
    api.excluir(7).subscribe();
    const req = http.expectOne(`${API}/fichas/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
  it('buscarFicha faz GET /fichas/7', () => {
    api.buscarFicha(7).subscribe();
    const req = http.expectOne(`${API}/fichas/7`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('atualizarRecursos faz PATCH /fichas/7/recursos so com o que foi passado e avisa que mudou', () => {
    const antes = api.versaoFichas();
    api.atualizarRecursos(7, { PV: 10 }).subscribe();
    const req = http.expectOne(`${API}/fichas/7/recursos`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ PV: 10 });
    req.flush({});
    expect(api.versaoFichas()).toBe(antes + 1);
  });

  it('testar faz POST /fichas/7/testes com alvo e codigo', () => {
    let resultado: unknown;
    api.testar(7, { alvo: 'PERICIA', codigo: 'INVESTIGACAO' }).subscribe((r) => (resultado = r));
    const req = http.expectOne(`${API}/fichas/7/testes`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ alvo: 'PERICIA', codigo: 'INVESTIGACAO' });
    const corpo = { alvo: 'PERICIA', codigo: 'INVESTIGACAO', nome: 'Investigação', dados: [4, 17, 9], mantido: 17, bonus: 5, total: 22 };
    req.flush(corpo);
    expect(resultado).toEqual(corpo);
  });

  describe('descricao e habilidades', () => {
    const HABILIDADE = { nome: 'Ataque Especial', tipo: 'CLASSE' as const, descricao: null };

    it('atualizarDescricao faz PUT /fichas/7/descricao e avisa que mudou', () => {
      const antes = api.versaoFichas();
      const descricao = { aparencia: 'Alta', historia: null, anotacoes: null };
      api.atualizarDescricao(7, descricao).subscribe();
      const req = http.expectOne(`${API}/fichas/7/descricao`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(descricao);
      req.flush(descricao);
      expect(api.versaoFichas()).toBe(antes + 1);
    });

    it('criarHabilidade faz POST /fichas/7/habilidades e avisa que mudou', () => {
      const antes = api.versaoFichas();
      api.criarHabilidade(7, HABILIDADE).subscribe();
      const req = http.expectOne(`${API}/fichas/7/habilidades`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(HABILIDADE);
      req.flush({ id: 3, ...HABILIDADE });
      expect(api.versaoFichas()).toBe(antes + 1);
    });

    it('atualizarHabilidade faz PUT /fichas/7/habilidades/3 e avisa que mudou', () => {
      const antes = api.versaoFichas();
      api.atualizarHabilidade(7, 3, HABILIDADE).subscribe();
      const req = http.expectOne(`${API}/fichas/7/habilidades/3`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(HABILIDADE);
      req.flush({ id: 3, ...HABILIDADE });
      expect(api.versaoFichas()).toBe(antes + 1);
    });

    it('excluirHabilidade faz DELETE /fichas/7/habilidades/3 e avisa que mudou', () => {
      const antes = api.versaoFichas();
      api.excluirHabilidade(7, 3).subscribe();
      const req = http.expectOne(`${API}/fichas/7/habilidades/3`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });
      expect(api.versaoFichas()).toBe(antes + 1);
    });
  });

  describe('inventario', () => {
    const ITEM = { nome: 'Faca', categoria: 0, espacos: 1, quantidade: 2, descricao: null };

    it('criarItem faz POST /fichas/7/itens e avisa que mudou', () => {
      const antes = api.versaoFichas();
      api.criarItem<ItemOrdem>(7, ITEM).subscribe();
      const req = http.expectOne(`${API}/fichas/7/itens`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(ITEM);
      req.flush({ id: 3, ...ITEM });
      expect(api.versaoFichas()).toBe(antes + 1);
    });

    it('atualizarItem faz PUT /fichas/7/itens/3 e avisa que mudou', () => {
      const antes = api.versaoFichas();
      api.atualizarItem<ItemOrdem>(7, 3, ITEM).subscribe();
      const req = http.expectOne(`${API}/fichas/7/itens/3`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(ITEM);
      req.flush({ id: 3, ...ITEM });
      expect(api.versaoFichas()).toBe(antes + 1);
    });

    it('excluirItem faz DELETE /fichas/7/itens/3 e avisa que mudou', () => {
      const antes = api.versaoFichas();
      api.excluirItem(7, 3).subscribe();
      const req = http.expectOne(`${API}/fichas/7/itens/3`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });
      expect(api.versaoFichas()).toBe(antes + 1);
    });

    it('atualizarMoedas faz PATCH /fichas/7/moedas com o corpo enviado e avisa que mudou', () => {
      const antes = api.versaoFichas();
      let devolvidas: unknown;
      api.atualizarMoedas(7, { po: 10 }).subscribe((m) => (devolvidas = m));
      const req = http.expectOne(`${API}/fichas/7/moedas`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ po: 10 });
      const todas = { pc: 0, pp: 0, pe: 0, po: 10, pl: 0 };
      req.flush(todas);
      expect(devolvidas).toEqual(todas);
      expect(api.versaoFichas()).toBe(antes + 1);
    });
  });

  describe('magias, rituais e espacos', () => {
    const MAGIA: MagiaDndEntrada = {
      nome: 'Bola de Fogo', nivel: 3, escola: 'Evocação', tempo: '1 ação', alcance: '45 m', componentes: 'V, S, M',
      duracao: 'Instantânea', concentracao: false, ritual: false, preparada: true, descricao: null,
    };
    const RITUAL: RitualOrdemEntrada = {
      nome: 'Rasgar a Realidade', circulo: 2, elemento: 'SANGUE', execucao: 'Padrão', alcance: 'Curto', alvo: null,
      duracao: null, resistencia: null, descricao: null,
    };

    it('criarMagia faz POST /fichas/7/magias e avisa que mudou', () => {
      const antes = api.versaoFichas();
      let criada: unknown;
      api.criarMagia(7, MAGIA).subscribe((m) => (criada = m));
      const req = http.expectOne(`${API}/fichas/7/magias`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(MAGIA);
      req.flush({ id: 1, ...MAGIA }, { status: 201, statusText: 'Created' });
      expect(criada).toEqual({ id: 1, ...MAGIA });
      expect(api.versaoFichas()).toBe(antes + 1);
    });

    it('atualizarMagia faz PUT /fichas/7/magias/1 e excluirMagia faz DELETE', () => {
      api.atualizarMagia(7, 1, MAGIA).subscribe();
      const put = http.expectOne(`${API}/fichas/7/magias/1`);
      expect(put.request.method).toBe('PUT');
      expect(put.request.body).toEqual(MAGIA);
      put.flush({ id: 1, ...MAGIA });

      api.excluirMagia(7, 1).subscribe();
      const del = http.expectOne(`${API}/fichas/7/magias/1`);
      expect(del.request.method).toBe('DELETE');
      del.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('criarRitual, atualizarRitual e excluirRitual usam /fichas/7/rituais', () => {
      api.criarRitual(7, RITUAL).subscribe();
      const post = http.expectOne(`${API}/fichas/7/rituais`);
      expect(post.request.method).toBe('POST');
      expect(post.request.body).toEqual(RITUAL);
      post.flush({ id: 2, ...RITUAL });

      api.atualizarRitual(7, 2, RITUAL).subscribe();
      const put = http.expectOne(`${API}/fichas/7/rituais/2`);
      expect(put.request.method).toBe('PUT');
      put.flush({ id: 2, ...RITUAL });

      api.excluirRitual(7, 2).subscribe();
      const del = http.expectOne(`${API}/fichas/7/rituais/2`);
      expect(del.request.method).toBe('DELETE');
      del.flush(null, { status: 204, statusText: 'No Content' });
    });

    it('atualizarEspacos faz PATCH /fichas/7/espacos so com os niveis enviados e devolve os nove', () => {
      const antes = api.versaoFichas();
      let devolvidos: unknown;
      api.atualizarEspacos(7, { '3': { total: 2, usados: 1 } }).subscribe((e) => (devolvidos = e));
      const req = http.expectOne(`${API}/fichas/7/espacos`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ '3': { total: 2, usados: 1 } });
      const todos = espacosZerados();
      todos['3'] = { total: 2, usados: 1 };
      req.flush(todos);
      expect(devolvidos).toEqual(todos);
      expect(api.versaoFichas()).toBe(antes + 1);
    });
  });

  it('buscarFicha devolve uma ficha de D&D sem mexer', () => {
    let ficha: unknown;
    api.buscarFicha(8).subscribe((f) => (ficha = f));
    const corpo = {
      id: 8, sistema: 'DND', jogador: 'A', personagem: 'B', genero: 'F', idade: null, retratoVersao: null,
      edicao: 'D2024', classe: 'Mago', especie: 'Elfo', antecedente: null, nivel: 3,
      atributos: { FOR: 8, DES: 14, CON: 12, INT: 16, SAB: 10, CAR: 9 },
      recursos: { PV: { atual: 10, maximo: 18 }, PV_TEMP: { atual: 0, maximo: null } },
      ca: 12, inspiracao: false, salvaguardas: ['INT', 'SAB'], pericias: { ARCANISMO: 'PROFICIENTE' },
    };
    http.expectOne(`${API}/fichas/8`).flush(corpo);
    expect(ficha).toEqual(corpo);
  });

  it('testar manda o modo quando vier e aceita salvaguarda', () => {
    api.testar(7, { alvo: 'SALVAGUARDA', codigo: 'DES', modo: 'VANTAGEM' }).subscribe();
    const req = http.expectOne(`${API}/fichas/7/testes`);
    expect(req.request.body).toEqual({ alvo: 'SALVAGUARDA', codigo: 'DES', modo: 'VANTAGEM' });
    req.flush({});
  });

  it('testar nao conta como alteracao da ficha', () => {
    const antes = api.versaoFichas();
    api.testar(7, { alvo: 'ATRIBUTO', codigo: 'AGI' }).subscribe();
    http.expectOne(`${API}/fichas/7/testes`).flush({});
    expect(api.versaoFichas()).toBe(antes);
  });


  it('avisa que as fichas mudaram so depois de criar, atualizar ou excluir com sucesso', () => {
    expect(api.versaoFichas()).toBe(0);

    api.criar(ENTRADA).subscribe();
    http.expectOne(`${API}/fichas`).flush({ id: 1, ...ENTRADA });
    api.atualizar(1, ENTRADA).subscribe();
    http.expectOne(`${API}/fichas/1`).flush({ id: 1, ...ENTRADA });
    api.excluir(1).subscribe();
    http.expectOne(`${API}/fichas/1`).flush(null, { status: 204, statusText: 'No Content' });
    expect(api.versaoFichas()).toBe(3);

    api.criar(ENTRADA).subscribe({ error: () => undefined });
    http.expectOne(`${API}/fichas`).flush({ title: 'Dados inválidos' }, { status: 400, statusText: 'Bad Request' });
    api.listar().subscribe();
    http.expectOne(`${API}/fichas`).flush([]);
    expect(api.versaoFichas()).toBe(3);
  });

  it('enviarRetrato faz PUT /fichas/7/retrato com o JPEG cru e avisa que as fichas mudaram', () => {
    const jpeg = new Blob([new Uint8Array([0xff, 0xd8, 0xff])], { type: 'image/jpeg' });
    api.enviarRetrato(7, jpeg).subscribe();
    const req = http.expectOne(`${API}/fichas/7/retrato`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.headers.get('Content-Type')).toBe('image/jpeg');
    expect(req.request.body).toBe(jpeg);
    req.flush(null, { status: 204, statusText: 'No Content' });
    expect(api.versaoFichas()).toBe(1);
  });

  it('removerRetrato faz DELETE /fichas/7/retrato e avisa que as fichas mudaram', () => {
    api.removerRetrato(7).subscribe();
    const req = http.expectOne(`${API}/fichas/7/retrato`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    expect(api.versaoFichas()).toBe(1);
  });

  it('baixarRetrato faz GET /fichas/7/retrato?v=123 pedindo um blob', () => {
    api.baixarRetrato(7, 123).subscribe();
    const req = http.expectOne(`${API}/fichas/7/retrato?v=123`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x']));
  });

  it('erros chegam como ApiError', () => {
    let recebido: ApiError | undefined;
    api.buscarFicha(9).subscribe({ error: (e: ApiError) => (recebido = e) });
    http
      .expectOne(`${API}/fichas/9`)
      .flush(
        { title: 'Ficha não encontrada', detail: 'Não existe ficha com o id 9.', status: 404 },
        { status: 404, statusText: 'Not Found' },
      );
    expect(recebido?.status).toBe(404);
    expect(recebido?.titulo).toBe('Ficha não encontrada');
  });

  describe('ataques e rolagem livre', () => {
    const ATAQUE_ORDEM = { nome: 'Faca', pericia: 'LUTA' as const, bonus: 2, dano: '1d4+1', tipoDano: 'corte', alcance: null, margem: 19, multiplicador: 3 };
    const ATAQUE_DND = { nome: 'Espada', atributo: 'FOR' as const, proficiente: true, bonus: 0, dano: '1d8+3', tipoDano: null, alcance: 'corpo a corpo' };

    it('criarAtaque faz POST /fichas/7/ataques e avisa que mudou', () => {
      const antes = api.versaoFichas();
      let criado: unknown;
      api.criarAtaque(7, ATAQUE_ORDEM).subscribe((a) => (criado = a));
      const req = http.expectOne(`${API}/fichas/7/ataques`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(ATAQUE_ORDEM);
      req.flush({ id: 3, ...ATAQUE_ORDEM });
      expect(criado).toEqual({ id: 3, ...ATAQUE_ORDEM });
      expect(api.versaoFichas()).toBe(antes + 1);
    });

    it('atualizarAtaque faz PUT /fichas/7/ataques/3 com os campos de D&D', () => {
      api.atualizarAtaque(7, 3, ATAQUE_DND).subscribe();
      const req = http.expectOne(`${API}/fichas/7/ataques/3`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(ATAQUE_DND);
      req.flush({ id: 3, ...ATAQUE_DND });
    });

    it('excluirAtaque faz DELETE /fichas/7/ataques/3 e avisa que mudou', () => {
      const antes = api.versaoFichas();
      api.excluirAtaque(7, 3).subscribe();
      const req = http.expectOne(`${API}/fichas/7/ataques/3`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });
      expect(api.versaoFichas()).toBe(antes + 1);
    });

    it('rolarAtaque sem modo faz POST .../ataques/3/rolagem com o corpo vazio e devolve o resultado', () => {
      let resultado: unknown;
      api.rolarAtaque(7, 3).subscribe((r) => (resultado = r));
      const req = http.expectOne(`${API}/fichas/7/ataques/3/rolagem`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      const corpo = {
        ataqueId: 3, nome: 'Espada longa', ataque: { dados: [14, 19], mantido: 19, bonus: 5, total: 24 },
        dano: { expressao: '1d8+3', dados: [{ lados: 8, valor: 6 }], bonus: 3, total: 9 }, critico: false,
      };
      req.flush(corpo);
      expect(resultado).toEqual(corpo);
    });

    it('rolarAtaque com modo manda o modo', () => {
      api.rolarAtaque(7, 3, 'VANTAGEM').subscribe();
      const req = http.expectOne(`${API}/fichas/7/ataques/3/rolagem`);
      expect(req.request.body).toEqual({ modo: 'VANTAGEM' });
      req.flush({});
    });

    it('rolarLivre faz POST /fichas/7/rolagens com a expressao e devolve o resultado', () => {
      let resultado: unknown;
      api.rolarLivre(7, '2d6+1d4+3').subscribe((r) => (resultado = r));
      const req = http.expectOne(`${API}/fichas/7/rolagens`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ expressao: '2d6+1d4+3' });
      const corpo = {
        expressao: '2d6+1d4+3', bonus: 3, total: 11,
        dados: [{ lados: 6, valor: 4 }, { lados: 6, valor: 1 }, { lados: 4, valor: 3 }],
      };
      req.flush(corpo);
      expect(resultado).toEqual(corpo);
    });

    it('rolar ataque e rolagem livre nao contam como alteracao da ficha', () => {
      const antes = api.versaoFichas();
      api.rolarAtaque(7, 3).subscribe();
      http.expectOne(`${API}/fichas/7/ataques/3/rolagem`).flush({});
      api.rolarLivre(7, '1d6').subscribe();
      http.expectOne(`${API}/fichas/7/rolagens`).flush({});
      expect(api.versaoFichas()).toBe(antes);
    });

    it('o erro de uma expressao invalida chega como ApiError com o campo expressao', () => {
      let erro: ApiError | undefined;
      api.rolarLivre(7, '1d7').subscribe({ error: (e: ApiError) => (erro = e) });
      http.expectOne(`${API}/fichas/7/rolagens`).flush(
        { title: 'Dados inválidos', status: 400, errors: { expressao: 'Não existe d7.' } },
        { status: 400, statusText: 'Bad Request' },
      );
      expect(erro?.campos['expressao']).toBe('Não existe d7.');
    });
  });
});
