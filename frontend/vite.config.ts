import { defineConfig } from 'vite'
import { devtools } from '@tanstack/devtools-vite'

import { tanstackStart } from '@tanstack/react-start/plugin/vite'

import viteReact from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Spring paths served through port 3000 so the browser only sees one origin.
// API_URL is the same setting the SSR side of #/lib/api uses.
// xfwd sends X-Forwarded-* so Spring builds URLs (OAuth redirects) for :3000.
const backend = { target: process.env.API_URL ?? 'http://localhost:8045', xfwd: true }

const config = defineConfig({
  resolve: { tsconfigPaths: true },
  server: {
    proxy: {
      '/api/': backend,
      '/oauth2/': backend,
      '/login/': backend,
      '/logout': backend,
    },
  },
  plugins: [devtools(), tailwindcss(), tanstackStart(), viteReact()],
})

export default config
