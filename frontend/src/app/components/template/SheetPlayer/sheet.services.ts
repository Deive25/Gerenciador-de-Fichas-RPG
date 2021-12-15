import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar'
import { Observable } from 'rxjs';
import { Sheet } from './Sheet.model';
@Injectable({
  providedIn: 'root'
})
export class SheetService {
  constructor(private snackBar: MatSnackBar, private http: HttpClient) { }
  baseUrl: string ="http://localhost:8080/sheetPlayer"
  showMenssage(msg: string): void{
    this.snackBar.open(msg, 'X', {
      duration:3000,
      horizontalPosition: "right",
      verticalPosition:"top",
      panelClass:['msg']
    })
  }
  create(sheet:Sheet):Observable<Sheet>{
    return this.http.post<Sheet>(this.baseUrl,sheet)
  }
  read(): Observable<Sheet[]> {
    return this.http.get<Sheet[]>(this.baseUrl)
  }
  readById(id: number): Observable<Sheet>{
    return this.http.get<Sheet>(`${this.baseUrl}/${id}`)
  }
  update(sheet:Sheet):Observable<Sheet>{
    return this.http.put<Sheet>(`${this.baseUrl}/${sheet.id}`,sheet)
  }
  delete(id:number):Observable<Sheet>{
    return this.http.delete<Sheet>(`${this.baseUrl}/${id}`)
  }
}
