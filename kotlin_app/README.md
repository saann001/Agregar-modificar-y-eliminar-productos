# Login App (Android nativo / Kotlin) — US01 a US08

App Android nativa en Kotlin con arquitectura **MVC + POO**, paleta de
**tonos de azul**, y textos de interfaz en español. Cubre:

- **US01** — Login + asignación local de rol.
- **US02** — Cierre de sesión con limpieza segura de credenciales.
- **US03** — Catálogo general (`GET /products`) con RecyclerView + Glide.
- **US04** — Filtro por categoría con chips (`GET /products/categories`).
- **US05** — Detalle del producto con interfaz dinámica según rol.
- **US06** — Alta de producto (`POST /products`), solo Administrador.
- **US07** — Edición de producto (`PUT /products/{id}`), solo Administrador.
- **US08** — Eliminación de producto (`DELETE /products/{id}`), solo Administrador.

## Arquitectura (MVC + POO)

```
com.example.loginapp/
  model/
    Role.kt, UserModel.kt, Product.kt, LoginResult.kt, UiState.kt
    ProductFormErrors.kt / ProductFormResult.kt   -> US06/US07
    dto/
      AuthDtos.kt, ProductDto.kt
      ProductWriteRequest.kt                       -> cuerpo de POST/PUT (US06/US07)

  network/
    FakeStoreApiService.kt  -> ahora con createProduct (POST) y updateProduct (PUT)
    ApiClient.kt

  controller/
    LoginController.kt, SessionController.kt
    CatalogController.kt, ProductDetailController.kt
    ProductFormController.kt   -> validación local + crear/editar (US06/US07)

  view/
    LoginActivity.kt, HomeActivity.kt, ProductAdapter.kt, ProductDetailActivity.kt
    ProductFormActivity.kt     -> clase base ABSTRACTA (herencia) con la lógica
                                  común de formulario: validación, categorías,
                                  bloqueo de acceso por rol, spinner de guardado
    CreateProductActivity.kt   -> hereda de ProductFormActivity (US06)
    EditProductActivity.kt     -> hereda de ProductFormActivity (US07)

  util/
    NetworkUtils.kt, CartState.kt, CategoryTranslator.kt
```

### Por qué una clase base abstracta (`ProductFormActivity`)

US06 y US07 piden **exactamente los mismos campos y las mismas
validaciones** (título, precio numérico, descripción, URL de imagen,
categoría). En vez de duplicar esa lógica en dos Activities, se creó
una clase abstracta con la lógica común (herencia, POO) y cada
subclase solo define:
1. Qué endpoint llama (`performSave`: POST o PUT).
2. Qué pasa tras guardar con éxito (`onSaveSuccess`).

## Escenarios cubiertos (nuevos)

| US | Escenario | Dónde |
|---|---|---|
| US06 | Creación exitosa + alerta con el nuevo ID + limpia el formulario | `CreateProductActivity.onSaveSuccess()` |
| US06 | Validación local de campos, sin llamar a la red | `ProductFormController.validate()` — se ejecuta ANTES de cualquier HTTP |
| US06 | Bloqueo de acceso para Cliente/Auditor (deep link) | `ProductFormActivity.onCreate()` → `canManageProducts()` → redirige al catálogo |
| US07 | Formulario pre-cargado con los datos actuales | `EditProductActivity.prefill()`, usa los extras que ya traía el detalle |
| US07 | Mensaje "Producto actualizado (Simulación)" + refresco visual sin nueva petición | `EditProductActivity.onSaveSuccess()` + `ProductDetailActivity.editProductLauncher` |
| US07 | Botón deshabilitado + spinner mientras la petición está en curso | `ProductFormActivity.setSaving()` |
| US08 | Confirmación obligatoria antes de un DELETE | `ProductDetailActivity.confirmDelete()` (AlertDialog) |
| US08 | Cancelar no dispara ninguna petición | botón "Cancelar" del diálogo sin listener |
| US08 | El botón de eliminar nunca se renderiza para Cliente/Auditor | `renderRoleBasedActions()`, ya existente desde US05 |

## Estilo

- Paleta 100% en tonos de azul (`colors.xml`): `primary`, `primary_dark`,
  `primary_variant`, `primary_light`, `accent`.
- Cabeceras con degradado azul en Login, Catálogo, Detalle y Formularios.
- Botón flotante (FAB) de "Agregar producto" en el catálogo, visible
  solo para Administrador — se instancia por código igual que los
  botones de gestión del detalle.
- Todos los textos visibles están en `strings.xml`, en español. Las
  categorías de la API (en inglés) se traducen solo para mostrarse
  (`CategoryTranslator`), conservando el valor original para las
  peticiones HTTP.

## Cómo abrir y ejecutar

1. Abre `android_kotlin_app/` con Android Studio (Hedgehog o superior).
2. Espera el "Gradle Sync" (Android Studio genera el `gradle-wrapper.jar`
   faltante automáticamente).
3. Ejecuta el módulo `app`. Usa un usuario con `id: 1` o `id: 2` de
   `GET https://fakestoreapi.com/users` para entrar como Administrador
   y ver las pantallas de creación/edición/eliminación.

**Importante (nota de negocio de las 3 historias):** Fake Store API
simula las respuestas de POST/PUT/DELETE, pero no persiste los cambios
en su servidor. Es normal que, tras crear/editar/eliminar un producto,
`GET /products` siga mostrando el catálogo original.
