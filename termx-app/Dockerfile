FROM eclipse-temurin:25-jre

RUN apt-get update && apt-get install -y curl

ARG APP_VERSION
ENV APP_VERSION=${APP_VERSION:-latest}

ARG BUILD_TIME
ENV BUILD_TIME=$BUILD_TIME

# Short commit sha and the merged PR number, surfaced on /info so a running
# container can be traced back to an exact build (version is just the image tag,
# e.g. "latest", which isn't enough on dev).
ARG GIT_COMMIT
ENV GIT_COMMIT=$GIT_COMMIT
ARG PR_NUMBER
ENV PR_NUMBER=$PR_NUMBER
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
#
# /app/logs is the standard volume-mount path the deploy compose files
# already export to /opt/.../logs on the host, so dumps survive container
# recreation. Create it eagerly so the JVM doesn't error if the volume
# isn't bound (dev runs without a mount).
RUN mkdir -p /app/logs

# FHIR terminology conformance testing (tx-ecosystem): bake in the HL7 FHIR validator so
# POST /tx-conformance/runs works out of the box. The jar is ~180MB and is NOT committed to the repo
# — it is fetched here. Pin VALIDATOR_CLI_URL to a specific release for a reproducible image; the
# default tracks the latest validator. TERMX_CONFORMANCE_VALIDATOR_JAR points the app at it.
ARG VALIDATOR_CLI_URL="https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar"
RUN curl -fsSL -o /app/validator_cli.jar "${VALIDATOR_CLI_URL}"
ENV TERMX_CONFORMANCE_VALIDATOR_JAR=/app/validator_cli.jar
# This server's own FHIR base, used as the -tx target of the conformance run (override per deployment).
ENV TERMX_CONFORMANCE_TX_URL=http://localhost:8200/fhir

ENV JVM_OOM_OPTS="-XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/"

COPY build/libs/*-all.jar terminology.jar
CMD java ${JVM_OOM_OPTS} ${JAVA_OPTS} -jar terminology.jar
ENTRYPOINT ["/entrypoint.sh"]
