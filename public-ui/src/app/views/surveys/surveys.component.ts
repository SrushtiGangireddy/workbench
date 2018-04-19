import { Component, OnInit } from '@angular/core';
import {DataBrowserService} from '../../../publicGenerated/api/dataBrowser.service';
import {DbDomain} from '../../../publicGenerated/model/dbDomain';
import {DbDomainListResponse} from '../../../publicGenerated/model/dbDomainListResponse';

@Component({
  selector: 'app-surveys',
  templateUrl: './surveys.component.html',
  styleUrls: ['../../styles/template.css', '../../styles/cards.css', './surveys.component.css']
})
export class SurveysComponent implements OnInit {
  title = 'Browse Survey Instruments';
  subTitle = 'Conduct a simple keyword search to quickly identify survey questions ' +
    'related to your area of interest.';
  pageImage = '/assets/images/create-account-male-standing.png';
  surveys = [];
  constructor(
    private api: DataBrowserService
  ) { }

  ngOnInit() {
    this.api.getSurveyList().subscribe(
      result => {
        this.surveys = result.items;
        console.log(this.surveys); }
        );
  }

}
