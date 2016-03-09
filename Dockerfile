FROM java:8

RUN cd /usr/local/bin && \
    curl -fsSLo boot https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh && \
    chmod 755 boot

