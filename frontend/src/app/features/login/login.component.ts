import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';
import { SessionService } from '../../core/session.service';
import { errorMessage } from '../../core/utils';

@Component({
  selector: 'app-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatIconModule],
  templateUrl: './login.component.html',
  styles: [
    `
      .login-wrap {
        min-height: 100vh;
        display: grid;
        place-items: center;
        background: linear-gradient(135deg, #5b3cc4 0%, #4726a8 100%);
        padding: 24px;
      }
      .card {
        width: 100%;
        max-width: 400px;
        background: rgba(255, 255, 255, 0.78);
        border-radius: 20px;
        padding: 40px 32px;
        backdrop-filter: blur(18px);
        -webkit-backdrop-filter: blur(18px);
        border: 1px solid rgba(255, 255, 255, 0.55);
        box-shadow: 0 24px 60px rgba(20, 10, 60, 0.35);
      }
      .logo {
        font-family: 'Poppins', sans-serif;
        font-size: 24px;
        font-weight: 700;
        color: var(--color-primary);
        margin: 0 0 4px;
      }
      .sub {
        color: var(--color-text-soft);
        font-size: 14px;
        margin: 0 0 24px;
      }
      form { display: flex; flex-direction: column; gap: 4px; }
      .error { color: var(--color-danger); font-size: 13px; margin: 8px 0 0; }
      .btn { margin-top: 16px; height: 46px; }
      .hint { margin-top: 24px; font-size: 13px; color: var(--color-text-soft); }
    `,
  ],
})
export class LoginComponent {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly session = inject(SessionService);

  readonly form = this.fb.group({
    username: this.fb.control('', Validators.required),
    password: this.fb.control('', Validators.required),
  });

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.error.set(null);
    this.loading.set(true);
    const { username, password } = this.form.getRawValue();
    this.auth.login(username, password).subscribe({
      next: (res) => {
        this.session.setSession(res.token, res.user);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err));
      },
      complete: () => this.loading.set(false),
    });
  }
}
