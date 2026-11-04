import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { ActivatedRoute, Route, Router } from '@angular/router';
import { Sheet } from '../Sheet.model';
import { SheetService } from '../sheet.services';

@Component({
  selector: 'app-sheet-player-get',
  templateUrl: './sheet-player-get.component.html',
  styleUrls: ['./sheet-player-get.component.css']
})
export class SheetPlayerGetComponent implements OnInit {

  formulario:FormGroup
  constructor(private sheetService: SheetService, private fb:FormBuilder, private router: 
    Router, private route: ActivatedRoute) { }
  id:number
  ngOnInit(): void {
    this.id = this.route.snapshot.params['id']
    
    this.createForm()
    this.sheetService.readById(this.id).subscribe({
      next:sheet1=>this.createFormP(sheet1),
      error:error=>console.log("erro")
    })
  }
  updateSheet(){
    this.sheetService.update(this.formulario.value).subscribe({
      next:sheet1=>{this.sheetService.showMenssage("Ficha Atualizada!");
      this.router.navigate(['/dash-board']).then(()=>window.location.reload())},
      error:error=>this.sheetService.showMenssage("Erro ao atualizar")
    })
  }
  cancel():void{
    this.router.navigate(['/dash-board'])
  }
  createForm(){
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
  }
  createFormP(sheet:Sheet){
    this.formulario=this.fb.group({
      id:[sheet.id],
      jogador:[sheet.jogador],
      personagem:[sheet.personagem],
      genero:[sheet.genero],
      idade:[sheet.idade],
      vidaAtual:[sheet.vidaAtual],
      vidaMaxima:[sheet.vidaMaxima],
      sanidadeAtual:[sheet.sanidadeAtual],
      sanidadeMaxima:[sheet.sanidadeMaxima],
      /*ATRIBUTOS*/
      forca:[sheet.forca],
      destreza:[sheet.destreza],
      constituicao:[sheet.constituicao],
      inteligencia:[sheet.inteligencia],
      carisma:[sheet.carisma],
      sabedoria:[sheet.sabedoria],
      tamanho:[sheet.tamanho],
      sorte:[sheet.sorte],
      poder:[sheet.poder],
      /*PERICIAS: grupo aninhado; chaves = codigos do enum Pericia*/
      pericias:this.fb.group({
        ACROBACIA:[sheet.pericias?.ACROBACIA ?? null],
        ATLETISMO:[sheet.pericias?.ATLETISMO ?? null],
        ADESTRAR_ANIMAIS:[sheet.pericias?.ADESTRAR_ANIMAIS ?? null],
        ATUACAO_LABIA:[sheet.pericias?.ATUACAO_LABIA ?? null],
        CHARME:[sheet.pericias?.CHARME ?? null],
        ESQUIVA:[sheet.pericias?.ESQUIVA ?? null],
        FURTIVIDADE:[sheet.pericias?.FURTIVIDADE ?? null],
        LUTA_CORPO_A_CORPO:[sheet.pericias?.LUTA_CORPO_A_CORPO ?? null],
        LUTA_ARMA_BRANCA:[sheet.pericias?.LUTA_ARMA_BRANCA ?? null],
        INTIMIDACAO:[sheet.pericias?.INTIMIDACAO ?? null],
        INTUICAO:[sheet.pericias?.INTUICAO ?? null],
        INVESTIGACAO:[sheet.pericias?.INVESTIGACAO ?? null],
        ARMAS_DE_FOGO_LEVES:[sheet.pericias?.ARMAS_DE_FOGO_LEVES ?? null],
        ARMAS_DE_FOGO_MEDIAS:[sheet.pericias?.ARMAS_DE_FOGO_MEDIAS ?? null],
        ARMAS_DE_FOGO_PESADAS:[sheet.pericias?.ARMAS_DE_FOGO_PESADAS ?? null],
        PERCEPCAO:[sheet.pericias?.PERCEPCAO ?? null],
        USAR_BIBLIOTECAS:[sheet.pericias?.USAR_BIBLIOTECAS ?? null],
        HISTORIA:[sheet.pericias?.HISTORIA ?? null],
        CURA:[sheet.pericias?.CURA ?? null],
        MALANDRAGEM:[sheet.pericias?.MALANDRAGEM ?? null],
        OCULTISMO:[sheet.pericias?.OCULTISMO ?? null],
        PSICOLOGIA:[sheet.pericias?.PSICOLOGIA ?? null],
        ARREMESSAR:[sheet.pericias?.ARREMESSAR ?? null]
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
}

