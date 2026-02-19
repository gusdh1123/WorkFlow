// =========================
// ESLint 설정 (ESLint 9+, JS/React)
// - Vite/React 환경용
// - 글로벌 무시, 플러그인, 규칙 정의
// =========================

import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  // =========================
  // 전체 프로젝트에서 무시할 파일/폴더
  // =========================
  globalIgnores(['dist']),

  {
    // =========================
    // 적용 파일: JS, JSX
    // =========================
    files: ['**/*.{js,jsx}'],

    // =========================
    // 확장 설정
    // - js.configs.recommended: 기본 JS 권장 규칙
    // - reactHooks.configs.flat.recommended: React Hooks 권장 규칙
    // - reactRefresh.configs.vite: Vite + React Refresh 관련 권장 규칙
    // =========================
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],

    // =========================
    // 언어 옵션
    // =========================
    languageOptions: {
      ecmaVersion: 2020, // ECMAScript 2020 기본
      globals: globals.browser, // 브라우저 글로벌 객체 허용
      parserOptions: {
        ecmaVersion: 'latest',
        ecmaFeatures: { jsx: true }, // JSX 허용
        sourceType: 'module', // ES 모듈
      },
    },

    // =========================
    // 커스텀 규칙
    // =========================
    rules: {
      // 사용하지 않는 변수 경고 (단, 대문자/언더스코어로 시작하는 변수는 무시)
      'no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]' }],
    },
  },
])
