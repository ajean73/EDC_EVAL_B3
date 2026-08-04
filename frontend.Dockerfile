FROM node:22-alpine AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# Nginx: sert les assets statiques Angular construits.
FROM nginx:1.27-alpine
COPY --from=build /app/dist/pmt-frontend/browser/ /usr/share/nginx/html/
COPY docker/nginx/prod.conf /etc/nginx/conf.d/prod.conf
EXPOSE 80
# Nginx doit rester au premier plan pour garder le conteneur actif.
CMD ["nginx", "-g", "daemon off;"]
