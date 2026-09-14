# OAB · 120 dias — Android

Primeira versão de um aplicativo Android nativo, em Java, para acompanhar o cronograma de estudos fornecido. Funciona offline, sem login, serviços pagos ou dependências de interface externas. Android 8.0 ou superior.

## O que está implementado

- Hoje: conteúdo do dia e progresso geral.
- Plano: navegação por semana, busca por tema ou número do dia e filtro de pendências.
- Dia: resumo, consulta ao roteiro detalhado disponível, checklist de teoria/revisão, questões, lei seca e erros.
- Registro de acertos, erros, anotações e conclusão por dia.
- Progresso: percentual de acertos, pendências anteriores e conclusão semanal.
- Ajustes: data inicial e extensão opcional até 126 dias.
- Persistência local automática com SharedPreferences; alterar datas não apaga os registros.

## Conteúdo e decisões

Fonte: calendário e PDFs semanais do Método VDE, edição OAB 46, fornecidos pelo usuário. O app não confirma datas oficiais de exames nem atualiza a legislação do material.

1. O calendário chamado “120 dias” contém **126 dias**. Os primeiros 120 formam o plano principal; os dias 121–126 são uma extensão opcional. A posição correspondente ao dia 123 está grafada “DIA 13” no calendário; o importador usa a posição da célula, preservando a sequência.
2. Foram disponibilizadas as semanas 1–10. Há detalhes dos dias 1–70 e do recesso 71–73, descrito na semana 10. Os dias 74–126 têm apenas o resumo do calendário, identificado no app.
3. A semana 5 tem “180 dias” no cabeçalho; seu conteúdo e numeração dos dias foram preservados.
4. As pausas de Natal, fim de ano e Carnaval mantêm sua posição no plano ao mudar a data inicial. Não são feriados calculados para a nova data.
5. O checklist é um recurso do aplicativo. A conclusão do dia é manual e independente das quatro etapas, pois há revisões, pausas e simulados.
6. Os roteiros são extrações textuais dos PDFs, não uma reprodução de sua diagramação. Referências aos livros/plataforma não incluem esses materiais nem um banco de questões.
7. O código está sob a licença do repositório. A licença do código não se estende ao material de terceiros presente em `schedule.json`.

## Gerar o APK no GitHub

O workflow `.github/workflows/android.yml` roda testes de dados, `assembleDebug` e `lintDebug`, e disponibiliza `oab-120-android-debug` como artefato. Em **Actions → Android APK**, abra a execução concluída e baixe o artefato. Descompacte e instale `app-debug.apk` no Android. É uma versão de teste, não um pacote de produção para a Play Store.

## Compilar localmente

Instale JDK 17, Android SDK 35 e Gradle 8.11.1. O projeto usa Android Gradle Plugin 8.9.2. Se usar Android Studio, configure o Gradle local ou gere o wrapper antes de importar:

```sh
gradle wrapper --gradle-version 8.11.1
```

Configure `ANDROID_HOME` para o SDK ou crie `local.properties` com `sdk.dir` apontando para sua instalação. Depois:

```sh
gradle assembleDebug lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Compatibilidade: https://developer.android.com/build/releases/agp-8-9-0-release-notes

## Reimportar os PDFs

```sh
python -m pip install PyMuPDF
python scripts/import_schedule.py /caminho/para/pdfs
python -m unittest discover -s tests
```

O importador usa coordenadas das células para evitar a ordem incorreta da extração textual do calendário. Ele foi feito para o layout dos arquivos fornecidos, não para PDFs arbitrários.

## Validação desta entrega

- Cinco testes de integridade de dados passaram: sequência, extensão, associação de células a dias, cobertura dos detalhes, pausas e simulados.
- Conferência visual de páginas do calendário para a associação espacial dos conteúdos.
- Compilação Android, lint e teste em aparelho **não executados no ambiente de criação**, que não dispõe do SDK/JDK de desenvolvimento e não conseguiu acessar o download do SDK. O workflow foi preparado para essas verificações de compilação e lint; seu resultado precisa ser conferido.

## Limitações da primeira versão

Ainda não há notificações, timer, backup/exportação, sincronização ou banco de questões. Desinstalar o aplicativo apaga o progresso. Os roteiros extensos usam uma visualização textual; uma próxima melhoria é separar metas e referências por tema. Testar em aparelho antes do uso diário, especialmente teclado, botão Voltar, fontes ampliadas e retomada do app.
