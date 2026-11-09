import { Component, viewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ListaEditavel } from './lista-editavel';

interface Item {
  id: number;
  nome: string;
}

@Component({
  imports: [ListaEditavel],
  template: `
    <app-lista-editavel
      [itens]="itens"
      nomeDoItem="habilidade"
      vazio="Nenhuma habilidade ainda."
      [tituloDe]="titulo"
      (remover)="removidos.push($event)"
    >
      <ng-template #visualizacao let-item><span class="vis">{{ item.nome }}</span></ng-template>
      <ng-template #formulario let-item><span class="form">{{ item ? 'editar ' + item.nome : 'novo' }}</span></ng-template>
    </app-lista-editavel>
  `,
})
class Hospedeiro {
  itens: Item[] = [];
  removidos: Item[] = [];
  readonly titulo = (item: Item) => item.nome;
  readonly lista = viewChild.required(ListaEditavel<Item>);
}

function montar(itens: Item[]) {
  TestBed.configureTestingModule({ imports: [Hospedeiro] });
  const fixture = TestBed.createComponent(Hospedeiro);
  fixture.componentInstance.itens = itens;
  fixture.detectChanges();
  const el = fixture.nativeElement as HTMLElement;
  const botao = (texto: string) =>
    Array.from(el.querySelectorAll('button')).find((b) => b.textContent?.trim() === texto) as HTMLButtonElement;
  const clicar = (b: HTMLButtonElement) => {
    b.click();
    fixture.detectChanges();
  };
  const formularios = () => Array.from(el.querySelectorAll('.form')).map((f) => f.textContent);
  return { fixture, el, botao, clicar, formularios, host: fixture.componentInstance };
}

const A: Item = { id: 1, nome: 'Ataque especial' };
const B: Item = { id: 2, nome: 'Sentido aguçado' };

describe('ListaEditavel', () => {
  it('lista vazia mostra o texto de vazio', () => {
    const { el } = montar([]);
    expect(el.textContent).toContain('Nenhuma habilidade ainda.');
  });

  it('com itens mostra a visualizacao de cada um e esconde o texto de vazio', () => {
    const { el } = montar([A, B]);
    expect(Array.from(el.querySelectorAll('.vis')).map((v) => v.textContent)).toEqual([A.nome, B.nome]);
    expect(el.textContent).not.toContain('Nenhuma habilidade ainda.');
  });

  it('Adicionar habilidade abre o formulario com null', () => {
    const { botao, clicar, formularios, host } = montar([A]);
    clicar(botao('Adicionar habilidade'));
    expect(formularios()).toEqual(['novo']);
    expect(host.lista().editando()).toBe('novo');
  });

  it('Editar abre o formulario daquele item e fecha qualquer outro', () => {
    const { el, botao, clicar, formularios, host } = montar([A, B]);
    clicar(el.querySelector('button[aria-label="Editar Ataque especial"]')!);
    expect(formularios()).toEqual(['editar Ataque especial']);
    expect(el.querySelectorAll('.vis').length).toBe(1);

    clicar(el.querySelector('button[aria-label="Editar Sentido aguçado"]')!);
    expect(formularios()).toEqual(['editar Sentido aguçado']);

    clicar(botao('Adicionar habilidade'));
    expect(formularios()).toEqual(['novo']);
    expect(host.lista().editando()).toBe('novo');
  });

  it('Remover pede confirmacao; Cancelar nao emite', () => {
    const { el, botao, clicar, host } = montar([A]);
    clicar(el.querySelector('button[aria-label="Remover Ataque especial"]')!);
    expect(el.textContent).toContain('Remover Ataque especial?');
    clicar(botao('Cancelar'));
    expect(host.removidos).toEqual([]);
    expect(el.textContent).not.toContain('Remover Ataque especial?');
  });

  it('Sim, remover emite remover com o item', () => {
    const { el, botao, clicar, host } = montar([A, B]);
    clicar(el.querySelector('button[aria-label="Remover Sentido aguçado"]')!);
    clicar(botao('Sim, remover'));
    expect(host.removidos).toEqual([B]);
  });

  it('os botoes tem aria-label com o titulo do item', () => {
    const { el } = montar([A]);
    expect(el.querySelector('button[aria-label="Editar Ataque especial"]')).not.toBeNull();
    expect(el.querySelector('button[aria-label="Remover Ataque especial"]')).not.toBeNull();
  });
});
