import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ApiError } from '../../../core/api-error';
import {
  INTEIRO,
  aplicarErrosDoServidor,
  caminhoDoCampo,
  decimal,
  faixa,
  focarPrimeiroInvalido,
  mensagemDoControle,
  paraDecimal,
  textoOuNulo,
} from './formulario-comum';

describe('mensagemDoControle', () => {
  it('maxlength usa o limite do proprio validador (255 e 4000)', () => {
    const curto = new FormControl('a'.repeat(256), Validators.maxLength(255));
    curto.markAsDirty();
    expect(mensagemDoControle(curto)).toBe('Máximo de 255 caracteres');

    const longo = new FormControl('a'.repeat(4001), Validators.maxLength(4000));
    longo.markAsDirty();
    expect(mensagemDoControle(longo)).toBe('Máximo de 4000 caracteres');
  });
});

function erroApi(campos: Record<string, string>): ApiError {
  return { status: 400, titulo: 'Dados inválidos', detalhe: 'x', campos };
}

describe('caminhoDoCampo', () => {
  it('normaliza as duas formas de chave do servidor', () => {
    expect(caminhoDoCampo('atributos[DES]')).toBe('atributos.DES');
    expect(caminhoDoCampo('recursos[PV].atual')).toBe('recursos.PV.atual');
    expect(caminhoDoCampo('atributos.CON')).toBe('atributos.CON');
    expect(caminhoDoCampo('nivel')).toBe('nivel');
  });
});

describe('INTEIRO e faixa', () => {
  it('INTEIRO aceita sinal opcional e recusa decimal', () => {
    expect(INTEIRO.test('-3')).toBe(true);
    expect(INTEIRO.test('12')).toBe(true);
    expect(INTEIRO.test('1.5')).toBe(false);
  });

  it('faixa com teto e sem teto; vazio e nao numerico ficam para os outros validadores', () => {
    expect(faixa(1, 20)(new FormControl('21'))).toEqual({ faixa: 'De 1 a 20' });
    expect(faixa(1, 20)(new FormControl('20'))).toBeNull();
    expect(faixa(0)(new FormControl('-1'))).toEqual({ faixa: '0 ou mais' });
    expect(faixa(0)(new FormControl('999999'))).toBeNull();
    expect(faixa(1, 20)(new FormControl(''))).toBeNull();
    expect(faixa(1, 20)(new FormControl('abc'))).toBeNull();
  });
});

describe('aplicarErrosDoServidor', () => {
  function formulario() {
    return new FormGroup({
      nome: new FormControl('', Validators.required),
      atributos: new FormGroup({ DES: new FormControl('10') }),
    });
  }

  it('poe o erro no controle certo, mantem os do cliente e devolve true', () => {
    const form = formulario();
    const caiu = aplicarErrosDoServidor(form, erroApi({ 'atributos[DES]': 'fora', nome: 'ruim' }));
    expect(caiu).toBe(true);
    expect(form.controls.atributos.controls.DES.errors).toEqual({ servidor: 'fora' });
    expect(form.controls.nome.errors).toEqual({ required: true, servidor: 'ruim' });
    expect(form.controls.nome.touched).toBe(true);
  });

  it('devolve false quando nenhuma chave aponta para um campo', () => {
    const form = formulario();
    expect(aplicarErrosDoServidor(form, erroApi({ inexistente: 'x' }))).toBe(false);
    expect(aplicarErrosDoServidor(form, erroApi({}))).toBe(false);
  });
});

describe('focarPrimeiroInvalido', () => {
  it('foca o primeiro input ou select invalido dentro do elemento', () => {
    const host = document.createElement('div');
    host.innerHTML = '<input id="a"><input id="b" class="ng-invalid"><input id="c" class="ng-invalid">';
    document.body.appendChild(host);
    focarPrimeiroInvalido(host);
    expect(document.activeElement?.id).toBe('b');
    host.remove();
  });
});

describe('paraDecimal', () => {
  it('aceita virgula ou ponto, ignora espacos e devolve null para vazio', () => {
    expect(paraDecimal('1,5')).toBe(1.5);
    expect(paraDecimal('1.50')).toBe(1.5);
    expect(paraDecimal('')).toBeNull();
    expect(paraDecimal('   ')).toBeNull();
    expect(paraDecimal(' 4 ')).toBe(4);
  });

  it('devolve NaN para o que nao e numero', () => {
    expect(paraDecimal('abc')).toBeNaN();
  });
});

describe('decimal', () => {
  const validar = (texto: string) => decimal(99999.99)(new FormControl(texto));

  it('aceita 0, virgula, ponto e o maximo', () => {
    for (const texto of ['0', '1,5', '1.25', '99999,99']) {
      expect(validar(texto), texto).toBeNull();
    }
  });

  it('recusa mais de 2 casas, texto, negativo e acima do maximo', () => {
    for (const texto of ['1,555', 'abc', '-1', '100000']) {
      expect(validar(texto), texto).toEqual({ decimal: expect.any(String) });
    }
  });

  it('deixa o vazio para o required', () => {
    expect(validar('')).toBeNull();
    expect(validar('  ')).toBeNull();
  });

  it('mensagemDoControle mostra o erro decimal', () => {
    const controle = new FormControl('1,555', decimal(99999.99));
    controle.markAsTouched();
    expect(mensagemDoControle(controle)).toBe(controle.errors?.['decimal']);
    expect(mensagemDoControle(controle)).toContain('99.999,99');
  });
});

describe('textoOuNulo', () => {
  it('tira os espacos das pontas e devolve null quando sobra nada', () => {
    expect(textoOuNulo('  V, S  ')).toBe('V, S');
    expect(textoOuNulo('   ')).toBeNull();
    expect(textoOuNulo('')).toBeNull();
  });
});
