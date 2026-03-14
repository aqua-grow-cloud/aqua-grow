FROM node:20-alpine AS build-stage
WORKDIR /app

# Accept build arguments for environment variables
ARG VITE_API_URL=http://localhost:8080
ARG VITE_KEYCLOAK_URL=http://localhost:8090/

# Set environment variables for the build
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL

# Install dependencies
COPY package*.json ./
RUN npm install

# Copy source code and build
COPY . .
RUN npm run build

FROM nginx:stable-alpine AS production-stage
COPY --from=build-stage /app/dist /usr/share/nginx/html
COPY ./nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
