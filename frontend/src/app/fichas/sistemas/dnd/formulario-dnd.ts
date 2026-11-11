import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  Injector,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
} from '@angular/core';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import {
  AtributoConjuracao,
  AtributoDnd,
  EdicaoDnd,
  Ficha,
  FichaDnd,
  FichaDndEntrada,
  GrauDnd,
  PericiaDnd,
  RecursoValor,
} from '../../ficha.model';
import {
  INTEIRO,
  aplicarErrosDoServidor,
  faixa,
  focarPrimeiroInvalido,
  focarPrimeiroInvalidoDepois,
  mensagemDoControle,
  paraNumero,
  registro,
  textoDe,
  textoOuNulo,
} from '../comum/formulario-comum';
import { ATRIBUTOS_DND, NOMES_EDICAO, NOME_GRAU_DND, PERICIAS_DND, rotuloEspecie } from './catalogo-dnd';

type CampoTexto = 'jogador' | 'personagem' | 'genero';

/** Valor bruto do formulário: números como texto ('' = vazio / sem proficiência), marcas como booleano. */
export interface ValorFormularioDnd {
  jogador: string;
  personagem: string;
  genero: string;
  idade: string;
  classe: string;
  especie: string;
  antecedente: string;
  nivel: string;
  /** '' = Nenhum (a ficha não conjura). */
  atributoConjuracao: string;
  atributos: Record<AtributoDnd, string>;
  recursos: { PV: { atual: string; maximo: string }; PV_TEMP: { atual: string } };
  ca: string;
  inspiracao: boolean;
  salvaguardas: Record<AtributoDnd, boolean>;
  pericias: Record<PericiaDnd, string>;
}

type Texto = FormControl<string>;
type Marca = FormControl<boolean>;
type FormularioDndGrupo = FormGroup<{
  jogador: Texto;
  personagem: Texto;
  genero: Texto;
  idade: Texto;
  classe: Texto;
  especie: Texto;
  antecedente: Texto;
  nivel: Texto;
  atributoConjuracao: Texto;
  atributos: FormGroup<Record<AtributoDnd, Texto>>;
  recursos: FormGroup<{
    PV: FormGroup<{ atual: Texto; maximo: Texto }>;
    PV_TEMP: FormGroup<{ atual: Texto }>;
  }>;
  ca: Texto;
  inspiracao: Marca;
  salvaguardas: FormGroup<Record<AtributoDnd, Marca>>;
  pericias: FormGroup<Record<PericiaDnd, Texto>>;
}>;

const CODIGOS_ATRIBUTO = ATRIBUTOS_DND.map((a) => a.codigo);
const CODIGOS_PERICIA = PERICIAS_DND.map((p) => p.codigo);

const NIVEL_MINIMO = 1;
const NIVEL_MAXIMO = 20;
const ATRIBUTO_MINIMO = 1;
const ATRIBUTO_MAXIMO = 30;

export function valorVazioDnd(): ValorFormularioDnd {
  return {
    jogador: '',
    personagem: '',
    genero: '',
    idade: '',
    classe: '',
    especie: '',
    antecedente: '',
    // Um personagem novo começa no nível 1 e com os atributos em 10 (modificador 0).
    nivel: String(NIVEL_MINIMO),
    atributoConjuracao: '',
    atributos: registro(CODIGOS_ATRIBUTO, () => '10'),
    recursos: { PV: { atual: '', maximo: '' }, PV_TEMP: { atual: '' } },
    ca: '',
    inspiracao: false,
    salvaguardas: registro(CODIGOS_ATRIBUTO, () => false),
    pericias: registro(CODIGOS_PERICIA, () => ''),
  };
}

/**
 * Converte o valor do formulário no corpo da API: vazio vira null, as salvaguardas marcadas viram
 * lista e as perícias sem proficiência saem do objeto. O PV temporário nunca leva máximo.
 */
export function paraEntradaDnd(valor: ValorFormularioDnd, edicao: EdicaoDnd): FichaDndEntrada {
  const pericias: Partial<Record<PericiaDnd, GrauDnd>> = {};
  for (const codigo of CODIGOS_PERICIA) {
    if (valor.pericias[codigo] !== '') {
      pericias[codigo] = valor.pericias[codigo] as GrauDnd;
    }
  }
  const pv: RecursoValor = { atual: paraNumero(valor.recursos.PV.atual), maximo: paraNumero(valor.recursos.PV.maximo) };
  const pvTemp: RecursoValor = { atual: paraNumero(valor.recursos.PV_TEMP.atual), maximo: null };
  return {
    sistema: 'DND',
    jogador: valor.jogador.trim(),
    personagem: valor.personagem.trim(),
    genero: valor.genero.trim(),
    idade: paraNumero(valor.idade),
    edicao,
    classe: textoOuNulo(valor.classe),
    especie: textoOuNulo(valor.especie),
    antecedente: textoOuNulo(valor.antecedente),
    nivel: Number(valor.nivel),
    atributoConjuracao: valor.atributoConjuracao === '' ? null : (valor.atributoConjuracao as AtributoConjuracao),
    atributos: registro(CODIGOS_ATRIBUTO, (codigo) => Number(valor.atributos[codigo])),
    recursos: { PV: pv, PV_TEMP: pvTemp },
    ca: paraNumero(valor.ca),
    inspiracao: valor.inspiracao,
    salvaguardas: CODIGOS_ATRIBUTO.filter((codigo) => valor.salvaguardas[codigo]),
    pericias,
  };
}

function paraValor(ficha: FichaDnd): ValorFormularioDnd {
  const valor = valorVazioDnd();
  valor.jogador = ficha.jogador;
  valor.personagem = ficha.personagem;
  valor.genero = ficha.genero;
  valor.idade = textoDe(ficha.idade);
  valor.classe = ficha.classe ?? '';
  valor.especie = ficha.especie ?? '';
  valor.antecedente = ficha.antecedente ?? '';
  valor.nivel = String(ficha.nivel);
  valor.atributoConjuracao = ficha.atributoConjuracao ?? '';
  for (const codigo of CODIGOS_ATRIBUTO) {
    valor.atributos[codigo] = String(ficha.atributos[codigo]);
    valor.salvaguardas[codigo] = ficha.salvaguardas.includes(codigo);
  }
  valor.recursos = {
    PV: { atual: textoDe(ficha.recursos.PV.atual), maximo: textoDe(ficha.recursos.PV.maximo) },
    PV_TEMP: { atual: textoDe(ficha.recursos.PV_TEMP.atual) },
  };
  valor.ca = textoDe(ficha.ca);
  valor.inspiracao = ficha.inspiracao;
  for (const codigo of CODIGOS_PERICIA) {
    valor.pericias[codigo] = ficha.pericias[codigo] ?? '';
  }
  return valor;
}

@Component({
  selector: 'app-formulario-dnd',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './formulario-dnd.html',
  styleUrl: './formulario-dnd.scss',
})
export class FormularioDnd {
  private readonly api = inject(FichaApi);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly cdr = inject(ChangeDetectorRef);

  /** A edição de uma ficha nova, vinda do `data` da rota (/fichas/nova/dnd-2014 ou dnd-2024). */
  readonly edicao = input<EdicaoDnd>();
  /** A ficha a editar, já carregada pela EdicaoFicha; sem ela o formulário é de uma ficha nova. */
  readonly ficha = input<FichaDnd | undefined>();
  readonly editando = computed(() => this.ficha() !== undefined);
  /** Na edição manda a edição da própria ficha (o formulário não oferece trocá-la). */
  readonly edicaoAtual = computed<EdicaoDnd>(() => this.ficha()?.edicao ?? this.edicao() ?? 'D2024');
  readonly nomeEdicao = computed(() => NOMES_EDICAO[this.edicaoAtual()]);
  readonly rotuloEspecie = computed(() => rotuloEspecie(this.edicaoAtual()));

  readonly atributos = ATRIBUTOS_DND;
  readonly pericias = PERICIAS_DND;
  readonly graus: readonly { valor: string; nome: string }[] = [
    { valor: '', nome: 'Sem proficiência' },
    { valor: 'PROFICIENTE', nome: NOME_GRAU_DND.PROFICIENTE },
    { valor: 'ESPECIALISTA', nome: NOME_GRAU_DND.ESPECIALISTA },
  ];
  /** Só os atributos de lançador; "Nenhum" (vazio) tira a CD e o ataque mágico da aba Magias. */
  readonly conjuracao: readonly { valor: string; nome: string }[] = [
    { valor: '', nome: 'Nenhum' },
    ...ATRIBUTOS_DND.filter((a) => a.codigo === 'INT' || a.codigo === 'SAB' || a.codigo === 'CAR').map((a) => ({
      valor: a.codigo,
      nome: a.nome,
    })),
  ];
  readonly identificacao: { chave: CampoTexto; nome: string }[] = [
    { chave: 'jogador', nome: 'Jogador(a)' },
    { chave: 'personagem', nome: 'Personagem' },
    { chave: 'genero', nome: 'Gênero' },
  ];

  readonly form: FormularioDndGrupo = this.criarFormulario();

  readonly salvando = signal(false);
  /** Erro ao salvar que não é de um campo específico (ex.: servidor indisponível). */
  readonly erroGeral = signal<ApiError | null>(null);

  readonly mensagem = mensagemDoControle;

  constructor() {
    effect(() => {
      const ficha = this.ficha();
      if (ficha !== undefined) {
        untracked(() => this.form.setValue(paraValor(ficha)));
      }
    });
  }

  private criarFormulario(): FormularioDndGrupo {
    const texto = () => this.fb.control('', [Validators.required, Validators.maxLength(255)]);
    const livre = () => this.fb.control('', Validators.maxLength(255));
    const inteiro = (...extras: ValidatorFn[]) => this.fb.control('', [Validators.pattern(INTEIRO), ...extras]);
    const padrao = valorVazioDnd();
    return this.fb.group({
      jogador: texto(),
      personagem: texto(),
      genero: texto(),
      idade: inteiro(),
      classe: livre(),
      especie: livre(),
      antecedente: livre(),
      nivel: this.fb.control(padrao.nivel, [
        Validators.required,
        Validators.pattern(INTEIRO),
        faixa(NIVEL_MINIMO, NIVEL_MAXIMO),
      ]),
      atributoConjuracao: this.fb.control(padrao.atributoConjuracao),
      atributos: this.fb.group(
        registro(CODIGOS_ATRIBUTO, () =>
          this.fb.control('10', [Validators.required, Validators.pattern(INTEIRO), faixa(ATRIBUTO_MINIMO, ATRIBUTO_MAXIMO)]),
        ),
      ),
      recursos: this.fb.group({
        PV: this.fb.group({ atual: inteiro(faixa(0)), maximo: inteiro(faixa(0)) }),
        PV_TEMP: this.fb.group({ atual: inteiro(faixa(0)) }),
      }),
      ca: inteiro(faixa(0)),
      inspiracao: this.fb.control(false),
      salvaguardas: this.fb.group(registro(CODIGOS_ATRIBUTO, () => this.fb.control(false))),
      pericias: this.fb.group(registro(CODIGOS_PERICIA, () => this.fb.control(''))),
    });
  }

  /** Só o nome do grau no select fechado. */
  nomeDoGrau(valor: string): string {
    return this.graus.find((g) => g.valor === valor)?.nome ?? '';
  }

  salvar(): void {
    if (this.salvando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      // No celular o formulário é longo: leva o usuário até o primeiro campo com problema.
      focarPrimeiroInvalido(this.host.nativeElement);
      return;
    }
    this.erroGeral.set(null);
    this.salvando.set(true);
    const entrada = paraEntradaDnd(this.form.getRawValue(), this.edicaoAtual());
    const ficha = this.ficha();
    const requisicao: Observable<Ficha> =
      ficha === undefined ? this.api.criar(entrada) : this.api.atualizar(ficha.id, entrada);
    // Só libera o botão se der erro: no sucesso a tela navega, e liberar antes disso
    // permitiria um segundo envio (ficha duplicada) enquanto a navegação termina.
    requisicao.subscribe({
      next: (salva) => void this.router.navigate(['/fichas', salva.id]),
      error: (erro: ApiError) => {
        this.salvando.set(false);
        this.mostrarErro(erro);
      },
    });
  }

  private mostrarErro(erro: ApiError): void {
    // O PV temporário só tem o campo "atual" na tela: o erro do máximo aparece nele.
    const campos = Object.fromEntries(
      Object.entries(erro.campos).map(([chave, texto]) => [
        chave === 'recursos.PV_TEMP.maximo' ? 'recursos.PV_TEMP.atual' : chave,
        texto,
      ]),
    );
    if (aplicarErrosDoServidor(this.form, { ...erro, campos })) {
      focarPrimeiroInvalidoDepois(this.host.nativeElement, this.injector, this.cdr);
    } else {
      this.erroGeral.set(erro);
    }
  }
}
