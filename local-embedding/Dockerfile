FROM python:3.11.9-slim-bookworm@sha256:2856e6af199e8128161abd320575eb9b341f3b76f017b5d0c9cd364f60d8a050

ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1 \
    HF_HUB_OFFLINE=1 TRANSFORMERS_OFFLINE=1 HF_DATASETS_OFFLINE=1 \
    CUDA_VISIBLE_DEVICES="" OMP_NUM_THREADS=1 MODEL_DIR=/model
WORKDIR /app
COPY tools/embedding-qualification/requirements.lock /app/requirements.lock
RUN pip install --require-hashes -r /app/requirements.lock
COPY tools/embedding-qualification/allowlist.json /app/allowlist.json
COPY local-embedding/embedding_service /app/embedding_service
USER 65532:65532
EXPOSE 8090
CMD ["python", "-m", "embedding_service.server"]
