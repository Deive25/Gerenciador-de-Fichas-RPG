import { HttpClient, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Images } from './imagesModel';

@Injectable({
  providedIn: 'root'
})
export class ImagesService {

  url : string = "http://localhost:8080/images";

  constructor(private httpClient : HttpClient) { }

  upload(files : Set<File>, id : number) {

    const formData = new FormData();
    files.forEach(file => formData.append("file", file, file.name));
    formData.append("id", id.toString());

    const request = new HttpRequest("POST", this.url, formData);

    return this.httpClient.request(request);
  }
  read(): Observable<Images[]> {
    return this.httpClient.get<Images[]>(this.url)
  }
  delete(id:number):Observable<Images>{
    return this.httpClient.delete<Images>(`${this.url}/${id}`)
  }

  

}
