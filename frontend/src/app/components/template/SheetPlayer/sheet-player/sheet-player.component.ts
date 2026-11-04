import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { SheetService } from '../sheet.services';

@Component({
  selector: 'app-sheet-player',
  templateUrl: './sheet-player.component.html',
  styleUrls: ['./sheet-player.component.css']
})
export class SheetPlayerComponent implements OnInit {
  formulario:FormGroup
  barra:any
  constructor(private sheetService: SheetService, private fb:FormBuilder, private router: Router) { }

  ngOnInit(): void {
    this.formulario=this.fb.group({
    id:[null],
    jogador:[null],
    personagem:[null],
  	genero:[null],
    idade:[null],
    vidaAtual:[null],
    vidaMaxima:[null],
    sanidadeAtual:[null],
    sanidadeMaxima:[null],
    /*ATRIBUTOS*/
    forca:[null],
    destreza:[null],
    constituicao:[null],
    inteligencia:[null],
    carisma:[null],
    sabedoria:[null],
    tamanho:[null],
    sorte:[null],
    poder:[null],
    /*PERICIAS: grupo aninhado; chaves = codigos do enum Pericia*/
      pericias:this.fb.group({
        ACROBACIA:[null],
        ATLETISMO:[null],
        ADESTRAR_ANIMAIS:[null],
        ATUACAO_LABIA:[null],
        CHARME:[null],
        ESQUIVA:[null],
        FURTIVIDADE:[null],
        LUTA_CORPO_A_CORPO:[null],
        LUTA_ARMA_BRANCA:[null],
        INTIMIDACAO:[null],
        INTUICAO:[null],
        INVESTIGACAO:[null],
        ARMAS_DE_FOGO_LEVES:[null],
        ARMAS_DE_FOGO_MEDIAS:[null],
        ARMAS_DE_FOGO_PESADAS:[null],
        PERCEPCAO:[null],
        USAR_BIBLIOTECAS:[null],
        HISTORIA:[null],
        CURA:[null],
        MALANDRAGEM:[null],
        OCULTISMO:[null],
        PSICOLOGIA:[null],
        ARREMESSAR:[null]
      }),
      })
   setInterval(function() {
    const maxWidth = 350;
    var life:number 
    var elementLA = (<HTMLInputElement>document.getElementById('lifeA'));
    var maxLife:number 
    var elementLM = (<HTMLInputElement>document.getElementById('lifeM'));

    if (elementLA != null) {
      life = parseInt(elementLA.value);
    }
    else {
      life = 0;
    }
    if (elementLM != null) {
      maxLife = parseInt(elementLM.value);
    }
    else {
        maxLife = 0;
    }
    var barra = document.getElementById('vida');
    if (!life && maxLife) life = maxLife;
    barra!.style.width = ((life * maxWidth) /maxLife) + "px";
    }, 200)

    setInterval(function() {
      const maxWidth = 350;
      var san:number 
      var elementSA = (<HTMLInputElement>document.getElementById('sanA'));
      var maxSan:number 
      var elementSM = (<HTMLInputElement>document.getElementById('sanM'));

      if (elementSA != null) {
        san = parseInt(elementSA.value);
      }
      else {
        san = 0;
      }
      if (elementSM != null) {
        maxSan = parseInt(elementSM.value);
      }
      else {
        maxSan = 0;
      }
      var barra = document.getElementById('sanidade');
      if (!san && maxSan) san = maxSan;
      barra!.style.width = ((san * maxWidth) /maxSan) + "px";
      }, 200)
    

  }
  createSheet():void{
    this.sheetService.create(this.formulario.value).subscribe({
      next:sheetSave=>{this.sheetService.showMenssage('Ficha Criada!');
      this.router.navigate(['/dash-board']).then(()=>window.location.reload())},
      error:sheetError=>this.sheetService.showMenssage("Erro ao Criar ficha! Falta preencher algum campo!")    
     })
  }
}
