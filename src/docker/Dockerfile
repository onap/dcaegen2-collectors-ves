FROM nexus3.onap.org:10001/onap/integration-java11:10.0.0

ENV HOSTALIASES /etc/host.aliases
ARG user=vescollector
ARG group=vescollector
WORKDIR /opt/app/VESCollector

ADD ./ ./
USER root

RUN addgroup $group && adduser --system --disabled-password --no-create-home --ingroup $group $user
RUN chown -R vescollector:vescollector /opt/app
RUN apk update && apk add procps && apk add less && apk add vim && apk add curl
USER $user
RUN mkdir /opt/app/VESCollector/logs
RUN chown -R vescollector:vescollector /opt/app/VESCollector/logs
RUN chown -R vescollector:vescollector /opt/app/VESCollector/etc
RUN chmod +x bin/*.sh

EXPOSE 8080 8443

CMD bin/docker-entry.sh
