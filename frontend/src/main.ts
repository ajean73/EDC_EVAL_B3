import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app/app.component';
import { appRoutes } from './app/app.routes';

// Application Angular standalone: providers globaux declares au bootstrap.
bootstrapApplication(AppComponent, {
  providers: [provideHttpClient(), provideRouter(appRoutes)]
}).catch(err => console.error(err));
