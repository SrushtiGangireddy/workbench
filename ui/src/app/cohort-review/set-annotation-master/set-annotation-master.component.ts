import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
} from 'generated';

@Component({
  selector: 'app-set-annotation-master',
  templateUrl: './set-annotation-master.component.html',
  styleUrls: ['./set-annotation-master.component.css']
})
export class SetAnnotationMasterComponent {
  readonly kinds = AnnotationType;
  private selected: CohortAnnotationDefinition[] = [];
  private posting = false;

  /* tslint:disable-next-line:no-unused-variable */
  private annotations$: Observable<CohortAnnotationDefinition[]> =
    this.state.annotationDefinitions$;

  constructor(
    private annotationAPI: CohortAnnotationDefinitionService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  add() {
    const open = true;
    const mode = 'create';
    this.state.annotationMgrState.next({open, mode});
  }

  edit() {
    const open = true;
    const mode = 'edit';
    const [defn] = this.selected;
    this.state.annotationMgrState.next({open, mode, defn});
  }

  delete(): void {
    const {ns, wsid, cid} = this.route.snapshot.params;

    const _deleteCalls = this.selected.map(({cohortAnnotationDefinitionId: id}) =>
      this.annotationAPI.deleteCohortAnnotationDefinition(ns, wsid, cid, id)
    );

    const allDefns$ = this.annotationAPI
      .getCohortAnnotationDefinitions(ns, wsid, cid)
      .pluck('items');

    const broadcast = (defns: CohortAnnotationDefinition[]) =>
      this.state.annotationDefinitions.next(defns);

    this.posting = true;
    Observable
      .forkJoin(..._deleteCalls)
      .switchMap(_ => allDefns$)
      .do(broadcast)
      .subscribe(_ => this.posting = false);
  }
}
