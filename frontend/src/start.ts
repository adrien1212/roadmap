import { createStart } from '@tanstack/react-start'

// SPA: the server only sends the HTML shell; every route loader runs in the browser,
// so all API calls go through the proxy and show up in DevTools.
export const startInstance = createStart(() => ({
  defaultSsr: false,
}))
