import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { Observable, finalize } from 'rxjs';
import { ApiError } from '../../core/api-error';
import { ATRIBUTOS, PERICIAS } from '../catalogo';
import { FichaApi } from '../ficha-api';
import { ChaveAtributo, CodigoPericia, Ficha, FichaEntrada } from '../ficha.model';

const CAMPOS_NUMERICOS = [
  'idade', 'vidaAtual', 'vidaMaxima', 'sanidadeAtual', 'sanidadeMaxima',
  'forca', 'destreza', 'constituicao', 'inteligencia', 'carisma', 'sabedoria', 'tamanho', 'sorte', 'poder',
] as const;

type CampoNumerico = (typeof CAMPOS_NUMERICOS)[number];
type CampoTexto = 'jogador' | 'personagem' | 'genero';

/** Valor bruto do formulário: tudo texto, como vem dos campos. */
export type ValorFormulario = { jogador: string; personagem: string; genero: string } & Record<CampoNumerico, string> & {
  pericias: Record<CodigoPericia, string>;
};

/** Inteiro com sinal opcional; vazio é permitido (vira null). */
const INTEIRO = /^-?\d+$/;

export function valorVazio(): ValorFormulario {
  const numericos = Object.fromEntries(CAMPOS_NUMERICOS.map((campo) => [campo, ''])) as Record<CampoNumerico, string>;
  const pericias = Object.fromEntries(PERICIAS.map((p) => [p.chave, ''])) as Record<CodigoPericia, string>;
  return { jogador: '', personagem: '', genero: '', ...numericos, pericias };
}

function paraNumero(texto: string): number | null {
  const limpo = texto.trim();
  return limpo === '' ? null : Number(limpo);
}

/** Converte o valor do formulário no corpo da API: vazio vira null e as perícias vazias saem do objeto. */
export function paraEntrada(valor: ValorFormulario): FichaEntrada {
  const numericos = Object.fromEntries(CAMPOS_NUMERICOS.map((campo) => [campo, paraNumero(valor[campo])])) as Record<
    CampoNumerico,
    number | null
  >;
  const pericias: Partial<Record<CodigoPericia, number>> = {};
  for (const { chave } of PERICIAS) {
    const numero = paraNumero(valor.pericias[chave]);
    if (numero !== null) {
      pericias[chave] = numero;
    }
  }
  return {
    jogador: valor.jogador.trim(),
    personagem: valor.personagem.trim(),
    genero: valor.genero.trim(),
    ...numericos,
    pericias,
  };
}

function paraValor(ficha: Ficha): ValorFormulario {
  const valor = valorVazio();
  valor.jogador = ficha.jogador;
  valor.personagem = ficha.personagem;
  valor.genero = ficha.genero;
  for (const campo of CAMPOS_NUMERICOS) {
    valor[campo] = ficha[campo] === null ? '' : String(ficha[campo]);
  }
  for (const { chave } of PERICIAS) {
    const numero = ficha.pericias[chave];
    valor.pericias[chave] = numero === undefined ? '' : String(numero);
  }
  return valor;
}

type ControlesNumericos = Record<CampoNumerico, FormControl<string>>;
type FormularioFicha = FormGroup<
  { jogador: FormControl<string>; personagem: FormControl<string>; genero: FormControl<string> } & ControlesNumericos & {
    pericias: FormGroup<Record<CodigoPericia, FormControl<string>>>;
  }
>;

@Component({
  selector: 'app-ficha-formulario',
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatTooltipModule],
  templateUrl: './ficha-formulario.html',
  styleUrl: './ficha-formulario.scss',
})
export class FichaFormulario {
  private readonly api = inject(FichaApi);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  /** Presente no modo editar (rota /fichas/:id/editar). */
  readonly id = input<string>();
  readonly editando = computed(() => this.id() !== undefined);

  readonly atributos = ATRIBUTOS;
  readonly pericias = PERICIAS;
  readonly identificacao: { chave: CampoTexto; nome: string }[] = [
    { chave: 'jogador', nome: 'Jogador(a)' },
    { chave: 'personagem', nome: 'Personagem' },
    { chave: 'genero', nome: 'Gênero' },
  ];
  readonly vidaESanidade: { chave: CampoNumerico; nome: string }[] = [
    { chave: 'vidaAtual', nome: 'Vida atual' },
    { chave: 'vidaMaxima', nome: 'Vida máxima' },
    { chave: 'sanidadeAtual', nome: 'Sanidade atual' },
    { chave: 'sanidadeMaxima', nome: 'Sanidade máxima' },
  ];

  readonly form: FormularioFicha = this.criarFormulario();

  readonly salvando = signal(false);
  readonly carregando = signal(false);
  readonly erroGeral = signal<ApiError | null>(null);

  constructor() {
    effect(() => {
      const id = this.id();
      if (id !== undefined) {
        untracked(() => this.carregar(Number(id)));
      }
    });
  }

  private criarFormulario(): FormularioFicha {
    const texto = () => this.fb.control('', [Validators.required, Validators.maxLength(255)]);
    const numero = () => this.fb.control('', Validators.pattern(INTEIRO));
    const numericos = Object.fromEntries(CAMPOS_NUMERICOS.map((campo) => [campo, numero()])) as ControlesNumericos;
    const pericias = Object.fromEntries(PERICIAS.map((p) => [p.chave, numero()])) as Record<
      CodigoPericia,
      FormControl<string>
    >;
    return this.fb.group({
      jogador: texto(),
      personagem: texto(),
      genero: texto(),
      ...numericos,
      pericias: this.fb.group(pericias),
    }) as FormularioFicha;
  }

  private carregar(id: number): void {
    this.carregando.set(true);
    this.api
      .buscar(id)
      .pipe(finalize(() => this.carregando.set(false)))
      .subscribe({
        next: (ficha) => this.form.setValue(paraValor(ficha)),
        error: (erro: ApiError) => this.erroGeral.set(erro),
      });
  }

  /** Mensagem do primeiro erro do controle, na ordem: servidor, obrigatório, tamanho, inteiro. */
  mensagem(controle: FormControl<string>): string | null {
    const erros = controle.errors;
    if (!erros || !(controle.touched || controle.dirty)) {
      return null;
    }
    if (erros['servidor']) {
      return erros['servidor'] as string;
    }
    if (erros['required']) {
      return 'Obrigatório';
    }
    if (erros['maxlength']) {
      return 'Máximo de 255 caracteres';
    }
    if (erros['pattern']) {
      return 'Use um número inteiro';
    }
    return null;
  }

  controle(campo: CampoTexto | CampoNumerico | ChaveAtributo): FormControl<string> {
    return this.form.controls[campo];
  }

  salvar(): void {
    if (this.salvando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.erroGeral.set(null);
    this.salvando.set(true);
    const entrada = paraEntrada(this.form.getRawValue());
    const id = this.id();
    const requisicao: Observable<Ficha> =
      id === undefined ? this.api.criar(entrada) : this.api.atualizar(Number(id), entrada);
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
    let algumNoCampo = false;
    for (const [campo, texto] of Object.entries(erro.campos)) {
      const controle = this.form.get(campo);
      if (controle) {
        controle.setErrors({ ...controle.errors, servidor: texto });
        controle.markAsTouched();
        algumNoCampo = true;
      }
    }
    if (!algumNoCampo) {
      this.erroGeral.set(erro);
    }
  }
}
