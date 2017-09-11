FROM ubuntu:16.04
#FROM dcae-alpine:8-jre-tini

MAINTAINER vv770d@att.com

WORKDIR /opt/app/manager

ENV http_proxy http://one.proxy.att.com:8080
ENV https_proxy http://one.proxy.att.com:8080
ENV HOME /opt/app/VESCollector
ENV JAVA_HOME /usr

COPY opt /opt

EXPOSE 9999 8080 8443

CMD [ "/opt/app/docker-entry.sh" ]
