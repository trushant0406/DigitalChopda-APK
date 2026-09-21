# Stage 1: Build React Vite app
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# Stage 2: Serve with lightweight Nginx Alpine
FROM nginx:alpine

# Clean default html
RUN rm -rf /usr/share/nginx/html/*

# Copy built production assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy Nginx SPA & reverse proxy configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
