FROM openjdk:8
MAINTAINER drallieiv

RUN mkdir -p /kinan/bin && mkdir -p /kinan/data
RUN apt-get update && apt-get install -y --no-install-recommends curl wget && rm -rf /var/lib/apt/lists/*

# Download last KinanCity core jar
ARG CACHEBUST=1
RUN curl -s https://api.github.com/repos/drallieiv/KinanCity/releases/latest | grep "browser_download_url" | grep "kinancity-core" | head -n 1 | cut -d '"' -f 4 | xargs wget -O /kinan/bin/kinancity-core.jar

COPY config.example.properties /kinan/bin/
COPY scripts/run.sh /run.sh
RUN chmod +x /run.sh

VOLUME /kinan

ENTRYPOINT ["/run.sh"]
