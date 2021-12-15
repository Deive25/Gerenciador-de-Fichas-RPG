import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HeaderComponent } from './components/template/header/header.component';
import { MatToolbarModule }  from '@angular/material/toolbar';
import { FooterComponent } from './components/template/footer/footer.component';
import { MatSidenavModule} from "@angular/material/sidenav"
import { MatListModule } from "@angular/material/list";
import { HomeComponent } from './components/template/home/home.component';
import { SheetPlayerComponent } from './components/template/SheetPlayer/sheet-player/sheet-player.component';
import { DashBoardComponent } from './components/template/dash-board/dash-board.component';
import { MatButtonModule } from "@angular/material/button";
import { MatSnackBarModule } from '@angular/material/snack-bar'
import { HttpClientModule} from '@angular/common/http'
import { ReactiveFormsModule } from '@angular/forms';
import { SheetPlayerUpdateComponent } from './components/template/SheetPlayer/sheet-player-update/sheet-player-update.component';
import {MatDialogModule} from '@angular/material/dialog';
import { BsModalService, ModalModule } from 'ngx-bootstrap/modal';
import { SheetPlayerGetComponent } from './components/template/SheetPlayer/sheet-player-get/sheet-player-get.component';



@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    FooterComponent,
    HomeComponent,
    SheetPlayerComponent,
    DashBoardComponent,
    SheetPlayerUpdateComponent,
    SheetPlayerGetComponent,
 
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatButtonModule,
    MatSnackBarModule,
    HttpClientModule,
    ReactiveFormsModule,
    ModalModule.forRoot(),
    MatDialogModule
  ],
  providers: [BsModalService],
  bootstrap: [AppComponent]
})
export class AppModule { }
