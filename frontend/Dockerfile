# Шаг 1: Установка зависимостей (кешируем этот слой для скорости)
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# Шаг 2: Сборка проекта
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# Шаг 3: Финальный супер-легкий образ для запуска
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV production

# Копируем только то, что нужно для работы (спасибо standalone режиму!)
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000
CMD ["node", "server.js"]