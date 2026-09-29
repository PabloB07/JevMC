# JevMC

![Version](https://img.shields.io/badge/version-1.0.0-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![PaperMC](https://img.shields.io/badge/PaperMC-1.21%2B-blue)
![AI](https://img.shields.io/badge/AI-Jev%20%2F%20TypeSafe.ai-purple)
![Tests](https://img.shields.io/badge/tests-4%2F4%20passing-brightgreen)
![Status](https://img.shields.io/badge/status-unofficial%20SDK-lightgrey)

Uma biblioteca Java simples para usar [Jev](https://docs.typesafe.ai/) da [TypeSafe.ai](https://typesafe.ai) em plugins de **PaperMC**.

JevMC é um **SDK Java não oficial** para a API da TypeSafe.ai: um **wrapper** HTTP leve e sem dependências em torno de `POST /v1/systemone`, expondo-a como classes Java limpas e tipadas. Não contém nenhuma lógica de IA própria.

Jev é um *System One Model*: você envia um **estado** e **perguntas tipadas**, e recebe de volta **decisões estruturadas** com probabilidades e confiança — sem texto gerado que precise ser parseado.

## Características

- As 3 primitivas: `Noul` (sim/não), `Choice` (escolher uma opção) e `Score` (avaliar com uma rubrica)
- API assíncrona via `CompletableFuture`, com retentativas automáticas (backoff exponencial) em caso de 429/529 e erros de rede
- Zero dependências empacotadas: usa `java.net.http` (Java 17+) e Gson, ambos já inclusos no Paper
- Tipos imutáveis (records) e respostas totalmente tipadas, incluindo a confiança

## Instalação

Adicione a biblioteca ao seu `pom.xml` (ou instale o `jevmc-1.0.0.jar` no seu repositório local com `mvn install`):

```xml
<dependency>
    <groupId>dev.blancocl</groupId>
    <artifactId>jevmc</artifactId>
    <version>1.0.0</version>
</dependency>
```

Compile com Java 17 ou superior (`<maven.compiler.release>17</maven.compiler.release>`). O jar também funciona em JVMs mais recentes (incluindo servidores com Java 21/25).

## Uso básico

```java
// Crie o cliente (idealmente uma única instância no seu plugin)
JevClient jev = new JevClient(System.getenv("TYPESAFE_API_KEY"));
```

```java
// Noul: pergunta de sim/não -> NoulAnswer (0.0 = não, 1.0 = sim)
Noul toxico = new Noul("A mensagem é tóxica ou insultuosa");

// Choice: escolher uma opção -> ChoiceAnswer(choice, probabilities, confidence)
Choice acao = new Choice("Ação sugerida para a staff",
        Map.of("nada", "A mensagem é inofensiva",
               "avisar", "Advertir o jogador",
               "punir", "Mute ou banimento"));

// Score: avaliar com uma rubrica -> ScoreAnswer(score, legend, probabilities, confidence)
Score gravidade = new Score("Gravidade da mensagem",
        List.of("Inofensiva", "Irritante", "Ofensiva", "Grave"));

// Uma única chamada avalia todas as perguntas em paralelo
jev.evaluateAsync(
        Map.of("jogador", "Steve", "mensagem", "esse servidor é um lixo"),
        Map.of("toxico", toxico, "acao", acao, "gravidade", gravidade)
).thenAccept(resposta -> {
    if (resposta.get("toxico", NoulAnswer.class).isLikelyTrue(0.8)) {
        String sugestao = resposta.get("acao", ChoiceAnswer.class).choice();
        // ...
    }
});
```

## Uso no PaperMC

**Regra de ouro:** nunca chame a API na thread principal do servidor. Use `evaluateAsync`
(eventos como `AsyncChatEvent` já rodam fora da thread principal) e volte à thread principal com
`Bukkit.getScheduler().runTask(plugin, ...)` antes de tocar na API do Bukkit.

```java
public class MeuPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        JevClient jev = new JevClient(System.getenv("TYPESAFE_API_KEY"));
        Bukkit.getPluginManager().registerEvents(
                new ChatModerationListener(this, jev), this);
    }
}
```

Um exemplo completo de moderação de chat que avalia toxicidade, gravidade e uma
ação sugerida em uma única requisição está incluído em
`dev.blancocl.jevmc.paper.ChatModerationListener`.

## Detalhes da API da TypeSafe.ai

- Endpoint: `POST https://api.typesafe.ai/v1/systemone`
- Autenticação: `Authorization: Bearer <API_KEY>` (obtenha sua chave em [console.typesafe.ai/keys](https://console.typesafe.ai/keys))
- Modelo: `jev-latest`

## Compilar e testar

```bash
mvn package   # compila e executa os testes
```
