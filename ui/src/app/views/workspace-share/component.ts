import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {ProfileService} from 'generated';
import {UserRole} from 'generated';
import {ShareWorkspaceResponse} from 'generated';
import {Workspace} from 'generated';
import {WorkspaceAccessLevel} from 'generated';
import {WorkspacesService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class WorkspaceShareComponent implements OnInit {
  workspace: Workspace;
  loadingWorkspace = true;
  toShare = '';
  selectedPermission = 'Select Permission';
  accessLevel: WorkspaceAccessLevel;
  userEmail: string;
  constructor(
      private errorHandlingService: ErrorHandlingService,
      private locationService: Location,
      private route: ActivatedRoute,
      private profileService: ProfileService,
      private workspacesService: WorkspacesService,
  ) {}

  ngOnInit(): void {
    this.errorHandlingService.retryApi(
        this.workspacesService.getWorkspace(
            this.route.snapshot.params['ns'],
            this.route.snapshot.params['wsid']))
      .subscribe((workspace) => {
        this.errorHandlingService.retryApi(
            this.profileService.getMe()).subscribe(profile => {
          this.loadingWorkspace = false;
          this.userEmail = profile.username;
        });
        this.workspace = workspace;
      }
    );
  }

  navigateBack(): void {
    this.locationService.back();
  }

  setAccess(dropdownSelected: string): void {
    this.selectedPermission = dropdownSelected;
    if (dropdownSelected === 'Owner') {
      this.accessLevel = WorkspaceAccessLevel.OWNER;
    } else if (dropdownSelected === 'Writer') {
      this.accessLevel = WorkspaceAccessLevel.WRITER;
    } else {
      this.accessLevel = WorkspaceAccessLevel.READER;
    }
  }

  addCollaborator(): void {
    this.workspace.userRoles.push({email: this.toShare, role: this.accessLevel});
    this.workspacesService.shareWorkspace(
      this.workspace.namespace, this.workspace.id, {
        workspaceEtag: this.workspace.etag,
        items: this.workspace.userRoles
      }).subscribe((resp: ShareWorkspaceResponse) => {
        this.workspace.etag = resp.workspaceEtag;
      });
  }

  removeCollaborator(user: UserRole): void {
    const position = this.workspace.userRoles.findIndex((userRole) => {
      if (user.email === userRole.email) {
        return true;
      } else {
        return false;
      }
    });
    this.workspace.userRoles.splice(position, 1);
    this.workspacesService.shareWorkspace(
      this.workspace.namespace, this.workspace.id, {
        workspaceEtag: this.workspace.etag,
        items: this.workspace.userRoles
      }).subscribe((resp: ShareWorkspaceResponse) => {
        this.workspace.etag = resp.workspaceEtag;
      });
  }
}
