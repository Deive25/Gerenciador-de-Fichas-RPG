import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashBoardComponent } from './components/template/dash-board/dash-board.component';
import { HomeComponent } from './components/template/home/home.component';
import { SheetPlayerGetComponent } from './components/template/SheetPlayer/sheet-player-get/sheet-player-get.component';
import { SheetPlayerUpdateComponent } from './components/template/SheetPlayer/sheet-player-update/sheet-player-update.component';
import { SheetPlayerComponent } from './components/template/SheetPlayer/sheet-player/sheet-player.component';


const routes: Routes = [
  { path:"",
  redirectTo:"home",
  pathMatch:"full" },
  {path:"home",
  component:HomeComponent},
  {path:"dash-board",
  component:DashBoardComponent}, 
  {path:"sheet-player",
  component:SheetPlayerComponent}, 
  {path:"sheet-player-update/:id",
  component:SheetPlayerUpdateComponent},
  {path:"sheet-player-get/:id",
  component:SheetPlayerGetComponent}
  ];
  

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
