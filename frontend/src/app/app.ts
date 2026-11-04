import { Component } from '@angular/core';
import { Shell } from './layout/shell';

@Component({
  imports: [Shell],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {}
