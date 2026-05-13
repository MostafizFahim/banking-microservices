import { Component, OnInit } from '@angular/core';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './components/shared/navbar/navbar.component';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: true,
  imports: [RouterModule, CommonModule, NavbarComponent]
})
export class AppComponent implements OnInit {
  title = 'Banking Application';
  showNavbar = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Check navbar visibility on route change
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        // Show navbar only when user is logged in AND not on login/register pages
        const isLoggedIn = this.authService.isLoggedIn();
        const isAuthPage = event.url === '/login' || event.url === '/register';
        this.showNavbar = isLoggedIn && !isAuthPage;
      }
    });
  }
}
