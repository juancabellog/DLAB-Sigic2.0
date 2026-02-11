import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
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
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

import { MessageService } from '../../../core/services/message.service';

@Component({
  selector: 'app-project-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatDividerModule,
    MatProgressSpinnerModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './project-edit.component.html',
  styleUrls: ['./project-edit.component.scss']
})
export class ProjectEditComponent implements OnInit {
  projectForm: FormGroup;
  isEditMode: boolean = false;
  projectId: number | null = null;
  isLoading: boolean = false;

  // Mock data for dropdowns
  institutions = [
    'Universidad de Chile',
    'Universidad Católica',
    'Universidad de Santiago',
    'Universidad de Concepción',
    'Universidad Austral',
    'Universidad de Valparaíso',
    'Universidad Técnica Federico Santa María'
  ];

  fundingAgencies = [
    'FONDECYT',
    'ANID',
    'CONICYT',
    'CORFO',
    'FONDEF',
    'FONIS',
    'International Funding'
  ];

  statusOptions = [
    { value: 'ACTIVE', label: 'Active' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'SUSPENDED', label: 'Suspended' },
    { value: 'CANCELLED', label: 'Cancelled' }
  ];

  researchAreas = [
    'Machine Learning',
    'Climate Science',
    'Data Analysis',
    'Biotechnology',
    'Agriculture',
    'Genetics',
    'Quantum Computing',
    'Theoretical Physics',
    'Algorithms',
    'Computer Science',
    'Biology',
    'Chemistry',
    'Mathematics',
    'Engineering',
    'Environmental Science'
  ];

  selectedResearchAreas: string[] = [];
  participants: any[] = [];

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService
  ) {
    this.projectForm = this.addForm();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.projectId = parseInt(id);
      this.loadProject(this.projectId);
    }
  }

  addForm(): FormGroup {
    return this.fb.group({
      title: ['', [Validators.required, Validators.minLength(5)]],
      description: ['', [Validators.required, Validators.minLength(20)]],
      principalInvestigator: ['', [Validators.required, Validators.minLength(2)]],
      institution: ['', Validators.required],
      fundingAgency: ['', Validators.required],
      projectCode: ['', [Validators.required, Validators.minLength(3)]],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      budget: [0, [Validators.required, Validators.min(1)]],
      status: ['ACTIVE', Validators.required]
    });
  }

  loadProject(id: number): void {
    // Mock data - in real app, this would come from a service
    const mockProjects = [
      {
        id: 1,
        title: 'Machine Learning for Climate Prediction',
        description: 'Development of advanced machine learning algorithms to predict climate patterns and extreme weather events.',
        principalInvestigator: 'Dr. María Elena González',
        institution: 'Universidad de Chile',
        fundingAgency: 'FONDECYT',
        projectCode: 'FONDECYT-2023-123456',
        startDate: '2023-01-01',
        endDate: '2026-12-31',
        budget: 150000000,
        status: 'ACTIVE',
        researchAreas: ['Machine Learning', 'Climate Science', 'Data Analysis'],
        participants: [
          { name: 'Dr. María Elena González', role: 'Principal Investigator' },
          { name: 'Dr. Carlos Pérez', role: 'Co-Investigator' },
          { name: 'Ana Torres', role: 'PhD Student' }
        ]
      },
      {
        id: 2,
        title: 'Biotechnology Applications in Agriculture',
        description: 'Research on genetic modification techniques to improve crop yield and resistance to climate change.',
        principalInvestigator: 'Dr. Roberto Silva',
        institution: 'Universidad Católica',
        fundingAgency: 'ANID',
        projectCode: 'ANID-2023-789012',
        startDate: '2023-03-01',
        endDate: '2025-02-28',
        budget: 80000000,
        status: 'ACTIVE',
        researchAreas: ['Biotechnology', 'Agriculture', 'Genetics'],
        participants: [
          { name: 'Dr. Roberto Silva', role: 'Principal Investigator' },
          { name: 'Dr. Patricia López', role: 'Co-Investigator' }
        ]
      }
    ];

    const project = mockProjects.find(p => p.id === id);
    if (project) {
      this.projectForm.patchValue({
        title: project.title,
        description: project.description,
        principalInvestigator: project.principalInvestigator,
        institution: project.institution,
        fundingAgency: project.fundingAgency,
        projectCode: project.projectCode,
        startDate: project.startDate,
        endDate: project.endDate,
        budget: project.budget,
        status: project.status
      });
      this.selectedResearchAreas = [...project.researchAreas];
      this.participants = [...project.participants];
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

  addParticipant(): void {
    this.participants.push({
      name: '',
      role: 'Co-Investigator'
    });
  }

  removeParticipant(index: number): void {
    this.participants.splice(index, 1);
  }

  onSubmit(): void {
    if (this.projectForm.valid && this.selectedResearchAreas.length > 0 && this.participants.length > 0) {
      this.isLoading = true;
      
      // Simulate API call
      setTimeout(() => {
        const action = this.isEditMode ? 'updated' : 'added';
        this.messageService.success(`Project has been successfully ${action}.`);
        this.goBack();
        this.isLoading = false;
      }, 1000);
    } else {
      this.messageService.error('Please fill in all required fields, select at least one research area, and add at least one participant.');
    }
  }

  goBack(): void {
    this.router.navigate(['/projects']);
  }

  getTitle(): string {
    return this.isEditMode ? 'Edit Project' : 'New Project';
  }
}
