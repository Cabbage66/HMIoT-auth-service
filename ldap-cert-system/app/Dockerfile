FROM python:3.11-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    openssl \
    ldap-utils \
    libldap2-dev \
    libsasl2-dev \
    libssl-dev \
    gcc \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ARG PIP_INDEX_URL=https://pypi.tuna.tsinghua.edu.cn/simple
ARG PIP_TRUSTED_HOST=pypi.tuna.tsinghua.edu.cn

COPY app/requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir \
    --index-url "${PIP_INDEX_URL}" \
    --trusted-host "${PIP_TRUSTED_HOST}" \
    -r /app/requirements.txt

COPY app /app
COPY openssl /app/openssl
COPY certs /app/certs
COPY scripts /app/scripts

RUN chmod +x /app/scripts/*.sh

EXPOSE 5000

# `LOGIN_CHALLENGES` is stored in-memory. Keep a single worker so that
# `phase=request` and `phase=verify` hit the same process.
CMD ["gunicorn", "-w", "1", "-b", "0.0.0.0:5000", "app:app"]
