# IntelliGarden Web (protótipo 3D)

Protótipo da nova interface do IntelliGarden: um "cantinho da janela" em 3D (three.js) com 6 plantas, sensor IG-01, ficha da planta, registro diário e irrigação simulada.

> **Estado atual:** os dados das plantas (água estimada, eventos, próxima rega) ainda são **fictícios** e ficam no próprio `index.html`. A próxima etapa é ligar a página à API do backend.

## Como abrir

É um único arquivo, sem build. Precisa de internet (three.js e fontes vêm de CDN).

```bash
cd intelligarden-web
python3 -m http.server 5173
# abra http://localhost:5173
```

## Como usar

- **Maquete**: visão geral; também recentraliza a câmera.
- **Por dentro**: um ponto de vista dentro do cômodo; arraste para olhar em volta.
- Clique numa planta (ou na lista de baixo) para abrir a ficha; `‹ ›` ou `← →` trocam de planta; `Esc` volta.
- Modo escuro do sistema = noite.
