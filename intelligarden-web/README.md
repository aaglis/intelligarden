# IntelliGarden Web (interface 3D)

Nova interface do IntelliGarden: um "cantinho da janela" em 3D (three.js) com 6 plantas, sensor IG-01, ficha da planta, registro diário e irrigação simulada.

A página mostra os **dados reais do backend**: água estimada de cada planta, clima da última leitura, status online/offline do sensor e os registros. Ela recebe as mudanças na hora pelo SSE (leituras novas, água estimada, irrigações automáticas).

## Como abrir

**Pelo backend (o jeito normal, e o da apresentação):** com o backend no ar, abra `http://localhost:8080/`. De outro aparelho na mesma rede (celular, notebook do professor), use `http://IP-da-máquina:8080/` (o IP sai em `hostname -I`). O backend copia esta pasta no build; depois de mudar a página, reinicie o backend.

**Para mexer na página** (sem reiniciar o backend a cada mudança):

```bash
cd intelligarden-web
python3 -m http.server 5173
# abra http://localhost:5173 (a página procura o backend na porta 8080 do mesmo host)
```

Não precisa de internet: o three.js 0.160.0 e as fontes (Bricolage Grotesque, Geist e Geist Mono) estão em `vendor/`, com as licenças (MIT e SIL OFL 1.1). Abrir o arquivo direto (`file://`) não funciona, porque o navegador bloqueia os módulos locais; use um dos dois endereços acima.

Por padrão a página usa o backend do mesmo endereço (ou a porta 8080, quando aberta na 5173) e acompanha o sensor `esp32-jardim-bloco-a`. Parâmetros no endereço:

| Parâmetro | Exemplo | Para quê |
|---|---|---|
| `api` | `?api=http://192.168.0.10:8080` | backend em outro endereço (lembre de liberar a origem no CORS do backend) |
| `device` | `?device=esp32-jardim-bloco-b` | outro sensor |
| `demo` | `?demo` | força os dados de exemplo |

**Sem servidor:** se o backend não responder, a página mostra os dados de exemplo, escreve "dados de exemplo" no topo e continua tentando a cada 15 s. Quando o backend responde, ela passa para os dados reais sozinha. No modo de exemplo, registros e irrigações ficam só na página.

## O que vem do backend

- **Plantas:** só aparecem no 3D as plantas com modelo na maquete (`lirio`, `samambaia`, `manjericao`, `mandacaru`, `jiboia`, `espada`) e ligadas ao sensor. Uma planta da maquete que não existe no servidor some da sala; uma planta do servidor sem modelo fica fora do 3D (aparece no console).
- **Ficha:** água estimada, limite, previsão da próxima irrigação (se o clima continuar igual), clima x faixa ideal da espécie, ritmo de perda e os últimos registros.
- **Registrar:** pesagem (com a opção "vaso cheio", que vira a referência), rega e observação são gravadas na API. A prévia da pesagem compara a perda medida com a estimada antes de salvar. A foto da observação ainda não é guardada no servidor, então o campo some quando a página está ligada ao backend.
- **Simular irrigação:** grava a irrigação, enche a água estimada e acende o LED do ESP32 (1 s a cada 10 mL). Em vasos reais (`autoIrrigation: false`) o botão fica desativado: a equipe rega e registra.
- **Irrigação automática:** quando uma planta virtual chega no limite, o backend irriga sozinho; a página mostra a chuva no vaso, o aviso e o LED da maquete aceso pelo mesmo tempo do LED da placa.
- **Sensor:** temperatura, umidade, VPD, online/offline e o botão "Piscar LED", que manda o comando ao ESP32.

## Como usar

- **Maquete**: visão geral; também recentraliza a câmera.
- **Por dentro**: um ponto de vista dentro do cômodo; arraste para olhar em volta.
- Clique numa planta (ou na lista de baixo) para abrir a ficha; `‹ ›` ou `← →` trocam de planta; `Esc` volta.
- Modo escuro do sistema = noite.
