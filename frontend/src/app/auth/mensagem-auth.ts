import { AbstractControl } from '@angular/forms';

/** Mensagem do primeiro erro do campo (o do servidor tem prioridade), só depois de tocado. */
export function mensagemAuth(controle: AbstractControl): string | null {
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
  if (erros['email']) {
    return 'Informe um e-mail válido';
  }
  if (erros['minlength']) {
    return `Mínimo de ${erros['minlength'].requiredLength} caracteres`;
  }
  if (erros['maxlength']) {
    return `Máximo de ${erros['maxlength'].requiredLength} caracteres`;
  }
  if (erros['requisitos']) {
    return 'Atenda aos requisitos abaixo';
  }
  return null;
}
