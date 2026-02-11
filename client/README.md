# SATORN Frontend

The frontend application for SATORN - AI-powered news verification platform.

## Tech Stack
- **Framework**: React 19 + Vite
- **Language**: TypeScript
- **Styling**: TailwindCSS v4
- **State Management**: Zustand
- **Data Fetching**: TanStack Query
- **Routing**: React Router DOM 7
- **Icons**: Lucide React

## Setup & Run

1.  **Install Dependencies**
    ```bash
    npm install
    ```

2.  **Environment Setup**
    Copy `.env.example` to `.env` and configure your API URL.
    ```bash
    cp .env.example .env
    ```

3.  **Run Development Server**
    ```bash
    npm run dev
    ```

4.  **Build for Production**
    ```bash
    npm run build
    ```

## Project Structure
- `src/app`: App setup (Router, Providers)
- `src/features`: Feature-based modules (Auth, Feed, Chat, Admin)
- `src/shared`: Shared utilities, types, hooks, and components
- `src/components`: Generic UI components

## Default Credentials (Dev)
- **Username**: `admin`
- **Password**: `password`
