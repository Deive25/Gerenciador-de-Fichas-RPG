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
    nameJ:[null],
    nameC:[null],
  	gender:[null],
    age:[null],
    lifeA:[null],
    lifeM:[null],
    sanA:[null],
    sanM:[null],
    /*ATRIBUTOS*/
    str:[null],
    dex:[null],
    cons:[null],
    int:[null],
    car:[null],
    sab:[null],
    tam:[null],
    sor:[null],
    pod:[null],
    /*PERICIAS*/
    atribute1:[null],
    atribute2:[null],
    atribute3:[null],
    atribute4:[null],
    atribute5:[null],
    atribute6:[null],
    atribute7:[null],
    atribute8:[null],
    atribute9:[null],
    atribute10:[null],
    atribute11:[null],
    atribute12:[null],
    atribute13:[null],
    atribute14:[null],
    atribute15:[null],
    atribute16:[null],
    atribute17:[null],
    atribute18:[null],
    atribute19:[null],
    atribute20:[null],
    atribute21:[null],
    atribute22:[null],
    atribute23:[null],
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
