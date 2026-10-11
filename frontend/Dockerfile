# Build: client assets + the SSR server bundle.
FROM node:24-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Run: the server bundle imports its dependencies at runtime, so production node_modules ship too.
# srvx serves the bundle's fetch handler and the static client files.
FROM node:24-slim
ENV NODE_ENV=production
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev && npm install -g srvx@1.0.5 && npm cache clean --force
COPY --from=build /app/dist ./dist
USER node
EXPOSE 3000
# --static is resolved from the entry's folder (dist/server).
CMD ["srvx", "serve", "--prod", "--entry", "dist/server/server.js", "--static", "../client", "--port", "3000"]
