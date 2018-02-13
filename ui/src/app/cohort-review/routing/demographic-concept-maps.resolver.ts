import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {
  CohortReviewService,
  ConceptIdName,
  ParticipantDemographics,
} from 'generated';

@Injectable()
export class DemographicConceptMapsResolver implements Resolve<ParticipantDemographics> {

  constructor(private reviewAPI: CohortReviewService) {}

  resolve(route: ActivatedRouteSnapshot): Observable<ParticipantDemographics> {
    const ns = route.parent.paramMap.get('ns');
    const wsid = route.parent.paramMap.get('wsid');
    const cid = +route.parent.paramMap.get('cid');

    const cdr = 1; // TODO(jms) plug in actual CDR version

    console.log(`Loading Demographics concept maps from resolver`);
    console.log(`ns: ${ns}, wsid: ${wsid}, cid: ${cid}, cdr id: ${cdr}`);
    console.dir(route);

    return this.reviewAPI.getParticipantDemographics(ns, wsid, cid, cdr);
  }
}
