import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { LoginComponent } from './features/login/login.component';

describe('LoginComponent smoke', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideAnimations(), provideHttpClient()],
    }).compileComponents();
  });

  it('renders the login form', () => {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Ingresar');
  });
});
