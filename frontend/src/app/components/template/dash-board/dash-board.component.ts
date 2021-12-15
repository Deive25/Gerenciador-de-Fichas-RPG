import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router'
import { EMPTY, switchMap, take } from 'rxjs';


import { Sheet } from '../SheetPlayer/Sheet.model';
import { SheetService } from '../SheetPlayer/sheet.services';

@Component({
  selector: 'app-dash-board',
  templateUrl: './dash-board.component.html',
  styleUrls: ['./dash-board.component.css']
})
export class DashBoardComponent implements OnInit {
  sheets: Sheet[]
  formulario:FormGroup
  sheetSelecionado: Sheet
  
  constructor(private router: Router, private sheetService : SheetService,private fb:FormBuilder) { }

  ngOnInit(): void {
    this.sheetService.read().subscribe(sheets =>{
      this.sheets = sheets
      console.log(sheets)
    })
  }

  navigateToSheet(): void{
    this.router.navigate(['/sheet-player'])
  }
  
  delete(sheet:Sheet){
    this.sheetSelecionado = sheet
    this.sheetService.delete(this.sheetSelecionado.id).subscribe({
      next: sucess=>{window.location.reload();this.sheetService.showMenssage('Ficha deletada!')},
      error: error=>this.sheetService.showMenssage('erro')
    })}

    roll(dice:number) {
      var rolagem = Math.floor(Math.random() * dice) + 1;
      document.getElementById("rollValue")!.innerHTML = (" " + rolagem);
    }

    resetRoll(){
      document.getElementById("rollValue")!.innerHTML = ("0")
    }

  }
  

