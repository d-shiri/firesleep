FROM python:3.13-slim AS builder

RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
RUN pip install --no-cache-dir \
    "fastapi>=0.136.1" \
    "uvicorn>=0.46.0" \
    "aiowebostv>=0.7.5" \
    "adb-shell>=0.4.4"

FROM python:3.13-slim

RUN useradd --system --uid 1000 --no-create-home app \
 && mkdir -p /data && chown app:app /data

COPY --from=builder /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH" \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app
COPY bridge.py bridge_firetv.py ./
COPY web ./web

USER app
EXPOSE 8765
CMD ["uvicorn", "bridge:app", "--host", "0.0.0.0", "--port", "8765"]
