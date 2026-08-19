# Manual do Usuário

## Primeiro uso

1. Instale o APK **2x2 Wallet**
2. Toque em **Criar carteira**
3. Defina uma senha forte (mínimo 8 caracteres)
4. Guarde o backup WIF em local seguro

## Receber 2X2

1. Abra a aba **Receber**
2. Copie o endereço ou mostre o **QR code** para quem vai enviar
3. Envie 2X2 para esse endereço na rede 2x2Coin

## Enviar 2X2

1. Abra **Enviar**
2. Informe o endereço de destino manualmente ou toque em **Escanear QR**
3. Informe a quantidade e confirme — a transação é assinada no celular e transmitida pela API oficial

## Biometria

1. Desbloqueie a carteira com a senha
2. Em **Configurações**, ative **Desbloqueio biométrico**
3. Confirme a senha quando solicitado
4. Nas próximas aberturas, use o botão **Biometria** na tela de login

A sessão é bloqueada automaticamente quando o app vai para segundo plano.

## Restaurar carteira (WIF)

1. Na aba **Receber**, toque em **Restaurar WIF**
2. Cole a chave privada exportada anteriormente
3. A carteira passa a usar essa conta na sessão atual

## Múltiplos endereços

1. Abra **Carteiras**
2. Toque em **Gerar novo endereço**
3. Cada endereço pode receber um label interno

## Backup

1. Em **Carteiras**, toque em **Exportar WIF da conta ativa**
2. Armazene a WIF offline
3. Nunca compartilhe a WIF

## Segurança

- A carteira é **não-custodial**
- A equipe 2x2Coin **não** tem acesso às suas chaves
- Sem internet, você ainda pode ver endereços já gerados após desbloquear
- Perder senha + WIF = perda permanente dos fundos

## Suporte de rede

- API principal: `https://server.2x2coin.com`
- Explorer de fallback: `https://serverexplorer.2x2coin.com`

App developer (HTTPS + curl, no VPS): [APP_API.md](APP_API.md).
Server operator: [SERVER.md](SERVER.md).

Se a API estiver indisponível, o saldo pode demorar a atualizar até o fallback responder.
