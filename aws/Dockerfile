# Imagem do pascoa-monolith para o sandbox AWS.
# Contexto de build = raiz do repositório. O jar deve ter sido gerado antes:
#   mvn -q -DskipTests -pl pascoa-monolith -am package
FROM eclipse-temurin:21-jre-alpine

# fontconfig/dejavu: zxing (QR code) e POI usam java.awt/ImageIO em modo headless.
RUN apk add --no-cache fontconfig ttf-dejavu tzdata curl \
 && addgroup -S -g 10001 pascoa && adduser -S -u 10001 -G pascoa pascoa \
 && mkdir -p /opt/pascoa/uploads && chown -R pascoa:pascoa /opt/pascoa

WORKDIR /opt/pascoa
COPY --chown=pascoa:pascoa pascoa-monolith/target/pascoa-monolith-*.jar app.jar

USER pascoa
EXPOSE 8080
ENV TZ=America/Sao_Paulo \
    JAVA_OPTS="-XX:MaxRAMPercentage=65 -Djava.awt.headless=true"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness | grep -q '"status":"UP"'

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /opt/pascoa/app.jar"]
