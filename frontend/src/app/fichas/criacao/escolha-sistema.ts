import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Primeiro passo de "nova ficha": escolher o sistema. Só Ordem tem formulário; os de D&D
 * aparecem desabilitados ("Em breve") para a escolha já ter o desenho final (2.1e).
 */
@Component({
  selector: 'app-escolha-sistema',
  imports: [RouterLink],
  templateUrl: './escolha-sistema.html',
  styleUrl: './escolha-sistema.scss',
})
export class EscolhaSistema {}
