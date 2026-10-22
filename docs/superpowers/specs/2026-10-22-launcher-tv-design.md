# Launcher TCL — design

Data: 2026-10-22 · Repo: este repositório

## Intenção

O dono liga a TV à noite para uma de duas coisas: **zapear no YouTube** pelos canais que segue, ou
**voltar para uma série** (The Mentalist, House, Criminal Minds — todas no Prime Video, inclusive via
canal Paramount+). A home atual (Google TV launcher + bloat TCL) é lenta e cheia de propaganda. O
objetivo é uma home que leva a qualquer um dos dois em 1–2 cliques e deixa a TV mais leve.

## Aparelho (medido)

- TCL, Android TV 11, `product C06_2K_GB`, tela 1280×720, density 213.
- RAM total ~920 MB, ~290 MB disponíveis com a home de fábrica. `/data` 71% usado, 767 MB livres.
- ADB por Wi-Fi: `<IP_DA_TV>:5555` (já autorizado).
- Home atual: `com.google.android.tvlauncher/.MainActivity`.
- Streamings: `com.google.android.youtube.tv`, `com.google.android.youtube.tvmusic`,
  `com.netflix.ninja`, `com.amazon.amazonvideo.livingroom`, `com.globo.globotv`.
- Chromecast: `com.google.android.apps.mediashell` — **nunca desativar**.
- Entradas: `com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744128` = HDMI 1, hoje
  conectada (provável Roku — confirmar trocando para ela). HDMI 2 = `HW1413744384`.
- Backup de pacotes/settings e print da home antiga em `backup/`.

## Metas

1. Home layout **C — dois modos lado a lado** (escolhido pelo dono):
   - Topo: saudação + relógio.
   - Esquerda "Zapear": bloco grande do YouTube, **foco inicial** ao abrir a home. Abre o YouTube
     (se o app TV aceitar, na aba Inscrições; senão a home do app).
   - Direita "Continuar série": um bloco por série (The Mentalist, House, Criminal Minds). Abre a
     página da série no Prime Video via deep link `https://`; se o deep link não funcionar nessa
     versão do app, abre o Prime na home dele.
   - Barra inferior: Netflix, Prime Video, Globoplay, YouTube Music, **Roku** (troca a TV para a
     entrada HDMI do Roku), **⋯** (grade com todos os apps instalados, incluindo Configurações).
   - Cartão **"Tocando agora"**: aparece quando há sessão de mídia ativa (MediaSession); mostra
     título, artista/app, capa e play/pause + próxima; clicar volta ao app. Some quando nada toca.
     Permissão de notification listener concedida via `adb shell cmd notification allow_listener`.
2. Launcher definido como HOME padrão; o botão Home do controle leva a ele.
3. **Debloat** com `pm disable-user --user 0` (nunca uninstall), lista proposta e aprovada pelo dono
   item a item; prioridade a liberar RAM.
4. Script de **restore** de um comando que reabilita tudo e devolve o launcher original.

## Não-metas

- Tocar vídeo dentro do launcher (DRM, e RAM insuficiente).
- Puxar inscrições do YouTube automaticamente (exigiria OAuth na conta Google).
- "Continuar assistindo" vindo de dentro do Prime/Netflix.
- AirPlay/Espelhamento Apple — investigação separada depois (ex.: AirScreen), fora deste escopo.

## Anti-metas (falha mesmo que "funcione")

- Launcher que deixa a TV mais lenta que antes: memória do processo em idle deve ficar bem abaixo
  do tvlauncher original (alvo < 60 MB PSS; medir com `dumpsys meminfo`).
- Navegação ruim no controle remoto (foco perdido, foco invisível, D-pad que "trava").
- Desativar algo que quebra Wi-Fi, Chromecast, controle remoto, entradas HDMI, Configurações,
  teclado, atualizações de segurança do sistema ou a Play Store.
- Qualquer desinstalação, root ou mudança irreversível.

## Restrições

- Nativo, leve: Kotlin + Views (ou Compose for TV só se o APK ficar pequeno e o PSS dentro do alvo).
  Partir do código do launcher de `github.com/Gathin23/cleanyourtv` é permitido e recomendado.
- minSdk 30 (Android 11), alvo landscape 1280×720.
- Build com JDK 17 em `/opt/homebrew/opt/openjdk@17` (Gradle wrapper do projeto). `adb` no PATH.
- Toda interação com a TV via `adb -s <IP_DA_TV>:5555`; verificar cada passo com screenshot
  (`adb shell screencap -p /sdcard/x.png && adb pull`). `exec-out screencap` não funciona nesta TV
  (o driver imprime lixo no stdout).
- Commit a cada marco verde no repo `tcl-tv`.

## Decisões do builder (calls do líder, ajustáveis)

- Visual: fundo escuro, tipografia grande legível a 3 m, foco = contorno branco + leve escala.
- Arte dos blocos de série: imagem estática empacotada no APK (poster), sem rede.
- Config (lista de séries com deep links, apps da barra, id da entrada HDMI) num JSON em assets,
  para trocar sem mexer em código.
- Saudação por horário ("Bom dia/Boa tarde/Boa noite").
