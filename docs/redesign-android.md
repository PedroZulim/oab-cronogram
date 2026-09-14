# Redesign nativo do OAB · 120 dias

Referência aprovada: https://lovable.dev/projects/b6f575ce-3cbc-4b55-9385-3fe36f6b1e8c

O APK implementa o visual com Views Android em Java. O protótipo web não é carregado no aplicativo e não é necessário acessar a internet.

## Interface

- Fundo #F7F6FC, cards brancos, roxo #6D4BD1, destaque em gradiente e verde para conclusão.
- Navegação inferior com ícones vetoriais e estado selecionado.
- Início com progresso, estudo do dia, faixa semanal, pendências e lembretes reais.
- Plano com seleção de semana, busca global e filtro de pendências.
- Estudo com cards para checklist, desempenho e anotações; conclusão manual reversível.
- Roteiro em tela própria, acessível pelo resumo do estudo: índice com atalhos aos temas, cartões por etapa e referências/histórico recolhíveis. O botão Voltar retorna ao estudo; a tela de roteiro é mantida ao recriar a atividade.
- A apresentação elimina cabeçalhos de página e campos vazios do PDF, recompõe linhas e recupera continuações antes do cabeçalho do dia seguinte. O texto original permanece consultável e o arquivo de dados é preservado.
- Progresso com indicadores e barras semanais; ajustes agrupados.

## Compatibilidade dos dados

O arquivo schedule.json, o nome oab120 de SharedPreferences e todas as chaves day.N.*, start, extension e reminder.* são preservados. Nenhuma migração ou limpeza é executada. Para atualizar um APK instalado sem perder dados, instale por cima com a mesma assinatura; não desinstale a versão anterior.

## Validação manual no emulador

1. Instale a versão anterior, marque uma etapa, registre acertos/erros, uma anotação e um horário de lembrete. Atualize com a mesma assinatura e confirme a permanência dos dados.
2. Abra as quatro abas, o roteiro detalhado e o próximo dia. Use Voltar e gire a tela.
3. Pesquise “1”, “Dia 1”, “DIA 01”, um tema sem acento e um termo inexistente. Com busca preenchida, a consulta abrange todas as semanas; ao limpar, a semana selecionada volta a valer.
4. Conclua e reabra um dia, confira o progresso e os lembretes. Etapas do checklist não concluem automaticamente o dia.
5. Altere a data inicial, ative/desative a extensão e confira os limites 120/126 e a semana parcial.
6. Confira navegação e campos em 360dp de largura, com teclado aberto e fonte ampliada; a faixa semanal pode rolar horizontalmente.
7. Confira as permissões e entrega dos lembretes em aparelho. A interface usa o agendamento nativo existente.

A compilação e o lint são executados pelo workflow Android APK do PR. A inspeção visual no emulador continua necessária.
