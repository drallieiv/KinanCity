FROM openjdk:8
MAINTAINER drallieiv

VOLUME ["/opt/data"]

RUN apt-get update && apt-get install -y --no-install-recommends \
		curl \
		wget \
	&& rm -rf /var/lib/apt/lists/*

# Download last KinanCity core jar
RUN curl -s https://api.github.com/repos/drallieiv/KinanCity/releases | grep "browser_download_url" | grep "KinanCity-core" | grep "jar" | head -n 1 | cut -d '"' -f 4 | xargs wget -O KinanCity-core.jar

ENTRYPOINT ["java" "-jar KinanCity-core.jar -o /opt/data/result.csv"]
