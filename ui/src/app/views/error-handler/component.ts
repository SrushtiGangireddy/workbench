import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import 'clarity-icons';

import {ErrorHandlingService} from 'app/services/error-handling.service';

@Component({
  selector: 'app-error-handler',
  styleUrls: ['./component.css'],
  templateUrl: './component.html'
})
export class ErrorHandlerComponent implements OnInit {

  constructor(
    private errorHandlingService: ErrorHandlingService,
  ) {}

  ngOnInit(): void {}

  closeServerError(): void {
    this.errorHandlingService.clearServerError();
  }

  closeNoServerResponse(): void {
    this.errorHandlingService.clearNoServerResponse();
  }
}