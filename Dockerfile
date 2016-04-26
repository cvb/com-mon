FROM java:8

RUN cd /usr/local/bin && \
    curl -fsSLo boot https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh && \
    chmod 755 boot

ADD . /var/app
WORKDIR /var/app

ENV PATH "/var/app/.bin:$PATH"
ENV BOOT_AS_ROOT yes
ENV BOOT_EMIT_TARGET no

RUN boot build
