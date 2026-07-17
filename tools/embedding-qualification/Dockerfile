FROM python:3.11.9-slim-bookworm@sha256:2856e6af199e8128161abd320575eb9b341f3b76f017b5d0c9cd364f60d8a050

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_NO_CACHE_DIR=1 \
    HF_HUB_OFFLINE=1 \
    TRANSFORMERS_OFFLINE=1 \
    HF_DATASETS_OFFLINE=1 \
    CUDA_VISIBLE_DEVICES="" \
    OMP_NUM_THREADS=1 \
    MKL_NUM_THREADS=1 \
    OPENBLAS_NUM_THREADS=1 \
    PYTHONPATH=/opt/qualification/src

WORKDIR /opt/qualification

COPY requirements.lock .
RUN python -m pip install --require-hashes -r requirements.lock \
    && rm -f /usr/local/bin/hf /usr/local/bin/huggingface-cli \
    && test ! -e /usr/local/bin/hf \
    && test ! -e /usr/local/bin/huggingface-cli \
    && ! command -v git \
    && ! command -v git-lfs \
    && ! command -v curl

COPY allowlist.json .
COPY src ./src

RUN useradd --create-home --uid 10001 qualification
USER qualification

ENTRYPOINT ["python", "-m", "embedding_qualification.cli"]
