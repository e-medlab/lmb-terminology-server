FROM eclipse-temurin:25-jre

RUN apt-get update && apt-get install -y curl

ARG APP_VERSION
ENV APP_VERSION=${APP_VERSION:-latest}

ARG BUILD_TIME
ENV BUILD_TIME=$BUILD_TIME

ADD entrypoint.sh /
RUN mkdir /certs

# Fail-fast on OutOfMemoryError instead of letting the JVM limp on with a depleted
# heap. Without this, a single OOM cascades into every concurrent request, the
# Netty selector, and unrelated scheduled tasks — all die with their own OOMs
# over several minutes while the container keeps running and never restarts.
# ExitOnOutOfMemoryError makes the JVM exit immediately; Docker's restart policy
# brings it back clean within seconds. HeapDumpOnOutOfMemoryError leaves a
# postmortem .hprof in /app/logs (volume-mounted in production) so the next
# incident can be diagnosed without re-OOMing the container to capture a dump.
RUN mkdir -p /app/logs

ENV JVM_OOM_OPTS="-XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/"

COPY build/libs/*-all.jar terminology.jar
CMD java ${JVM_OOM_OPTS} ${JAVA_OPTS} -jar terminology.jar
ENTRYPOINT ["/entrypoint.sh"]
