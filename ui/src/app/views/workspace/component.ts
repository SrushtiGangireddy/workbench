import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {Headers, Http} from '@angular/http';
import {DOCUMENT} from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';
import {Comparator, StringFilter} from 'clarity-angular';
import {Observable} from 'rxjs/Observable';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {SignInService} from 'app/services/sign-in.service';

import {
  Cluster,
  ClusterService,
  Cohort,
  CohortsService,
  FileDetail,
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';


class Notebook {
  constructor(public name: string, public description: string, public path: string,
              public push: boolean) {}
}
/*
* Search filters used by the cohort and notebook data tables to
* determine which of the cohorts loaded into client side memory
* are displayed.
*/
class CohortNameFilter implements StringFilter<Cohort> {
  accepts(cohort: Cohort, search: string): boolean {
    return cohort.name.toLowerCase().indexOf(search) >= 0;
  }
}
class CohortDescriptionFilter implements StringFilter<Cohort> {
  accepts(cohort: Cohort, search: string): boolean {
    return cohort.description.toLowerCase().indexOf(search) >= 0;
  }
}
class NotebookNameFilter implements StringFilter<Notebook> {
  accepts(notebook: Notebook, search: string): boolean {
    return notebook.name.toLowerCase().indexOf(search) >= 0;
  }
}
class NotebookDescriptionFilter implements StringFilter<Notebook> {
  accepts(notebook: Notebook, search: string): boolean {
    return notebook.description.toLowerCase().indexOf(search) >= 0;
  }
}
/*
* Sort comparators used by the cohort and notebook data tables to
* determine the order that the cohorts loaded into client side memory
* are displayed.
*/
class CohortNameComparator implements Comparator<Cohort> {
  compare(a: Cohort, b: Cohort) {
    return a.name.localeCompare(b.name);
  }
}
class CohortDescriptionComparator implements Comparator<Cohort> {
  compare(a: Cohort, b: Cohort) {
    return a.description.localeCompare(b.description);
  }
}
class NotebookNameComparator implements Comparator<Notebook> {
  compare(a: Notebook, b: Notebook) {
    return a.name.localeCompare(b.name);
  }
}
class NotebookDescriptionComparator implements Comparator<Notebook> {
  compare(a: Notebook, b: Notebook) {
    return a.description.localeCompare(b.description);
  }
}


@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class WorkspaceComponent implements OnInit, OnDestroy {

  /* tslint:disable:no-unused-variable */
  /* All these are used in the template, not the class */
  private cohortNameFilter = new CohortNameFilter();
  private cohortDescriptionFilter = new CohortDescriptionFilter();
  private notebookNameFilter = new NotebookNameFilter();
  private notebookDescriptionFilter = new NotebookDescriptionFilter();
  private cohortNameComparator = new CohortNameComparator();
  private cohortDescriptionComparator = new CohortDescriptionComparator();
  private notebookNameComparator = new NotebookNameComparator();
  private notebookDescriptionComparator = new NotebookDescriptionComparator();
  /* tslint:enable:no-unused-variable */

  workspace: Workspace;
  wsId: string;
  wsNamespace: string;
  workspaceLoading = true;
  cohortsLoading = true;
  cohortsError = false;
  notebooksLoading = false;
  cohortList: Cohort[] = [];
  cluster: Cluster;
  clusterPulled = false;
  clusterLoading = false;
  notFound = false;
  private accessLevel: WorkspaceAccessLevel;
  deleting = false;
  saveSuccess = false;
  saveError = false;
  notebookSelected = false;
  // TODO: Replace with real data/notebooks read in from GCS
  notebookList: Notebook[] = [];
  editHover = false;
  shareHover = false;
  trashHover = false;
  listenerAdded = false;
  notebookAuthListener: EventListenerOrEventListenerObject;

  constructor(
      private route: ActivatedRoute,
      private cohortsService: CohortsService,
      private clusterService: ClusterService,
      private errorHandlingService: ErrorHandlingService,
      private http: Http,
      private router: Router,
      private signInService: SignInService,
      private workspacesService: WorkspacesService,
      /* tslint:disable-next-line:no-unused-variable */
      @Inject(DOCUMENT) private document: any
  ) {}

  ngOnInit(): void {
    this.workspaceLoading = true;
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];

    this.errorHandlingService.retryApi(this.workspacesService
      .getWorkspace(this.wsNamespace, this.wsId))
        .subscribe(
          workspaceResponse => {
            this.workspace = workspaceResponse.workspace;
            this.accessLevel = workspaceResponse.accessLevel;
            this.workspaceLoading = false;
            this.errorHandlingService.retryApi(this.cohortsService
                .getCohortsInWorkspace(this.wsNamespace, this.wsId))
                .subscribe(
                    cohortsReceived => {
                      for (const coho of cohortsReceived.items) {
                        this.cohortList.push(coho);
                      }
                      this.cohortsLoading = false;
                    },
                    error => {
                      this.cohortsLoading = false;
                      this.cohortsError = true;
                    });
            this.errorHandlingService.retryApi(this.workspacesService
              .getNoteBookList(this.wsNamespace, this.wsId))
                .subscribe(
                  FileList => {
                    for (const fileDetail of FileList){
                      fileDetail.push = false;
                      this.notebookList.push(fileDetail);

                    }
                  },
                  error => {
                    if (error.status === 404) {
                      this.notFound = true;
                    } else {
                      this.workspaceLoading = false;
                    }
                  });
            },
          error => {
            if (error.status === 404) {
              this.notFound = true;
            } else {
              this.workspaceLoading = false;
            }
          });
  }

  ngOnDestroy(): void {
    window.removeEventListener('message', this.notebookAuthListener);
  }

  launchNotebook(): void {
    this.errorHandlingService.retryApi(this.pollCluster()).subscribe(cluster => {
      this.cluster = cluster;
      this.clusterPulled = true;
    });
  }

  pollCluster(): Observable<Cluster> {
    // Polls for cluster startup every minute.
    return new Observable<Cluster>(observer => {
      this.errorHandlingService.retryApi(this.clusterService.getCluster(
          this.workspace.namespace, this.workspace.id)).subscribe((cluster) => {
        if (cluster.status !== 'Running' && cluster.status !== 'Deleting') {
          setTimeout(() => {
              this.errorHandlingService.retryApi(this.pollCluster()).subscribe(newCluster => {
                this.cluster = newCluster;
                observer.next(newCluster);
                observer.complete();
              });
            }, 60000
          );
        } else {
          this.cluster = cluster;
          observer.next(cluster);
          observer.complete();
        }
      });
    });
  }

  cancelCluster(): void {
    this.clusterPulled = false;
  }

  openCluster(): void {
    // TODO (blrubenstein): Make this configurable by environment
    const leoBaseUrl = 'https://leonardo.dsde-dev.broadinstitute.org';
    const leoNotebookUrl = leoBaseUrl + '/notebooks/'
        + this.cluster.clusterNamespace + '/'
        + this.cluster.clusterName;
    const leoSetCookieUrl = leoNotebookUrl + '/setCookie';

    const headers = new Headers();
    headers.append('Authorization', 'Bearer ' + this.signInService.currentAccessToken);
    this.http.get(leoSetCookieUrl, {
      headers: headers,
      withCredentials: true
    }).subscribe(() => {
      const notebook = window.open(leoNotebookUrl, '_blank');
      this.clusterPulled = false;
      // TODO (blrubenstein): Make the notebook page a list of pages, and
      //    move this to component scope.
      if (!this.listenerAdded) {
        this.notebookAuthListener = (e: MessageEvent) => {
          if (e.source !== notebook) {
            return;
          }
          if (e.origin !== leoBaseUrl) {
            return;
          }
          if (e.data.type !== 'bootstrap-auth.request') {
            return;
          }
          notebook.postMessage({
            'type': 'bootstrap-auth.response',
            'body': {
                'googleClientId': this.signInService.clientId
            }
          }, leoBaseUrl);
        };
        window.addEventListener('message', this.notebookAuthListener);
        this.listenerAdded = true;
      }
    });
  }

  createAndLaunchNotebook(): void {
    this.clusterLoading = true;
    this.errorHandlingService.retryApi(this.clusterService
        .createCluster(this.workspace.namespace, this.workspace.id)).subscribe(() => {
      this.errorHandlingService.retryApi(this.pollCluster()).subscribe(polledCluster => {
        this.clusterLoading = false;
        this.cluster = polledCluster;
        this.clusterPulled = true;
      });
    });
  }

  killNotebook(): void {
    this.errorHandlingService.retryApi(this.clusterService.deleteCluster(
        this.workspace.namespace, this.workspace.id)).subscribe(() => {});
  }

  edit(): void {
    this.router.navigate(['edit'], {relativeTo : this.route});
  }

  share(): void {
    this.router.navigate(['share'], {relativeTo : this.route});
  }

  checkBoxEvent(notebook): void {
    if (notebook.push === true) {
      this.notebookSelected = true;
    } else {
        this.notebookSelected = false;
        for (const file of this.notebookList){
          if (file.push === true) {
            this.notebookSelected = true;
            break;
          }
        }
    }
  }
  delete(): void {
    this.deleting = true;
    this.workspacesService.deleteWorkspace(
        this.workspace.namespace, this.workspace.id).subscribe(() => {
          this.router.navigate(['/']);
        });
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
        || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get ownerPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  pushToNotebookServer(notebook): void {
    const fileList: Array<FileDetail> = [];
    for (const file of notebook){
      if (file.push) {
        fileList.push(file);
      }
    }
    this.clusterService
    .localizeNotebook(this.workspace.namespace, this.workspace.id, fileList).subscribe( () => {
      this.saveSuccess = true;
      this.saveError = false;
    }, () => {
      this.saveError = true;
      this.saveSuccess = false;
    });
  }
}
