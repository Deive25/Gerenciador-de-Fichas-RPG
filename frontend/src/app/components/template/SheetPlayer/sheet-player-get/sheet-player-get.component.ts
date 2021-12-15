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
  }
  createFormP(sheet:Sheet){
    this.formulario=this.fb.group({
      id:[sheet.id],
      nameJ:[sheet.nameJ],
      nameC:[sheet.nameC],
      gender:[sheet.gender],
      age:[sheet.age],
      lifeA:[sheet.lifeA],
      lifeM:[sheet.lifeM],
      sanA:[sheet.sanA],
      sanM:[sheet.sanM],
      /*ATRIBUTOS*/
      str:[sheet.str],
      dex:[sheet.dex],
      cons:[sheet.cons],
      int:[sheet.int],
      car:[sheet.car],
      sab:[sheet.sab],
      tam:[sheet.tam],
      sor:[sheet.sor],
      pod:[sheet.pod],
      /*PERICIAS*/
      atribute1:[sheet.atribute1],
      atribute2:[sheet.atribute2],
      atribute3:[sheet.atribute3],
      atribute4:[sheet.atribute4],
      atribute5:[sheet.atribute5],
      atribute6:[sheet.atribute6],
      atribute7:[sheet.atribute7],
      atribute8:[sheet.atribute8],
      atribute9:[sheet.atribute9],
      atribute10:[sheet.atribute10],
      atribute11:[sheet.atribute11],
      atribute12:[sheet.atribute12],
      atribute13:[sheet.atribute13],
      atribute14:[sheet.atribute14],
      atribute15:[sheet.atribute15],
      atribute16:[sheet.atribute16],
      atribute17:[sheet.atribute17],
      atribute18:[sheet.atribute18],
      atribute19:[sheet.atribute19],
      atribute20:[sheet.atribute20],
      atribute21:[sheet.atribute21],
      atribute22:[sheet.atribute22],
      atribute23:[sheet.atribute23],
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

