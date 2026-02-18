import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { analysisGuard } from './core/guards/analysis.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
      {
        path: 'publications',
        loadComponent: () => import('./modules/publications/publication-list/publication-list.component').then(m => m.PublicationListComponent),
        canActivate: [authGuard]
      },
      {
        path: 'publications/new',
        loadComponent: () => import('./modules/publications/publication-edit/publication-edit.component').then(m => m.PublicationEditComponent),
        canActivate: [authGuard]
      },
      {
        path: 'publications/:id',
        loadComponent: () => import('./modules/publications/publication-detail/publication-detail.component').then(m => m.PublicationDetailComponent),
        canActivate: [authGuard]
      },
      {
        path: 'publications/:id/edit',
        loadComponent: () => import('./modules/publications/publication-edit/publication-edit.component').then(m => m.PublicationEditComponent),
        canActivate: [authGuard]
      },
  {
    path: 'scientific-events',
    loadComponent: () => import('./modules/scientific-events/scientific-events-list/scientific-events-list.component').then(m => m.ScientificEventsListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'scientific-events/new',
    loadComponent: () => import('./modules/scientific-events/scientific-events-edit/scientific-events-edit.component').then(m => m.ScientificEventsEditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'scientific-events/:id',
    loadComponent: () => import('./modules/scientific-events/scientific-events-view/scientific-events-view.component').then(m => m.ScientificEventsViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'scientific-events/:id/edit',
    loadComponent: () => import('./modules/scientific-events/scientific-events-edit/scientific-events-edit.component').then(m => m.ScientificEventsEditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'thesis-students',
    loadComponent: () => import('./modules/thesis-students/thesis-student-list/thesis-student-list.component').then(m => m.ThesisStudentListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'thesis-students/new',
    loadComponent: () => import('./modules/thesis-students/thesis-student-edit/thesis-student-edit.component').then(m => m.ThesisStudentEditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'thesis-students/:id',
    loadComponent: () => import('./modules/thesis-students/thesis-student-view/thesis-student-view.component').then(m => m.ThesisStudentViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'thesis-students/:id/edit',
    loadComponent: () => import('./modules/thesis-students/thesis-student-edit/thesis-student-edit.component').then(m => m.ThesisStudentEditComponent),
    canActivate: [authGuard]
  },
      {
        path: 'technology-transfer',
        loadComponent: () => import('./modules/technology-transfer/tt-list/tt-list.component').then(m => m.TTListComponent),
        canActivate: [authGuard]
      },
      {
        path: 'technology-transfer/new',
        loadComponent: () => import('./modules/technology-transfer/tt-edit/tt-edit.component').then(m => m.TtEditComponent),
        canActivate: [authGuard]
      },
      {
        path: 'technology-transfer/:id',
        loadComponent: () => import('./modules/technology-transfer/tt-view/tt-view.component').then(m => m.TtViewComponent),
        canActivate: [authGuard]
      },
      {
        path: 'technology-transfer/:id/edit',
        loadComponent: () => import('./modules/technology-transfer/tt-edit/tt-edit.component').then(m => m.TtEditComponent),
        canActivate: [authGuard]
      },
      {
        path: 'postdoctoral-fellows',
        loadComponent: () => import('./modules/postdoctoral-fellows/pf-list/pf-list.component').then(m => m.PFListComponent),
        canActivate: [authGuard]
      },
      {
        path: 'postdoctoral-fellows/new',
        loadComponent: () => import('./modules/postdoctoral-fellows/pf-edit/pf-edit.component').then(m => m.PfEditComponent),
        canActivate: [authGuard]
      },
      {
        path: 'postdoctoral-fellows/:id',
        loadComponent: () => import('./modules/postdoctoral-fellows/pf-view/pf-view.component').then(m => m.PfViewComponent),
        canActivate: [authGuard]
      },
      {
        path: 'postdoctoral-fellows/:id/edit',
        loadComponent: () => import('./modules/postdoctoral-fellows/pf-edit/pf-edit.component').then(m => m.PfEditComponent),
        canActivate: [authGuard]
      },
      {
        path: 'outreach-activities',
        loadComponent: () => import('./modules/outreach-activities/oa-list/oa-list.component').then(m => m.OAListComponent),
        canActivate: [authGuard]
      },
      {
        path: 'outreach-activities/new',
        loadComponent: () => import('./modules/outreach-activities/oa-edit/oa-edit.component').then(m => m.OaEditComponent),
        canActivate: [authGuard]
      },
      {
        path: 'outreach-activities/:id',
        loadComponent: () => import('./modules/outreach-activities/oa-view/oa-view.component').then(m => m.OaViewComponent),
        canActivate: [authGuard]
      },
      {
        path: 'outreach-activities/:id/edit',
        loadComponent: () => import('./modules/outreach-activities/oa-edit/oa-edit.component').then(m => m.OaEditComponent),
        canActivate: [authGuard]
      },
      {
        path: 'scientific-collaborations',
        loadComponent: () => import('./modules/scientific-collaborations/sc-list/sc-list.component').then(m => m.SCListComponent),
        canActivate: [authGuard]
      },
      {
        path: 'scientific-collaborations/new',
        loadComponent: () => import('./modules/scientific-collaborations/sc-edit/sc-edit.component').then(m => m.ScEditComponent),
        canActivate: [authGuard]
      },
      {
        path: 'scientific-collaborations/:id',
        loadComponent: () => import('./modules/scientific-collaborations/sc-view/sc-view.component').then(m => m.ScViewComponent),
        canActivate: [authGuard]
      },
      {
        path: 'scientific-collaborations/:id/edit',
        loadComponent: () => import('./modules/scientific-collaborations/sc-edit/sc-edit.component').then(m => m.ScEditComponent),
        canActivate: [authGuard]
      },
      {
        path: 'researchers',
        loadComponent: () => import('./modules/researchers/researcher-list/researcher-list.component').then(m => m.ResearcherListComponent),
        canActivate: [authGuard]
      },
      {
        path: 'researchers/new',
        loadComponent: () => import('./modules/researchers/researcher-edit/researcher-edit.component').then(m => m.ResearcherEditComponent),
        canActivate: [authGuard]
      },
      {
        path: 'researchers/:id',
        loadComponent: () => import('./modules/researchers/researcher-view/researcher-view.component').then(m => m.ResearcherViewComponent),
        canActivate: [authGuard]
      },
      {
        path: 'researchers/:id/edit',
        loadComponent: () => import('./modules/researchers/researcher-edit/researcher-edit.component').then(m => m.ResearcherEditComponent),
        canActivate: [authGuard]
      },
  {
    path: 'projects',
    loadComponent: () => import('./modules/projects/project-list/project-list.component').then(m => m.ProjectListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'projects/new',
    loadComponent: () => import('./modules/projects/project-edit/project-edit.component').then(m => m.ProjectEditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'projects/:id',
    loadComponent: () => import('./modules/projects/project-view/project-view.component').then(m => m.ProjectViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'projects/:id/edit',
    loadComponent: () => import('./modules/projects/project-edit/project-edit.component').then(m => m.ProjectEditComponent),
    canActivate: [authGuard]
  },
  // Analysis routes
  {
    path: 'analysis',
    loadComponent: () => import('./modules/analysis/analysis-landing/analysis-landing.component').then(m => m.AnalysisLandingComponent),
    canActivate: [analysisGuard]
  },
  {
    path: 'analysis/collaboration-networks',
    loadComponent: () => import('./modules/analysis/collaboration-networks/collaboration-networks.component').then(m => m.CollaborationNetworksComponent),
    canActivate: [analysisGuard]
  },
  {
    path: 'analysis/research-knowledge-map',
    loadComponent: () => import('./modules/analysis/research-knowledge-map/research-knowledge-map.component').then(m => m.ResearchKnowledgeMapComponent),
    canActivate: [analysisGuard]
  },
  {
    path: 'analysis/temporal-evolution',
    loadComponent: () => import('./modules/analysis/temporal-evolution/temporal-evolution.component').then(m => m.TemporalEvolutionComponent),
    canActivate: [analysisGuard]
  },
  {
    path: 'analysis/reports/scientific-impact',
    loadComponent: () => import('./modules/analysis/reports/scientific-impact-report/scientific-impact-report.component').then(m => m.ScientificImpactReportComponent),
    canActivate: [analysisGuard]
  },
  {
    path: 'analysis/reports/research-performance',
    loadComponent: () => import('./modules/analysis/reports/research-performance-report/research-performance-report.component').then(m => m.ResearchPerformanceReportComponent),
    canActivate: [analysisGuard]
  },
  {
    path: 'analysis/reports/investigator-productivity',
    loadComponent: () => import('./modules/analysis/reports/investigator-productivity-report/investigator-productivity-report.component').then(m => m.InvestigatorProductivityReportComponent),
    canActivate: [analysisGuard]
  },
  // Generic catalog routes
  {
    path: 'catalogs',
    loadComponent: () => import('./modules/catalogs/catalog-list/catalog-list.component').then(m => m.CatalogListComponent),
    canActivate: [authGuard]
  },
  
  // Participation Types routes
  {
    path: 'catalogs/participation-types',
    loadComponent: () => import('./modules/catalogs/catalog-list/catalog-list.component').then(m => m.CatalogListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/participation-types/new',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/participation-types/:id',
    loadComponent: () => import('./modules/catalogs/catalog-view/catalog-view.component').then(m => m.CatalogViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/participation-types/:id/edit',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },

  // Product Types routes
  {
    path: 'catalogs/product-types',
    loadComponent: () => import('./modules/catalogs/catalog-list/catalog-list.component').then(m => m.CatalogListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/product-types/new',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/product-types/:id',
    loadComponent: () => import('./modules/catalogs/catalog-view/catalog-view.component').then(m => m.CatalogViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/product-types/:id/edit',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },

  // Product Status routes
  {
    path: 'catalogs/product-status',
    loadComponent: () => import('./modules/catalogs/catalog-list/catalog-list.component').then(m => m.CatalogListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/product-status/new',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/product-status/:id',
    loadComponent: () => import('./modules/catalogs/catalog-view/catalog-view.component').then(m => m.CatalogViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/product-status/:id/edit',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },

  // Researcher Types routes
  {
    path: 'catalogs/researcher-types',
    loadComponent: () => import('./modules/catalogs/catalog-list/catalog-list.component').then(m => m.CatalogListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/researcher-types/new',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/researcher-types/:id',
    loadComponent: () => import('./modules/catalogs/catalog-view/catalog-view.component').then(m => m.CatalogViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/researcher-types/:id/edit',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },

  // Journals routes
  {
    path: 'catalogs/journals',
    loadComponent: () => import('./modules/catalogs/catalog-list/catalog-list.component').then(m => m.CatalogListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/journals/new',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/journals/:id',
    loadComponent: () => import('./modules/catalogs/catalog-view/catalog-view.component').then(m => m.CatalogViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/journals/:id/edit',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },

  // Index Types routes
  {
    path: 'catalogs/index-types',
    loadComponent: () => import('./modules/catalogs/catalog-list/catalog-list.component').then(m => m.CatalogListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/index-types/new',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/index-types/:id',
    loadComponent: () => import('./modules/catalogs/catalog-view/catalog-view.component').then(m => m.CatalogViewComponent),
    canActivate: [authGuard]
  },
  {
    path: 'catalogs/index-types/:id/edit',
    loadComponent: () => import('./modules/catalogs/catalog-edit/catalog-edit.component').then(m => m.CatalogEditComponent),
    canActivate: [authGuard]
  },
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];