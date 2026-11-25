# Isabelle — assistente de IA para Android

Projeto Android (Kotlin + Jetpack Compose) com a interface futurista da sua imagem.

> **Aviso honesto:** este código foi escrito mas **ainda não foi compilado nem testado em um celular**. Espere ajustar alguns erros de compilação no primeiro build.

## Como rodar
1. Instale o **Android Studio** (versão recente) e abra a pasta `Isabelle`.
2. Aguarde o Gradle sincronizar (ele baixa as dependências e cria o wrapper).
3. Ative a Depuração USB no celular (Android 11+) e clique em **Run**.
4. No app, aba **Ajustes**: preencha URL, modelo e chave da API de IA.

## O que cada parte faz
| Pedido | Como funciona | Limite do Android |
|---|---|---|
| Interface futurista | `ui/` (orbe animado, cartões de vidro, onda de áudio) | — |
| Ouvir "Isabelle" | `voice/WakeWordService` | Gasta bateria. Para uso diário, troque por Picovoice Porcupine (offline). Após reiniciar o celular, abra o app uma vez para religar. |
| Responder perguntas / internet | `core/AiClient` (qualquer API chat/completions) | Busca na web só se o modelo tiver busca (ex.: Perplexity `sonar`). |
| Alarmes | Intent `AlarmClock` | — |
| Agenda | Abre o calendário já preenchido | Você toca em **Salvar**. |
| WhatsApp | Abre a conversa com a mensagem pronta | Enviar sozinha exige ligar a **Acessibilidade** da Isabelle (opcional). |
| YouTube Music | Intent de busca + teclas de mídia | Depende do app aceitar o comando de busca. |
| Organizar fotos | `photos/` — ML Kit no aparelho, move para `Pictures/Isabelle/<categoria>` | Mover e apagar pedem sua confirmação (regra do Android 11+). |
| Tempo de uso e bloqueio | `usage/` — limite por app, modo foco, lista de essenciais | Precisa de "Acesso de uso" e "Exibir sobre outros apps". Não dá para impedir outros apps de *também* ler o uso; o Android não permite exclusividade. Quem sabe o caminho pode desligar o serviço nas configurações. |
| Ligar ao PC | `pc/PcLink` + `pc_agent/pc_agent.py` | Veja abaixo. |

## Conectar ao Windows pela internet
1. No PC e no celular, instale o **Tailscale** (grátis) e entre na mesma conta.
2. No PC: `pip install websockets`, defina `ISABELLE_TOKEN` com uma senha grande e rode `python pc_agent/pc_agent.py`.
3. No app, em Ajustes: endereço `ws://<IP do Tailscale do PC>:8765` e o mesmo token.
4. Teste: "Isabelle, bloqueia o PC".

Comandos do PC: bloquear, suspender, desligar (60 s para cancelar), teclas de mídia/volume e abrir sites ou apps da lista `APPS`.

## Gerar o APK pelo GitHub (sem Android Studio)
1. Crie um repositório **privado** no GitHub e envie todo o conteúdo desta pasta (inclusive `.github`).
2. Aba **Actions** → workflow **Build APK** (roda sozinho a cada envio; ou use **Run workflow**).
3. Quando ficar verde, abra a execução e baixe **Isabelle-APK** em *Artifacts* (vem em .zip; extraia).
4. Passe o `app-debug.apk` para o celular e instale (permita "instalar apps desconhecidos").
Se a execução ficar vermelha, abra o passo "Compilar APK de teste" e me mande o erro.
