import { fileURLToPath, URL } from 'node:url'
import fs from 'fs'
import path from 'path'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    // Dev-only middleware: serve the static client page for any `/client*` path
    {
      name: 'serve-client-route',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          try {
            if (req && req.url && req.url.startsWith('/client')) {
              const __dirname = path.dirname(fileURLToPath(import.meta.url));
              // resolve the requested path under public/client
              // strip the leading /client from url, keep leading slash for join
              const rel = req.url.replace(/^\/client/, '') || '/';
              const candidate = path.join(__dirname, 'public', 'client', rel);
              // if the requested file exists on disk (e.g., /client/login.html or /client/products/index.html),
              // let the static middleware handle it by calling next()
              if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
                return next();
              }
              // also support extensionless requests like /client/login -> public/client/login.html
              const candidateHtml = candidate + '.html';
              if (fs.existsSync(candidateHtml) && fs.statSync(candidateHtml).isFile()) {
                const html = fs.readFileSync(candidateHtml, 'utf8');
                res.setHeader('Content-Type', 'text/html; charset=utf-8');
                res.end(html);
                return;
              }
              // otherwise serve the SPA index so client-side routing can handle it
              const fp = path.join(__dirname, 'public', 'client', 'index.html');
              if (fs.existsSync(fp)) {
                const html = fs.readFileSync(fp, 'utf8');
                res.setHeader('Content-Type', 'text/html; charset=utf-8');
                res.end(html);
                return;
              }
            }
          } catch (e) {
            // fall through to next middleware on error
          }
          next();
        });
      }
    }
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
