FROM nexus3.onap.org:10001/onap/integration-java11:10.0.0

USER root

ENV HOSTALIASES /etc/host.aliases
WORKDIR /opt/app/VESCollector

ADD ./ ./

RUN addgroup vescollector
RUN adduser --system --disabled-password --no-create-home --ingroup vescollector vescollector
RUN chown -R vescollector:vescollector /opt/app
RUN mkdir /opt/app/VESCollector/logs
RUN chown -R vescollector:vescollector /opt/app/VESCollector/logs
RUN chown -R vescollector:vescollector /opt/app/VESCollector/etc
RUN chmod +x bin/*.sh
RUN apk update && apk add -y --no-install-recommends procps && apk add -y less && apk add -y vim && apk add -y curl && apk clean all

EXPOSE 8080 8443

USER vescollector
CMD bin/docker-entry.sh
