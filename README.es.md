# JevMC

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![PaperMC](https://img.shields.io/badge/PaperMC-1.21%2B-blue)
![AI](https://img.shields.io/badge/AI-Jev%20%2F%20TypeSafe.ai-purple)
![Tests](https://img.shields.io/badge/tests-4%2F4%20passing-brightgreen)
![Status](https://img.shields.io/badge/status-unofficial%20SDK-lightgrey)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Una librería Java simple para usar [Jev](https://docs.typesafe.ai/) de [TypeSafe.ai](https://typesafe.ai) en plugins de **PaperMC**.

JevMC es un **SDK de Java no oficial** para la API de TypeSafe.ai: un **wrapper** HTTP ligero y sin dependencias alrededor de `POST /v1/systemone`, que la expone como clases Java limpias y tipadas. No contiene lógica de IA propia.

Jev es un *System One Model*: le envías un **estado** y **preguntas tipadas**, y devuelve **decisiones estructuradas** con probabilidades y confianza — sin texto generado que haya que parsear.

## Características

- Las 3 primitivas: `Noul` (sí/no), `Choice` (elegir una opción) y `Score` (calificar con una rúbrica)
- API asíncrona vía `CompletableFuture`, con reintentos automáticos (backoff exponencial) ante 429/529 y errores de red
- Cero dependencias empaquetadas: usa `java.net.http` (Java 17+) y Gson, ambos ya incluidos en Paper
- Tipos inmutables (records) y respuestas totalmente tipadas, incluida la confianza

## Instalación

Añade la librería a tu `pom.xml` (o instala `jevmc-1.0.0.jar` en tu repositorio local con `mvn install`):

```xml
<dependency>
    <groupId>dev.blancocl</groupId>
    <artifactId>jevmc</artifactId>
    <version>1.0.0</version>
</dependency>
```

Compila con Java 17 o superior (`<maven.compiler.release>17</maven.compiler.release>`). El jar también funciona en JVMs más nuevas (servidores con Java 21/25 incluidos).

## Uso básico

```java
// Crea el cliente (idealmente una sola instancia en tu plugin)
JevClient jev = new JevClient(System.getenv("TYPESAFE_API_KEY"));
```

```java
// Noul: pregunta de sí/no -> NoulAnswer (0.0 = no, 1.0 = sí)
Noul toxico = new Noul("El mensaje es tóxico o insultante");

// Choice: elegir una opción -> ChoiceAnswer(choice, probabilities, confidence)
Choice accion = new Choice("Acción sugerida para el staff",
        Map.of("nada", "El mensaje es inofensivo",
               "avisar", "Advertir al jugador",
               "sancionar", "Mute o baneo"));

// Score: calificar con una rúbrica -> ScoreAnswer(score, legend, probabilities, confidence)
Score gravedad = new Score("Gravedad del mensaje",
        List.of("Inofensivo", "Molesto", "Ofensivo", "Grave"));

// Una sola llamada evalúa todas las preguntas en paralelo
jev.evaluateAsync(
        Map.of("jugador", "Steve", "mensaje", "este servidor es una basura"),
        Map.of("toxico", toxico, "accion", accion, "gravedad", gravedad)
).thenAccept(respuesta -> {
    if (respuesta.get("toxico", NoulAnswer.class).isLikelyTrue(0.8)) {
        String sugerencia = respuesta.get("accion", ChoiceAnswer.class).choice();
        // ...
    }
});
```

## Uso en PaperMC

**Regla de oro:** nunca llames a la API desde el hilo principal del servidor. Usa `evaluateAsync`
(eventos como `AsyncChatEvent` ya corren fuera del hilo principal) y vuelve al hilo principal con
`Bukkit.getScheduler().runTask(plugin, ...)` antes de tocar la API de Bukkit.

```java
public class MiPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        JevClient jev = new JevClient(System.getenv("TYPESAFE_API_KEY"));
        Bukkit.getPluginManager().registerEvents(
                new ChatModerationListener(this, jev), this);
    }
}
```

Un ejemplo completo de moderación de chat que evalúa toxicidad, gravedad y una
acción sugerida en una sola petición está incluido en
`dev.blancocl.jevmc.paper.ChatModerationListener`.

## Detalles de la API de TypeSafe.ai

- Endpoint: `POST https://api.typesafe.ai/v1/systemone`
- Autenticación: `Authorization: Bearer <API_KEY>` (consigue tu clave en [console.typesafe.ai/keys](https://console.typesafe.ai/keys))
- Modelo: `jev-latest`

## Compilar y probar

```bash
mvn package   # compila y ejecuta los tests
```
