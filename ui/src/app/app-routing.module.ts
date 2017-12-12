// Based on the URL mapping in "routes" below, the RouterModule attaches
// UI Components to the <router-outlet> element in the main AppComponent.
import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {HomePageComponent} from 'app/views/home-page/component';
import {ReviewComponent} from 'app/views/review/component';

import {WorkspaceEditComponent} from 'app/views/workspace-edit/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';
import {WorkspaceComponent} from 'app/views/workspace/component';

import { HomeComponent } from 'app/data-browser/home/home.component';
import { SearchComponent } from 'app/data-browser/search/search.component';


const routes: Routes = [
  {path: '', component: HomePageComponent, data: {title: 'View Workspaces'}},
  {path: 'workspace/:ns/:wsid',
          component: WorkspaceComponent,
          data: {title: 'View Workspace Details'}},
  {path: 'workspace/:ns/:wsid/cohorts/:cid/edit',
          component: CohortEditComponent,
          data: {title: 'Edit Cohort', adding: false}},
  {path: 'workspace/:ns/:wsid/cohorts/create',
          component: CohortEditComponent,
          data: {title: 'Create Cohort', adding: true}},
  {path: 'workspace/build',
          component: WorkspaceEditComponent,
          data: {title: 'Create Workspace', adding: true}},
  {path: 'workspace/:ns/:wsid/edit',
          component: WorkspaceEditComponent,
          data: {title: 'Edit Workspace', adding: false}},
  {path: 'review',
          component: ReviewComponent,
          data: {title: 'Review Research Purposes'}},
  {path: 'workspace/:ns/:wsid/share',
          component: WorkspaceShareComponent,
          data: {title: 'Share Workspace'}},
    {path: 'data-browser/home',
        component: HomeComponent,
        data: {title: 'Data Browser'}},
    {path: 'data-browser/browse',
        component: SearchComponent,
        data: {title: 'Browse'}}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
