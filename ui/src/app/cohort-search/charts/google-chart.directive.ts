import {
  Directive,
  ElementRef,
  Input,
  OnChanges,
  SimpleChanges
} from '@angular/core';

declare var google: any;

@Directive({
  selector: '[appGoogleChart]'
})
export class GoogleChartDirective implements OnChanges {
  public _element: any;
  @Input() public chartType: string;
  @Input() public options: object;
  @Input() public dataTable: object;

  constructor(public element: ElementRef) {
    this._element = this.element.nativeElement;
  }

  ngOnChanges(changes: SimpleChanges) {
    const chart = {
      chartType: this.chartType,
      dataTable: this.dataTable,
      options: this.options,
      containerId: this._element.id
    };

    for (const prop of Object.keys(changes)) {
      chart[prop] = changes[prop].currentValue;
    }
    this.draw(chart);
  }

  draw(chart) {
    google.charts.setOnLoadCallback(() => {
      const wrapper = new google.visualization.ChartWrapper(chart);
      wrapper.draw();
    });
  }
}
