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
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ApiError } from '../../../core/api-error';
import { FichaApi } from '../../ficha-api';
import {
  AtributoOrdem,
  ClasseOrdem,
  CodigoRecurso,
  Ficha,
  FichaOrdem,
  FichaOrdemEntrada,
  GrauOrdem,
  PericiaOrdem,
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
} from '../comum/formulario-comum';
import { ATRIBUTOS_ORDEM, PERICIAS_ORDEM, rotuloClasse } from './catalogo-ordem';

type CampoTexto = 'jogador' | 'personagem' | 'genero';

/** Valor bruto do formulário: tudo texto, como vem dos campos ('' = vazio / "A definir" / destreinado). */
export interface ValorFormulario {
  jogador: string;
  personagem: string;
  genero: string;
  idade: string;
  classe: string;
  origem: string;
  nex: string;
  atributos: Record<AtributoOrdem, string>;
  recursos: Record<CodigoRecurso, { atual: string; maximo: string }>;
  pericias: Record<PericiaOrdem, string>;
}

type Texto = FormControl<string>;
type FormularioOrdemGrupo = FormGroup<{
  jogador: Texto;
  personagem: Texto;
  genero: Texto;
  idade: Texto;
  classe: Texto;
  origem: Texto;
  nex: Texto;
  atributos: FormGroup<Record<AtributoOrdem, Texto>>;
  recursos: FormGroup<Record<CodigoRecurso, FormGroup<{ atual: Texto; maximo: Texto }>>>;
  pericias: FormGroup<Record<PericiaOrdem, Texto>>;
}>;

const CODIGOS_ATRIBUTO = ATRIBUTOS_ORDEM.map((a) => a.codigo);
const CODIGOS_PERICIA = PERICIAS_ORDEM.map((p) => p.codigo);
const RECURSOS: readonly { codigo: CodigoRecurso; nome: string }[] = [
  { codigo: 'PV', nome: 'PV' },
  { codigo: 'SAN', nome: 'SAN' },
  { codigo: 'PE', nome: 'PE' },
];

const NEX_MINIMO = 5;
const NEX_MAXIMO = 99;
const ATRIBUTO_MAXIMO = 5;

/** O NEX mínimo de cada grau (as mesmas regras do servidor). */
const NEX_DO_GRAU: Record<GrauOrdem, number> = { TREINADO: 0, VETERANO: 35, EXPERT: 70 };

const CLASSES: readonly ClasseOrdem[] = ['COMBATENTE', 'ESPECIALISTA', 'OCULTISTA'];

export function valorVazio(): ValorFormulario {
  return {
    jogador: '',
    personagem: '',
    genero: '',
    idade: '',
    classe: '',
    origem: '',
    // Um personagem novo começa no NEX 5 com os atributos em 1 (o servidor exige os 5).
    nex: String(NEX_MINIMO),
    atributos: registro(CODIGOS_ATRIBUTO, () => '1'),
    recursos: registro(['PV', 'SAN', 'PE'] as const, () => ({ atual: '', maximo: '' })),
    pericias: registro(CODIGOS_PERICIA, () => ''),
  };
}

/** Converte o valor do formulário no corpo da API: vazio vira null e as perícias sem grau saem do objeto. */
export function paraEntrada(valor: ValorFormulario): FichaOrdemEntrada {
  const pericias: Partial<Record<PericiaOrdem, GrauOrdem>> = {};
  for (const codigo of CODIGOS_PERICIA) {
    if (valor.pericias[codigo] !== '') {
      pericias[codigo] = valor.pericias[codigo] as GrauOrdem;
    }
  }
  const recurso = (v: { atual: string; maximo: string }): RecursoValor => ({
    atual: paraNumero(v.atual),
    maximo: paraNumero(v.maximo),
  });
  return {
    sistema: 'ORDEM_PARANORMAL',
    jogador: valor.jogador.trim(),
    personagem: valor.personagem.trim(),
    genero: valor.genero.trim(),
    idade: paraNumero(valor.idade),
    classe: valor.classe === '' ? null : (valor.classe as ClasseOrdem),
    origem: valor.origem.trim() === '' ? null : valor.origem.trim(),
    nex: Number(valor.nex),
    atributos: registro(CODIGOS_ATRIBUTO, (codigo) => Number(valor.atributos[codigo])),
    recursos: { PV: recurso(valor.recursos.PV), SAN: recurso(valor.recursos.SAN), PE: recurso(valor.recursos.PE) },
    pericias,
  };
}

function paraValor(ficha: FichaOrdem): ValorFormulario {
  const valor = valorVazio();
  valor.jogador = ficha.jogador;
  valor.personagem = ficha.personagem;
  valor.genero = ficha.genero;
  valor.idade = textoDe(ficha.idade);
  valor.classe = ficha.classe ?? '';
  valor.origem = ficha.origem ?? '';
  valor.nex = String(ficha.nex);
  for (const codigo of CODIGOS_ATRIBUTO) {
    valor.atributos[codigo] = String(ficha.atributos[codigo]);
  }
  for (const { codigo } of RECURSOS) {
    valor.recursos[codigo] = {
      atual: textoDe(ficha.recursos[codigo].atual),
      maximo: textoDe(ficha.recursos[codigo].maximo),
    };
  }
  for (const codigo of CODIGOS_PERICIA) {
    valor.pericias[codigo] = ficha.pericias[codigo] ?? '';
  }
  return valor;
}

/** Tira só o erro do servidor, mantendo os do próprio formulário (obrigatório, faixa...). */
function limparErroServidor(controle: AbstractControl): void {
  const { servidor: _servidor, ...resto } = controle.errors ?? {};
  controle.setErrors(Object.keys(resto).length > 0 ? resto : null);
}

@Component({
  selector: 'app-formulario-ordem',
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatSelectModule, MatTooltipModule],
  templateUrl: './formulario-ordem.html',
  styleUrl: './formulario-ordem.scss',
})
export class FormularioOrdem {
  private readonly api = inject(FichaApi);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly cdr = inject(ChangeDetectorRef);

  /** A ficha a editar, já carregada pela EdicaoFicha; sem ela o formulário é de uma ficha nova. */
  readonly ficha = input<FichaOrdem | undefined>();
  readonly editando = computed(() => this.ficha() !== undefined);

  readonly atributos = ATRIBUTOS_ORDEM;
  readonly pericias = PERICIAS_ORDEM;
  readonly recursos = RECURSOS;
  readonly classes = [
    { valor: '', nome: rotuloClasse(null) },
    ...CLASSES.map((classe) => ({ valor: classe as string, nome: rotuloClasse(classe) })),
  ];
  readonly graus: readonly { valor: string; nome: string; nexMinimo: number }[] = [
    { valor: '', nome: 'Destreinado', nexMinimo: 0 },
    { valor: 'TREINADO', nome: 'Treinado', nexMinimo: NEX_DO_GRAU.TREINADO },
    { valor: 'VETERANO', nome: 'Veterano', nexMinimo: NEX_DO_GRAU.VETERANO },
    { valor: 'EXPERT', nome: 'Expert', nexMinimo: NEX_DO_GRAU.EXPERT },
  ];
  readonly identificacao: { chave: CampoTexto; nome: string }[] = [
    { chave: 'jogador', nome: 'Jogador(a)' },
    { chave: 'personagem', nome: 'Personagem' },
    { chave: 'genero', nome: 'Gênero' },
  ];

  readonly form: FormularioOrdemGrupo = this.criarFormulario();

  /** O NEX digitado, como número: os graus que ele não permite ficam desabilitados. */
  private readonly nexTexto = toSignal(this.form.controls.nex.valueChanges, {
    initialValue: this.form.controls.nex.value,
  });
  readonly nex = computed(() => {
    const numero = paraNumero(this.nexTexto());
    return numero === null || Number.isNaN(numero) ? 0 : numero;
  });

  readonly salvando = signal(false);
  /** Erro ao salvar que não é de um campo específico (ex.: servidor indisponível). */
  readonly erroGeral = signal<ApiError | null>(null);

  constructor() {
    effect(() => {
      const ficha = this.ficha();
      if (ficha !== undefined) {
        untracked(() => this.form.setValue(paraValor(ficha)));
      }
    });
    // Um erro "Veterano exige NEX 35%" colado numa perícia deixaria o formulário inválido
    // mesmo depois de a pessoa corrigir o NEX: mexer no NEX limpa os erros do servidor das perícias.
    this.form.controls.nex.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      for (const controle of Object.values(this.form.controls.pericias.controls)) {
        if (controle.errors?.['servidor']) {
          limparErroServidor(controle);
        }
      }
    });
  }

  private criarFormulario(): FormularioOrdemGrupo {
    const texto = () => this.fb.control('', [Validators.required, Validators.maxLength(255)]);
    const inteiro = (...extras: ValidatorFn[]) => this.fb.control('', [Validators.pattern(INTEIRO), ...extras]);
    const padrao = valorVazio();
    return this.fb.group({
      jogador: texto(),
      personagem: texto(),
      genero: texto(),
      idade: inteiro(),
      classe: this.fb.control(''),
      origem: this.fb.control('', Validators.maxLength(255)),
      nex: this.fb.control(padrao.nex, [Validators.required, Validators.pattern(INTEIRO), faixa(NEX_MINIMO, NEX_MAXIMO)]),
      atributos: this.fb.group(
        registro(CODIGOS_ATRIBUTO, () =>
          this.fb.control('1', [Validators.required, Validators.pattern(INTEIRO), faixa(0, ATRIBUTO_MAXIMO)]),
        ),
      ),
      recursos: this.fb.group(
        registro(['PV', 'SAN', 'PE'] as const, () =>
          this.fb.group({
            atual: inteiro(faixa(0)),
            maximo: inteiro(faixa(0)),
          }),
        ),
      ),
      pericias: this.fb.group(registro(CODIGOS_PERICIA, () => this.fb.control(''))),
    });
  }

  readonly mensagem = mensagemDoControle;

  /** Só o nome do grau no select fechado (a dica "exige NEX" fica na lista de opções). */
  nomeDoGrau(valor: string): string {
    return this.graus.find((g) => g.valor === valor)?.nome ?? '';
  }

  /** O grau só pode ser escolhido se o NEX atual o permite (o servidor confere de novo). */
  grauPermitido(nexMinimo: number): boolean {
    return this.nex() >= nexMinimo;
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
    const entrada = paraEntrada(this.form.getRawValue());
    const ficha = this.ficha();
    const requisicao: Observable<Ficha> =
      ficha === undefined ? this.api.criar(entrada) : this.api.atualizar(ficha.id, entrada);
    // Só libera o botão se der erro: no sucesso a tela navega, e liberar antes disso
    // permitiria um segundo envio (ficha duplicada) enquanto a navegação termina.
    requisicao.subscribe({
      next: (ficha) => void this.router.navigate(['/fichas', ficha.id]),
      error: (erro: ApiError) => {
        this.salvando.set(false);
        this.mostrarErro(erro);
      },
    });
  }

  private mostrarErro(erro: ApiError): void {
    if (aplicarErrosDoServidor(this.form, erro)) {
      focarPrimeiroInvalidoDepois(this.host.nativeElement, this.injector, this.cdr);
    } else {
      this.erroGeral.set(erro);
    }
  }
}
