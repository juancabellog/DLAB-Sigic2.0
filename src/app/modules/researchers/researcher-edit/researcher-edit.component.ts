import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MessageService } from '../../../core/services/message.service';

@Component({
  selector: 'app-researcher-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './researcher-edit.component.html',
  styleUrls: ['./researcher-edit.component.scss']
})
export class ResearcherEditComponent implements OnInit {
  researcherForm: FormGroup;
  isEditMode: boolean = false;
  researcherId: number | null = null;
  isLoading: boolean = false;

  // Mock data for dropdowns
  departments = [
    'Computer Science',
    'Biotechnology',
    'Physics',
    'Mathematics',
    'Chemistry',
    'Biology',
    'Engineering'
  ];

  positions = [
    'Assistant Professor',
    'Associate Professor',
    'Full Professor',
    'Research Scientist',
    'Postdoctoral Researcher',
    'PhD Student',
    'Research Assistant'
  ];

  statusOptions = [
    { value: 'ACTIVE', label: 'Active' },
    { value: 'ON_LEAVE', label: 'On Leave' },
    { value: 'INACTIVE', label: 'Inactive' }
  ];

  researchAreas = [
    'Machine Learning',
    'Data Science',
    'Artificial Intelligence',
    'Molecular Biology',
    'Genetics',
    'Bioinformatics',
    'Quantum Physics',
    'Theoretical Physics',
    'Applied Mathematics',
    'Organic Chemistry',
    'Biochemistry',
    'Environmental Science'
  ];

  selectedResearchAreas: string[] = [];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService
  ) {
    this.researcherForm = this.addForm();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.researcherId = parseInt(id);
      this.loadResearcher(this.researcherId);
    }
  }

  addForm(): FormGroup {
    return this.fb.group({
      fullName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      department: ['', Validators.required],
      position: ['', Validators.required],
      status: ['ACTIVE', Validators.required],
      publications: [0, [Validators.required, Validators.min(0)]],
      projects: [0, [Validators.required, Validators.min(0)]]
    });
  }

  loadResearcher(id: number): void {
    // Mock data - in real app, this would come from a service
    const mockResearchers = [
      {
        id: 1,
        fullName: 'Dr. María Elena González',
        email: 'maria.gonzalez@university.cl',
        department: 'Computer Science',
        position: 'Associate Professor',
        researchAreas: ['Machine Learning', 'Data Science', 'Artificial Intelligence'],
        publications: 25,
        projects: 8,
        status: 'ACTIVE'
      },
      {
        id: 2,
        fullName: 'Dr. Carlos Alberto Pérez',
        email: 'carlos.perez@university.cl',
        department: 'Biotechnology',
        position: 'Full Professor',
        researchAreas: ['Molecular Biology', 'Genetics', 'Bioinformatics'],
        publications: 42,
        projects: 12,
        status: 'ACTIVE'
      }
    ];

    const researcher = mockResearchers.find(r => r.id === id);
    if (researcher) {
      this.researcherForm.patchValue({
        fullName: researcher.fullName,
        email: researcher.email,
        department: researcher.department,
        position: researcher.position,
        status: researcher.status,
        publications: researcher.publications,
        projects: researcher.projects
      });
      this.selectedResearchAreas = [...researcher.researchAreas];
    }
  }

  toggleResearchArea(area: string): void {
    const index = this.selectedResearchAreas.indexOf(area);
    if (index > -1) {
      this.selectedResearchAreas.splice(index, 1);
    } else {
      this.selectedResearchAreas.push(area);
    }
  }

  isResearchAreaSelected(area: string): boolean {
    return this.selectedResearchAreas.includes(area);
  }

  onSubmit(): void {
    if (this.researcherForm.valid && this.selectedResearchAreas.length > 0) {
      this.isLoading = true;
      
      // Simulate API call
      setTimeout(() => {
        const action = this.isEditMode ? 'updated' : 'added';
        this.messageService.success(`Researcher has been successfully ${action}.`);
        this.goBack();
        this.isLoading = false;
      }, 1000);
    } else {
      this.messageService.error('Please fill in all required fields and select at least one research area.');
    }
  }

  goBack(): void {
    this.router.navigate(['/researchers']);
  }

  getTitle(): string {
    return this.isEditMode ? 'Edit Researcher' : 'New Researcher';
  }
}

