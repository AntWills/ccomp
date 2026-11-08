# Fusos horários de eventos

## Contrato

Cada evento tem um fuso horário IANA, salvo no campo `time_zone`. O padrão configurado para registros sem um fuso
é `America/Sao_Paulo`. A API exige que a criação informe `time_zone` explicitamente; o cliente pode escolher essa
zona padrão ou outra, como `America/Manaus`, `America/Rio_Branco` ou `America/Noronha`.

Os campos `start_date`, `end_date`, `enrollment_start_date` e `enrollment_end_date` são horários locais,
representados sem offset. Eles devem ser interpretados no `time_zone` do evento. Por exemplo, `2026-11-08T09:05`
com `time_zone: "America/Fortaleza"` significa 09:05 no horário local de Fortaleza, não 09:05 no fuso do servidor
ou do navegador.

As datas das atividades também são horários locais e herdam o fuso do evento pai. As respostas das APIs de evento
e atividade incluem `time_zone` para que clientes possam apresentar os horários corretamente.

## Regras temporais

O backend usa o instante atual em UTC e o converte para o fuso do evento antes de avaliar publicação, execução,
período de inscrições e check-in de atividades. Assim, a configuração de fuso da JVM e a localização do servidor
não alteram o significado dos horários cadastrados.

O limite final é exclusivo: no instante exato de `end_date`, o evento é considerado finalizado e o período de
inscrições é fechado. Antes desse instante, o evento ainda está em andamento e as inscrições podem permanecer abertas.

## Criar um evento

Envie `time_zone` usando um identificador IANA. O nome do campo JSON segue o padrão snake_case da API:

```json
{
  "title": "Encontro de Computação",
  "category": "ACADEMIC_EDUCATIONAL",
  "format": "IN_PERSON",
  "time_zone": "America/Manaus",
  "start_date": "2026-11-08T09:05:00",
  "end_date": "2026-11-08T12:00:00"
}
```

O fuso é obrigatório na criação, inclusive para eventos online. Para eventos online, escolha e informe explicitamente
o fuso que será usado como referência de agenda. Para eventos presenciais e híbridos, use o fuso do local do evento.

## Atualizar um evento

`time_zone` é opcional no `PATCH`. Se for omitido ou enviado como `null`, o fuso atual permanece. Se o fuso for
alterado, os horários locais existentes passam a ser interpretados no novo fuso; o backend não soma nem subtrai
horas dos campos de data automaticamente. Atualize as datas no mesmo pedido se a mudança de fuso exigir novos
horários locais.

O fuso precisa ser um ID IANA válido. Abreviações como `BRT`, `EST` ou nomes de cidades sem região, como
`Fortaleza`, não são aceitos.

## Eventos existentes

A migração inicial atribui `America/Sao_Paulo` aos eventos existentes porque os registros antigos não guardavam
um fuso e não há informação suficiente para inferi-lo com segurança. Confirme os eventos realizados em outras
regiões e atualize `time_zone` para o fuso correto.

## Exemplos de fusos brasileiros

Use a zona correspondente ao local do evento. Exemplos de IDs IANA brasileiros:

- `America/Noronha` — Fernando de Noronha e ilhas oceânicas (UTC−2).
- `America/Sao_Paulo` — horário de Brasília e maior parte do Brasil (UTC−3).
- `America/Manaus` — exemplo de localidades na região UTC−4.
- `America/Rio_Branco` — Acre e parte do Amazonas (UTC−5).

As regras civis podem mudar. Use os IDs IANA em vez de offsets fixos como `-03:00`, pois a base de fusos do Java
aplica as regras correspondentes à data do evento.

Para conferir a referência oficial de hora legal por estado e ilhas, consulte o
[Observatório Nacional](https://www.gov.br/observatorio/pt-br/assuntos/areas-de-atuacao/tempo-e-frequencia/brasil).
