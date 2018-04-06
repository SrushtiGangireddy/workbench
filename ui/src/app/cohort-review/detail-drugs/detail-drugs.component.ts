import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ClrDatagridStateInterface} from '@clr/angular';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {
  CohortReviewService,
  PageFilterRequest,
  PageFilterType,
  ParticipantDrug,
  ParticipantDrugsColumns as Columns,
  SortOrder,
} from 'generated';

@Component({
  selector: 'app-detail-drugs',
  templateUrl: './detail-drugs.component.html',
  styleUrls: ['./detail-drugs.component.css']
})
export class DetailDrugsComponent implements OnInit, OnDestroy {
  /* Maps string values to Enum values */
  readonly reverseColumnEnum = {
    itemDate: Columns.ItemDate,
    standardVocabulary: Columns.StandardVocabulary,
    standardName: Columns.StandardName,
    sourceValue: Columns.SourceValue,
    sourceVocabulary: Columns.SourceVocabulary,
    sourceName: Columns.SourceName,

    age: Columns.Age,
    signature: Columns.Signature,
  };

  loading = false;

  drugs: ParticipantDrug[];
  request;
  totalCount: number;
  apiCaller: (any) => Observable<any>;
  subscription: Subscription;

  constructor(
    private route: ActivatedRoute,
    private reviewApi: CohortReviewService,
  ) {}

  ngOnInit() {
    console.dir(this.route);
    this.subscription = this.route.data
      .map(({participant}) => participant)
      .withLatestFrom(
        this.route.parent.data.map(({cohort}) => cohort),
        this.route.parent.data.map(({workspace}) => workspace),
      )
      .subscribe(([participant, cohort, workspace]) => {
        this.loading = true;

        this.apiCaller = (request) => this.reviewApi.getParticipantData(
          workspace.namespace,
          workspace.id,
          cohort.id,
          workspace.cdrVersionId,
          participant.participantId,
          request
        );

        this.request = <PageFilterRequest>{
          page: 0,
          pageSize: 50,
          includeTotal: true,
          sortOrder: SortOrder.Asc,
          sortColumn: Columns.ItemDate,
          pageFilterType: PageFilterType.ParticipantDrugs,
        };

        this.callApi();
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  callApi() {
    this.loading = true;
    this.apiCaller(this.request).subscribe(resp => {
      this.drugs = resp.items;
      this.totalCount = resp.count;
      this.request = resp.pageRequest;
      this.request.pageFilterType = PageFilterType.ParticipantDrugs;
      this.loading = false;
    });
  }

  update(state: ClrDatagridStateInterface) {
    console.log('Datagrid state: ');
    console.dir(state);
    const page = Math.floor(state.page.from / state.page.size);
    const pageSize = state.page.size;
    this.request = {...this.request, page, pageSize};

    if (state.sort) {
      const sortby = <string>(state.sort.by);
      this.request.sortColumn = this.reverseColumnEnum[sortby];
      this.request.sortOrder = state.sort.reverse ? SortOrder.Desc : SortOrder.Asc;
    }
    this.callApi();
  }
}
