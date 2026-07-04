FROM python:3.11-slim

# Instaliraj GCC za C, Valgrind za memory analizu
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    valgrind \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ ./app/

# Folder za privremene datoteke
RUN mkdir -p /tmp/code-execution

EXPOSE 8001

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]