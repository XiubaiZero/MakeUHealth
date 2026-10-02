FROM node:24-alpine AS build
WORKDIR /build
COPY health-management-frontend/package.json health-management-frontend/package-lock.json ./
RUN npm ci
COPY health-management-frontend/ ./
RUN npm run build

FROM nginx:stable-alpine
COPY deploy/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
