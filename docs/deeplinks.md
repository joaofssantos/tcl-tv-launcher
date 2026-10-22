# Deep links verificados na TCL

Data do teste: 2026-10-22. Todos os comandos abaixo usaram `adb -s <IP_DA_TV>:5555`.

## Prime Video

Nesta versão (`com.amazon.amazonvideo.livingroom`), URLs `www.primevideo.com/detail/...`
**não** são resolvidas pelo app. A URL que o Android resolve para
`com.amazon.amazonvideo.livingroom/.IgnitionActivity` é a forma
`https://app.primevideo.com/detail?gti=...`.

| Série | URL configurada |
| --- | --- |
| The Mentalist | `https://app.primevideo.com/detail?gti=0MZ27X55B8J5R468ZCH5PZ45FP` |
| House | `https://app.primevideo.com/detail?gti=0J04VNQF9MMAGLO7BKVUF6EM9X` |
| Criminal Minds | `https://app.primevideo.com/detail?gti=0OH76CLO9I7QI0T14VZZ0P1KGP` |

`am start -a android.intent.action.VIEW -d <url>` iniciou `IgnitionActivity` para
The Mentalist e entregou as URLs seguintes a `MainActivity` do Prime. A primeira
execução exibiu o seletor de perfil; esse seletor absorve o link pendente, portanto
a confirmação visual da página de cada título fica para depois de escolher o perfil.
`SeriesLauncher` mantém o fallback para a home do Prime se o intent falhar.

## YouTube TV

`am start -a android.intent.action.VIEW -p com.google.android.youtube.tv -d
https://www.youtube.com/feed/subscriptions` iniciou
`com.google.android.youtube.tv/...MainActivity`. A captura de tela ADB não mostrou
a interface nessa execução, então não foi possível confirmar visualmente que a aba
Inscrições foi selecionada. O launcher tenta essa URL e volta à home do YouTube se
ela não for aceita.

## Roku / HDMI 1

O input confirmado pelo `dumpsys tv_input` é:

```
com.tcl.tvinput/.passthroughinput.TvPassThroughService/HW1413744128
```

Ele informa `hdmi_port=1` e estado conectado. Abrir
`content://android.media.tv/passthrough/<input-id codificado>` com `ACTION_VIEW`
trouxe `com.tcl.tv/.TVActivity` para frente. `TvInputSwitcher` usa
`TvContract.buildChannelUriForPassthroughInput` para construir exatamente essa URI.
