import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'Fanel',
        short_name: 'Fanel',
        theme_color: '#2C6E8E',
        background_color: '#FBF7EF',
        display: 'standalone',
        start_url: '/',
        icons: [],
      },
      workbox: { globPatterns: ['**/*.{js,css,html,ico,png,svg}'] },
    }),
  ],
  server: {
    proxy: { '/api': 'http://localhost:8080' },
  },
})
