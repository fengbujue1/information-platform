FROM node:24.18.0-alpine3.24 AS build

WORKDIR /app

COPY frontend/information-hub-web/package.json frontend/information-hub-web/package-lock.json ./
RUN npm ci

COPY frontend/information-hub-web/ ./
RUN npm run build

FROM nginx:1.30.4-alpine3.24

COPY deploy/web/nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080
