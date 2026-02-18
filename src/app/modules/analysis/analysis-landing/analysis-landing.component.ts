import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-analysis-landing',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './analysis-landing.component.html',
  styleUrls: ['./analysis-landing.component.scss']
})
export class AnalysisLandingComponent {
  analysisOptions = [
    {
      title: 'Collaboration Networks',
      description: 'Visualize connections between researchers and collaboration patterns',
      icon: 'account_tree',
      route: '/analysis/collaboration-networks',
      color: '#1976d2'
    },
    {
      title: 'Research Knowledge Map',
      description: 'Explore thematic clusters and strategic research lines',
      icon: 'hub',
      route: '/analysis/research-knowledge-map',
      color: '#388e3c'
    },
    {
      title: 'Temporal Evolution',
      description: 'Comparative analysis by period and collaboration evolution',
      icon: 'timeline',
      route: '/analysis/temporal-evolution',
      color: '#f57c00'
    }
  ];

  reportOptions = [
    {
      title: 'Scientific Impact Report',
      description: 'Publications by author with journal impact metrics',
      icon: 'summarize',
      route: '/analysis/reports/scientific-impact',
      color: '#424242'
    },
    {
      title: 'Research Performance Report',
      description: 'Comprehensive institutional indicators and metrics',
      icon: 'assessment',
      route: '/analysis/reports/research-performance',
      color: '#424242'
    },
    {
      title: 'Investigator Productivity Report',
      description: 'Publications, student supervision and awarded project funding',
      icon: 'person',
      route: '/analysis/reports/investigator-productivity',
      color: '#424242'
    }
  ];
}
