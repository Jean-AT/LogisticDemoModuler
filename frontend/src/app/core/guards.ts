import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserRole } from './models';
import { SessionService } from './session.service';

export const authGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);
  if (session.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login']);
};

export const roleGuard =
  (roles: UserRole[]): CanActivateFn =>
  () => {
    const session = inject(SessionService);
    const router = inject(Router);
    if (!session.isAuthenticated()) {
      return router.createUrlTree(['/login']);
    }
    if (session.hasAnyRole(...roles)) {
      return true;
    }
    return router.createUrlTree(['/dashboard']);
  };
