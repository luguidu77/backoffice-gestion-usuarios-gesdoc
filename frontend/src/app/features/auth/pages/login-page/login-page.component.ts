import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

/**
 * Componente de login del backoffice.
 * 
 * Responsabilidades:
 *   - Mostrar formulario de login
 *   - Validar credenciales
 *   - Redirigir a dashboard tras login exitoso (o a la URL solicitada originalmente)
 *   - Mostrar mensajes de error
 */
@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.scss']
})
export class LoginPageComponent implements OnInit {
  loginForm: FormGroup;
  loading = false;
  errorMessage = '';
  returnUrl = '/dashboard';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  ngOnInit(): void {
    // Si el usuario ya está autenticado, redirigir al dashboard
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    // Obtener la URL de retorno de los query params (si existe)
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
  }

  /**
   * Procesa el envío del formulario de login.
   */
  onSubmit(): void {
    // Limpiar mensaje de error previo
    this.errorMessage = '';

    // Validar formulario
    if (this.loginForm.invalid) {
      this.markFormGroupTouched(this.loginForm);
      return;
    }

    // Mostrar indicador de carga
    this.loading = true;

    const credentials = this.loginForm.value;

    // Llamar al servicio de autenticación
    this.authService.login(credentials).subscribe({
      next: (response) => {
        console.log('Login exitoso:', response);
        // Redirigir a la URL original o al dashboard
        this.router.navigateByUrl(this.returnUrl);
      },
      error: (error) => {
        console.error('Error de login:', error);
        this.loading = false;
        
        // Mostrar mensaje de error al usuario
        this.errorMessage = error.error?.message || 'Error al iniciar sesión. Verifica tus credenciales.';
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  /**
   * Marca todos los controles del formulario como tocados
   * para mostrar los errores de validación.
   */
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  /**
   * Verifica si un campo tiene errores de validación.
   */
  hasError(field: string, error: string): boolean {
    const control = this.loginForm.get(field);
    return !!(control?.hasError(error) && control?.touched);
  }

  /**
   * Verifica si un campo es inválido.
   */
  isFieldInvalid(field: string): boolean {
    const control = this.loginForm.get(field);
    return !!(control?.invalid && control?.touched);
  }
}
