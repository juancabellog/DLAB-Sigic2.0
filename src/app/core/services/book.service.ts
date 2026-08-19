import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseHttpService } from './base-http.service';
import { BookProductDTO, SearchFiltersDTO, PaginatedResponseDTO } from '../models/backend-dtos';

@Injectable({
  providedIn: 'root'
})
export class BookService {

  constructor(private baseHttp: BaseHttpService) {}

  getBooks(filters?: SearchFiltersDTO): Observable<PaginatedResponseDTO<BookProductDTO>> {
    return this.baseHttp.getPaginated<PaginatedResponseDTO<BookProductDTO>>('/books', filters);
  }

  getBook(id: number): Observable<BookProductDTO> {
    return this.baseHttp.get<BookProductDTO>(`/books/${id}`);
  }

  createBook(book: BookProductDTO): Observable<BookProductDTO> {
    return this.baseHttp.post<BookProductDTO>('/books', book);
  }

  updateBook(id: number, book: BookProductDTO): Observable<BookProductDTO> {
    return this.baseHttp.put<BookProductDTO>(`/books/${id}`, book);
  }

  deleteBook(id: number): Observable<boolean> {
    return this.baseHttp.delete<boolean>(`/books/${id}`);
  }
}
