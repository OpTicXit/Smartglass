# =========================================================
# T6 · Imagen de SmartGlass para Render o Railway.
# Dos etapas: compila el JAR con el wrapper de Maven y lo ejecuta en un JRE limpio.
# Java 26 = <java.version> del pom.xml. Ningun secreto se copia a la imagen.
# =========================================================

# ---------- 1) Compilacion ----------
FROM eclipse-temurin:26-jdk-noble AS build
WORKDIR /app

# Primero solo lo que define dependencias: si el codigo cambia, esta capa se reutiliza
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -DskipTests package && cp target/*.jar /app/smartglass.jar

# ---------- 2) Ejecucion ----------
FROM eclipse-temurin:26-jre-noble
WORKDIR /app

# Usuario sin privilegios y carpeta de fotos (montar aqui el volumen persistente)
RUN useradd --system --create-home --uid 10001 smartglass \
 && mkdir -p /data/uploads/img && chown -R smartglass:smartglass /data
COPY --from=build /app/smartglass.jar /app/smartglass.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    UPLOADS_DIR=/data/uploads \
    PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
# Los volumenes de Railway y los discos de Render se montan como root: se arranca como root solo
# para dar la carpeta al usuario "smartglass" y Java corre sin privilegios (setpriv, util-linux).
# La plataforma vigila /salud (SaludController); el puerto llega en PORT.
ENTRYPOINT ["sh", "-c", "mkdir -p \"$UPLOADS_DIR/img\" && chown -R smartglass:smartglass \"$UPLOADS_DIR\" && exec setpriv --reuid=smartglass --regid=smartglass --init-groups java $JAVA_OPTS -jar /app/smartglass.jar"]
