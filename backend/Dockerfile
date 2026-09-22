FROM node:20-bookworm-slim

# Install Chromium, fonts, dumb-init, and process tools for Puppeteer & WhatsApp Web
RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium \
    fonts-liberation \
    fonts-dejavu-core \
    fonts-noto-core \
    fonts-indic \
    fonts-noto-color-emoji \
    dumb-init \
    procps \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Point Puppeteer to system Chromium and prevent downloading bundled Chrome
ENV PUPPETEER_SKIP_DOWNLOAD=true \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    CHROME_PATH=/usr/bin/chromium \
    NODE_ENV=production

WORKDIR /app

# Copy dependency manifests and scripts required for postinstall hook
COPY package*.json ./
COPY scripts/ ./scripts/

# Install production dependencies and clean cache to save disk space
RUN npm install --omit=dev && npm cache clean --force && rm -rf /root/.cache

# Copy application source
COPY src/ ./src/
COPY entrypoint.sh ./

# Fix Windows CRLF if any and make executable
RUN sed -i 's/\r$//' entrypoint.sh && chmod +x entrypoint.sh

# Create uploads and whatsapp session directories
RUN mkdir -p /app/uploads /app/.wwebjs_auth

EXPOSE 4000

ENTRYPOINT ["./entrypoint.sh"]
