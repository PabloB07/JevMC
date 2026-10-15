# JevMC

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![PaperMC](https://img.shields.io/badge/PaperMC-1.21%2B-blue)
![AI](https://img.shields.io/badge/AI-Jev%20%2F%20TypeSafe.ai-purple)
![Tests](https://img.shields.io/badge/tests-4%2F4%20passing-brightgreen)
![Status](https://img.shields.io/badge/status-unofficial%20SDK-lightgrey)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A simple Java library for using [Jev](https://docs.typesafe.ai/) by [TypeSafe.ai](https://typesafe.ai) in **PaperMC** plugins.

JevMC is an **unofficial Java SDK** for the TypeSafe.ai API: a thin, dependency-free HTTP **wrapper** around `POST /v1/systemone` that exposes it as clean, typed Java classes. It contains no AI logic of its own.

Jev is a *System One Model*: you send it a **state** and **typed questions**, and it returns **structured decisions** with probabilities and confidence — no generated text to parse.

## Features

- The 3 primitives: `Noul` (yes/no), `Choice` (pick an option) and `Score` (rate against a rubric)
- Asynchronous API via `CompletableFuture`, with automatic retries (exponential backoff) on 429/529 and network errors
- Zero shaded dependencies: uses `java.net.http` (Java 17+) and Gson, both already bundled with Paper
- Immutable types (records) and fully typed answers, including confidence

## Installation

Add the library to your `pom.xml` (or install `jevmc-1.0.0.jar` into your local repository with `mvn install`):

```xml
<dependency>
    <groupId>com.github.PabloB07</groupId>
    <artifactId>JevMC</artifactId>
    <version>1.0.0</version>
</dependency>
```

Compile against Java 17 or higher (`<maven.compiler.release>17</maven.compiler.release>`). The jar also runs fine on newer JVMs (Java 21/25 servers included).

## Basic usage

```java
// Create the client (ideally a single instance in your plugin)
JevClient jev = new JevClient(System.getenv("TYPESAFE_API_KEY"));
```

```java
// Noul: yes/no question -> NoulAnswer (0.0 = no, 1.0 = yes)
Noul toxic = new Noul("The message is toxic or insulting");

// Choice: pick an option -> ChoiceAnswer(choice, probabilities, confidence)
Choice action = new Choice("Suggested action for staff",
        Map.of("none", "The message is harmless",
               "warn", "Warn the player",
               "punish", "Mute or ban"));

// Score: rate against a rubric -> ScoreAnswer(score, legend, probabilities, confidence)
Score severity = new Score("Severity of the message",
        List.of("Harmless", "Annoying", "Offensive", "Severe"));

// A single call evaluates every question in parallel
jev.evaluateAsync(
        Map.of("player", "Steve", "message", "this server is garbage"),
        Map.of("toxic", toxic, "action", action, "severity", severity)
).thenAccept(response -> {
    if (response.get("toxic", NoulAnswer.class).isLikelyTrue(0.8)) {
        String suggestion = response.get("action", ChoiceAnswer.class).choice();
        // ...
    }
});
```

## Usage in PaperMC

**Golden rule:** never call the API on the main server thread. Use `evaluateAsync`
(events like `AsyncChatEvent` already run off the main thread) and hop back with
`Bukkit.getScheduler().runTask(plugin, ...)` before touching the Bukkit API.

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        JevClient jev = new JevClient(System.getenv("TYPESAFE_API_KEY"));
        Bukkit.getPluginManager().registerEvents(
                new ChatModerationListener(this, jev), this);
    }
}
```

A complete chat-moderation example that evaluates toxicity, severity and a
suggested action in a single request ships in
`dev.blancocl.jevmc.paper.ChatModerationListener`.

## TypeSafe.ai API details

- Endpoint: `POST https://api.typesafe.ai/v1/systemone`
- Authentication: `Authorization: Bearer <API_KEY>` (get your key at [console.typesafe.ai/keys](https://console.typesafe.ai/keys))
- Model: `jev-latest`

## Build and test

```bash
mvn package   # compiles and runs the tests
```
