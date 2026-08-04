import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  // Shell minimal: tout le rendu est delegue aux routes standalone.
  template: '<router-outlet></router-outlet>'
})
export class AppComponent {}
