# Catálogo Móvil 📱

App Android que automatiza la creación de fichas técnicas de productos usando IA (Gemini), lienzo interactivo y sincronización con Google Workspace.

[![Android CI](https://github.com/TU_USUARIO/TU_REPO/actions/workflows/android.yml/badge.svg)](https://github.com/TU_USUARIO/TU_REPO/actions/workflows/android.yml)

---

## Requisitos

- Android Studio Ladybug (2024.2) o superior
- JDK 17
- Android SDK 35

---

## Configuración local

1. Clona el repositorio
2. Crea un archivo `.env` en la raíz del proyecto:
   ```
   GEMINI_API_KEY=tu_clave_aquí
   ```
3. Abre el proyecto en Android Studio y ejecútalo en un emulador o dispositivo físico.

> El archivo `.env` está en `.gitignore`; nunca lo subas al repositorio.

---

## Configurar GitHub Actions (CI/CD)

Ve a **Settings → Secrets and variables → Actions** en tu repositorio y agrega:

| Secret | Descripción |
|---|---|
| `GEMINI_API_KEY` | Tu API key de Gemini (obligatorio) |
| `KEYSTORE_BASE64` | Tu keystore en base64: `base64 -i my-upload-key.jks` (opcional, para release) |
| `STORE_PASSWORD` | Contraseña del keystore (opcional) |
| `KEY_PASSWORD` | Contraseña de la key (opcional) |

El workflow de CI compila el **debug APK** automáticamente en cada push. El **release APK** se compila solo cuando los secrets del keystore están presentes.

---

## Estructura del proyecto

```
├── app/
│   ├── src/main/java/com/example/
│   │   ├── data/           # Room DB, repositorios, servicios Gemini y Google Workspace
│   │   ├── presentation/   # ViewModels
│   │   └── ui/             # Pantallas Compose, canvas, tema
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml  # Catálogo de versiones
├── .github/
│   └── workflows/
│       └── android.yml     # Pipeline CI/CD
└── .env.example            # Plantilla de variables de entorno
```
