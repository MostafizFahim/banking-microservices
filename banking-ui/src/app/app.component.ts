import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from './components/shared/navbar/navbar.component';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: true,  // This is true by default in Angular 17+
  imports: [RouterModule, NavbarComponent]  // Import what you need
})
export class AppComponent {
  title = 'Banking Application';
}